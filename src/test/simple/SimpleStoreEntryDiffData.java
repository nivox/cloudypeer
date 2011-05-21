/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package test.simple;

import cloudypeer.store.StoreEntryDiffData;

/**
 * Simple Implementation of StoreEntryDiffData
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleStoreEntryDiffData implements StoreEntryDiffData {

  private String key;

  public SimpleStoreEntryDiffData(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
