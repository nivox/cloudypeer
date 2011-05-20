/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.network;

import java.io.Serializable;

import cloudypeer.PeerNode;

/**
 * Defines a transparent way for the network helper to dispatch messages.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface NetworkClient {

  /**
   * Method used to dispatch message form the NetworkHelper.
   * This method runs in the NetworkHelper thread, hence it should be relatively small and fast to
   * return control to the NetworkHelper.
   *
   * @param sender The remote peer which sent the message
   * @param message The opaque message sent by the remote peer
   */
  public void processMessage(PeerNode sender, Serializable message);

  /**
   * Return the peer node associated with the client.
   *
   * @return Client's PeerNode instance
   */
  public PeerNode getNode();
}
