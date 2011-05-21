/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store.simple;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cloudypeer.store.StoreEntryMetadata;

/**
 * Simple StoreEntryMetadata implementation.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleStoreEntryMetadata implements StoreEntryMetadata {

  private Date modifiedTimestamp;
  private long contentLength;
  private String contentMD5;
  private String contentType;
  private HashMap<String, String> userMetadata;

  /**
   * Builds a new SimpleStoreEntryMetadata instance.
   *
   * @param modifiedTimestamp Modification timestamp
   * @param contentLength Content length
   * @param contentMD5 Content MD5
   * @param contentType Content type
   * @param userMetadata User metadata
   */
  public SimpleStoreEntryMetadata(Date modifiedTimestamp, long contentLength, String contentMD5,
                                  String contentType, Map<String,String> userMetadata)
  {
    if (modifiedTimestamp != null)
      this.modifiedTimestamp = modifiedTimestamp;
    else
      throw new IllegalArgumentException("Null modified timestamp");

    if (contentLength >= 0)
      this.contentLength = contentLength;
    else
      throw new IllegalArgumentException("Content length must be greater than or equal to 0");

    if (contentMD5 != null)
      this.contentMD5 = contentMD5.toLowerCase();
    else
      throw new IllegalArgumentException("Null content MD5");

    if (contentType != null)
      this.contentType = contentType;
    else
      this.contentType = "";

    if (userMetadata != null)
      this.userMetadata = new HashMap<String, String>(userMetadata);
    else
      this.userMetadata = new HashMap<String, String>();
  }

  /**
   * Returns the content type.
   *
   * @return Content type
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Returns the content length.
   *
   * @return Content type
   */
  public long getContentLength() {
    return contentLength;
  }

  /**
   * Returns the user metadata.
   *
   * @return User metadata
   */
  public HashMap<String, String> getUserMetadata() {
    return userMetadata;
  }

  /**
   * Returns the last date in which the associated entry was modified.
   *
   * @return Entry modification date
   */
  public Date getModifiedTimestamp() {
    return modifiedTimestamp;
  }

  /**
   * Returns the content MD5 hash.
   *
   * @return Content MD5
   */
  public String getContentMD5() {
    return contentMD5;
  }
}
