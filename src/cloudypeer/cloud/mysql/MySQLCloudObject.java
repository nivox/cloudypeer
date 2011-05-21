/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud.mysql;

import java.io.InputStream;
import java.net.URISyntaxException;

import cloudypeer.cloud.CloudMetadata;
import cloudypeer.cloud.CloudObject;
import cloudypeer.cloud.CloudURI;

/**
 * Implementation of CloudObject for MySQL cloud.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class MySQLCloudObject implements CloudObject {

  private MySQLCloudURI objectURI;
  private MySQLCloudObjectInputStream in;
  private MySQLCloudMetadata meta;

  /**
   * Creates a new <code>MySQLCloudObject</code> instance.
   *
   */
  public MySQLCloudObject(CloudURI objectURI, MySQLCloudObjectInputStream in, MySQLCloudMetadata meta)
    throws URISyntaxException
  {
    if (objectURI == null)
      throw new IllegalArgumentException("Object URI not specified");

    if (objectURI.getBucket() == null || objectURI.getBucket().trim().equals(""))
      throw new IllegalArgumentException("Object bucket not specified!");

    if (objectURI.getKey() == null || objectURI.getKey().trim().equals(""))
      throw new IllegalArgumentException("Object key not specified!");

    this.objectURI = new MySQLCloudURI(objectURI.getBaseURI(), objectURI.getBucket(), objectURI.getKey(), null);

    this.in = in;
    this.meta = meta;
  }

  /**
   * Returns the key associated to this cloud object
   *
   * @return Associated key
   */
  public String getKey() {
    return objectURI.getKey();
  }

  /**
   * Returns the input stream associated to this cloud object.
   *
   * @return Data input stream for this cloud object
   */
  public final InputStream getInputStream() {
    return in;
  }

  /**
   * Returns the URI of this object
   *
   * @return CloudURI for this object
   */
  public final CloudURI getCloudURI() {
    return objectURI;
  }

  /**
   * Returns the metadata associated to the object.
   *
   * @return Object metadata
   */
  public final CloudMetadata getMetadata() {
    return meta;
  }
}
