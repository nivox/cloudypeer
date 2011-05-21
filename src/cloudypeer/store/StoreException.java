/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store;

/**
 * Exception raised when an error occurs performing a store operation
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class StoreException extends RuntimeException {

  /**
   * Creates a new <code>StoreException</code> instance.
   *
   * @param message Error message
   * @param cause Cause
   */
  public StoreException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new <code>StoreException</code> instance.
   *
   * @param message Error message
   */
  public StoreException(String message) {
    super(message);
  }

  /**
   * Creates a new <code>StoreException</code> instance.
   *
   */
  public StoreException() {
    super();
  }
}
