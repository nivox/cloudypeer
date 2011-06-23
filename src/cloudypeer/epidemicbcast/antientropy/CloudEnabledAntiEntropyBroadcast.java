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
 *  <li>ExampleProvider(Node localNode, PeerSelector peerSelector, Store store, Store store)</li>
 * </ul>
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class CloudEnabledAntiEntropyBroadcast extends EpidemicBroadcast {

  public static final String PROVIDERS_CONFIGURATION = "cloudypeer_cloudantientropy.properties";
  public static final String DEFAULT_PROVIDER = "default";

  protected static final int DEFAULT_PERIOD = 30;

  /**
   * Map of CloudEnabledAntiEntropyBroadcast providers
   */
  private static Map<String, Class<? extends CloudEnabledAntiEntropyBroadcast>> antiEntropyProviders =
    DynamicProviderHelper.loadProvidersConfiguration(CloudEnabledAntiEntropyBroadcast.class,
                                                     PROVIDERS_CONFIGURATION);

  /* *********************************************************************
   * Instance variables
   ***********************************************************************/

  /**
   * Store instance that backs the cloud
   */
  protected Store cloudStore;

  /* *********************************************************************
   * Constructors implementation
   ***********************************************************************/

  /**
   * Creates a new <code>CloudEnabledAntiEntropyBroadcast</code> instance using the default values
   *
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @param cloudStore Store instance that represent the cloud
   * @exception NullPointerException Raised if localNode is null
   */
  protected CloudEnabledAntiEntropyBroadcast(PeerNode localNode, PeerSelector peerSelector, Store store,
                                    Store cloudStore)
  {
    super(localNode, peerSelector, store, DEFAULT_PERIOD);
    this.cloudStore = cloudStore;
  }

  /* *********************************************************************
   * Implementation of instantiation methods
   ***********************************************************************/

  /**
   * Creates an instance of the default CloudEnabledAntiEntropyBroadcast provider using the default values.
   *
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @param cloudStore Store instance that represent the cloud
   * @return An instance of the default CloudEnabledAntiEntropyBroadcast instance
   * @exception RuntimeException If an error is encountered while loading the default provider
   */
  public static CloudEnabledAntiEntropyBroadcast
    getDefaultInstance(PeerNode localNode, PeerSelector peerSelector, Store store, Store cloudStore)
  {
    try {
      return getInstance(DEFAULT_PROVIDER, localNode, peerSelector, store, cloudStore);
    } catch (Exception e) {
      throw new RuntimeException("Error loading the default anti entropy broadcast provider", e);
    }
  }

  /**
   * Creates an instance of the specified CloudEnabledAntiEntropyBroadcast provider.
   *
   * @param provider The CloudEnabledAntiEntropyBroadcast provider to load
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @param cloudStore Store instance that represent the cloud
   * @return An instance of the default CloudEnabledAntiEntropyBroadcast instance
   * @exception RuntimeException If an error is encountered while loading the default provider
   */
  public static CloudEnabledAntiEntropyBroadcast
    getInstance(String provider, PeerNode localNode, PeerSelector peerSelector, Store store,
                Store cloudStore) throws InstantiationException
  {
    Class signature[] = {PeerNode.class, PeerSelector.class, Store.class, Store.class};
    Object params[] = {localNode, peerSelector, store, cloudStore};

    return DynamicProviderHelper.newInstance(antiEntropyProviders, provider, signature, params);
  }

  /* *********************************************************************
   * Getters/Setters
   ***********************************************************************/
  public Store getCloudStore() {
    return cloudStore;
  }

  public void setCloudStore(Store cloudStore) {
    if (wasStarted())
      throw new IllegalStateException("Cloud antientropy broadcast protocol already started");

    this.cloudStore = cloudStore;
  }
}
