/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.SocketTimeoutException;

/**
 * Interface that defines the operation to be supported by a network connection
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface NetworkConnection {

  /**
   * Send an object on this connection.
   *
   * @param obj Object to send
   * @exception IOException if an error occurs
   * @exception NetworkException if an error occurs
   */
  public void send(Serializable obj) throws IOException, NetworkException;

  /**
   * Receive an object on this connection.
   *
   * @param timeout How much block before returning null
   * @return Received object or null
   * @exception IOException if an error occurs
   * @exception NetworkException if an error occurs
   */
  public Object receive(int timeout) throws IOException, NetworkException,
                                            SocketTimeoutException;

  /**
   * Returns the current connection input stream.
   *
   * @return Connection input stream
   * @exception IOException if an error occurs
   * @exception NetworkException if an error occurs
   */
  public InputStream getInputStream() throws IOException, NetworkException;

  /**
   * Returns the current connection output stream
   *
   * @return Connection output stream
   * @exception IOException if an error occurs
   * @exception NetworkException if an error occurs
   */
  public OutputStream getOutputStream() throws IOException, NetworkException;

  /**
   * Close this connection.
   *
   * @exception IOException if an error occurs
   * @exception NetworkException if an error occurs
   */
  public void close() throws IOException, NetworkException;
}
