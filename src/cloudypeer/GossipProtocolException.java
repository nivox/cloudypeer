/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */

package cloudypeer;

/**
 * This exception is raised by a GossipProtocol implementation on error
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class GossipProtocolException extends RuntimeException {

  /**
   * Creates a new blank GossipProtocol exception.
   *
   */
  public GossipProtocolException() {
    super();
  }

  /**
   * Creates a new GossipProtocol exception with the specified message.
   *
   * @param message Error message
   */
  public GossipProtocolException(String message) {
    super(message);
  }

  /**
   * Creates a new GossipProtocol exception with the specified message and cause.
   *
   * @param message Error message
   * @param cause Error cause
   */
  public GossipProtocolException(String message, Throwable cause) {
    super(message, cause);
  }
}