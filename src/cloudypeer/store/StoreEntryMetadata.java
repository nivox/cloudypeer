/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Defines the metadata associated to a Store's entry.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface StoreEntryMetadata extends Serializable {

  /**
   * Returns the user metadata associated to the entry.
   *
   * @return User metadata
   */
  public Map<String, String> getUserMetadata();

  /**
   * Returns the last timestamp in which the entry was modified.
   *
   * @return Last modification timestamp
   */
  public Date getModifiedTimestamp();

  /**
   * Returns the MD5 hash of the content
   *
   * @return Content MD5
   */
  public String getContentMD5();

  /**
   * Returns the content type of the entry.
   *
   * @return Content type
   */
  public String getContentType();

  /**
   * Returns the content length.
   *
   * @return Content length
   */
  public long getContentLength();


}
