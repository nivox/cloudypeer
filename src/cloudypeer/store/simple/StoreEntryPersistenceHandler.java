/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.simple;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryMetadata;

/**
 * This class defines a common way to delegate the persistence operation to application specific
 * classes. This way a common Store implementation can be used with different persistence strategy.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface StoreEntryPersistenceHandler {


  /**
   * Builds and persist a new StoreEntry.
   *
   * @param key Key to write
   * @param in InputStream holding the content
   * @param contentType Content type
   * @param userMetadata User metadata associated to this entry
   * @exception RuntimeException if an error occurs
   */
  public void write(String key, InputStream in, String contentType, Map<String, String> userMetadata)
    throws RuntimeException;

  /**
   * Persist the specified entry (overwriting any matching entry already present in the store).
   *
   * @param e Entry to persist
   * @exception RuntimeException if an error occurs
   */
  public void write(StoreEntry e) throws RuntimeException;

  /**
   * Update the metadata of the already persistent entry associated to the specified key.
   *
   * @param key Key pointing to the entry to update
   * @param metadata New metadata for the entry.
   * @exception RuntimeException if an error occurs
   */
  public void writeMetadata(String key, StoreEntryMetadata metadata) throws RuntimeException;

  /**
   * Read the entry associated to the specified key.
   *
   * @param key Key to read
   * @return A StoreEntry instance or null if no such key was present
   * @exception RuntimeException if an error occurs
   */
  public StoreEntry read(String key) throws RuntimeException;

  /**
   * Read the entry metadata associated to the specified key.
   *
   * @param key Key to read
   * @return A StoreEntryMetadata instance or null if no such key was present
   * @exception RuntimeException if an error occurs
   */
  public StoreEntryMetadata readMetadata(String key) throws RuntimeException;

  /**
   * List all the entries' key that have a modification date greater than the specified
   * timestamp. If the timestamp is null, lists all the entries.
   *
   * @param timestamp Threshold timestamp or null
   * @return Entries array
   * @exception RuntimeException if an error occurs
   */
  public String[] listEntries(Date timestamp) throws RuntimeException;

  /**
   * Checks if the specified entry is present
   *
   * @param key Key to check
   * @return True if present
   * @exception RuntimeException if an error occurs
   */
  public boolean contains(String key) throws RuntimeException;
}
