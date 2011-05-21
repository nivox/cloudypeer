/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.utils;

import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simple InputStream wrapper which computer length and md5 of the underlying input stream
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class MD5InputStream extends InputStream {

  /**
   * Utility function that converts a byte array in an hex string
   *
   * @param raw Byte array to convert
   * @return Hex string
   */
  private static String getHex(byte[] raw) {
    StringBuffer hexString = new StringBuffer();
    String buff;

    for (int i = 0; i < raw.length; i++) {
      buff = Integer.toHexString(0xFF & raw[i]);

      if (buff.length() < 2) {
        buff = "0" + buff;
      }

    hexString.append(buff);
    }

    return hexString.toString().toLowerCase();
  }

  private InputStream in;
  private MessageDigest md5;
  private long length;
  private String md5String;

  /**
   * Builds a new MD5InputStream wrapping the specified InputStream
   *
   * @param in Source input stream
   */
  public MD5InputStream (InputStream in) {
    this.in = in;
    try {
      this.md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("No MD5 algorithm found", e);
    }
    this.length = 0;
    this.md5String = null;
  }

  @Override
  public int read() throws IOException {
    int read = in.read();
    if (read >= 0) {
      length += 1;
      md5.update((byte) read);
    }

    return read;
  }

  @Override
  public int read(byte b[]) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    int num = in.read(b, off, len);

    if (num > 0) {
      length += num;
      md5.update(b, off, num);
    }

    return num;
  }

  @Override
  public boolean markSupported() {
    return false;
  }


  /**
   * Returns the MD5 of the data transited through this InputStream
   *
   * @return Data MD5
   */
  public String getMD5() {
    if (in != null) throw new IllegalStateException("InputStream not closed");

    return md5String;
  }


  /**
   * Returns the length of the data transited through this InputStream
   *
   * @return Data length
   */
  public long getLength() {
    if (in != null) throw new IllegalStateException("InputStream not closed");
    return length;
  }

  @Override
  public void close() throws IOException {
    if (in == null) return;
    in.close();
    in = null;
    byte[] md5Digest = md5.digest();

    md5String = getHex(md5Digest);
    md5 = null;
  }

  @Override
  public void mark(int readlimit) {
    throw new RuntimeException("Not supported");
  }

  @Override
  public void reset() {
    throw new RuntimeException("Not supported");
  }

  @Override
  public long skip(long n) {
    throw new RuntimeException("Not supported");
  }
}
