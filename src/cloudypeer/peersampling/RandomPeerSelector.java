/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.peersampling;

import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import cloudypeer.Node;
import cloudypeer.PeerNode;
import cloudypeer.PeerSelector;
import java.util.HashSet;

/**
 * Implementation of a random peer selector which uses a PeerSampler protocol instance as the source
 * for peers.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class RandomPeerSelector implements PeerSelector {

  private PeerSampler ps;
  private Set<PeerNode> excludedPeers = new HashSet<PeerNode>();
  private boolean excludeCloud = false;
  private Random random;

  /**
   * Creates a new <code>RandomPeerSelector</code> instance using the specified PeerSampler.
   *
   * @param ps PeerSampler backing this selector
   */
  public RandomPeerSelector(PeerSampler ps) {
    this.ps = ps;
    this.random = new Random();
  }

  /**
   * Returns the list of peer node which are excluded from the selection. <br>
   * Modification to the returned list will affect successive selections.
   *
   * @return List of excluded peers backing the peer selector
   */
  public Set<PeerNode> getExcludedPeers() {
    return excludedPeers;
  }

  /**
   * Specify whether ignore cloud entries.
   *
   * @param b True to exclude the cloud from the selection
   */
  public void excludeCloud(boolean b) {
    excludeCloud = b;
  }

  /*
   * Implementation of PeerSelector.getNode()
   */
  public Node getNode() {
    View view = ps.getView();

    Set<Node> nodeSet = view.getNodes();
    Node nodes[] = nodeSet.toArray(new Node[nodeSet.size()]);
    BitSet excludedMap = new BitSet(nodes.length);
    int excluded = 0;

    Node peer;
    int n;
    while (excluded < nodes.length) {
      n = random.nextInt(nodes.length);

      /* Have we already excluded it */
      if (excludedMap.get(n)) continue;
      peer = nodes[n];

      /* Should we exclude it 'cause it's a cloud node? */
      if (peer.isCloud() && excludeCloud) {
        excludedMap.set(n);
        excluded++;
        continue;
      }

      /* It's an excluded peer? */
      if (excludedPeers.contains(peer)) {
        excludedMap.set(n);
        excluded++;
        continue;
      }

      /* We found a good candidate */
      return peer;
    }

    /* We've excluded all the peer in the view */
    return null;
  }
}
