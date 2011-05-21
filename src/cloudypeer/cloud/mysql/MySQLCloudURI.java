/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud.mysql;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import cloudypeer.cloud.CloudURI;

/**
 * CloudURI implementation for MySQL cloud
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class MySQLCloudURI extends CloudURI {

  public static String SCHEME = "mysql";

  private URI uri;
  private URI baseURI;
  private String bucket;
  private String key;
  private String[] authInfo;

  /**
   * Creates a new <code>MySQLCloudURI</code> instance from an URI.
   *
   * @param cloudURI uri pointing to a cloud or cloud key.
   */
  public MySQLCloudURI(URI cloudURI) throws URISyntaxException {
    if (!cloudURI.getScheme().equals(SCHEME))
      throw new IllegalArgumentException("Not a valid MySQL cloud URI");

    String host = cloudURI.getHost();
    int port = cloudURI.getPort();
    String database = null;
    String table = null;
    String key = null;
    String username = null;
    String password = null;

    String components[];
    /* Parse the path in database, table and key */
    String path = cloudURI.getPath();
    if (!path.startsWith("/"))
      throw new IllegalArgumentException("URI path not absolute!");

    /* Fist spot always empty due to the leading slash */
    components = path.split("/");

    if (components.length < 2)
      throw new IllegalArgumentException("Too few path components");

    if (components.length > 4)
      throw new IllegalArgumentException("Too many path components");

    if (components.length > 1) database = components[1];
    if (components.length > 2) table = components[2];
    if (components.length > 3) key = components[3];

    /* Parse the user credentials */
    String userInfo = cloudURI.getUserInfo();
    ArrayList<String> userCredentials = new ArrayList<String>(2);
    if (userInfo != null) {
      components = userInfo.split(":");
      if (components.length > 0) userCredentials.add(components[0]);
      if (components.length > 1) userCredentials.add(components[1]);
      if (components.length > 2) throw new IllegalArgumentException("Invalid user credentials");
    }

    URI baseURI = new URI(SCHEME, null, host, port, "/" + database, null, null);
    String userCredentialsArray[] = userCredentials.toArray(new String[userCredentials.size()]);
    initCloudURI(baseURI.normalize(), table, key, userCredentialsArray);
  }

  public MySQLCloudURI(URI baseURI, String bucketTable, String key, String[] userCredentials)
    throws URISyntaxException
  {
    String host = baseURI.getHost();
    int port = baseURI.getPort();
    String database = null;

    String path = baseURI.getPath();
    if (path == null) throw new IllegalArgumentException("Path not specified");
    if (!path.startsWith("/")) throw new IllegalArgumentException("Path not absolute!");
    String components[] = path.split("/");
    if (components.length < 2) throw new IllegalArgumentException("Database not specified");
    database = components[1];

    URI finalBaseURI = new URI(SCHEME, null, host, port, "/" + database, null, null);
    initCloudURI(finalBaseURI.normalize(), bucketTable, key, userCredentials);
  }
}
