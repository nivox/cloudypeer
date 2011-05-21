/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.simple;


import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryDiff;
import cloudypeer.store.StoreEntryDiffData;

/**
 * This class defines a common way to delegate the diffing operation to application specific
 * classes. This way a common Store implementation can be used with different diffing strategies.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface StoreEntryDiffHandler {

  /**
   * Produce an object containing all the necessary information for a remote party to produce a
   * diff. <br>
   *
   * @param key Entry key
   * @param e Entry to process or null if entry unknown locally
   * @return Information to produce a diff of the specified entry.
   */
  public StoreEntryDiffData produceStoreEntryDiffData(String key, StoreEntry e) throws RuntimeException;

  /**
   * Produce a diff of the specified entry
   *
   * @param e Entry to diff
   * @param diffData Data to use to produce the diff
   * @return Diff of the specified entry
   */
  public StoreEntryDiff produceStoreEntryDiff(StoreEntry e, StoreEntryDiffData diffData)
    throws RuntimeException;

  /**
   * Patch an entry using the specified diff.
   *
   * @param e Entry to patch
   * @param diff Diff to use for the patch
   * @return Patched entry
   */
  public StoreEntry patchStoreEntry(StoreEntry e, StoreEntryDiff diff) throws RuntimeException;
}
