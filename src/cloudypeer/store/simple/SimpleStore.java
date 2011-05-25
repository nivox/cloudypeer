/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.simple;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cloudypeer.store.Store;
import cloudypeer.store.StoreCompareResult;
import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryDiff;
import cloudypeer.store.StoreEntryDiffData;
import cloudypeer.store.StoreEntryMetadata;
import cloudypeer.store.StoreException;
import cloudypeer.store.StoreUpdateHandler;
import org.apache.log4j.Logger;

/**
 * Simple store implementation base class.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleStore implements Store {

  static Logger logger = Logger.getLogger(SimpleStore.class);

  private static final int DEFAULT_LIST_THRESHOLD = 3600 * 24;

  /* *********************************************************************
   * Instance variables
   ***********************************************************************/

  /**
   * Handler managing the persistence of the StoreEntries
   */
  private StoreEntryPersistenceHandler persistenceHandler;

  /**
   * StoreEntry manipulation handler
   */
  protected StoreEntryDiffHandler diffHandler;

  /**
   * Update handler list for this instance
   */
  private List<StoreUpdateHandler> handlers = new ArrayList<StoreUpdateHandler>();

  /**
   * Define the windows of active entries: i.e. from now() - listThreshold to now()
   */
  private long listThreshold = DEFAULT_LIST_THRESHOLD * 1000;

  /* *********************************************************************
   * Constructors
   ***********************************************************************/

  /**
   * Default constructor
   *
   * @param persistenceHandler Handler responsible of the entry persistence
   * @param diffHandler Handler responsible of the entry diffing/patching
   */
  public SimpleStore(StoreEntryPersistenceHandler persistenceHandler,
                            StoreEntryDiffHandler diffHandler)
  {
    if (diffHandler == null) throw new IllegalArgumentException("Null diff handler");
    if (persistenceHandler == null) throw new IllegalArgumentException("Null persistence handler");
    this.diffHandler = diffHandler;
    this.persistenceHandler = persistenceHandler;
  }

  /* *********************************************************************
   * Getters/Setters
   ***********************************************************************/

  /**
   * Returns the threshold used to filter active entries. <br>
   * Entries are considered active if they're modified date is included in the time window:
   * now() - listThreshold : now(). Set this parameter to 0 to consider all entries as active.<br>
   * The default value is {@value DEFAULT_LIST_THRESHOLD} seconds.
   *
   * @return List threshold
   */
  public long getListThreshold() {
    return listThreshold / 1000;
  }

  /**
   * Sets the list threshold
   *
   * @param threshold List threshold.
   */
  public void setListThreshold(long threshold) {
    if (threshold < 0) threshold = 0;
    this.listThreshold = threshold * 1000;
  }

  /* *********************************************************************
   * Utils methods
   ***********************************************************************/

  /**
   * Notifies all the update handlers associated to this instance
   *
   * @param keys The key that were updated
   */
  protected void notifyUpdates(String keys[]) {
    if (keys == null || keys.length == 0) {
      logger.warn("Trying to notify null or empty key list");
      return;
    }

    logger.info(String.format("Notifying handlers of keys update (%d)", keys.length));
    synchronized (handlers) {
      for (StoreUpdateHandler h: handlers) {
        h.notifyUpdate(keys, this);
      }
    }
  }

  /* *********************************************************************
   * Implementation of Store common methods
   ***********************************************************************/

  /*
   * Implementation of Store.addUpdateHandler(handler)
   */
  public void addUpdateHandler(StoreUpdateHandler handler) {
    synchronized (handlers) {
      handlers.add(handler);
    }
  }

  /*
   * Implementation of Store.removeUpdateHandler(handler)
   */
  public void removeUpdateHandler(StoreUpdateHandler handler) {
    synchronized (handlers) {
      Iterator<StoreUpdateHandler> i = handlers.iterator();
      while(i.hasNext()) {
        StoreUpdateHandler h = i.next();
        if (h.equals(handler)) {
          i.remove();
          return;
        }
      }
    }
  }

  /*
   * Implementation of Store.listStoreEntries()
   */
  public String[] listStoreEntries() {
    if (listThreshold > 0) {
      return listStoreEntries(new Date(System.currentTimeMillis() - listThreshold));
    } else {
      return listStoreEntries(null);
    }
  }

  /*
   * Implementation of Store.getStoreEntriesMetadata()
   */
  public HashMap getStoreEntriesMetadata() throws StoreException {
    String[] keys = listStoreEntries();
    logger.trace("Getting metadata for " + keys.length + " keys");
    return getStoreEntriesMetadata(keys);
  }

  /*
   * Implementation of Store.getStoreEntriesMetadata(keys)
   */
  public HashMap<String, StoreEntryMetadata> getStoreEntriesMetadata(String keys[]) {
    HashMap<String, StoreEntryMetadata> metaMap = new HashMap<String, StoreEntryMetadata>();

    StoreEntryMetadata meta;
    for (String key: keys) {
      try {
        meta = getStoreEntryMetadata(key);
        if (meta != null) metaMap.put(key, meta);
      } catch (Exception e) {
        logger.warn("Error retrieving metadata for key: " + key, e);
      }
    }

    return metaMap;
  }

  /*
   * Implementation of Store.updateMetadas(metadatas)
   */
  public void updateMetadatas(Map<String, StoreEntryMetadata> metadatas) {
    for (String key: metadatas.keySet()) {
      try {
        updateMetadata(key, metadatas.get(key));
      } catch (IllegalArgumentException e) {
        logger.info(String.format("Error updating metadata for key %s: skipping", key));
      }
    }
  }

  /*
   * Implementation of Store.updateMetada(key, metadata)
   */
  public void updateMetadata(String key, StoreEntryMetadata meta) {
    StoreEntryMetadata oldMeta = getStoreEntryMetadata(key);
    if (oldMeta == null)
      throw new IllegalArgumentException("No entry for the specified key: " + key);

    StoreEntryMetadata newMeta;
    HashMap<String, String> userMetadata;

    userMetadata = new HashMap<String, String>(meta.getUserMetadata());

    newMeta = new SimpleStoreEntryMetadata(meta.getModifiedTimestamp(),
                                           oldMeta.getContentLength(),
                                           oldMeta.getContentMD5(),
                                           meta.getContentType(),
                                           userMetadata);
    putStoreEntryMetadata(key, newMeta);
    logger.info("Updating metadata for key: " + key);
    boolean update = false;
    if (!newMeta.getContentType().equals(oldMeta.getContentType())) {
      logger.trace("Changed metadata (content type): notifying handlers");
      update = true;
    } else if (!newMeta.getUserMetadata().equals(oldMeta.getUserMetadata())) {
      logger.trace("Changed metadata (user meta): notifying handlers");
      update = true;
    }

    if (update) notifyUpdates(new String[] {key});
  }

  /*
   * Implementation of Store.getStoreEntries()
   */
  public StoreEntry[] getStoreEntries() {
    String[] keys = listStoreEntries();

    return getStoreEntries(keys);
  }

  /*
   * Implementation of Store.getStoreEntries(keys)
   */
  public StoreEntry[] getStoreEntries(String[] keys) {
    ArrayList<StoreEntry> dump = new ArrayList<StoreEntry>(keys.length);

    for (String key: keys) {
      try {
        dump.add(getStoreEntry(key));
      } catch (Exception e) {
        logger.warn("Error retrieving metadata for key: " + key, e);
      }
    }

    return dump.toArray(new StoreEntry[dump.size()]);
  }

  /*
   * Implementation of Store.putStoreEntry(key, ...)
   */
  public void putStoreEntry(String key, InputStream in, String contentType,
                            HashMap<String,String> userMetadata)
    throws IllegalStateException
  {
    try {
      persistenceHandler.write(key, in, contentType, userMetadata);
    } catch (Exception e) {
      throw new StoreException("Error putting entry " + key, e);
    }

    notifyUpdates(new String[]{key});
  }


  /*
   * Implementation of Store.updateStoreEntries(entries)
   */
  public void updateStoreEntries(StoreEntry[] entries)  {
    StoreEntryMetadata localMeta;
    StoreEntryMetadata newMeta;


    List<String> keys = new ArrayList<String>();
    for (StoreEntry e: entries) {
      try {
        localMeta = getStoreEntryMetadata(e.getKey());
      } catch (Exception ex) {
        logger.warn("Error retrieving metadata: skip update for " + e.getKey(), ex);
        continue;
      }
      newMeta = e.getMetadata();

      long timestamp = 0;
      String md5 = null;
      if (localMeta != null) {
        timestamp = localMeta.getModifiedTimestamp().getTime();
        md5 = localMeta.getContentMD5();
      }


      if (timestamp < newMeta.getModifiedTimestamp().getTime()) {
        try {
          logger.info("Putting entry " + e.getKey());
          putStoreEntry(e);
          keys.add(e.getKey());
        } catch (Exception ex) {
        logger.warn("Error updating entry. Skipping " + e.getKey(), ex);
        continue;
        }
      }
    }

    if (keys.size() > 0) notifyUpdates(keys.toArray(new String[keys.size()]));
  }

  /*
   * Implementation of Store.listStoreEntries()
   */
  public String[] listStoreEntries(Date timestamp) throws StoreException {
    try {
      return persistenceHandler.listEntries(timestamp);
    } catch (RuntimeException e) {
      throw new StoreException("Error listing entries", e);
    }
  }

  /*
   * Implementation of Store.getStoreEntryMetadata(key)
   */
  public StoreEntryMetadata getStoreEntryMetadata(String key) {
    try {
      return persistenceHandler.readMetadata(key);
    } catch (RuntimeException e) {
      throw new StoreException("Error listing entries", e);
    }
  }

  /*
   * Implementation of Store.getStoreEntry(key)
   */
  public StoreEntry getStoreEntry(String key) {
    try {
      return persistenceHandler.read(key);
    } catch (RuntimeException e) {
      throw new StoreException("Error listing entries", e);
    }
  }

  /*
   * Implementation of Store.containsStoreEntry(key)
   */
  public boolean containsStoreEntry(String key) {
    try {
      return persistenceHandler.contains(key);
    } catch (RuntimeException e) {
      throw new StoreException("Error listing entries", e);
    }
  }

  /*
   * Implementation of Store.updateUserMetada(key, userMetadata)
   */
  public void updateUserMetadata(String key, HashMap<String, String> userMetadata)
    throws StoreException
  {
    StoreEntryMetadata oldMeta;
    StoreEntryMetadata newMeta;

    try {
      oldMeta = persistenceHandler.readMetadata(key);
    } catch (RuntimeException e) {
      throw new StoreException("Error reading current metadata for key " + key, e);
    }

    if (oldMeta == null)
      throw new IllegalArgumentException("No entry for the specified key");


    newMeta = new SimpleStoreEntryMetadata(oldMeta.getModifiedTimestamp(),
                                           oldMeta.getContentLength(),
                                           oldMeta.getContentMD5(),
                                           oldMeta.getContentType(),
                                           userMetadata);

    try {
      persistenceHandler.writeMetadata(key, newMeta);
    } catch (RuntimeException e) {
      throw new StoreException("Error updating metadata for key " + key, e);
    }
  }

  /*
   * Implementation of Store.compareStoreEntries(remoteEntries)
   */
  public StoreCompareResult compareStoreEntries(StoreEntry[] remoteEntries) {
    HashMap<String, StoreEntryMetadata> remoteMeta = new HashMap<String, StoreEntryMetadata>();

    for (StoreEntry e: remoteEntries) remoteMeta.put(e.getKey(), e.getMetadata());

    return compareStoreEntries(remoteMeta);
  }

  /*
   * Implementation Store.compareStoreEntries(remoteMeta)
   */
  public StoreCompareResult compareStoreEntries(HashMap<String, StoreEntryMetadata> remoteMetadata) {
    List<String> keysFresherOnRemote = new ArrayList<String>();
    List<String> keysFresherOnLocal = new ArrayList<String>();
    List<String> metaChangedOnRemote = new ArrayList<String>();
    List<String> metaChangedOnLocal = new ArrayList<String>();
    Set<String> remoteKeys = remoteMetadata.keySet();

    StoreEntryMetadata local;
    StoreEntryMetadata remote;
    for (String key: remoteKeys) {
      try {
        local = getStoreEntryMetadata(key);
      } catch (Exception e) {
        logger.warn("Error comparing entry. Skipping " + key, e);
        continue;
      }

      if (local == null) {
        /* Unknown key... it must be new */
        keysFresherOnRemote.add(key);
        continue;
      }

      remote = remoteMetadata.get(key);

      long localTimestamp = local.getModifiedTimestamp().getTime();
      long remoteTimestamp = remote.getModifiedTimestamp().getTime();
      if (localTimestamp < remoteTimestamp) {
        if (!local.getContentMD5().equals(remote.getContentMD5()))
          keysFresherOnRemote.add(key);
        else
          if (localTimestamp != remoteTimestamp) {
            metaChangedOnRemote.add(key);
          }
      } else {
        if (!local.getContentMD5().equals(remote.getContentMD5()))
          keysFresherOnLocal.add(key);
        else
          if (localTimestamp != remoteTimestamp) {
            metaChangedOnLocal.add(key);
          }
      }
    }

    HashSet<String> localKeys = new HashSet<String>();

    String[] keysList = listStoreEntries();
    for (String key: keysList) localKeys.add(key);

    localKeys.removeAll(remoteKeys);
    for (String key: localKeys) {
      keysFresherOnLocal.add(key);
    }

    return new StoreCompareResult(keysFresherOnLocal.toArray(new String[keysFresherOnLocal.size()]),
                                  keysFresherOnRemote.toArray(new String[keysFresherOnRemote.size()]),
                                  metaChangedOnLocal.toArray(new String[metaChangedOnLocal.size()]),
                                  metaChangedOnRemote.toArray(new String[metaChangedOnRemote.size()]));
  }

  /*
   * Implementation Store.produceStoreEntriesDiffData(keys)
   */
  public StoreEntryDiffData[] produceStoreEntriesDiffData(String keys[]) {
    ArrayList<StoreEntryDiffData> diffData = new ArrayList<StoreEntryDiffData>();
    StoreEntry entry;
    for (String key: keys) {
      try {
        entry = getStoreEntry(key);
        diffData.add(diffHandler.produceStoreEntryDiffData(key, entry));
      } catch (Exception e) {
        logger.warn("Error producing diff data for entry: " + key, e);
      }
    }

    return diffData.toArray(new StoreEntryDiffData[diffData.size()]);
  }

  /*
   * Implementation Store.diffStoreEntries(diffData)
   */
  public StoreEntryDiff[] diffStoreEntries(StoreEntryDiffData diffDataArray[]) {
    ArrayList<StoreEntryDiff> diff = new ArrayList<StoreEntryDiff>();

    StoreEntry entry;
    for (StoreEntryDiffData diffData: diffDataArray) {
      if (diffData == null) {
        logger.warn("Null diff data... ignoring");
        continue;
      }

      try {
        entry = getStoreEntry(diffData.getKey());
        diff.add(diffHandler.produceStoreEntryDiff(entry, diffData));
      } catch (Exception e) {
        logger.warn("Error producing diff for entry: " + diffData.getKey(), e);
      }
    }

    return diff.toArray(new StoreEntryDiff[diff.size()]);
  }

  /*
   * Implementation Store.patchStoreEntries(diff)
   */
  public void patchStoreEntries(StoreEntryDiff diffArray[]) {
    ArrayList<StoreEntry> entries = new ArrayList<StoreEntry>();

    StoreEntry entry;
    for (StoreEntryDiff diff: diffArray) {
      try {
        entry = getStoreEntry(diff.getKey());
        entries.add(diffHandler.patchStoreEntry(entry, diff));
      } catch (Exception e) {
        logger.warn("Error patching entry: " + diff.getKey(), e);
      }
    }

    updateStoreEntries(entries.toArray(new StoreEntry[entries.size()]));
  }

  /*
   * Implementation of putStoreEntryMetadata(key, meta)
   */
  protected void putStoreEntryMetadata(String key, StoreEntryMetadata meta)
    throws StoreException
  {
    try {
      persistenceHandler.writeMetadata(key, meta);
    } catch (RuntimeException e) {
      throw new StoreException("Error updating metadata for key: " + key, e);
    }
  }

  /*
   * Implementation of putStoreEntry(entry)
   */
  protected void putStoreEntry(StoreEntry entry) throws StoreException {
    try {
      persistenceHandler.write(entry);
    } catch (RuntimeException e) {
      throw new StoreException("Error writing entry: " + entry.getKey(), e);
    }
  }
}
