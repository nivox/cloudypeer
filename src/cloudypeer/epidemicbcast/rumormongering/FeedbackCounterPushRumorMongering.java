/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.epidemicbcast.rumormongering;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cloudypeer.GossipProtocolException;
import cloudypeer.Node;
import cloudypeer.PeerNode;
import cloudypeer.PeerSelector;
import cloudypeer.network.NetworkClient;
import cloudypeer.network.NetworkConnection;
import cloudypeer.network.NetworkException;
import cloudypeer.network.NetworkHelper;
import cloudypeer.store.Store;
import cloudypeer.store.StoreCompareResult;
import cloudypeer.store.StoreEntryDiff;
import cloudypeer.store.StoreEntryDiffData;
import cloudypeer.store.StoreEntryMetadata;
import cloudypeer.store.StoreUpdateHandler;
import org.apache.log4j.Logger;


/**
 * Feedback Counter implementation of rumor mongering broadcast based on TCP connections.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class FeedbackCounterPushRumorMongering
 extends RumorMongeringBroadcast
  implements NetworkClient, StoreUpdateHandler
{

  static Logger logger = Logger.getLogger(FeedbackCounterPushRumorMongering.class);

  private static final int CONNECTION_TIMEOUT = 5000;
  private static final int RECEIVE_TIMEOUT = 5000;

  /* *********************************************************************
   * Instance variables
   ***********************************************************************/

  /**
   * The network helper instance in use by this protocol
   */
  private NetworkHelper netHelper;

  /**
   * Timestamp of the last active cycle
   */
  private long lastCycleTimestamp = 0;

  /**
   * Map that relates news with their associated counter
   */
  private Map<String, Integer> newsMap = new HashMap<String, Integer>();

  /* *********************************************************************
   * Constructors
   ***********************************************************************/

  /**
   * Creates a new <code>FeedbackCounterPushRumorMongering</code> instance.
   *
   * @param localNode Node descriptor for the local node
   * @param peerSelector Peer selector in use by this instance
   * @param store Store backing this instance
   * @param persistence Parameter influencing the persistence at spreading news
   */
  public FeedbackCounterPushRumorMongering(PeerNode localNode, PeerSelector peerSelector,
                                       Store store, int persistence)
  {
    super(localNode, peerSelector, store, persistence);
  }

  /* *********************************************************************
   * Implementation of NetworkClient's methods
   ***********************************************************************/

  public void processMessage(PeerNode sender, Serializable message) {
    /* Datagram messages not enabled */
  }

  public PeerNode getNode() {
    return localNode;
  }

  /* *********************************************************************
   * Implementation of StoreUpdateHandler's method
   ***********************************************************************/
  public void notifyUpdate(String keys[], Store source) {
    synchronized (newsMap) {
      for (String key: keys) {
        logger.trace(String.format("Adding news to rumor mongering broadcast: %s\n", key));
        newsMap.put(key, 0);
      }
    }
  }

  /* *********************************************************************
   * Implementation of util methods
   ***********************************************************************/
  private int timeUntillNextActiveCycle() {
    int delta = (int) ((lastCycleTimestamp + (period * 1000)) - System.currentTimeMillis());
    return (delta > 0) ? (delta / 1000) : 0;
  }

  /* *********************************************************************
   * Implementation of pushNews and receiveNews
   ***********************************************************************/

  private void pushNews(PeerNode remote) throws NetworkException, SocketTimeoutException {
    NetworkConnection conn = null;
    try {
      logger.info("Pushing news to " + remote);
      conn = netHelper.createConnection(this, remote, CONNECTION_TIMEOUT);

      Set<String> knownKeys;
      synchronized (newsMap) {
        knownKeys = new HashSet<String>(newsMap.keySet());
      }

      String newsKeys[] = knownKeys.toArray(new String[knownKeys.size()]);
      HashMap<String, StoreEntryMetadata> entriesMetadata = store.getStoreEntriesMetadata(newsKeys);
      logger.trace("Pushing news: sending metadata");
      conn.send(entriesMetadata);
      logger.trace("Pushing news: reading diff data");
      StoreEntryDiffData[] diffData = (StoreEntryDiffData[]) conn.receive(timeUntillNextActiveCycle());
      if (diffData == null) return;
      StoreEntryDiff[] toPush = store.diffStoreEntries(diffData);
      logger.trace("Pushing news: sending diff");
      conn.send(toPush);
      logger.trace("Pushing news: closing connection");
      conn.close();

      /* Ages the news that where already known */
      for (StoreEntryDiffData d: diffData) {
        if (d == null || d.getKey() == null) continue;
        knownKeys.remove(d.getKey());
        logger.trace(String.format("News %s fresh on remote\n", d.getKey()));
      }
      synchronized (newsMap) {
        int counter;
        for (String k: knownKeys) {
          if (!newsMap.containsKey(k)) continue;
          counter = newsMap.get(k);

          if (++counter < persistence) {
            newsMap.put(k, counter);
            logger.trace(String.format("News %s known on remote: counter=%d\n", k, counter));
          } else {
            newsMap.remove(k);
            logger.trace(String.format("News %s known on remote: counter=%d (removed)\n", k, counter));
          }
        }
      }

    }
    catch (IOException e) {
      /* Something gone bad. Abort active cycle */
      logger.warn("Error pushing news. Input/Output error", e);
    } catch (ClassCastException e) {
      logger.warn("Error pushing news. Unknown data", e);
    } finally {
      try {
        if (conn != null) conn.close();
      } catch (IOException e) {}
    }

  }

  private void receiveNews(NetworkConnection conn) {
    try {
      logger.info("Receiving news");
      HashMap<String, StoreEntryMetadata> remoteMetadata;
      logger.trace("Receiving news: reading metadata");
      remoteMetadata = (HashMap<String, StoreEntryMetadata>) conn.receive(timeUntillNextActiveCycle());
      StoreCompareResult cmpresult = store.compareStoreEntries(remoteMetadata);
      StoreEntryDiffData[] diffData = store.produceStoreEntriesDiffData(cmpresult.getKeysFresherOnRemoteNode());
      logger.trace("Receiving news: sending diff data");
      conn.send(diffData);
      logger.trace("Receiving news: reading diff");
      StoreEntryDiff[] news = (StoreEntryDiff[]) conn.receive(timeUntillNextActiveCycle());
      logger.trace("Receiving news: closing connection");
      conn.close();

      store.patchStoreEntries(news);

      /* Add the received new to the newsMap to spread them */
      synchronized (newsMap) {
        for (StoreEntryDiff d: news) {
          if (d == null || d.getKey() == null) continue;

          newsMap.put(d.getKey(), 0);
        }
      }
    } catch (IOException e) {
      /* Something gone bad... give up this passive cycle */
      logger.warn("Error resolving difference (passive). Input/Output error", e);
    } catch (ClassCastException e) {
      logger.warn("Error resolving difference (passive). Unknown data", e);
    } finally {
      try {
        conn.close();
      } catch (IOException e) {}
    }
  }

  /* *********************************************************************
   * Implementation of GossipProtocol's methods
   ***********************************************************************/
  @Override
  public void init() throws GossipProtocolException {
    try {
      netHelper = (NetworkHelper) this.getProtocolData("nethelper");
    } catch (Exception e) {}
    if (netHelper == null)
      netHelper = NetworkHelper.getConfiguredInstance();
    if (netHelper == null) {
      logger.error("No NetworkHelper instance specified as protocol data nor a configured instance is present");
      throw new GossipProtocolException("NetworkHelper instance not passed nor configured");
    }

    netHelper.registerClient(this, 1);
    store.addUpdateHandler(this);
  }

  /*
   * Implementation of abstract method runActiveThread
   */
  public void runActiveThread() {
    long sleepTime;
    while(!isTerminated()) {
      Node remote = peerSelector.getNode();

      if (lastCycleTimestamp == 0) lastCycleTimestamp = System.currentTimeMillis();
      else lastCycleTimestamp += period * 1000;

      try {
        if (remote != null && newsMap.size() > 0) {
          if (remote.isCloud()) {
            logger.warn("RumorMongering protocols don't support cloud nodes. Use an appropriate PeerSelector!");
          } else {
            pushNews((PeerNode) remote);
          }
        }
      } catch (ClassCastException e) {
        logger.error("Error: remote peer class not supported", e);
      } catch (NetworkException e) {
        logger.warn("Network error resolving differences", e);
      } catch (SocketTimeoutException e) {
        logger.warn("Network timeout resolving differences", e);
      } catch (IOException e) {
        logger.warn("Input/Output error resolving differences", e);
      } catch (IllegalArgumentException e) {
        logger.warn("Argument error", e);
      } catch (RuntimeException e) {
        logger.fatal("Uncatched exception", e);
      }

      if (isTerminated()) break;

      sleepTime = timeUntillNextActiveCycle() * 1000;
      try {
        if (sleepTime > 0) Thread.currentThread().sleep(sleepTime);
      } catch (InterruptedException e) {
        /* If we were terminated the while will exit */
        logger.trace("Catched an InterruptedException while waiting for next cycle");
      }
    }

    netHelper.unregisterClient(this);
    store.removeUpdateHandler(this);
    logger.trace("Active thread terminated");
  }

  /*
   * Implementation of abstract method runPassiveThread
   */
  public void runPassiveThread() {
    NetworkConnection conn;
    while (!isTerminated()) {
      try {
        logger.trace("Waiting for incoming connection");
        conn = netHelper.acceptConnection(this);
        logger.trace("Incoming connection: receiving news");
        receiveNews(conn);
      } catch (InterruptedException e) {
        /* If it's time to quit the while will take care of that */
        logger.trace("Catched an InterruptedException while waiting for contact");
      } catch (Exception e) {
        logger.error("Catched an exception in passive thread", e);
      }
    }
  }
}
