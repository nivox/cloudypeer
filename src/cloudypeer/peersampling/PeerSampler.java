/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.peersampling;

import java.util.Map;

import cloudypeer.GossipProtocol;
import cloudypeer.Node;
import cloudypeer.PeerNode;


/**
 * Base abstract class for all the peer sampling algorithms. <br>
 * This class takes care of the common setup operations and let subclass define protocol specific
 * behaviors. <br>
 * <br>
 * Each protocol must override the following methods:
 * <ul>
 *   <li><code>{@link #addNode(Node)}</code>
 *   <li><code>{@link #removeNode(Node)}</code>
 *   <li><code>{@link #getView()}</code>
 * </ul>
 * If protocol designer want to support multiple providers for the same protocols they should
 * implements also the following static methods (where E denotes an instance of the protocol):
 * <ul>
 *   <li> E getDefaultInstance(...);
 *   <li> E getInstance(String provider, ...) throws InstantiationException;
 *   <li> String[] getProviderList();
 * </ul>
 * To handle providers configuration via properties file the convenience static method
 * {@link #loadProvidersConfiguration(Class, String)} is provided.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class PeerSampler extends GossipProtocol {

  protected static final int DEFAULT_VIEW = 20;
  protected static final int DEFAULT_PARTIAL_VIEW = 5;
  protected static final int DEFAULT_PERIOD = 10;

  /**
   * Defines the size of the node view. <br>
   * Defaults to {@value DEFAULT_VIEW}.
   */
  protected int viewSize = DEFAULT_VIEW;

  /**
   * Defines the size of the partial view sent to remote nodes. <br>
   * Defaults to {@value DEFAULT_PARTIAL_VIEW}.
   */
  protected int partialViewSize = DEFAULT_PARTIAL_VIEW;


  /* **********************************************************************
   *  Constructors
   ************************************************************************/

  /**
   * Initialize the common fields to the default values and setup the peersampler.
   *
   * @param localNode Node to be used by the peer sampler as local node
   * @exception NullPointerException Raised if localNode is null
   */
  protected PeerSampler(PeerNode localNode) {
    super(localNode, DEFAULT_PERIOD);
  }

  /**
   * Initialize the common fields and setup the peersampler.
   *
   * @param localNode Node to be used by the peer sampler as local node
   * @param viewSize size of the node's view
   * @param partialViewSize size of the partial view sent to remote nodes
   * @param period period of the active thread in seconds
   * @exception NullPointerException Raised if localNode is null
   */
  protected PeerSampler(PeerNode localNode, int viewSize, int partialViewSize, int period) {
    super(localNode, period);

    this.viewSize = viewSize;
    this.partialViewSize = partialViewSize;
  }


  /* **********************************************************************
   *  Abstract methods
   ************************************************************************/

  /**
   * Instruct the peer sampling protocol to add the specified node to the cache.
   *
   * @param n node descriptor to add
   * @return True on success, false otherwise
   * @exception PeerSamplerException Peer sampler protocol error
   * @exception IllegalStateException Raised when the peer sampler is not active
   */
  public abstract boolean addNode(Node n) throws PeerSamplerException;

  /**
   * Instruct the peer sampling protocol to remove the specified node from the cache.
   *
   * @param n node descriptor to remove
   * @return True on success, false otherwise
   * @exception PeerSamplerException Peer sampler protocol error
   * @exception IllegalStateException Raised when the peer sampler is not active
   */
  public abstract boolean removeNode(Node n) throws PeerSamplerException;

  /**
   * Retrieve the peer sampling protocol current view.
   *
   * @return Current network view
   * @exception PeerSamplerException Peer sampler protocol error
   * @exception IllegalStateException Raised when the peer sampler is not active
   */
  public abstract View getView() throws PeerSamplerException;

  /* **********************************************************************
   *  Getter/Setters
   * **********************************************************************/

  /**
   * Returns the peersampler maximum view size.
   *
   * @return peersampler max view size
   */
  public int getViewSize() {
    return viewSize;
  }

  /**
   * Sets the maximum size of the peersampler view
   *
   * @param viewSize peersampler max view size
   * @exception IllegalStateException Raised if this PeerSampler instance was already running
   */
  public void setViewSize(int viewSize) {
    if (wasStarted()) throw new IllegalStateException("Peersampler protocol already started");
    this.viewSize = viewSize;
  }

  /**
   * Returns the peersampler partial view size.
   *
   * @return peersampler partial view size
   */
  public int getPartialViewSize() {
    return viewSize;
  }

  /**
   * Sets the maximum size of the peersampler partial view
   *
   * @param viewSize peersampler partial view size
   * @exception IllegalStateException Raised if this PeerSampler instance was already running
   */
  public void setParialViewSize(int viewSize) {
    if (wasStarted()) throw new IllegalStateException("Peersampler protocol already started");
    this.viewSize = viewSize;
  }
}