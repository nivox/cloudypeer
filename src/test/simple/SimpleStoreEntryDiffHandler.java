/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package test.simple;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryDiff;
import cloudypeer.store.StoreEntryDiffData;
import cloudypeer.store.simple.StoreEntryDiffHandler;
import cloudypeer.store.simple.SimpleStore;
import org.apache.log4j.Logger;



/**
 * Simple entry handler.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleStoreEntryDiffHandler implements StoreEntryDiffHandler {

    static Logger logger = Logger.getLogger(SimpleStore.class);

  public StoreEntryDiffData produceStoreEntryDiffData(String key, StoreEntry e) throws RuntimeException  {
    logger.trace("Producing diff data for entry " + key);
    return new SimpleStoreEntryDiffData(key);
  }

  public StoreEntryDiff produceStoreEntryDiff(StoreEntry e, StoreEntryDiffData diffData)
    throws RuntimeException
  {
    logger.trace("Diffing entry " + e.getKey());
    return new SimpleStoreEntryDiff(e);
  }

  public StoreEntry patchStoreEntry(StoreEntry e, StoreEntryDiff diff) throws RuntimeException
  {
    logger.trace("Patching entry " + diff.getKey());
    SimpleStoreEntryDiff simpleDiff = (SimpleStoreEntryDiff) diff;

    return simpleDiff.getEntry();
  }
}
