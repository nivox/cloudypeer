/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.epidemicbcast;

import cloudypeer.GossipProtocol;
import cloudypeer.PeerNode;
import cloudypeer.PeerSelector;
import cloudypeer.store.Store;

/**
 * Defines the base class for all the epidemic brroadcast protocols.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class EpidemicBroadcast extends GossipProtocol {
  protected PeerSelector peerSelector;
  protected Store store;

  /**
   * Creates a new <code>EpidemicBroadcast</code> instance using default values. <br>
   *
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @param period The period of the active thread of this epidemic broadcast protocol
   */
  protected EpidemicBroadcast(PeerNode localNode, PeerSelector peerSelector, Store store, int period) {
    super(localNode, period);
    this.peerSelector = peerSelector;
    this.store = store;
  }

  /* **********************************************************************
   *  Getter/Setters
   * **********************************************************************/

  /**
   * Gets the store currently in use by this epidemic broadcast protocol.
   *
   * @return Store in use
   */
  public Store getStore() {
    return this.store;
  }

  /**
   * Sets the store to be used by this epidemic broadcast protocol instance.
   *
   * @param store Store to be used
   * @exception IllegalStateException Raised if this EpidemicBroadcast instance was already running
   */
  public void setStore(Store store) {
    if (wasStarted())
      throw new IllegalStateException("Epidemic broadcast protocol already started");

    this.store = store;
  }

  /**
   * Gets the peer selector currently in use by this epidemic broadcast protocol.
   *
   * @return Peer selector currently in use
   */
  public PeerSelector getPeerSelector() {
    return this.peerSelector;
  }

  /**
   * Sets the peer selector to be used by this epidemic broadcast protocol instance.
   *
   * @param peerSelector Peer selector to be used
   * @exception IllegalStateException Raised if this EpidemicBroadcast instance was already running
   */
  public void setPeerSelector(PeerSelector peerSelector) {
    if (wasStarted())
      throw new IllegalStateException("Epidemic broadcast protocol already started");

    this.peerSelector = peerSelector;
  }
}
