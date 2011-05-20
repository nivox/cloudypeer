/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.epidemicbcast.rumormongering;

import java.util.Map;

import cloudypeer.DynamicProviderHelper;
import cloudypeer.PeerNode;
import cloudypeer.PeerSelector;
import cloudypeer.epidemicbcast.EpidemicBroadcast;
import cloudypeer.store.Store;

/**
 * This class represent the base for all rumor mongering epidemic broadcast implementations. <br>
 * <br>
 *
 * Providers must implements the following constructors:
 * <ul>
 *  <li>ExampleProvider(Node localNode, PeerSelector peerSelector, Store store)</li>
 * </ul>
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class RumorMongeringBroadcast extends EpidemicBroadcast {

  public static final String PROVIDERS_CONFIGURATION = "cloudypeer_rumormongering.properties";
  public static final String DEFAULT_PROVIDER = "default";

  protected static final int DEFAULT_PERIOD = 5;

  /**
   * Map of RumorMongeringBroadcast providers
   */
  private static Map<String, Class<? extends RumorMongeringBroadcast>> antiEntropyProviders =
    DynamicProviderHelper.loadProvidersConfiguration(RumorMongeringBroadcast.class, PROVIDERS_CONFIGURATION);

  /* *********************************************************************
   * Instance variable
   ***********************************************************************/
  protected int persistence;

  /* *********************************************************************
   * Constructors implementation
   ***********************************************************************/

  /**
   * Creates a new <code>RumorMongeringBroadcast</code> instance using the default values
   *
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @param persistence The parameter that influence how much the protocol should persist in
   * spreading the news. (In the literature this is the k parameter)
   * @exception NullPointerException Raised if localNode is null
   */
  protected RumorMongeringBroadcast(PeerNode localNode, PeerSelector peerSelector,
                                    Store store, int persistence)
  {
    super(localNode, peerSelector, store, DEFAULT_PERIOD);
    this.persistence = persistence;
  }

  /* *********************************************************************
   * Implementation of instantiation methods
   ***********************************************************************/

  /**
   * Creates an instance of the default RumorMongeringBroadcast provider using the default values.
   *
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @param persistence The parameter that influence how much the protocol should persist in
   * spreading the news. (In the literature this is the k parameter)
   * @return An instance of the default RumorMongeringBroadcast instance
   * @exception RuntimeException If an error is encountered while loading the default provider
   */
  public static RumorMongeringBroadcast
    getDefaultInstance(PeerNode localNode, PeerSelector peerSelector,
                       Store store, int persistence)
  {
    try {
      return getInstance(DEFAULT_PROVIDER, localNode, peerSelector, store, persistence);
    } catch (Exception e) {
      throw new RuntimeException("Error loading the default anti entropy broadcast provider", e);
    }
  }

  /**
   * Creates an instance of the specified RumorMongeringBroadcast provider.
   *
   * @param provider The RumorMongeringBroadcast provider to load
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @param persistence The parameter that influence how much the protocol should persist in
   * spreading the news. (In the literature this is the k parameter)
   * @return An instance of the default RumorMongeringBroadcast instance
   * @exception RuntimeException If an error is encountered while loading the default provider
   */
  public static RumorMongeringBroadcast
    getInstance(String provider, PeerNode localNode, PeerSelector peerSelector,
                Store store, int persistence)
    throws InstantiationException
  {
    Class signature[] = {PeerNode.class, PeerSelector.class, Store.class, int.class};
    Object params[] = {localNode, peerSelector, store, persistence};

    return DynamicProviderHelper.newInstance(antiEntropyProviders, provider, signature, params);
  }

  /* *********************************************************************
   * Getters/Setters
   ***********************************************************************/

  /**
   * Sets the parameter that influence how much the protocol should persist at spreading the
   * news. <br>
   * In the paper <cite>Epidemic algorithms for replicated database maintenance</cite> by Demers et
   * al this is referred as the k parameter.
   *
   * @param persistence Persistence in spreading the news
   */
  public void setPersistence(int persistence) {
    this.persistence = persistence;
  }

  /**
   * Returns the parameter that influence how much the protocol should persist at spreading the
   * news. <br>
   * In the paper <cite>Epidemic algorithms for replicated database maintenance</cite> by Demers et
   * al this is referred as the k parameter.
   *
   * @param persistence Persistence in spreading the news
   */
  public int getPersistence() {
    return this.persistence;
  }
}
