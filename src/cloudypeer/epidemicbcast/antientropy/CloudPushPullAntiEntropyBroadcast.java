/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.epidemicbcast.antientropy;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import cloudypeer.CloudNode;
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
import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryDiff;
import cloudypeer.store.StoreEntryDiffData;
import cloudypeer.store.StoreEntryMetadata;
import cloudypeer.store.StoreException;
import org.apache.log4j.Logger;
import java.net.SocketTimeoutException;


/**
 * Implementation of the antientropy epidemic broadcast protocol described by Demers et. al. in
 * "Epidemic algorithms for replicated database maintenance".
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class CloudPushPullAntiEntropyBroadcast extends CloudEnabledAntiEntropyBroadcast implements NetworkClient{

  static Logger logger = Logger.getLogger(CloudPushPullAntiEntropyBroadcast.class);

  private static final int CONNECTION_TIMEOUT = 5000;
  private static final int RECEIVE_TIMEOUT = 5000;
  private static final int BOOTSTRAP_PERIOD = 1000;

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
   * Flag for bootstrap phase
   */
  private boolean bootstrap = true;

  /* *********************************************************************
   * Constructors
   ***********************************************************************/

  /**
   * Creates a new <code>CloudPushPullAntiEntropyBroadcast</code> instance.
   *
   * @param localNode Node descriptor used as local node for the current epidemic broadcast protocol
   * @param peerSelector Peer selector used to get remote node descriptor
   * @param store Store that backs this epidemic broadcast instance
   * @exception NullPointerException Raised if localNode is null
   */
  public CloudPushPullAntiEntropyBroadcast(PeerNode localNode, PeerSelector peerSelector,
                                              Store store, Store cloudStore) {
    super(localNode, peerSelector, store, cloudStore);
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
   * Implementation of util methods
   ***********************************************************************/

  private int timeUntillNextActiveCycle() {
    int delta = (int) ((lastCycleTimestamp + (period * 1000)) - System.currentTimeMillis());
    return (delta > 0) ? delta : 0;
  }

  /* *********************************************************************
   * Implementation of resolveDifference functions
   ***********************************************************************/
  private void resolveDifferenceCloud(CloudNode c) {
    HashMap<String, StoreEntryMetadata> metadataToUpdate;
    /* Obtain cloud entries metadata and compare them with local store */
    logger.trace("Resolve difference cloud");
    HashMap<String, StoreEntryMetadata> entriesMetadata = cloudStore.getStoreEntriesMetadata();
    logger.trace(String.format("Cloud has %d entries", entriesMetadata.size()));
    StoreCompareResult cmpresult = store.compareStoreEntries(entriesMetadata);


    try {
      /* Step 1: update local metadata if there's any fresher on the cloud */
      String[] metadataChangedOnRemote = cmpresult.getMetadataChangedOnRemoteNode();
      if (metadataChangedOnRemote.length > 0) {
        metadataToUpdate = new HashMap<String, StoreEntryMetadata>();
        logger.trace(String.format("Updating local metadata for %d entries",
                                   metadataChangedOnRemote.length));
        for (String key: metadataChangedOnRemote)
          metadataToUpdate.put(key, entriesMetadata.get(key));
        store.updateMetadatas(metadataToUpdate);
      }

      /* Step 2: update cloud metadata if there's any fresher locally */
      String[] metadataChangedOnLocal = cmpresult.getMetadataChangedOnLocalNode();
      if (metadataChangedOnLocal.length > 0) {
        metadataToUpdate = new HashMap<String, StoreEntryMetadata>();
        logger.trace(String.format("Updating cloud metadata for %d entries",
                                   metadataChangedOnLocal.length));
        for (String key: metadataChangedOnLocal)
          metadataToUpdate.put(key, store.getStoreEntryMetadata(key));
        cloudStore.updateMetadatas(metadataToUpdate);
      }

      /* Step 3: reading cloud's fresher entries */
      StoreEntry[] toPull = cloudStore.getStoreEntries(cmpresult.getKeysFresherOnRemoteNode());
      logger.trace(String.format("Read %d entries from cloud", toPull.length));

      /* Step 4: pushing locally fresher entries */
      StoreEntry[] toPush = store.getStoreEntries(cmpresult.getKeyFresherOnLocalNode());
      logger.trace(String.format("Pushing %d entries to cloud", toPush.length));
      cloudStore.updateStoreEntries(toPush);

      logger.trace("Updating the local store...");
      store.updateStoreEntries(toPull);
    } catch (StoreException e) {
      /* Something has gone bad */
      logger.warn("Error performing a store operation", e);
    }
  }

  private void resolveDifferencePeer(PeerNode p)
    throws InterruptedException, NetworkException, SocketTimeoutException, IOException
  {
    NetworkConnection conn = netHelper.createConnection(this, p, CONNECTION_TIMEOUT);
    Map<String, StoreEntryMetadata> metadataToUpdate;
    try {
      logger.trace("Resolving difference with " + p);
      HashMap<String, StoreEntryMetadata> entriesMetadata = store.getStoreEntriesMetadata();

      /* PUSH phase: out Map<String, StoreEntryMetadata>, in StoreEntryDiffData[], out
       * StoreEntryDiff[]  */
      logger.trace("Performing push active phase...");
      conn.send(entriesMetadata);
      StoreEntryDiffData[] diffDataIn = (StoreEntryDiffData[]) conn.receive(timeUntillNextActiveCycle());
      if (diffDataIn == null) return;
      StoreEntryDiff[] toPush = store.diffStoreEntries(diffDataIn);
      conn.send(toPush);

      /* PULL phase: in Map<String, StoreEntryMetadata>, in String[], out StoreEntryDiffData[], in
       * StoreEntryDiff[] */
      logger.trace("Performing pull active phase...");
      metadataToUpdate = (Map<String, StoreEntryMetadata>) conn.receive(timeUntillNextActiveCycle());
      String[] keysToPull = (String[]) conn.receive(timeUntillNextActiveCycle());
      if (keysToPull == null) return;
      StoreEntryDiffData[] diffDataOut = store.produceStoreEntriesDiffData(keysToPull);
      conn.send(diffDataOut);
      StoreEntryDiff[] toPull = (StoreEntryDiff[]) conn.receive(timeUntillNextActiveCycle());

      /* Finally patch the store... */
      logger.trace("Updating local store...");
      store.patchStoreEntries(toPull);

      /* ... and update metadata changed on remote */
      if (metadataToUpdate != null) {
        store.updateMetadatas(metadataToUpdate);
      }
    } catch (SocketTimeoutException e) {
      logger.warn("Error resolving difference (active). Receive timeout", e);
    } catch (NetworkException e) {
      logger.warn("Error resolving difference (active). Network error", e);
    } catch (IOException e) {
      /* Something gone bad. Abort active cycle */
      logger.warn("Error resolving difference (active). Input/Output error", e);
    } catch (ClassCastException e) {
      logger.warn("Error resolving difference (active). Unknown data", e);
    } finally {
      try {
        if (conn != null) conn.close();
      } catch (IOException e) {}
    }
  }

  /* *********************************************************************
   * Implementation of resolveDifferencePassive
   ***********************************************************************/
  private void resolveDifferencePassive(NetworkConnection conn) {
    HashMap<String, StoreEntryMetadata> metadataToUpdate;
    try {
      /* PULL phase: in Map<String, StoreEntryMetadata>, out StoreEntryDiffData[],
       * in  StoreEntryDiff[] */
      logger.trace("Performing pull passive phase...");
      HashMap<String, StoreEntryMetadata> remoteMetadata;
      remoteMetadata = (HashMap<String, StoreEntryMetadata>) conn.receive(timeUntillNextActiveCycle());
      StoreCompareResult cmpresult = store.compareStoreEntries(remoteMetadata);
      StoreEntryDiffData[] diffDataOut = store.produceStoreEntriesDiffData(cmpresult.getKeysFresherOnRemoteNode());
      conn.send(diffDataOut);
      StoreEntryDiff[] toPull = (StoreEntryDiff[]) conn.receive(timeUntillNextActiveCycle());

      /* PUSH phase: out Map<String, StoreEntryMetadata>, String[], in StoreEntryDiffData[], out
       * StoreEntryDiff[] */
      logger.trace("Performing push passive phase...");

      /* Preparing metadata to push */
      String[] metadataChangedOnLocal = cmpresult.getMetadataChangedOnLocalNode();
      if (metadataChangedOnLocal.length > 0) {
        metadataToUpdate = new HashMap<String, StoreEntryMetadata>();
        for (String key: metadataChangedOnLocal)
          metadataToUpdate.put(key, store.getStoreEntryMetadata(key));
        conn.send(metadataToUpdate);
      } else {
        conn.send(null);
      }

      conn.send(cmpresult.getKeyFresherOnLocalNode());
      StoreEntryDiffData[] diffDataIn = (StoreEntryDiffData[]) conn.receive(timeUntillNextActiveCycle());
      StoreEntryDiff[] toPush = store.diffStoreEntries(diffDataIn);
      conn.send(toPush);
      conn.close();

      /* Finally patch the store... */
      logger.trace("Updating store...");
      store.patchStoreEntries(toPull);

      /* ... and update metadata changed on remote */
      String[] metadataChangedOnRemote = cmpresult.getMetadataChangedOnRemoteNode();
      if (metadataChangedOnRemote.length > 0) {
        metadataToUpdate = new HashMap<String, StoreEntryMetadata>();
        for (String key: metadataChangedOnRemote) {
          logger.info("Updating metadata for key " + key);
          metadataToUpdate.put(key, remoteMetadata.get(key));
        }
        store.updateMetadatas(metadataToUpdate);
      }
    } catch (SocketTimeoutException e) {
      logger.info("Error resolving difference (passive). Receive timeout", e);
    } catch (NetworkException e) {
      logger.warn("Error resolving difference (passive). Network error", e);
    } catch (IOException e) {
      /* Something gone bad... give up this passive cycle */
      logger.info("Error resolving difference (passive). Input/Output error", e);
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

    netHelper.registerClient(this, 0);
  }

  /*
   * Implementation of abstract method runActiveThread
   */
  public void runActiveThread() {
    long sleepTime;
    long tentativeTimestamp;
    boolean success;
    int count = 0;
    while(!isTerminated()) {
      Node remote = peerSelector.getNode();
      tentativeTimestamp = System.currentTimeMillis();
      success = false;
      count++;
      try {
        logger.trace("Active cycle: " + remote);
        if (remote != null) {
          if (remote.isCloud()) resolveDifferenceCloud((CloudNode) remote);
          else resolveDifferencePeer((PeerNode) remote);
        }

        if (remote != null && bootstrap) bootstrap = false;
        success = true;
      } catch (InterruptedException e) {
        /* The check for the termination is done right after */
        logger.warn("Catched an InterruptedException while resolving difference");
      } catch (ClassCastException e) {
        logger.warn("Error: remote peer class not supported", e);
      } catch (NetworkException e) {
        logger.warn("Network error resolving differences", e);
      } catch (SocketTimeoutException e) {
        logger.info("Network timeout resolving differences", e);
      } catch (IOException e) {
        logger.info("Input/Output error resolving differences", e);
      } catch (IllegalArgumentException e) {
        logger.warn("Argument error", e);
      } catch (RuntimeException e) {
        logger.fatal("Uncatched exception", e);
      }

      /* If we weren't able to complete the active cycle retry in a second... */
      if (bootstrap || (!success && count < (period / 2))) {
        logger.trace("Failed active cycle, retrying");
        try {
          Thread.currentThread().sleep(BOOTSTRAP_PERIOD);
          continue;
        } catch (InterruptedException e) {}
      }

      if (isTerminated()) break;

      count = 0;
      if (lastCycleTimestamp == 0) lastCycleTimestamp = tentativeTimestamp;
      else lastCycleTimestamp += period * 1000;

      sleepTime = timeUntillNextActiveCycle();
      try {
        if (sleepTime > 0) Thread.currentThread().sleep(sleepTime);
      } catch (InterruptedException e) {
        /* If we were terminated the while will exit */
        logger.trace("Catched an InterruptedException while waiting for next cycle");
      }
    }
    netHelper.unregisterClient(this);
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
        logger.trace("Incoming connection: resolving differences");
        resolveDifferencePassive(conn);
      } catch (InterruptedException e) {
        /* If it's time to quit the while will take care of that */
        logger.trace("Catched an InterruptedException while waiting for contact");
      }
    }
  }
}
