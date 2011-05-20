/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud;

import java.util.Date;
import java.util.Map;

/**
 * Defines common method to access the cloud metadata
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface CloudMetadata {

  /**
   * Returns the MD5 hash of this object.
   *
   * @return Object's MD5 hash
   */
  public String getContentMD5();

  /**
   * Returns the content type of this object.
   *
   * @return Object content type
   */
  public String getContentType();

  /**
   * Returns the length of the content.
   *
   * @return Content length
   */
  public long getContentLength();

  /**
   * Returns the date of last modification of this object.
   *
   * @return Modification date
   */
  public Date getLastModified();

  /**
   * If available returns the version ID of this object otherwise return null.
   *
   * @return VersionID of this object
   */
  public String getVersionID();

  /**
   * Returns the user metadata associated to this object.
   *
   * @return User metadata
   */
  public Map<String, String> getUserMetadata();
}
