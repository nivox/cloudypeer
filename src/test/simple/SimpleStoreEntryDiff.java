/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package test.simple;

import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryDiff;

/**
 * Simple Implementation of StoreEntryDiffData
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleStoreEntryDiff implements StoreEntryDiff {

  private StoreEntry entry;
  public SimpleStoreEntryDiff(StoreEntry entry) {
    this.entry = entry;
  }

  public String getKey() {
    return entry.getKey();
  }

  public StoreEntry getEntry() {
    return entry;
  }
}
