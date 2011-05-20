/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer;

import java.io.Serializable;

/**
 * General interface for node descriptors.
 * Defines a standard way to discriminate between cloud and peers.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface Node extends Serializable {

  /**
   * Discriminate between peer and cloud descriptors
   *
   * @return true if cloud descriptor
   */
  public boolean isCloud();
}
