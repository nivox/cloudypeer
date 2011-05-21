/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.network.simple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import cloudypeer.PeerNode;
import cloudypeer.network.NetworkConnection;
import cloudypeer.network.NetworkException;
import cloudypeer.network.NetworkHelper;
import cloudypeer.network.NetworkMessage;
import org.apache.log4j.Logger;

/**
 * Simple NetworkHelper implementation
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleNetworkHelper extends NetworkHelper {

  static Logger logger = Logger.getLogger(SimpleNetworkHelper.class);

  /**
   * Class responsible of dispatching incoming TCP connections
   */
  private class ConnectionDispatcher extends Thread {
    private Socket conn;

    public ConnectionDispatcher(Socket conn) {
      this.conn = conn;
    }

    public void run() {
      InputStream in;
      OutputStream out;
      ByteBuffer byteBuff = ByteBuffer.allocate(4);
      byte buff[] = new byte[4];

      try {
        int timeout = conn.getSoTimeout();
        conn.setSoTimeout(3000);

        /* Read the info for the dispatch */
        in = conn.getInputStream();

        in.read(buff);
        byteBuff.put(buff, 0, 4);

        int clientID = byteBuff.getInt(0);
        out = conn.getOutputStream();
        if (clientReadyForConnection(clientID)) {
          logger.trace("Dispatching connection with clientID: " + clientID);
          out.write(1);
          conn.setSoTimeout(timeout);
          if (!dispatchConnection(byteBuff.getInt(0), new SimpleNetworkConnection(conn))) {
            logger.warn("Error dispatching connection to clientID. Client not ready anymore.");
            conn.close();
          }
        } else {
          logger.warn("Error dispatching connection to clientID. Client not ready.");
          out.write(0);
          conn.close();
        }
      } catch (SocketTimeoutException e) {
        logger.warn("Timeout while negotiating the connection.");
        try {
          conn.close();
        } catch (IOException ex) {}
      } catch (Exception e) {
        logger.error("Error dispatching the connection.", e);
        try {
          conn.close();
        } catch (IOException ex) {}
      }
    }
  }

  /**
   * Class responsible of handling incoming TCP connections
   */
  private class ConnectionServer extends Thread {
    private ServerSocket socket;
    private volatile boolean terminated;

    public ConnectionServer(ServerSocket socket) {
      this.socket = socket;
      this.terminated = false;
    }

    public void terminate() {
      terminated = true;
      try {
        socket.close();
      } catch (IOException e) {}
    }

    public void run() {
      while (!terminated) {
        try {
          Socket conn = socket.accept();

          ConnectionDispatcher dispatcher = new ConnectionDispatcher(conn);
          dispatcher.start();
        } catch (IOException e) {
          logger.warn("Input/Output error accepting connection.", e);
        } catch (Exception e) {
          logger.error("Error accepting connection.", e);
        }
      }
    }
  }

  /**
   * Class responsible of handling incoming UDP packets
   */
  private class DatagramServer extends Thread {
    private DatagramSocket socket;
    private volatile boolean terminated;

    public DatagramServer(DatagramSocket socket) {
      this.socket = socket;
      this.terminated = false;
    }

    public void terminate() {
      terminated = true;
      socket.close();
    }

    public void run() {
      DatagramPacket pkt;
      ByteArrayInputStream in;
      ObjectInputStream objIn;
      NetworkMessage msg;
      byte buff[];
      while (!terminated) {
        try {
          buff = new byte[socket.getReceiveBufferSize()];
          pkt = new DatagramPacket(buff, buff.length);
          socket.receive(pkt);

          in = new ByteArrayInputStream(pkt.getData(), 0, pkt.getLength());
          objIn = new ObjectInputStream(in);

          msg = (NetworkMessage) objIn.readObject();
          dispatchDatagramMessage(msg);
        } catch (IOException e) {
          logger.warn("Input/Output error reading the message.", e);
        } catch (Exception e) {
          logger.error("Error receiving message", e);
        }
      }
    }
  }

  /* *********************************************************************
   * Instance variables
   ***********************************************************************/
  private ConnectionServer connServer;
  private ServerSocket connSocket;

  private DatagramServer dgramServer;
  private DatagramSocket dgramSocket;

  /* *********************************************************************
   * Constructors implementation
   ***********************************************************************/
  public SimpleNetworkHelper(InetAddress addr, int port) throws IOException {
    super(addr, port);
  }

  /* *********************************************************************
   * Abstract methods implementations
   ***********************************************************************/

  public void start() throws IOException, NetworkException {
    connSocket = new ServerSocket(localNode.getPort());
    dgramSocket = new DatagramSocket(localNode.getPort());

    connServer = new ConnectionServer(connSocket);
    connServer.start();

    dgramServer = new DatagramServer(dgramSocket);
    dgramServer.start();
  }

  protected void terminateImpl() {
    connServer.terminate();
    dgramServer.terminate();
  }

  protected void sendDatagramMessageImpl(PeerNode destination, NetworkMessage message)
    throws IOException, NetworkException
  {
    ObjectOutputStream out;
    DatagramPacket pkt;
    ByteArrayOutputStream buff = new ByteArrayOutputStream();

    try {
      out = new ObjectOutputStream(buff);
      out.writeObject(message);
      out.close();

      if (buff.size() > dgramSocket.getSendBufferSize())
        throw new NetworkException(String.format("Message to big: length=%d, max=%d",
                                                 buff.size(), dgramSocket.getSendBufferSize()));

      pkt = new DatagramPacket(buff.toByteArray(), buff.size(), destination.getInetAddress(),
                               destination.getPort());

      dgramSocket.send(pkt);
    } catch (IOException e) {
      logger.warn("Input/output error sending the message", e);
      throw e;
    } catch (NetworkException e) {
      logger.warn(e.getMessage());
      throw e;
    } catch (Exception e) {
      logger.error("Error sending the message.", e);
      throw new NetworkException("Error sending the message", e);
    }
  }

  protected NetworkConnection createConnectionImpl(PeerNode endpoint, int clientID, int timeout)
    throws IOException, NetworkException
  {
    Socket conn = new Socket();
    ByteBuffer byteBuff = ByteBuffer.allocate(4);
    byte clientIDBytes[] = new byte[4];
    byteBuff.putInt(0, clientID);
    byteBuff.get(clientIDBytes, 0, 4);
    try {
      OutputStream out;
      InputStream in;
      conn.connect(new InetSocketAddress(endpoint.getInetAddress(), endpoint.getPort()),
                   timeout);

      int currentTimeout = conn.getSoTimeout();
      conn.setSoTimeout(timeout);

      out = conn.getOutputStream();
      out.write(clientIDBytes);

      in = conn.getInputStream();
      byte status = (byte) in.read();
      if (status == 0) {
        conn.close();
        throw new NetworkException("Remote peer not available for connection");
      } else if (status != 1) {
        conn.close();
        throw new NetworkException("Remote peer not implements the protocol correctly");
      }

      conn.setSoTimeout(currentTimeout);
      return new SimpleNetworkConnection(conn);
    } catch (SocketTimeoutException e) {
      logger.warn("Timeout while negotiating the connection.");
      return null;
    } catch (IOException e) {
      logger.warn("Input/Output error connecting.", e);
      try {
        conn.close();
      } catch (IOException ex) {}
      throw e;
    }  catch (NetworkException e) {
      throw e;
    } catch (Exception e) {
      try {
        conn.close();
      } catch (IOException ex) {}
      logger.error("Error connecting", e);
      throw new NetworkException("Error connecting to endpoint", e);
    }
  }
}
