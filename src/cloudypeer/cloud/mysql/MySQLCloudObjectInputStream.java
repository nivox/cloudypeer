/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.cloud.mysql;

import java.io.InputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * InputStream wrapper which get the InputStream of the MySQL cloud value on demand.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class MySQLCloudObjectInputStream extends InputStream {

  private Connection conn = null;
  private Statement stmt = null;
  private ResultSet result = null;
  private InputStream in = null;

  public MySQLCloudObjectInputStream(Connection conn, Statement stmt, ResultSet result, InputStream in) {
    this.conn = conn;
    this.stmt = stmt;
    this.result = result;
    this.in = in;
  }

  public void close() throws IOException {
    try {
      in.close();
    } catch (IOException e) {}

    try {
      result.close();
    } catch (SQLException e) {}

    try {
      stmt.close();
    } catch (SQLException e) {}

    try {
      conn.close();
    } catch (SQLException e) {}
  }

  public int read() throws IOException {
    if (in == null) throw new IllegalStateException("Not yet opened");
    return in.read();
  }

  public int read(byte b[]) throws IOException {
    if (in == null) throw new IllegalStateException("Not yet opened");
    return in.read(b);
  }

  public int read(byte b[], int off, int len) throws IOException {
    if (in == null) throw new IllegalStateException("Not yet opened");
    return in.read(b, off, len);
  }

  public boolean markSupported() {
    if (in == null) throw new IllegalStateException("Not yet opened");
    return in.markSupported();
  }

  public void mark(int readlimit) {
    if (in == null) throw new IllegalStateException("Not yet opened");
    in.mark(readlimit);
  }

  public void reset() throws IOException {
    if (in == null) throw new IllegalStateException("Not yet opened");
    in.reset();
  }

  public long skip(long n) throws IOException {
    if (in == null) throw new IllegalStateException("Not yet opened");
    return in.skip(n);
  }
}
