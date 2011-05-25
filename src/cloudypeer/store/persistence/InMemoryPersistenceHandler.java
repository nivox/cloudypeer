/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryMetadata;
import cloudypeer.store.StoreException;
import cloudypeer.store.simple.SimpleStoreEntry;
import cloudypeer.store.simple.SimpleStoreEntryMetadata;
import cloudypeer.store.simple.StoreEntryPersistenceHandler;
import cloudypeer.utils.MD5InputStream;
import java.util.Map;

/**
 * Simple store persistence handler that keeps entry in memory and cleans them when their last
 * modification date overcomes a specified threshold.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class InMemoryPersistenceHandler implements StoreEntryPersistenceHandler {

  public static int DEFAULT_CLEAN_THRESHOLD = 60 * 60 * 24;

  /* *********************************************************************
   * Instance variables
   ***********************************************************************/

  /**
   * Store of entry's data
   */
  private HashMap<String, byte[]> storeData = new HashMap<String, byte[]>();


  /**
   * Store of entry's metadata
   */
  private HashMap<String, StoreEntryMetadata> storeMetadata = new HashMap<String, StoreEntryMetadata>();

  /**
   * Entry clean threshold
   */
  private int cleanThreshold = DEFAULT_CLEAN_THRESHOLD * 1000;

  /* *********************************************************************
   * Getters/Setters
   ***********************************************************************/

  /**
   * Return the threshold used to clean old entries
   *
   * @return Clean threshold in seconds
   */
  public int getCleanThreshold() {
    return cleanThreshold;
  }

  /**
   * Sets the threshold used to clean old entries
   *
   * @param threshol Clean threshold in seconds
   */
  public void setCleanThreshold(int threshold) {
    this.cleanThreshold = threshold;
  }

  /* *********************************************************************
   * Utils methods
   ***********************************************************************/

  /**
   * Read the input stream into a byte array and close the corresponding input stream.
   *
   * @param in InputStream from which read the data
   * @return Read data byte array
   * @exception IOException if an error occurs
   */
  protected byte[] readData(InputStream in) throws IOException {
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();

      byte buff[] = new byte[1024];
      int len = 0;
      while ((len = in.read(buff, 0, buff.length)) >= 0) {
        out.write(buff, 0, len);
      }

      return out.toByteArray();
    } finally {
      try {
        if (out != null) out.close();
      } catch (IOException ex) {}

      try {
        if (in != null) in.close();
      } catch (IOException ex) {}
    }
  }

  /* *********************************************************************
   * Implementation of StoreEntryPersistenceHandler interface
   ***********************************************************************/
  public boolean contains(String key) throws RuntimeException {
    return storeData.containsKey(key);
  }

  public void write(String key, InputStream in, String contentType,
                    Map<String,String> userMetadata) throws RuntimeException
  {
    MD5InputStream md5In = new MD5InputStream(in);
    StoreEntryMetadata meta;
    byte data[];
    try {
      data = readData(md5In);
      md5In.close();
    } catch (IOException e) {
      throw new RuntimeException("Input/Output error reading entry data", e);
    }

    meta = new SimpleStoreEntryMetadata(new Date(), md5In.getLength(), md5In.getMD5(),
                                        contentType, userMetadata);

    storeData.put(key, data);
    storeMetadata.put(key, meta);
  }


  public void write(StoreEntry entry) throws RuntimeException {
    try {
      storeData.put(entry.getKey(), readData(entry.getInputStream()));
      storeMetadata.put(entry.getKey(), entry.getMetadata());
    } catch (IOException e) {
      throw new StoreException("Error reading entry data", e);
    }
  }

  public StoreEntry read(String key) throws RuntimeException {
    byte data[] = storeData.get(key);
    if (data == null) return null;

    StoreEntryMetadata metadata = readMetadata(key);
    return new SimpleStoreEntry(key, new ByteArrayInputStream(data), metadata);
  }

  public final void writeMetadata(String key, StoreEntryMetadata meta) throws RuntimeException {
    if (!storeMetadata.containsKey(key))
      throw new RuntimeException("Cannot write metadata for unknown entry: " + key);

    storeMetadata.put(key, meta);
  }

  public StoreEntryMetadata readMetadata(String key) throws RuntimeException {
    return storeMetadata.get(key);
  }

  public String[] listEntries(Date timestamp) throws RuntimeException {
    ArrayList<String> list = new ArrayList<String>();
    ArrayList<String> toClean = new ArrayList<String>();
    long currTime = System.currentTimeMillis();

    StoreEntryMetadata meta;
    for (String key: storeMetadata.keySet()) {
      meta = storeMetadata.get(key);

      if (meta.getModifiedTimestamp().getTime() < (currTime - cleanThreshold)) {
        toClean.add(key);
        continue;
      }

      if (meta.getModifiedTimestamp().getTime() > timestamp.getTime()) list.add(key);
    }

    for (String key: toClean) {
      storeMetadata.remove(key);
      storeData.remove(key);
    }

    return list.toArray(new String[list.size()]);
  }
}
