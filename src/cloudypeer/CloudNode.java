/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer;

import cloudypeer.cloud.CloudURI;

/**
 * Cloud node descriptor. <br>
 * The node specify the cloud URI that it stands for.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class CloudNode implements Node {

  /**
   * The URI of the cloud which this node represent
   */
  protected CloudURI cloudURI;

  /**
   * Creates a new <code>CloudNode</code> instance.
   *
   * @param cloud Cloud represented by this instance
   */
  public CloudNode(CloudURI cloud) {
    this.cloudURI = cloud;
  }

  /*
   * Interface Node.isCloud implementation
   */
  public boolean isCloud() {
    return true;
  }

  /**
   * Return the cloud URI that this cloud node descriptor represents
   *
   * @return Cloud uri of this node
   */
  public CloudURI getCloudURI() {
    return this.cloudURI;
  }
}