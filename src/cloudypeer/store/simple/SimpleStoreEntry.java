/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.simple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryMetadata;

/**
 * Simple implementation of StoreEntry.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleStoreEntry implements StoreEntry {

  private String key;
  private InputStream in;
  private StoreEntryMetadata metadata;

  /**
   * Creates a new SimpleStoreEntry instance. <br>
   * This instance will be backed by the specified input stream. Closing it will cause the
   * impossibility to read the entry's content.
   *
   * @param key Key of the entry
   * @param in InputStream holding the entry content
   * @param meta Metadata associated to this entry
   */
  public SimpleStoreEntry(String key, InputStream in, StoreEntryMetadata meta) {
    this.key = key;
    this.in = in;
    this.metadata = meta;
  }

  /**
   * Returns the key of this entry
   *
   * @return Entry key
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns the input stream of the content data.
   *
   * @return Content input stream
   */
  public InputStream getInputStream() {
    return in;
  }

  /**
   * Returns the metadata associated to this entry
   *
   * @return Entry metadata
   */
  public StoreEntryMetadata getMetadata() {
    return metadata;
  }

  private long pipeData(InputStream in, OutputStream out) throws IOException {
    byte buff[] = new byte[1024];
    long count = 0;
    int len = 0;
    try {
      while ((len = in.read(buff)) >= 0) {
        count += len;
        out.write(buff, 0, len);
      }
    } finally {
      in.close();
    }

    return count;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.writeUTF(key);
    out.writeObject(metadata);

    long length = pipeData(this.in, out);
    if (length != metadata.getContentLength())
      throw new IOException("Actual content length differs from advertised one");
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    this.key = in.readUTF();
    this.metadata = (StoreEntryMetadata) in.readObject();

    long length = pipeData(in, out);
    if (length != metadata.getContentLength())
      throw new IOException("Actual content length differs from advertised one");

    out.close();
    this.in = new ByteArrayInputStream(out.toByteArray());
   }
}
