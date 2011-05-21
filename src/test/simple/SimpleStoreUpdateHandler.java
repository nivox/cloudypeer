/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package test.simple;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import cloudypeer.store.Store;
import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreUpdateHandler;
import java.io.ByteArrayOutputStream;

/**
 * Simple store update handler implementation
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleStoreUpdateHandler implements StoreUpdateHandler {

  private String name;

  public SimpleStoreUpdateHandler(String name) {
    this.name = name;
  }

  public void notifyUpdate(String[] keys, Store store) {
    for (String key: keys) {
      StoreEntry e = store.getStoreEntry(key);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte buff[] = new byte[256];
      try {
        int len = 0;
        int read;
        InputStream in = e.getInputStream();
        while ((read = in.read(buff)) >= 0) {
          out.write(buff, 0, read);
          len += read;
        }
        out.close();
      } catch (IOException ex) {
        System.out.format("Error reading input stream for: " + key);
        continue;
      }

      String entry = new String(out.toByteArray());
      System.out.format("Updated %s: %s=%s\n", name, key, entry);
    }
  }
}
