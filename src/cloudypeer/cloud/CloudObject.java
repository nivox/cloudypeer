/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud;

import java.io.InputStream;

/**
 * Defines common methods to access object information
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface CloudObject {

  /**
   * Returns the URI pointing to this specifying key
   *
   * @return Key URI
   */
  public CloudURI getCloudURI();

  /**
   * Returns the key associated to this object
   *
   * @return Object's key
   */
  public String getKey();

  /**
   * Returns the metadata associated to this object
   *
   * @return Object's metadata
   */
  public CloudMetadata getMetadata();

  /**
   * Returns the input stream from which read the object content
   *
   * @return Object's input stream
   */
  public InputStream getInputStream();
}
