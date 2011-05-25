/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud.mysql;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import cloudypeer.cloud.CloudException;
import cloudypeer.cloud.CloudMetadata;
import cloudypeer.cloud.CloudObject;
import cloudypeer.cloud.CloudURI;
import cloudypeer.cloud.StorageCloud;
import cloudypeer.utils.MD5InputStream;
import org.apache.log4j.Logger;


/**
 * MySQL cloud implementation. <br>
 * This storage cloud can operate on any MySQL database. Each table is treated as a different
 * bucket. The format of a table used as bucket MUST respect the following structure:
 * <ul>
 *   <li>{@value #FIELD_NAME_KEY}: VARCHAR, primary key</li>
 *   <li>{@value #FIELD_NAME_VALUE}: BLOB </li>
 *   <li>{@value #FIELD_NAME_LAST_MODIFIED}: INT (stored as seconds)</li>
 *   <li>{@value #FIELD_NAME_CONTENT_LENGTH}: INT</li>
 *   <li>{@value #FIELD_NAME_CONTENT_MD5}: VARCHAR</li>
 *   <li>{@value #FIELD_NAME_CONTENT_TYPE}: VARCHAR</li>
 * <ul>
 * The table can have other fields but they must be <b>nullable</b>
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class MySQLStorageCloud extends StorageCloud {

  static Logger logger = Logger.getLogger(MySQLStorageCloud.class);

  protected static String FIELD_NAME_KEY = "cloud_key";
  protected static String FIELD_NAME_VALUE = "cloud_value";
  protected static String FIELD_NAME_LAST_MODIFIED = "cloud_timestamp";
  protected static String FIELD_NAME_CONTENT_LENGTH = "cloud_content_length";
  protected static String FIELD_NAME_CONTENT_MD5 = "cloud_content_md5";
  protected static String FIELD_NAME_CONTENT_TYPE = "cloud_content_type";

  private MySQLCloudURI mysqlCloudURI;
  private String jdbcURL;

  /* *********************************************************************
   * Constructor implementation
   ***********************************************************************/
  public MySQLStorageCloud (CloudURI cloudURI) throws IllegalArgumentException, CloudException {
    if (!(cloudURI instanceof MySQLCloudURI))
      throw new IllegalArgumentException("CloudURI not supported");

    /* Load MySQL jdbc driver */
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch(ClassNotFoundException e) {
      throw new CloudException("Cannot load MySQL cloud driver", e);
    }

    /* Preprocess authentication information */
    String credentials[] = cloudURI.getAuthenticationInfo();
    String authInfo[] = new String[2];
    if (credentials != null && credentials.length > 0 && credentials[0] != null)
      authInfo[0] = credentials[0];
    else
      throw new IllegalArgumentException("MySQL username not specified!");

    if (credentials != null && credentials.length > 1 && credentials[1] != null)
      authInfo[1] = credentials[1];
    else
      authInfo[1] = "";

    /* Generate final cloud and jdbc URIs */
    try {
      mysqlCloudURI = new MySQLCloudURI(cloudURI.getBaseURI(), cloudURI.getBucket(), null, authInfo);
    } catch (URISyntaxException e) {
      throw new CloudException("This should not be happening!", e);
    }
    jdbcURL = String.format("jdbc:%s", mysqlCloudURI.getBaseURI().toString());
  }

  /* *********************************************************************
   * Common method implementation
   ***********************************************************************/
  protected Connection getConnection() {
    String credentials[] = mysqlCloudURI.getAuthenticationInfo();

    Connection conn;
    try {
      conn = DriverManager.getConnection(jdbcURL, credentials[0], credentials[1]);
    } catch (SQLException e) {
      throw new CloudException("Cannot create connection to the database", e);
    }
    return conn;
  }

  /* *********************************************************************
   * StorageCloud abstract methods implementation
   ***********************************************************************/
  /*
   * Implementation of abstract method getCloudURI
   */
  public CloudURI getCloudURI() {
    try {
      return new MySQLCloudURI(mysqlCloudURI.getBaseURI(), mysqlCloudURI.getBucket(), null, null);
    } catch (URISyntaxException e) {
      throw new CloudException("This should not be happening!", e);
    }
  }


  /*
   * Implementation of abstract method get
   */
  public CloudObject get(String key) throws CloudException {
    String query = String.format("SELECT %s, %s, %s, %s, %s from %s WHERE %s='%s'",
                                 FIELD_NAME_VALUE,
                                 FIELD_NAME_CONTENT_LENGTH,
                                 FIELD_NAME_LAST_MODIFIED,
                                 FIELD_NAME_CONTENT_MD5,
                                 FIELD_NAME_CONTENT_TYPE,
                                 mysqlCloudURI.getBucket(),
                                 FIELD_NAME_KEY, key);

    InputStream in = null;
    long lastModified = 0;
    String contentMD5 = null;
    long contentLength = 0;
    String contentType = null;

    Connection conn = getConnection();
    Statement stmt = null;
    ResultSet result = null;
    try {
      stmt = conn.createStatement();
      result = stmt.executeQuery(query);

      if (result.next()) {
        lastModified = result.getLong(FIELD_NAME_LAST_MODIFIED);
        contentLength = result.getLong(FIELD_NAME_CONTENT_LENGTH);
        contentMD5 = result.getString(FIELD_NAME_CONTENT_MD5);
        contentType = result.getString(FIELD_NAME_CONTENT_TYPE);
        in = result.getBinaryStream(FIELD_NAME_VALUE);
      }
    } catch (SQLException e) {
      try {
        if (result != null) result.close();
      } catch (SQLException ex) {}

      try {
        if(stmt != null) stmt.close();
      }  catch (SQLException ex) {}

      try {
        if (conn != null) conn.close();
      } catch (SQLException ex) {}

      throw new CloudException("Error retrieving metadata", e);
    }

    MySQLCloudObjectInputStream mysqlIn;
    mysqlIn = new MySQLCloudObjectInputStream(conn, stmt, result, in);

    MySQLCloudMetadata meta = new MySQLCloudMetadata(contentLength, new Date(lastModified * 1000),
                                                     contentMD5, contentType);
    MySQLCloudURI uri;
    try {
      uri = new MySQLCloudURI(mysqlCloudURI.getBaseURI(), mysqlCloudURI.getBucket(), key, null);
      return new MySQLCloudObject(uri, mysqlIn, meta);
    } catch (URISyntaxException e) {
      try {
        mysqlIn.close();
      } catch (IOException ex) {}
      throw new CloudException("Error creating object cloud URI", e);
    }
  }

  /*
   * Implementation of abstract method getMetadata
   */
  public CloudMetadata getMetadata(String key) throws CloudException {
    String query = String.format("SELECT %s, %s, %s, %s from %s WHERE %s='%s'",
                                 FIELD_NAME_CONTENT_LENGTH,
                                 FIELD_NAME_LAST_MODIFIED,
                                 FIELD_NAME_CONTENT_MD5,
                                 FIELD_NAME_CONTENT_TYPE,
                                 mysqlCloudURI.getBucket(),
                                 FIELD_NAME_KEY, key);

    long lastModified = 0;
    String contentMD5 = null;
    long contentLength = 0;
    String contentType = null;

    Connection conn = getConnection();
    Statement stmt = null;
    ResultSet result = null;
    try {
      stmt = conn.createStatement();
      result = stmt.executeQuery(query);

      if (result.next()) {
        lastModified = result.getLong(FIELD_NAME_LAST_MODIFIED);
        contentLength = result.getLong(FIELD_NAME_CONTENT_LENGTH);
        contentMD5 = result.getString(FIELD_NAME_CONTENT_MD5);
        contentType = result.getString(FIELD_NAME_CONTENT_TYPE);
      } else return null;
    } catch (SQLException e) {
      throw new CloudException("Error retrieving metadata", e);
    } finally {
      try {
        if (result != null) result.close();
      } catch (SQLException e) {}

      try {
        if (stmt != null) stmt.close();
      } catch (SQLException e) {}

      try {
        if (conn != null) conn.close();
      } catch (SQLException e) {}
    }

    return new MySQLCloudMetadata(contentLength, new Date(lastModified * 1000),
                                  contentMD5, contentType);
  }

  public void remove(String key) throws CloudException {
    Connection conn = getConnection();
    Statement stmt = null;
    try {
      stmt = conn.createStatement();
      String query = String.format("DELETE from %s WHERE %s='%s'", mysqlCloudURI.getBucket(),
                                   FIELD_NAME_KEY, key);

      stmt.executeUpdate(query);
    } catch (SQLException e) {
      throw new CloudException("Error performing delete operation", e);
    } finally {
      try {
        if (stmt != null) stmt.close();
      } catch (SQLException e) {}

      try {
        conn.close();
      } catch (SQLException e) {}
    }
  }

  public void put(String key, String contentType, InputStream valueInputStream,
                  Map<String, String> userMetadata) throws CloudException
  {
    logger.info("Putting entry: " + key);
    long timestamp = System.currentTimeMillis() / 1000;

    /* Query that setup the key on the cloud */
    String prepareQuery = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) " +
                                        "VALUES ('%s', NULL, %d, 0, '0', '') ON DUPLICATE KEY UPDATE " +
                                        " %4$s=%9$d",
                                        mysqlCloudURI.getBucket(),
                                        FIELD_NAME_KEY, FIELD_NAME_VALUE, FIELD_NAME_LAST_MODIFIED,
                                        FIELD_NAME_CONTENT_LENGTH, FIELD_NAME_CONTENT_MD5,
                                        FIELD_NAME_CONTENT_TYPE, key, timestamp);

    /* Query that updates the value */
    String valueQuery = String.format("UPDATE %s SET %s=? WHERE %s='%s'",
                                      mysqlCloudURI.getBucket(),
                                      FIELD_NAME_VALUE, FIELD_NAME_KEY, key);

    /* Query that updates the metadata */
    String metadataQuery = String.format("UPDATE %s SET %s=?, %s=?, %s='%s' WHERE %s='%s'",
                                         mysqlCloudURI.getBucket(),
                                         FIELD_NAME_CONTENT_LENGTH, FIELD_NAME_CONTENT_MD5,
                                         FIELD_NAME_CONTENT_TYPE, contentType, FIELD_NAME_KEY,
                                         key);

    Connection conn = null;
    Statement insert = null;
    PreparedStatement updateValue = null;
    PreparedStatement updateMetadata = null;
    MD5InputStream in = null;
    try {
      conn = getConnection();
      conn.setAutoCommit(false);

      insert = conn.createStatement();
      insert.executeUpdate(prepareQuery);
      insert.close();

      in = new MD5InputStream(valueInputStream);

      updateValue = conn.prepareStatement(valueQuery);
      updateValue.setBinaryStream(1, in);
      updateValue.executeUpdate();
      updateValue.close();

      in.close();
      String md5 = in.getMD5();
      logger.info(md5);
      long length = in.getLength();
      updateMetadata = conn.prepareStatement(metadataQuery);
      updateMetadata.setLong(1, length);
      updateMetadata.setString(2, md5);
      updateMetadata.executeUpdate();
      updateMetadata.close();

      conn.commit();
    } catch (Exception e) {
      if (conn != null) {
        try {
          conn.rollback();
        } catch(SQLException e1) {}
      }
      throw new CloudException("Error performing put operation", e);
    } finally {
      try {
        if (in != null) in.close();
      } catch (IOException e) {}

      try {
        if (insert != null) insert.close();
      } catch (SQLException e) {}

      try {
        if (updateValue != null) updateValue.close();
      } catch (SQLException e) {}

      try {
        if (updateMetadata != null) updateMetadata.close();
      } catch (SQLException e) {}

      try {
        if (conn != null) conn.close();
      } catch (SQLException e) {}
    }
  }

  public void putMetadata(String key, String contentType, Map<String, String> userMetadata)
    throws IOException, CloudException
  {
    if (userMetadata != null && userMetadata.size() > 0) {
      logger.warn("User metadata not supported by MySQL cloud: dropping them.");
    }

    /* Query that updates the metadata */
    String metadataQuery = String.format("UPDATE %s SET %s='%s' WHERE %s='%s'",
                                         mysqlCloudURI.getBucket(),
                                         FIELD_NAME_CONTENT_TYPE, contentType,
                                         FIELD_NAME_KEY, key);
    Connection conn = null;
    Statement updateStmt = null;
    try {
      conn = getConnection();
      updateStmt = conn.createStatement();
      updateStmt.executeUpdate(metadataQuery);
    } catch (SQLException e) {
      throw new CloudException("Error performing put operation", e);
    } finally {
      try {
        if (updateStmt != null) updateStmt.close();
      } catch (SQLException e) {}

      try {
        if (conn != null) conn.close();
      } catch (SQLException e) {}
    }
  }

  public boolean supportsListByPrefix() {
    return true;
  }

  public boolean supportsListByDate() {
    return true;
  }

  public String[] list(Date tstamp, String prefix) throws IOException, CloudException {
    String query;
    long timestamp = (tstamp != null) ? tstamp.getTime() : 0;
    if (prefix == null) prefix = "";

    query = String.format("SELECT %s FROM %s WHERE %s > %d AND %s LIKE '%s%%'",
                          FIELD_NAME_KEY,
                          mysqlCloudURI.getBucket(),
                          FIELD_NAME_LAST_MODIFIED, timestamp / 1000,
                          FIELD_NAME_KEY, prefix);


    Connection conn = null;
    Statement queryStmt = null;
    ResultSet result = null;
    try {
      logger.trace("Listing cloud entries: " + query);
      conn = getConnection();
      queryStmt = conn.createStatement();
      result = queryStmt.executeQuery(query);

      ArrayList<String> keys = new ArrayList<String>();
      while (result.next()) {
        keys.add(result.getString(FIELD_NAME_KEY));
      }
      logger.trace("Found " + keys.size() + " entries");
      return keys.toArray(new String[keys.size()]);
    } catch (SQLException e) {
      throw new CloudException("Error listing entries", e);
    } finally {
      try {
        if (result != null) result.close();
      } catch (SQLException e) {}

      try {
        if (queryStmt != null) queryStmt.close();
      } catch (SQLException e) {}

      try {
        if (conn != null) conn.close();
      } catch (SQLException e) {}
    }
  }
}
