/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package test.simple;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cloudypeer.cloud.CloudException;
import cloudypeer.cloud.CloudMetadata;
import cloudypeer.cloud.CloudObject;
import cloudypeer.cloud.StorageCloud;
import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryMetadata;
import cloudypeer.store.simple.SimpleStoreEntry;
import cloudypeer.store.simple.SimpleStoreEntryMetadata;
import cloudypeer.store.simple.StoreEntryPersistenceHandler;
import org.apache.log4j.Logger;
import java.util.Map;

/**
 * Simple implementation of cloud persistence.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class CloudPersistenceHandler implements StoreEntryPersistenceHandler {

    static Logger logger = Logger.getLogger(StoreEntryPersistenceHandler.class);

  /* *********************************************************************
   * Utility classes
   ***********************************************************************/

  /**
   * Cointainer for cached entry/metadata
   */
  private class CachedData<E> {

    private E data;
    private long refreshTimestamp;

    public CachedData(E data) {
      this.data = data;
      this.refreshTimestamp = System.currentTimeMillis();
    }

    public void refresh(E data) {
      this.data = data;
      this.refreshTimestamp = System.currentTimeMillis();
    }

    public E getData() {
      return data;
    }

    public long getTimestamp() {
      return refreshTimestamp;
    }

    public boolean needsRefresh(long refreshThreshold) {
      return (System.currentTimeMillis() - refreshTimestamp) >= refreshThreshold;
    }
  }

  /* *********************************************************************
   * Instance variables
   ***********************************************************************/

  /**
   * Instance of StorageCloud backing this cloud store
   */
  private StorageCloud storageCloud;

  /**
   * Cache of the keys on the cloud
   */
  private Set<String> keysCache = new HashSet<String>();

  /**
   * Last keys cache refresh timestamp
   */
  private long keysCacheLastRefresh = 0;

  /**
   * Treshold after which refresh the keys cache
   */
  private long keysRefreshThreshold;

  /**
   * Cache of cloud metadata
   */
  private HashMap<String, CachedData<StoreEntryMetadata>> metadataCache =
    new HashMap<String, CachedData<StoreEntryMetadata>>();

  /**
   * Treshold after which refresh metadata
   */
  private long metadataRefreshThreshold;


  /* *********************************************************************
   * Constructor
   ***********************************************************************/
  /**
   * Initialize a CloudPersistenceHandler instance.
   *
   * @param cloudProvider Provider to be used for cloud access
   * @param cloudURI URI of the cloud instance to use
   * @param keysRefreshThresholdSec How much time before refreshing cached keys
   * @param metadataRefreshThresholdSec How much time before refreshing cached metadata
   * @param listThresholdSec How much going back in hystory when listing
   */
  public CloudPersistenceHandler(StorageCloud cloud, long keysRefreshThresholdSec,
                                 long metadataRefreshThresholdSec)
  {
    this.storageCloud = cloud;
    this.keysRefreshThreshold = keysRefreshThresholdSec * 1000;
    this.metadataRefreshThreshold = metadataRefreshThresholdSec * 1000;
  }


  /* *********************************************************************
   * Utils methods
   ***********************************************************************/
  /**
   * Checks if the keys cache is up to date and otherwise updates it
   */
  private void checkKeysCache() {
    if ((System.currentTimeMillis() - keysCacheLastRefresh) > keysRefreshThreshold) {
      logger.trace("Time to update keys cache");
      String currentKeys[];

      try {
        currentKeys = storageCloud.list(null);
        logger.trace(String.format("There are %d keys on the cloud", currentKeys.length));
        keysCache.clear();
        for (String key: currentKeys) {
          if (key != null) keysCache.add(key);
        }

        keysCacheLastRefresh = System.currentTimeMillis();
      } catch (IOException e)  {
        logger.warn("Input/Output error updating cloud key cache", e);
      } catch (CloudException e) {
        logger.warn("Error updating cloud key cache", e);
      }
    }
  }

  private CachedData<StoreEntryMetadata> addMetadataToCache(String key, StoreEntryMetadata newMeta)
  {
    CachedData<StoreEntryMetadata> cachedMeta = metadataCache.get(key);
    if (cachedMeta == null) {
      cachedMeta = new CachedData<StoreEntryMetadata>(newMeta);
      synchronized (metadataCache) {
        metadataCache.put(key, cachedMeta);
      }
    } else {
      cachedMeta.refresh(newMeta);
    }

    synchronized (keysCache) {
      keysCache.add(key);
    }

    return cachedMeta;
  }

  private void removeMetadataFromCache(String key) {
    synchronized (metadataCache) {
      metadataCache.remove(key);
    }

    synchronized (keysCache) {
      keysCache.remove(key);
    }
  }

  /* *********************************************************************
   * Implementation of StoreEntryPersistenceHandler interface
   ***********************************************************************/
  public boolean contains(String key) throws RuntimeException {
    checkKeysCache();
    return keysCache.contains(key);
  }

  public void write(String key, InputStream in, String contentType,
                    Map<String,String> userMetadata) throws RuntimeException
  {
    try {
      storageCloud.put(key, contentType, in, userMetadata);
    } catch (IOException e)  {
      throw new RuntimeException("Input/Output error putting cloud entry: " + key, e);
    } catch (CloudException e) {
      throw new RuntimeException("Error putting entry: " + key, e);
    }
  }


  public void write(StoreEntry entry) throws RuntimeException {
    StoreEntryMetadata meta = entry.getMetadata();
    write(entry.getKey(), entry.getInputStream(), meta.getContentType(), meta.getUserMetadata());
    addMetadataToCache(entry.getKey(), meta);
  }

  public void writeMetadata(String key, StoreEntryMetadata meta) throws RuntimeException {
    try {
      storageCloud.putMetadata(key, meta.getContentType(), meta.getUserMetadata());

      addMetadataToCache(key, meta);
    } catch (IOException e)  {
      throw new RuntimeException("Input/Output error putting cloud entry metadata: " + key, e);
    } catch (CloudException e) {
      throw new RuntimeException("Error putting entry metadata: " + key, e);
    }
  }

  public StoreEntry read(String key) throws RuntimeException {
    CloudObject cloudObject;
    CloudMetadata cloudMeta;
    try {
      cloudObject = storageCloud.get(key);
    } catch (IOException e)  {
      throw new RuntimeException("Input/Output error retrieving entry from cloud: " + key , e);
    }  catch (CloudException e) {
      throw new RuntimeException("Error retrieving entry from cloud: " + key, e);
    }

    StoreEntryMetadata metadata;
    HashMap<String, String> userMeta;

    cloudMeta = cloudObject.getMetadata();
    userMeta = new HashMap<String,String>(cloudMeta.getUserMetadata());
    metadata = new SimpleStoreEntryMetadata(cloudMeta.getLastModified(),
                                           cloudMeta.getContentLength(),
                                           cloudMeta.getContentMD5(),
                                           cloudMeta.getContentType(),
                                           userMeta);
    addMetadataToCache(key, metadata);
    return new SimpleStoreEntry(key, cloudObject.getInputStream(), metadata);
  }



  public StoreEntryMetadata readMetadata(String key) throws RuntimeException {
    CachedData<StoreEntryMetadata> cachedMeta = metadataCache.get(key);

    if (cachedMeta == null || cachedMeta.needsRefresh(metadataRefreshThreshold)) {
      logger.trace("Refreshing metadata for key " + key);
      HashMap<String, String> userMeta;
      StoreEntryMetadata newMeta;
      CloudMetadata cloudMeta;

      try {
        cloudMeta = storageCloud.getMetadata(key);
      } catch (IOException e)  {
        throw new RuntimeException("Input/Output error retrieving metadata from cloud: " + key , e);
      } catch (CloudException e) {
        throw new RuntimeException("Error retrieving metadata from cloud:" + key, e);
      }

      if (cloudMeta == null) {
        removeMetadataFromCache(key);
        return null;
      }

      userMeta = new HashMap<String,String>(cloudMeta.getUserMetadata());
      newMeta = new SimpleStoreEntryMetadata(cloudMeta.getLastModified(),
                                             cloudMeta.getContentLength(),
                                             cloudMeta.getContentMD5(),
                                             cloudMeta.getContentType(),
                                             userMeta);
      cachedMeta = addMetadataToCache(key, newMeta);
    } else {
      logger.trace("Using cached metadata for key " + key);
    }

    return cachedMeta.getData();
  }

  public String[] listEntries(Date timestamp) throws RuntimeException {
    try {
      return storageCloud.list(timestamp);

    } catch (IOException e)  {
      throw new RuntimeException("Input/Output error listing cloud entries", e);
    } catch (CloudException e) {
      throw new RuntimeException("Error listing cloud entries", e);
    }
  }
}
