/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.peersampling.cloudcast;

import java.util.Map;

import cloudypeer.DynamicProviderHelper;
import cloudypeer.PeerNode;
import cloudypeer.cloud.CloudURI;
import cloudypeer.peersampling.PeerSampler;


/**
 * CloudCast peer sampling protocol common interface. <br>
 * <br>
 *
 * Providers must implements the following constructors:
 * <ul>
 *  <li>ExampleProvider(Node localNode)</li>
 *  <li>ExampleProvider(Node localNode, int viewSize, int partialViewSize, int period,
 *                      int threshold)</li>
 * </ul>
 * See methods {@link #getInstance(String, Node)} and
 * {@link #getInstance(String, Node, int, int, int, int)} for further informations.
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class CloudCast extends PeerSampler{

  public static final String PROVIDERS_CONFIGURATION = "cloudypeer_cloudcast.properties";
  public static final String DEFAULT_PROVIDER = "default";

  private static final int DEFAULT_THRESHOLD = 4;

  /**
   * Map of CloudCast providers.
   */
  private static Map<String, Class<? extends CloudCast>> cloudCastProviders =
    DynamicProviderHelper.loadProvidersConfiguration(CloudCast.class, PROVIDERS_CONFIGURATION);

  /**
   * Defines the threshold parameter used by cloudcast. This parameters among other thing influences
   * the number of cloud node descriptor kept in the network. <br>
   * Defaults to {@value DEFAULT_THRESHOLD}.
   */
  protected int threshold = DEFAULT_THRESHOLD;


  /**
   * CloudURI of the cloud path to be used as view.
   */
  protected CloudURI cloudURI;


  /* *********************************************************************
   * Implementation of instantiation methods
   ***********************************************************************/

  /**
   * Creates an instance of the default CloudCast provider with the default parameters.
   *
   * @param localNode Node descriptor to be used as local node
   * @param cloud The CloudURI that identifies the cloud path to be used as view
   * @return An instance of the default CloudCast provider
   * @exception RuntimeException If an error is encountered while loading the default provider
   */
  public static CloudCast getDefaultInstance(PeerNode localNode, CloudURI cloud) {
    try {
      return getInstance(DEFAULT_PROVIDER, localNode, cloud);
    } catch (Exception e) {
      throw new RuntimeException("Error loading the default CloudCast provider", e);
    }
  }

  /**
   * Creates an instance of the default CloudCast provider with the specified parameters.
   *
   * @param localNode Node descriptor to be used as local node
   * @param cloud The CloudURI that identifies the cloud path to be used as view
   * @param viewSize The maximum size of the local peer view
   * @param partialViewSize The maximum size of the partial view sent to remote peers
   * @param period The period of the active thread of cloudcast
   * @param threshold The threshold value of the cloudcast protocol
   * @return An instance of the default CloudCast provider
   * @exception RuntimeException If an error is encountered while loading the default provider
   */
  public static CloudCast getDefaultInstance(PeerNode localNode, CloudURI cloud, int viewSize,
                                             int partialViewSize, int period, int threshold)
  {
    try {
      return getInstance(DEFAULT_PROVIDER, localNode, cloud, viewSize, partialViewSize, period, threshold);
    }catch (Exception e) {
      throw new RuntimeException("Error loading the default CloudCast provider", e);
    }
  }


  /**
   * Creates an instance of the specified CloudCast provider with the default parameters.
   *
   * @param provider CloudCast provider to instantiate
   * @param localNode Node descriptor to be used as local node
   * @param cloud The CloudURI that identifies the cloud path to be used as view
   * @return An instance of the specifies CloudCast provider
   * @exception InstantiationException If an error occurs while instantiating the CloudCast
   * provider. Usually indicates a bad implementation of the provider itself.
   * @exception IllegalArgumentException If the specified provider is not configured
   */
  public static CloudCast getInstance(String provider, PeerNode localNode, CloudURI cloud)
    throws InstantiationException
  {
    Class signature[] = {PeerNode.class, CloudURI.class};
    Object params[] = {localNode, cloud};

    return DynamicProviderHelper.newInstance(cloudCastProviders, provider, signature, params);
  }

  /**
   * Creates an instance of the specified CloudCast provider with the default parameters.
   *
   * @param provider CloudCast provider to instantiate
   * @param localNode Node descriptor to be used as local node
   * @param cloud The CloudURI that identifies the cloud path to be used as view
   * @param viewSize The maximum size of the local peer view
   * @param partialViewSize The maximum size of the partial view sent to remote peers
   * @param period The period of the active thread of cloudcast
   * @param threshold The threshold value of the cloudcast protocol
   * @return An instance of the specifies CloudCast provider
   * @exception InstantiationException If an error occurs while instantiating the CloudCast
   * provider. Usually indicates a bad implementation of the provider itself.
   * @exception IllegalArgumentException If the specified provider is not configured
   */
  public static CloudCast getInstance(String provider, PeerNode localNode, CloudURI cloud,
                                      int viewSize, int partialViewSize, int period, int threshold)
    throws InstantiationException
  {
    Class signature[] = {PeerNode.class, CloudURI.class, int.class, int.class, int.class, int.class};
    Object params[] = {localNode, cloud, viewSize, partialViewSize, period, threshold};

    return DynamicProviderHelper.newInstance(cloudCastProviders, provider, signature, params);
  }


  /* *********************************************************************
   * Implementation of constructors
   ***********************************************************************/

  /**
   * Creates an instance of CloudCast and initialize the underlying PeerSampler class using the
   * default values.
   *
   * @param localNode Node descriptor used as local node by this peer sampler instance
   * @param cloud The CloudURI that identifies the cloud path to be used as view
   * @exception NullPointerException If cloud is null or localNode is null
   */
  protected CloudCast(PeerNode localNode, CloudURI cloud) {
    super(localNode);

    if (cloud == null) throw new NullPointerException("Null cloud URI reference");
    this.cloudURI = cloud;
  }

  /**
   * Creates an instance of CloudCast and initialize the underlying PeerSampler class.
   *
   * @param localNode Node descriptor used as local node by this peer sampler instance
   * @param cloud The CloudURI that identifies the cloud path to be used as view
   * @param viewSize size of the node's view
   * @param partialViewSize size of the partial view sent to remote nodes
   * @param period period of the active thread in seconds
   * @param threshold Cloudcast's threshold parameter value
   * @exception NullPointerException If cloud is null or localNode is null
   */
  protected CloudCast(PeerNode localNode, CloudURI cloud, int viewSize, int partialViewSize, int period, int threshold) {
    super(localNode, viewSize, partialViewSize, period);

    if (cloud == null) throw new NullPointerException("Null cloud URI reference");
    this.cloudURI = cloud;
    this.threshold = threshold;
  }
}
