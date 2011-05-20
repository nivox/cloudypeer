/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.network;

import java.io.Serializable;

import cloudypeer.PeerNode;

/**
 * Simple implementation of a network message
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class NetworkMessage implements Serializable{
  private PeerNode source;
  private int clientID;
  private Serializable message;

  /**
   * Builds a new NetworkMessage
   *
   * @param source The source of this message
   * @param clientID The clientID of this message
   * @param message The actual message to be sent
   */
  public NetworkMessage(PeerNode source, int clientID, Serializable message) {
    this.source = source;
    this.clientID = clientID;
    this.message = message;
  }

  /**
   * Returns the source of this message
   *
   * @return Source peer
   */
  public PeerNode getSource() {
    return this.source;
  }

  /**
   * Return the ID of the client which generated this message and symmetrically to which this
   * message is destined.
   *
   * @return Client ID
   */
  public int getClientID() {
    return clientID;
  }

  /**
   * Returns the actual message
   *
   * @return Actual message
   */
  public Serializable getMessage() {
    return message;
  }
}