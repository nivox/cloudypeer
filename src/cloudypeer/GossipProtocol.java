/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * Base abstract class for all the gossip protocols. <br>
 * This class provides an easy way to implement gossip protocols which reflect the standard active +
 * passive thread skeleton. <br>
 * Each protocol must override the following methods:
 * <ul>
 *   <li><code>{@link #runActiveThread()}</code>
 *   <li><code>{@link #runPassiveThread()}</code>
 * </ul>
 * The code in these two methods run in separate threads. The method <code>isTerminated</code>
 * should be interrogated when appropriate to check for termination requests. <br>
 * Protocol specific parameters can be set via the <code>setProperties</code> methods.
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class GossipProtocol {

  private Thread activeThread;
  private Thread passiveThread;

  private volatile boolean terminated;
  private volatile boolean started;

  /**
   * Node descriptor used as local node by this gossip protocol instance
   */
  protected PeerNode localNode;

  /**
   * Defines the period of the active thread in seconds. <br>
   */
  protected int period;

  /**
   * Container for protocol specific parameters
   */
  protected Properties protocolConfiguration = new Properties();

  /**
   * Container for protocol specific data
   */
  protected HashMap<String, Object> protocolData = new HashMap<String, Object>();

  /**
   * Creates a new <code>GossipProtocol</code> instance.
   *
   * @param localNode Node descriptor to be used as local node
   * @param peeriod The period of the active thread of this gossip protocol
   * @exception NullPointerException Raised if localNode is null
   */
  protected GossipProtocol(PeerNode localNode, int period) {
    if (localNode == null) throw new NullPointerException("Null local node");

    this.terminated = false;
    this.started = false;
    this.period = period;

    this.activeThread = new Thread() {
        public void run() { runActiveThread(); }
      };

    this.passiveThread = new Thread() {
        public void run() { runPassiveThread(); }
      };

    this.localNode = localNode;
  }


  /************************************************************************
   *  Common methods implementation
   ************************************************************************/

  /**
   * Poll the interruption state. When this method returns a positive value, the calling thread
   * should clean up and terminate.
   *
   * @return True if the peer sampler was terminated
   */
  protected boolean isTerminated() {
    return terminated;
  }

  /**
   * Checks if the current peer sampler protocol was ever started. That is if the method start was
   * called.
   *
   * @return True if started, false otherwise
   */
  protected final boolean wasStarted() {
    return started;
  }

  /**
   * Set the termination flag for the gossip protocol and try to interrupts
   * (see {@link java.lang.Thread#interrupt()}) the protocol threads. <br>
   * <br>
   * Sub classes can override this methods but should always invoke it as the first operation
   * maintain consistency.
   *
   * @exception IllegalStateEception if the protocol has not been started yet
   */
  public void terminate() throws IllegalStateException{
    if (!wasStarted()) throw new IllegalStateException("Gossip protocol not started yet");

    terminated = true;
    activeThread.interrupt();
    passiveThread.interrupt();
  }


  /**
   * Starts the active thread of the current instance. <br>
   * Sub classes can override this method to alter this behavior (i.e. with an empty definition).
   */
  protected void startActiveThread() {
    activeThread.start();
  }

  /**
   * Starts the passive thread of the current instance. <br>
   * Sub classes can override this method to alter this behavior (i.e. with an empty definition).
   */
  protected void startPassiveThread() {
    passiveThread.start();
  }

  /**
   * Sub classes can override this method to setup auxiliary components not initializable at
   * construction time.
   *
   * @exception GossipProtocolException Raised if the configuration is not supported by this protocol
   * or another error happens while initializing the gossip protocol
   */
  protected void init() throws GossipProtocolException {};

  /**
   * Starts the peer sampler protocol. <br>
   * <br>
   * This methods simply finalize the protocol initialization and starts the active and passive
   * threads.
   * @exception GossipProtocolException Raised if the configuration is not supported by this protocol
   * or another error happens while initializing the gossip protocol
   * @exception IllegalStateException Raised if the gossip protocol was already started
   */
  public final void start() throws GossipProtocolException {
    if (wasStarted()) throw new IllegalStateException("Gossip protocol already started");
    this.started = true;
    init();
    startActiveThread();
    startPassiveThread();
  }

  /**
   * Implementation of the active thread of the gossip protocol. For all intents and purposes
   * this should be treated as {@link java.lang.Thread#run()}.<br>
   * This method should periodically check for the termination flag (see {@link #isTerminated()})
   * and act accordingly. <br>
   * <br>
   * Whenever the termination of the protocol is asked (see {@link #terminate()}) the thread
   * associated to this method will be interrupted (see {@link java.lang.Thread#interrupt()}).
   */
  protected abstract void runActiveThread();

  /**
   * Implementation of the passive thread of the gossip protocol. For all intents and
   * purposes this should be treated as {@link java.lang.Thread#run()}. <br>
   * This method should periodically check for the termination flag (see {@link #isTerminated()})
   * and act accordingly.<br>
   * <br>
   * Whenever the termination of the protocol is asked (see {@link #terminate()}) the thread
   * associated to this method will be interrupted (see {@link java.lang.Thread#interrupt()}).
   */
  protected abstract void runPassiveThread();


  /* **********************************************************************
   *  Getter/Setters
   * **********************************************************************/

  /**
   * Returns the node descriptor used as local node by this peer sampler instance.
   *
   * @return Current local node descriptor
   */
  public PeerNode getLocalNode() {
    return localNode;
  }

  /**
   * Sets the node descriptor to be used as local node by this peer sampler instance.
   *
   * @param localNode Local node descriptor
   * @exception IllegalStateException Raised if this PeerSampler instance was already running
   * @exception NullPointerException Raised if localNode is null
   */
  public void setLocalNode(PeerNode localNode) {
    if (wasStarted()) throw new IllegalStateException("Peersampler protocol already started");

    if (localNode == null) throw new NullPointerException("Null local node");
    this.localNode = localNode;
  }

  /**
   * Returns the current GossipProtocol period in seconds.
   *
   * @return PeerSampler period
   */
  public int getPeriod() {
    return period;
  }

  /**
   * Set the period (in seconds) to be used by the active thread.
   *
   * @param period Period in seconds
   * @exception IllegalStateException Raised if this gossip protocol instance was already running
   */
  public void setPeriod(int period) throws IllegalStateException {
    if (wasStarted()) throw new IllegalStateException("Gossip protocol already started");
    this.period = period;
  }

  /**
   * Returns the configuration currently set for the specific protocol instance.
   *
   * @return Protocol specific configuration
   */
  public Properties getProtocolConfiguration() {
    return this.protocolConfiguration;
  }

  /**
   * Sets the protocol specific configuration.
   *
   * @param conf Protocol specific configuration
   * @exception IllegalStateException Raised if this gossip protocol instance was already running
   */
  public void setProtocolConfiguration(Properties conf) {
    if (wasStarted()) throw new IllegalStateException("Gossip protocol already started");
    if (conf == null) throw new NullPointerException("Null protocol configuration");

    this.protocolConfiguration = conf;
  }

  /**
   * Tries to load the protocol configuration from the specified resource.<br>
   * First treats propertiesPath as a file path. If that fail tries to load the resource via the
   * classpath. <br>
   * The return value specify whether the resource was successfully loaded or not.
   *
   * @param propertiesPath Path of the properties file holding the protocol configuration
   * @return True on success, false if the resource cannot be found.
   * @exception NullPoionterException if propertiesPath is null
   * @exception IOException if there was an error reading the configuration
   * @exception IllegalArgumentException if the resource contains a malformed Unicode escape
   * sequence
   * @exception IllegalStateException Raised if this gossip protocol instance was already started
   */
  public boolean setProtocolConfiguration(String propertiesPath) throws IOException {
    if (wasStarted()) throw new IllegalStateException("Gossip protocol already started");

    File propertiesFile = new File(propertiesPath);
    boolean success = false;

    /* Try to load the properties via filesystem */

    FileInputStream fileIn = new FileInputStream(propertiesFile);

    protocolConfiguration.clear();
    try {
      protocolConfiguration.load(fileIn);
      success = true;
    } finally {
      try { fileIn.close(); } catch (IOException e) {}
    }
    if (success) return success;

    /* If that failed try load them from classpath */
    InputStream in = getClass().getResourceAsStream(propertiesPath);
    if (in == null) return false;
    try {
      protocolConfiguration.load(in);
      success = true;
    } finally {
      try { in.close(); } catch (IOException e) {}
    }

    return success;
  }


  public Object getProtocolData(String key) {
    return protocolData.get(key);
  }

  public void setProtocolData(String key, Object data) {
    protocolData.put(key, data);
  }
}
