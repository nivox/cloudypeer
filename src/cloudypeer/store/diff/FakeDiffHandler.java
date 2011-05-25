/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.diff;


import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryDiff;
import cloudypeer.store.StoreEntryDiffData;
import cloudypeer.store.simple.StoreEntryDiffHandler;
import cloudypeer.store.simple.SimpleStore;
import org.apache.log4j.Logger;


/**
 * Simple diff handler which doesn't implement a real diff strategy. It's useful for applications
 * that don't have entry modified (for instance in a broadcast scenario).
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class FakeDiffHandler implements StoreEntryDiffHandler {

    static Logger logger = Logger.getLogger(SimpleStore.class);

  public StoreEntryDiffData produceStoreEntryDiffData(String key, StoreEntry e) throws RuntimeException  {
    logger.trace("Producing diff data for entry " + key);
    return new FakeDiffData(key);
  }

  public StoreEntryDiff produceStoreEntryDiff(StoreEntry e, StoreEntryDiffData diffData)
    throws RuntimeException
  {
    logger.trace("Diffing entry " + e.getKey());
    return new FakeDiff(e);
  }

  public StoreEntry patchStoreEntry(StoreEntry e, StoreEntryDiff diff) throws RuntimeException
  {
    logger.trace("Patching entry " + diff.getKey());
    FakeDiff simpleDiff = (FakeDiff) diff;

    return simpleDiff.getEntry();
  }
}
