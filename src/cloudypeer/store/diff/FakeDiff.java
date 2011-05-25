/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.diff;

import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryDiff;

/**
 * This class actually contains all the data of the entry, not only the diff.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class FakeDiff implements StoreEntryDiff {

  private StoreEntry entry;
  public FakeDiff(StoreEntry entry) {
    this.entry = entry;
  }

  public String getKey() {
    return entry.getKey();
  }

  public StoreEntry getEntry() {
    return entry;
  }
}
