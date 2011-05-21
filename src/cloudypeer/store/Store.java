/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This interface defines the methods needed by epidemic broadcast protocol to diffuse data.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface Store {
  /**
   * Adds an update handler for the store instance. <br>
   * The store must notify each handler whenever an entry is added or updated.
   *
   * @param handler Update handler to add
   */
  public void addUpdateHandler(StoreUpdateHandler handler);

  /**
   * Removes the specified update handlers.
   *
   * @param handler Update handler to remove
   */
  public void removeUpdateHandler(StoreUpdateHandler handler);

  /**
   * List all the entries currently active in the store. <br>
   * This method may list only a subset of the total entries based on application logic. If a full
   * list is needed use {@link listStoreEntries(long)} with a timestamp of 0.
   *
   * @return Active entries in the store
   */
  public String[] listStoreEntries() throws StoreException;

  /**
   * List all the entries fresher than the specified timestamp.
   *
   * @param thresholdTimestamp Timestamp used to filter entries
   * @return Entries with a modified timestamp grater than the threshold timestamp
   */
  public String[] listStoreEntries(Date thresholdTimestamp) throws StoreException;

  /**
   * Returns a map of the hash value associated to each key in this store.
   *
   * @return A map of the hash for this store's data.
   */
  public HashMap<String, StoreEntryMetadata> getStoreEntriesMetadata()
    throws StoreException;

  /**
   * Returns the metadata corresponding to the specified keys.
   *
   * @param keys Entry keys to retrieve
   * @return Requested metadata
   */
  public HashMap<String, StoreEntryMetadata> getStoreEntriesMetadata(String keys[])
    throws StoreException;

  /**
   * Returns the metadata corresponding to the specified key.
   *
   * @param key Entry key to retrieve
   * @return Requested metadata
   */
  public StoreEntryMetadata getStoreEntryMetadata(String key) throws StoreException;

  /**
   * Returns the entries in the store.
   *
   * @return Store's entries
   */
  public StoreEntry[] getStoreEntries() throws StoreException;


  /**
   * Returns the entries corresponding to the specified keys.
   *
   * @param keys Entry keys to retrieve
   * @return Requested store entries
   */
  public StoreEntry[] getStoreEntries(String[] keys) throws StoreException;


  /**
   * Returns the entry corresponding to the specified key
   *
   * @param key Entry keys to retrieve
   * @return Requested store entry or null
   */
  public StoreEntry getStoreEntry(String key) throws StoreException;


  /**
   * Sets the value of the specified store entry reading it from input stream.
   *
   * @param key Key of the store entry
   * @param in Input stream from which to read the content
   * @param contentType ContentType of this entry or null
   * @param userMetadata User metadata to associate to this content or null
   */
  public void putStoreEntry(String key, InputStream in, String contentType,
                            HashMap<String,String> userMetadata)
    throws StoreException;

  /**
   * Checks whether the specified store entry is present in the store
   *
   * @param key Key of the store entry to check
   * @return True if present
   */
  public boolean containsStoreEntry(String key) throws StoreException;

  /**
   * Updates the metadata associated to some store entry (only the modifiable part: content type,
   * user metadata, ...).
   *
   * @param metadatas the metadata map to update
   */
  public void updateMetadatas(Map<String, StoreEntryMetadata> metadatas)
    throws StoreException;

  /**
   * Updates the metadata associated to a store entry (only the modifiable part: content type, user
   * metadata, ...).
   *
   * @param key Store entry to update
   * @param metadata Entry's associated metadata
   * @exception IllegalArgumentException No entry for the specified key
   */
  public void updateMetadata(String key, StoreEntryMetadata metadata)
    throws StoreException;

  /**
   * Update the store entries of this instance using the specified StoreEntry array.
   *
   * @param entries The store entries to merge
   */
  public void updateStoreEntries(StoreEntry[] entries) throws StoreException;

  /**
   * Compare the local store with the provided entries. <br>
   *
   * @param entries The entries to diff with the local store
   * @return The result of the diff
   */
  public StoreCompareResult compareStoreEntries(StoreEntry[] remoteEntries)
    throws StoreException;

  /**
   * Compare the local store with the provided entries metadata
   *
   * @param remoteMetadata Metadata of remote entries
   * @return The result of the comparison
   */
  public StoreCompareResult compareStoreEntries(HashMap<String, StoreEntryMetadata> remoteMetadata)
    throws StoreException;

  /**
   * Produce the diff data for the specified entries.
   *
   * @param keys Entry keys for which prepare the diff data
   * @return Diff data array
   */
  public StoreEntryDiffData[] produceStoreEntriesDiffData(String keys[])
    throws StoreException;

  /**
   * Produce the diffs using the specified diffs data
   * @param diffData Data array to use to produce the diffs
   * @return Diff array
   */
  public StoreEntryDiff[] diffStoreEntries(StoreEntryDiffData diffData[])
    throws StoreException;

  /**
   * Patch the store using the specified diffs.
   *
   * @param diff Diffs array
   */
  public void patchStoreEntries(StoreEntryDiff diff[])
    throws StoreException;
}
