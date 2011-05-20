/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import cloudypeer.DynamicProviderHelper;

/**
 * This class defines standard methods to interact with a cloud storage provider and represent the
 * base class for all cloud storage providers. <br>
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public abstract class StorageCloud {

  public static final String PROVIDERS_CONFIGURATION = "cloudypeer_storagecloud.properties";

  /**
   * Map of SotrageCloud providers
   */
  private static Map<String, Class<? extends StorageCloud>> storageCloudProviders =
    DynamicProviderHelper.loadProvidersConfiguration(StorageCloud.class, PROVIDERS_CONFIGURATION);

  /* *********************************************************************
   * Implementation of instantiation methods
   ***********************************************************************/

  /**
   * Creates an instance of the specified StorageCloud.
   *
   * @param provider The storage cloud provider
   * @param cloudURI The URI of the cloud
   */
  public static StorageCloud getInstance(String provider, CloudURI cloudURI)
    throws InstantiationException
  {
    Class signature[] = {CloudURI.class};
    Object params[] = {cloudURI};


    return DynamicProviderHelper.newInstance(storageCloudProviders, provider, signature, params);
  }

  /* *********************************************************************
   * Abstract methods
   ***********************************************************************/


  /**
   * Returns the CloudURI that describe this cloud
   *
   * @return a <code>CloudURI</code> value
   */
  public abstract CloudURI getCloudURI();

  /**
   * Lists the key present on the cloud
   *
   * @param timestamp If not null, returns only keys fresher than the specified timestamp
   * @return Array of the keys present on the cloud
   * @exception IOException If an IO error occurs while handling the request
   * @exception CloudException If any other error occurs
   */
  public abstract String[] list(Date timestamp) throws IOException, CloudException;

  /**
   * Returns the object associated to this key on the cloud
   *
   * @param key The key to retrieve
   * @return The CloudObject associated to the key
   * @exception IOException If an IO error occurs while handling the request
   * @exception CloudException If any other error occurs
   */
  public abstract CloudObject get(String key) throws IOException, CloudException;

  /**
   * Returns the metadata associated to this key on the cloud
   *
   * @param key The key to retrieve
   * @return The CloudMetadata associated to the key
   * @exception IOException If an IO error occurs while handling the request
   * @exception CloudException If any other error occurs
   */
  public abstract CloudMetadata getMetadata(String key) throws IOException, CloudException;

  /**
   * Set the value of the specified key to the content of the input stream. If metadata is present
   * also the object metadata will be set.
   *
   * @param key Key to update
   * @param contentType Content type of this object
   * @param valueInputStream The input stream from which read the value
   * @param userMetadata The user metadata to associate to the key or null if not used
   * @exception IOException If an IO error occurs while handling the request
   * @exception CloudException If any other error occurs
   */
  public abstract void put(String key, String contentType, InputStream valueInputStream, Map<String, String> userMetadata)
    throws IOException, CloudException;

  /**
   * Sets the metadata associated to this key on the cloud
   *
   * @param key Key to update
   * @param contentType New content type
   * @param userMetadata New user metadata
   * @exception IOException If an IO error occurs while handling the request
   * @exception CloudException if an error occurs
   */
  public abstract void putMetadata(String key, String contentType, Map<String, String> userMetadata)
    throws IOException, CloudException;

  /**
   * Remove the specified key from the cloud.
   *
   * @param key Key to remove
   * @exception IOException If an IO error occurs while handling the request
   * @exception CloudException If any other error occurs
   */
  public abstract void remove(String key) throws IOException, CloudException;
}
