/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


/**
 * Node descriptor for peers.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class PeerNode implements Node {

  protected InetAddress ip;
  protected int port;

  public PeerNode(InetSocketAddress addr) {
    if (addr == null) throw new IllegalArgumentException("Null address");
    if (port < 0 || port > 65536) throw new IllegalArgumentException("Invalid port number");

    this.ip = addr.getAddress();
    this.port = addr.getPort();
  }

  public PeerNode(InetAddress ip, int port) {
    if (ip == null) throw new IllegalArgumentException("Null ip address");
    if (port < 0 || port > 65536) throw new IllegalArgumentException("Invalid port number");

    this.ip = ip;
    this.port = port;
  }

  public PeerNode(String host, int port) throws UnknownHostException{
    if (host == null) throw new IllegalArgumentException("Null host name");
    if (port < 0 || port > 65536) throw new IllegalArgumentException("Invalid port number");
    this.ip = InetAddress.getByName(host);
    this.port = port;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PeerNode)) return false;
    PeerNode p = (PeerNode) o;

    return (ip.equals(p.ip) && port == p.port);
  }

  @Override
  public int hashCode() {
    return (ip.hashCode() * 41) + (port * 41);
  }


  /**
   * Returns the node address
   *
   * @return InetAddress of the node
   */
  public InetAddress getInetAddress() {
    return ip;
  }

  /**
   * Returns the node port.
   *
   * @return port number of the node
   */
  public int getPort() {
    return port;
  }

  /*
   * Interface Node.isCloud() implementation
   */
  public boolean isCloud() {
    return false;
  }

  @Override
  public String toString() {
    return String.format("%s:%d", ip.getHostAddress(), port);
  }
}
