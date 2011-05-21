/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store;


/**
 * Simple container for Store compare result
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class StoreCompareResult {

  private String[] keysFresherOnLocalNode;
  private String[] keysFresherOnRemoteNode;
  private String[] metadataChangedOnLocalNode;
  private String[] metadataChangedOnRemoteNode;

  /**
   * Creates a new <code>StoreCompareResult</code> instance.
   *
   * @param keysFresherOnLocalNode The entry keys fresher on the local node
   * @param keysFresherOnRemoteNode The entry keys fresher on the remote node
   */
  public StoreCompareResult(String[] keysFresherOnLocalNode, String[] keysFresherOnRemoteNode,
                            String[] metadataChangedOnLocalNode, String[] metadataChangedOnRemoteNode) {
    this.keysFresherOnLocalNode = keysFresherOnLocalNode;
    this.keysFresherOnRemoteNode = keysFresherOnRemoteNode;
    this.metadataChangedOnLocalNode = metadataChangedOnLocalNode;
    this.metadataChangedOnRemoteNode = metadataChangedOnRemoteNode;
  }

  /**
   * Returns the entry keys fresher on the local node.
   *
   * @return Entries fresher on the local node
   */
  public String[] getKeyFresherOnLocalNode() {
    return keysFresherOnLocalNode;
  }

  /**
   * Return the entry keys fresher on the remote node.
   *
   * @return Entries fresher on the remote node
   */
  public String[] getKeysFresherOnRemoteNode() {
    return keysFresherOnRemoteNode;
  }

  /**
   * Return the entry key that have only metadata changes on the local node.
   *
   * @return Entries with fresh metadata changes on local node
   */
  public String[] getMetadataChangedOnLocalNode() {
    return metadataChangedOnLocalNode;
  }

  /**
   * Return the entry key that have only metadata changes on the remote node.
   *
   * @return Entries with fresh metadata changes on remote node
   */
  public String[] getMetadataChangedOnRemoteNode() {
    return metadataChangedOnRemoteNode;
  }
}
