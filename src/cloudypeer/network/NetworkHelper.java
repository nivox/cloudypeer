/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloudypeer.DynamicProviderHelper;
import cloudypeer.PeerNode;
import org.apache.log4j.Logger;

/**
 * Base abstract class for all NetworkHelper implementation
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class NetworkHelper {

  static Logger logger = Logger.getLogger(NetworkHelper.class);

  public static final String PROVIDERS_CONFIGURATION = "cloudypeer_networkhelper.properties";
  public static final String DEFAULT_PROVIDER = "default";

  /**
   * Map of NetworkHelper providers
   */
  private static Map<String, Class<? extends NetworkHelper>> netHelpersProviders =
    DynamicProviderHelper.loadProvidersConfiguration(NetworkHelper.class, PROVIDERS_CONFIGURATION);

  /**
   * NetworkHelper instance to be returned to client.
   */
  private static NetworkHelper configuredInstance;

  /* *********************************************************************
   * Instance variables
   ***********************************************************************/

  /**
   * Used to signal the termination.
   */
  private volatile boolean terminated;

  /**
   * The local node represented by this NetworkHelper.
   */
  protected PeerNode localNode;

  /**
   * Map which relates ClientIDs to actual NetworkClient instances
   */
  private Map<Integer, NetworkClient> clientMap = new HashMap<Integer, NetworkClient>();

  /**
   * Map which relates actual NetworkClient instances to ClientIDs
   */
  private Map<NetworkClient, Integer> reverseClientMap = new HashMap<NetworkClient, Integer>();

  /**
   * Map which relates NetworkClients to incoming connections
   */
  private Map<Integer, List<NetworkConnection>> connectionMap = new HashMap<Integer, List<NetworkConnection>>();

  /* *********************************************************************
   * Constructors implementation
   ***********************************************************************/

  /**
   * Returns the instance configured to be used by NetworkClients or null if no instance was configured.
   *
   * @return Configured instance.
   */
  public static final NetworkHelper getConfiguredInstance() {
    return configuredInstance;
  }

  /**
   * Configures the default provider as the instance to be used by NetworkClients.
   *
   * @param addr InetAddress to use
   * @param port Port to bound
   * @return Configured NetworkHelper
   * @exception IOException If an IO error occurs
   * @exception RuntimeException If an error occurs while loading the default provider
   */
  public synchronized static final NetworkHelper configureDefaultInstance(InetAddress addr, int port)
    throws IOException, RuntimeException
  {
    if (configuredInstance != null)
      throw new IllegalStateException("Configured instance already present");

    try {
      return configureInstance(DEFAULT_PROVIDER, addr, port);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Error loading the default network helper provider", e);
    }
  }

  /**
   * Configures the specified provider as the instance to be used by NetworkClients.
   *
   * @param provider Provider to load
   * @param addr InetAddress to use
   * @param port Port to bound
   * @return Configured NetworkHelper
   * @exception InstantiationException If an error occurs while loading the specified provider
   * @exception IOException IF an IO error occurs
   */
  public synchronized static NetworkHelper configureInstance(String provider, InetAddress addr,
                                                             int port)
    throws InstantiationException, IOException
  {
    if (configuredInstance != null)
      throw new IllegalStateException("Configured instance already present");

    configuredInstance = getInstance(provider, addr, port);
    return configuredInstance;
  }

  /**
   * Returns an instance of the default NetworkHelper provider.
   *
   * @param addr InetAddress to use
   * @param port Port to bound
   * @return A NetworkHelper instance
   * @exception IOException If an IO error occurs
   * @exception RuntimeException If an error occurs while loading the default provider
   */
  public static NetworkHelper getDefaultInstance(InetAddress addr, int port) throws IOException {
    try {
      return getInstance(DEFAULT_PROVIDER, addr, port);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Error loading the default network helper provider", e);
    }
  }

  /**
   * Returns an instance of the specified NetworkHelper provider.
   *
   * @param provider Provider to load
   * @param addr InetAddress to use
   * @param port Port to bound
   * @return Configured NetworkHelper
   * @exception InstantiationException If an error occurs while loading the specified provider
   * @exception IOException IF an IO error occurs
   */
  public static NetworkHelper getInstance(String provider, InetAddress addr, int port)
    throws InstantiationException, IOException
  {
    Class signature[] = {InetAddress.class, int.class};
    Object params[] = {addr, port};

    return DynamicProviderHelper.newInstance(netHelpersProviders, provider, signature, params);
  }

  /**
   * Creates a new <code>NetworkHelper</code> instance.
   *
   * @param addr Address to use
   * @param port Port to bound
   */
  protected NetworkHelper(InetAddress addr, int port) {
    this.localNode = new PeerNode(addr, port);
    this.terminated = false;
  }

  /* *********************************************************************
   * Common methods implementation
   ***********************************************************************/

  /**
   * Terminates this NetworkHelper instance
   */
  public final void terminate() {
    terminated = true;
    terminateImpl();
  }

  /**
   * Checks whether this instance was terminated
   *
   * @return True if this instance was terminated
   */
  public final boolean isTerminated() {
    return terminated;
  }

  /**
   * Return the node descriptor associated to this NetworkHelper instance.
   *
   * @return a <code>PeerNode</code> value
   */
  public PeerNode getLocalNode() {
    return localNode;
  }

  /**
   * Register a NetworkClient with this NetworkHelper instance.
   *
   * @param c NetworkClient to register
   * @param clientID ID to associate with the NetworkClient
   * @exception IllegalArgumentException if an error occurs
   */
  public void registerClient(NetworkClient c, int clientID) throws IllegalArgumentException {
    synchronized(clientMap) {
      if (reverseClientMap.containsKey(c))
        throw new IllegalArgumentException("Network client already registered");

      if (clientMap.containsKey(clientID))
        throw new IllegalArgumentException("ClientID already bound");

      clientMap.put(clientID, c);
      reverseClientMap.put(c, clientID);
    }
  }


  /**
   * Unregister a NetworkClient.
   *
   * @param c NetworkClient to unregister
   */
  public void unregisterClient(NetworkClient c) {
    synchronized(clientMap) {
      int id = reverseClientMap.remove(c);
      clientMap.remove(id);
    }
  }

  /**
   * Returns the clientID associated with the specified client or null if the clientID is not bound.
   *
   * @param clientID The clientID to retrieve
   * @return An associated NetworkClient or null
   */
  private NetworkClient getClientForID(int clientID) {
    synchronized(clientMap) {
      return clientMap.get(clientID);
    }
  }

  /**
   * Returns the clientID associated to an instance of NetworkClient or null.
   *
   * @param client NetworkClient to retrieve
   * @return The clientID or null
   */
  private Integer getIDForClient(NetworkClient client) {
    synchronized(clientMap) {
      return reverseClientMap.get(client);
    }
  }

  /**
   * Dispatch the message to the registered protocol
   * @param msg Message to dispatch
   */
  protected void dispatchDatagramMessage(NetworkMessage msg) {
    NetworkClient client = getClientForID(msg.getClientID());

    if (client != null)
      client.processMessage(msg.getSource(), msg.getMessage());
  }

  /**
   * Returns whether the client is ready to accept a connection in this instant.
   *
   * @param clientID ID of the client
   * @return True if ready
   */
  protected boolean clientReadyForConnection(int clientID) {
    return connectionMap.containsKey(clientID);
  }

  /**
   * Dispatch the incoming connection to the correct NetworkClient.
   *
   * @param clientID ID of the NetworkClient for which this connection is intended
   * @param conn Connection to pass to the NetworkClient
   * @return True if the associated NetworkClient has enabled incoming connection
   */
  protected boolean dispatchConnection(int clientID, NetworkConnection conn) {
    logger.trace("Dispatching connection for client id: " + clientID);
    List<NetworkConnection> connList;
    synchronized(connectionMap) {
      if (!connectionMap.containsKey(clientID)) {
        logger.trace(String.format("Client id %d not ready in connection map", clientID));
        return false;
      }
      connList = connectionMap.get(clientID);
    }

    synchronized (connList) {
      connList.add(conn);
      logger.trace(String.format("Connection dispatched to client id %d", clientID));
      connList.notifyAll();
    }
    return true;
  }

  /**
   * Wait for an incoming connection and accepts it.
   *
   * @param client NetworkClient responsible for the connection.
   * @return Incoming connection
   * @exception InterruptedException If the operation is interrupted
   */
  public NetworkConnection acceptConnection(NetworkClient client)
    throws InterruptedException {
    Integer clientID = getIDForClient(client);
    logger.trace(String.format("Preparing client id %d for connection", clientID));
    if (clientID == null) {
      logger.warn(String.format("Error accepting connection. Client id %d not registered", clientID));
      throw new IllegalArgumentException("Client not registered");
    }

    List<NetworkConnection> connList = new ArrayList<NetworkConnection>(1);
    synchronized(connectionMap) {
      connectionMap.put(clientID, connList);
    }

    logger.trace(String.format("Client id %d ready for connection", clientID));

    NetworkConnection conn = null;
    try {
      synchronized (connList) {
        if (connList.size() == 0) connList.wait();
        if (connList.size() > 0) conn = connList.remove(0);
      }
    } finally {
      synchronized(connectionMap) {
        connList.remove(clientID);
      }
    }

    return conn;
  }

  /**
   * Sends a datagram message to a peer
   *
   * @param client NetworkClient responsible for this message
   * @param destination The peer to send the message to
   * @param message The actual message to send
   * @exception IOException If an IO error occurs
   * @exception IllegalArgumentException If the client is not registered
   * @exception NetworkException If an error occur processing the message
   */
  public void sendDatagraMessage(NetworkClient client, PeerNode destination, Serializable message)
    throws IOException, IllegalArgumentException, NetworkException {
    if (destination == null || client == null || message == null)
      throw new IllegalArgumentException("Null parameter");

    Integer clientID = getIDForClient(client);
    if (clientID == null) throw new IllegalArgumentException("Client not registered");

    NetworkMessage msg = new NetworkMessage(client.getNode(), clientID, message);
    sendDatagramMessageImpl(destination, msg);
  }

  /**
   * Creates a connection to a peer
   *
   * @param client NetworkClient responsible for this connection
   * @param endpoint The peer with which establish the connection
   * @param timeout Timeout after which abort
   * @return A NetworkConnection instance
   * @exception IOException if an error occurs
   * @exception IllegalArgumentException If the client is not registered
   * @exception NetworkException If an error occur creating the connection
   */
  public NetworkConnection createConnection(NetworkClient client, PeerNode endpoint, int timeout)
    throws IOException, IllegalArgumentException, NetworkException
  {
    if (endpoint == null || client == null)
      throw new IllegalArgumentException("Null parameter");

    Integer clientID = getIDForClient(client);
    if (clientID == null) throw new IllegalArgumentException("Client not registered");

    return createConnectionImpl(endpoint, clientID, timeout);
  }

  /* *********************************************************************
   * Abstract methods declarations
   ***********************************************************************/

  /**
   * Send message implementation
   *
   * @param destination Destination of the message
   * @param message Actual message
   * @exception IOException if an error occurs
   * @exception NetworkException if an error occurs
   */
  protected abstract void sendDatagramMessageImpl(PeerNode destination, NetworkMessage message)
    throws IOException, NetworkException;

  /**
   * Create connection implementation
   * @param endpoint Endpoint of the connection
   * @param clientID ID of the NetworkClient which is responsible for the connection
   * @param timeout Timeout after which abort
   */
  protected abstract NetworkConnection createConnectionImpl(PeerNode endpoint, int clientID,
                                                            int timeout)
    throws IOException, NetworkException, SocketTimeoutException;

  /**
   * Starts this NetworkHelper instance
   */
  public abstract void start() throws IOException, NetworkException;

  /**
   * This method can be overridden by providers to perform any termination code.
   */
  protected abstract void terminateImpl();
}
