/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud;

/**
 * Exception thrown when a Cloud provider encounters an error not directly related to the current
 * network conditions.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class CloudException extends RuntimeException {


  /**
   * Creates a new <code>CloudException</code> instance.
   *
   * @param message Error message
   * @param cause Error cause
   */
  public CloudException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new <code>CloudException</code> instance.
   *
   * @param message Error message
   */
  public CloudException(String message) {
    super(message);
  }

  /**
   * Creates a new <code>CloudException</code> instance.
   */
  public CloudException() {}
}
