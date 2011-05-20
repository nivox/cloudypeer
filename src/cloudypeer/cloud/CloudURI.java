/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import cloudypeer.DynamicProviderHelper;


/**
 * Cloud URI interface. <br>
 * This interface define the methods needed to interact in a transparent way with cloud URIs.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class CloudURI {

  public static final String PROVIDERS_CONFIGURATION = "cloudypeer_clouduri.properties";

  /**
   * Map of CloudURI providers
   */
  private static Map<String, Class<? extends CloudURI>> cloudURIProviders =
    DynamicProviderHelper.loadProvidersConfiguration(CloudURI.class, PROVIDERS_CONFIGURATION);


  /* *********************************************************************
   * Instance variables
   ***********************************************************************/
  protected URI uri;
  protected URI baseURI;
  protected String bucket;
  protected String key;
  protected String authInfo[];

  /* *********************************************************************
   * Implementation of instantiation methods
   ***********************************************************************/

  /**
   * Creates an instance of the specified StorageCloud.
   *
   * @param provider The cloudURI provider
   * @param cloudURI The URI of the cloud
   */
  public static CloudURI getInstance(String provider, URI cloudURI)
    throws InstantiationException
  {
    Class signature[] = {URI.class};
    Object params[] = {cloudURI};


    return DynamicProviderHelper.newInstance(cloudURIProviders, provider, signature, params);
  }

  /* *********************************************************************
   * Constructors
   ***********************************************************************/

  /**
   * Initialize this CloudURI instance.
   *
   * @param baseURI baseURI of the cloud.
   * @param bucket bucket or null
   * @param key key or null
   * @param authInfo authentication information
   */
  protected final void initCloudURI(URI baseURI, String bucket, String key, String authInfo[]) throws URISyntaxException {
    if (baseURI == null) throw new IllegalArgumentException("Missing cloud base URI");
    if (bucket == null && key != null)
      throw new IllegalArgumentException("Key specified without bucket!");

    String scheme = baseURI.getScheme();
    String host = baseURI.getHost();
    int port = baseURI.getPort();
    String path = baseURI.getPath();
    String userInfo = null;

    if (bucket != null) {
      path = String.format("%s/%s", path, bucket);
      if (key != null) path = String.format("%s/%s", path, key);
    }

    if (authInfo != null) {
      for (String c: authInfo) {
        if (userInfo == null) userInfo = c;
        else userInfo = String.format("%s:%s", userInfo, c);
      }
    }


    this.uri = new URI(scheme, userInfo, host, port, path, null, null).normalize();
    this.baseURI = baseURI;
    this.bucket = bucket;
    this.key = key;
    this.authInfo = authInfo;
  }

  /**
   * Creates a new <code>CloudURI</code> instance.
   */
  protected CloudURI() {}


  /**
   * Builds a general cloud uri from its basic components
   *
   * @param baseURI The base uri of the cloud provider
   * @param bucket The bucket to use
   * @param key The key to reference or null
   * @param authInfo The authentication info or null
   */
  public CloudURI(URI baseURI, String bucket, String key, String authInfo[]) {
    try {
      initCloudURI(baseURI, bucket, key, authInfo);
    } catch (URISyntaxException e) {
      throw new CloudException("Syntax error while creating cloud URI!", e);
    }
  }


  /* *********************************************************************
   * Abstract methods
   ***********************************************************************/

  /**
   * Returns the URI associated with this cloud descriptor.
   *
   * @return The URI representation of this cloud descriptor.
   */
  public URI getURI() {
    return uri;
  }

  /**
   * Returns the authentication information for this cloud descriptor or null if no such information
   * was provided.
   *
   * @return Authentication information
   */
  public String[] getAuthenticationInfo() {
    return authInfo;
  }

  /**
   * Returns the URI of the cloud without further information. That is the URI represented by this
   * instance without the bucket, key and authentication information.
   *
   * @return Cloud provider URI
   */
  public URI getBaseURI() {
    return baseURI;
  }

  /**
   * Returns the bucket pointed by this cloud descriptor.
   * <br>
   * A bucket is an account specific container. Different provider use different names for this
   * element:
   * <ul>
   *  <li><b>Amazon S3:<b> bucket </li>
   *  <li><b>Google Storage:<b> bucket </li>
   *  <li><b>Microsoft Azure Storage:<b> container </li>
   * </ul>
   *
   * @return Bucket pointed by this could descriptor
   */
  public String getBucket() {
    return bucket;
  }

  /**
   * Returns the key pointed by this cloud descriptor or null if this CloudURI don't point to
   * a specific key.
   *
   * @return Key pointed by this cloud descriptor
   */
  public String getKey() {
    return key;
  }
}
