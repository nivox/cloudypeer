/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.peersampling;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cloudypeer.Metadata;
import cloudypeer.Node;

/**
 * General implementation of a peersampler protocol view network view.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class View implements Iterable {

  private HashMap<Node, Metadata> view = new HashMap<Node, Metadata>();

  /**
   * Creates a new <code>View</code> instance.
   *
   * @param view Map of nodes and their metadata
   * @exception NullPointerException if a null Node is found or view is null
   */
  public View(Map<Node, Metadata> view) {
    for (Map.Entry<Node, Metadata> e: view.entrySet()) {
      Node node;
      Metadata metadata;

      node = e.getKey();
      metadata = e.getValue();
      if (node == null) throw new NullPointerException("Null node in view");
      this.view.put(node, metadata);
    }
  }

  /**
   * Creates a new <code>View</code> instance.
   *
   * @param nodes List of the nodes to insert in the view
   * @param metadatas List of the nodes' metadata
   * @exception NullPointerException if a null Node is found or nodes is  null
   * @exception IllegalArgumentException if the length of the lists differ or metadatas in null
   */
  public View(List<Node> nodes, List<Metadata> metadatas) {
    if (nodes.size() != metadatas.size()) throw new IllegalArgumentException("Nodes number differ from number of metadatas");

    Iterator<Node> nodeIter = nodes.iterator();
    Iterator<Metadata> metaIter = metadatas.iterator();

    while (nodeIter.hasNext()) {
      Node node;
      Metadata metadata;

      node = nodeIter.next();
      metadata = metaIter.next();
      if (node == null) throw new NullPointerException("Null node in view");
      view.put(node, metadata);
    }
  }

  /**
   * Returns the set of nodes in the view. <br>
   * Changes to the set do not affect the underlying view.
   *
   * @return Nodes set
   */
  public Set<Node> getNodes() {
    Set<Node> s = new HashSet<Node>();
    s.addAll(view.keySet());
    return s;
  }

  /**
   * Returns the set of node:metadata entries in the view. <br>
   * Changes to the set do not affect the underlying view.
   *
   * @return Node:metadata entry set
   */
  public Set<Map.Entry<Node, Metadata>> getMetadatas() {
    Set<Map.Entry<Node, Metadata>> s = new HashSet<Map.Entry<Node, Metadata>>();
    s.addAll(view.entrySet());
    return s;
  }

  /**
   * Returns an iterator over the entries of the view. <br>
   * Changes to performed with the iterator will affect the underlying view.
   *
   * @return An iterator over the entries of the view
   */
  public Iterator<Map.Entry<Node, Metadata>> iterator() {
    return view.entrySet().iterator();
  }
}