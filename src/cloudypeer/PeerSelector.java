/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer;

/**
 * This interface defines a general way to select peers.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface PeerSelector {

  /**
   * Returns a node descriptor based on some politic.
   *
   * @return Node descriptor
   */
  public Node getNode();
}
