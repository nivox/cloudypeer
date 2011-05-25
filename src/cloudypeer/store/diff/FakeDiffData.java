/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.diff;

import cloudypeer.store.StoreEntryDiffData;

/**
 * This class just carries the key of the entries to diff.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class FakeDiffData implements StoreEntryDiffData {

  private String key;

  public FakeDiffData(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
