/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.persistence;

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
import java.util.ArrayList;

/**
 * Simple implementation of cloud persistence.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class BasicCloudPersistenceHandler implements StoreEntryPersistenceHandler {

  static Logger logger = Logger.getLogger(StoreEntryPersistenceHandler.class);

  public static int DEFAULT_KEY_REFRESH_THRESHOLD = 10;
  public static int DEFAULT_METADATA_REFRESH_THRESHOLD = 10;


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
   * Key prefix to prepend to entries
   */
  private String baseKey;

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
  private int keysRefreshThreshold = DEFAULT_KEY_REFRESH_THRESHOLD * 1000;

  /**
   * Cache of cloud metadata
   */
  private HashMap<String, CachedData<StoreEntryMetadata>> metadataCache =
    new HashMap<String, CachedData<StoreEntryMetadata>>();

  /**
   * Treshold after which refresh metadata
   */
  private int metadataRefreshThreshold = DEFAULT_METADATA_REFRESH_THRESHOLD * 1000;


  /* *********************************************************************
   * Constructor
   ***********************************************************************/
  /**
   * Initialize a BasicCloudPersistenceHandler instance.
   *
   * @param storageCloud Storage cloud implementation to use
   */
  public BasicCloudPersistenceHandler(StorageCloud cloud, String baseKey) {
    this.storageCloud = cloud;
    this.baseKey = baseKey;
    if (this.baseKey == null) this.baseKey = "";

    logger.trace(String.format("Configured with baseKey '%s' for cloud '%s'",
                               this.baseKey, cloud.getCloudURI()));
  }

  /* *********************************************************************
   * Getters/Setters
   ***********************************************************************/

  /**
   * Returns the amount of time after which refresh the list of entry key currently on the cloud.
   *
   * @return Key refresh threshold in seconds
   */
  public int getKeysRefreshThreshold() {
    return keysRefreshThreshold / 1000;
  }

  /**
   * Sets the amount of time after which refresh the list of entry key currently on the cloud.
   *
   * @param threshold Keys refresh threshold in seconds
   */
  public void setKeysRefreshThreshold(int threshold) {
    this.keysRefreshThreshold = threshold;
  }


  /**
   * Returns the amount of time after which refresh a cached metadata
   *
   * @return Metadta refresh threshold in seconds
   */
  public int getMetadataRefreshThreshold() {
    return metadataRefreshThreshold / 1000;
  }

  /**
   * Sets the amount of time after which refresh a cached metadata
   *
   * @param threshold Metadata refresh threshold in seconds
   */
  public void setMetadataRefreshThreshold(int threshold) {
    this.metadataRefreshThreshold = threshold;
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
        currentKeys = storageCloud.list(null, baseKey);
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
      storageCloud.put(baseKey + key, contentType, in, userMetadata);
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
      storageCloud.putMetadata(baseKey + key, meta.getContentType(), meta.getUserMetadata());

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
      cloudObject = storageCloud.get(baseKey + key);
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
        cloudMeta = storageCloud.getMetadata(baseKey + key);
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
      ArrayList<String> normalizedKeys;
      String[] cloudKeys =  storageCloud.list(timestamp, baseKey);

      if (baseKey.equals("")) return cloudKeys;

      normalizedKeys = new ArrayList<String>();
      for (String k: cloudKeys) {
        if (k.startsWith(baseKey)) {
          normalizedKeys.add(k.substring(baseKey.length()));
        }
      }

      return normalizedKeys.toArray(new String[normalizedKeys.size()]);
    } catch (IOException e)  {
      throw new RuntimeException("Input/Output error listing cloud entries", e);
    } catch (CloudException e) {
      throw new RuntimeException("Error listing cloud entries", e);
    }
  }
}
