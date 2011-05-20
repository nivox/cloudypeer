/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */

package cloudypeer.peersampling;

import cloudypeer.GossipProtocolException;

/**
 * This exception is raised by a PeerSampler implementation on error
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class PeerSamplerException extends GossipProtocolException {

  /**
   * Creates a new blank PeerSampler exception.
   *
   */
  public PeerSamplerException() {
    super();
  }

  /**
   * Creates a new PeerSampler exception with the specified message.
   *
   * @param message Error message
   */
  public PeerSamplerException(String message) {
    super(message);
  }

  /**
   * Creates a new PeerSampler exception with the specified message and cause.
   *
   * @param message Error message
   * @param cause Error cause
   */
  public PeerSamplerException(String message, Throwable cause) {
    super(message, cause);
  }
}