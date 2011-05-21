/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud.mysql;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cloudypeer.cloud.CloudMetadata;

/**
 * Implementation of CloudMetadata for MySQL cloud.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class MySQLCloudMetadata implements CloudMetadata {

  private long contentLength = 0;
  private Date lastModified = null;
  private String contentMD5 = null;
  private String contentType = null;

  /**
   * Creates a new <code>MySQLCloudMetadata</code> instance.
   *
   * @param contentLength Object's content length
   * @param lastModified Object's last modification date
   * @param contentMD5 Object's MD5 hash
   * @param contentType Object's content type
   */
  public MySQLCloudMetadata(long contentLength, Date lastModified, String contentMD5,
                            String contentType)
  {
    if (contentLength < 0) throw new IllegalArgumentException("Illegal content length");
    this.contentLength = contentLength;

    if (lastModified == null) throw new IllegalArgumentException("Null modification date");
    this.lastModified = lastModified;

    if (contentMD5 == null) throw new IllegalArgumentException("Null content MD5");
    this.contentMD5 = contentMD5.toLowerCase().trim();

    this.contentType = contentType;
  }

  /**
   * Returns the content type for the associated object.
   *
   * @return Object's content type
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Returns the content length for the associated object.
   *
   * @return Object's content length
   */
  public long getContentLength() {
    return contentLength;
  }

  /**
   * Returns the last modification date for the associated object.
   *
   * @return Object's last modification date
   */
  public Date getLastModified() {
    return lastModified;
  }

  /**
   * Returns the content's md5 hash for the associated object.
   *
   * @return Object's md5 hash of the content
   */
  public final String getContentMD5() {
    return contentMD5;
  }

  /**
   * Returns the user metadata for the associated object. <br>
   * Not supported by MySQL cloud
   *
   * @return Empty metadata
   */
  public final Map<String,String> getUserMetadata() {
    return new HashMap<String, String>();
  }

  /**
   * Not supported by MySQL cloud
   *
   * @return null
   */
  public String getVersionID() {
    return null;
  }
}
