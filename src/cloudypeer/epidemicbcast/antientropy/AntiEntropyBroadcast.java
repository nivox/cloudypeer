/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.epidemicbcast.antientropy;

import java.util.Map;

import cloudypeer.DynamicProviderHelper;
import cloudypeer.PeerNode;
import cloudypeer.PeerSelector;
import cloudypeer.epidemicbcast.EpidemicBroadcast;
import cloudypeer.store.Store;

/**
 * This class represent the base for all antientropy epidemic broadcast implementations. <br>
 * <br>
 *
 * Providers must implements the following constructors:
 * <ul>
 *  <li>ExampleProvider(Node localNode, PeerSelector peerSelector, Store store)</li>
 * </ul>
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class AntiEntropyBroadcast extends EpidemicBroadcast {

  public static final String PROVIDERS_CONFIGURATION = "cloudypeer_antientropy_providers.properties";
  public static final String DEFAULT_PROVIDER = "default";

  protected static final int DEFAULT_PERIOD = 30;

  /**
   * Map of AntiEntropyBroadcast providers
   */
  private static Map<String, Class<? extends AntiEntropyBroadcast>> antiEntropyProviders =
    DynamicProviderHelper.loadProvidersConfiguration(AntiEntropyBroadcast.class,
                                                     PROVIDERS_CONFIGURATION);

  /* *********************************************************************
   * Constructors implementation
   ***********************************************************************/

  /**
   * Creates a new <code>AntiEntropyBroadcast</code> instance using the default values
   *
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @exception NullPointerException Raised if localNode is null
   */
  protected AntiEntropyBroadcast(PeerNode localNode, PeerSelector peerSelector, Store store) {
    super(localNode, peerSelector, store, DEFAULT_PERIOD);
  }

  /* *********************************************************************
   * Implementation of instantiation methods
   ***********************************************************************/

  /**
   * Creates an instance of the default AntiEntropyBroadcast provider using the default values.
   *
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @return An instance of the default AntiEntropyBroadcast instance
   * @exception RuntimeException If an error is encountered while loading the default provider
   */
  public static AntiEntropyBroadcast
    getDefaultInstance(PeerNode localNode, PeerSelector peerSelector, Store store)
  {
    try {
      return getInstance(DEFAULT_PROVIDER, localNode, peerSelector, store);
    } catch (Exception e) {
      throw new RuntimeException("Error loading the default anti entropy broadcast provider", e);
    }
  }

  /**
   * Creates an instance of the specified AntiEntropyBroadcast provider.
   *
   * @param provider The AntiEntropyBroadcast provider to load
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @return An instance of the default AntiEntropyBroadcast instance
   * @exception RuntimeException If an error is encountered while loading the default provider
   */
  public static AntiEntropyBroadcast
    getInstance(String provider, PeerNode localNode, PeerSelector peerSelector, Store store)
    throws InstantiationException
  {
    Class signature[] = {PeerNode.class, PeerSelector.class, Store.class};
    Object params[] = {localNode, peerSelector, store};

    return DynamicProviderHelper.newInstance(antiEntropyProviders, provider, signature, params);
  }
}
