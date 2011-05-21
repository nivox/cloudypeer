/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.network.simple;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;

import cloudypeer.network.NetworkConnection;
import cloudypeer.network.NetworkException;

/**
 * Simple NetworkConnection implementation
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleNetworkConnection implements NetworkConnection {

  /**
   * Simple OutputStream wrapper which protects the underlying OutputStream from being closed
   */
  private class ProtectedOutputStream extends OutputStream {
    private OutputStream out;
    public ProtectedOutputStream(OutputStream out) {
      this.out = out;
    }
    public void write(int b) throws IOException {
      out.write(b);
    }
    public void write(byte[] b) throws IOException {
      out.write(b);
    }
    public void write(byte[] b, int off, int len) throws IOException {
      out.write(b, off, len);
    }
  }

  /**
   * Simple InputStream wrapper which protects the underlying InputStream from being closed
   */
  private class ProtectedInputStream extends InputStream {
    private InputStream in;
    public ProtectedInputStream(InputStream in) {
      this.in = in;
    }
    public int read() throws IOException {
      return in.read();
    }
    public int read(byte[] b) throws IOException {
      return in.read(b);
    }
    public int read(byte[] b, int off, int len) throws IOException {
      return in.read(b, off, len);
    }
  }

  /**
   * Socket holding the connection
   */
  private Socket conn;

  protected SimpleNetworkConnection(Socket conn) {
    this.conn = conn;
  }

  /*
   * Implementation of NetworkConnection.close()
   */
  public void close() throws IOException, NetworkException {
    conn.close();
  }

  /*
   * Implementation of NetworkConnection.send()
   */
  public void send(Serializable object) throws IOException, NetworkException {
    ProtectedOutputStream pout = new ProtectedOutputStream(conn.getOutputStream());
    ObjectOutputStream out = new ObjectOutputStream(pout);

    out.writeObject(object);
    out.close();
  }

  /*
   * Implementation of NetworkConnection.getInputStream()
   */
  public InputStream getInputStream() throws IOException, NetworkException {
    return conn.getInputStream();
  }

  /*
   * Implementation of NetworkConnection.getOutputStream()
   */
  public OutputStream getOutputStream() throws IOException, NetworkException {
    return conn.getOutputStream();
  }

  /*
   * Implementation of NetworkConnection.receive()
   */
  public Object receive(int timeout) throws IOException, NetworkException {
    ProtectedInputStream pin;
    ObjectInputStream in = null;
    Object obj = null;
    try {
      conn.setSoTimeout(timeout);
      pin = new ProtectedInputStream(conn.getInputStream());
      in = new ObjectInputStream(pin);

      obj = in.readObject();
    } catch (ClassNotFoundException e) {
      throw new NetworkException("Error receiving data", e);
    } finally {
      try {
        if (in != null) in.close();
      } catch (IOException e) {}
    }
    return obj;
  }
}
