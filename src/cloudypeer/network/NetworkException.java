/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.network;

/**
 * Exception raised by a NetworkHelper provider on error
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class NetworkException extends RuntimeException{

  /**
   * Creates a new <code>NetworkException</code> instance.
   *
   * @param message Error message
   * @param cause Cause of the exception
   */
  public NetworkException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new <code>NetworkException</code> instance.
   *
   * @param message Error message
   */
  public NetworkException(String message) {
    super(message);
  }

  /**
   * Creates a new <code>NetworkException</code> instance.
   */
  public NetworkException() {}

}
