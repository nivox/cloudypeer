/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package test.simple;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Date;

import cloudypeer.PeerNode;
import cloudypeer.cloud.CloudURI;
import cloudypeer.cloud.StorageCloud;
import cloudypeer.epidemicbcast.antientropy.CloudEnabledAntiEntropyBroadcast;
import cloudypeer.epidemicbcast.rumormongering.RumorMongeringBroadcast;
import cloudypeer.network.NetworkHelper;
import cloudypeer.peersampling.RandomPeerSelector;
import cloudypeer.peersampling.cloudcast.CloudCast;
import cloudypeer.store.Store;
import cloudypeer.store.StoreUpdateHandler;
import cloudypeer.store.diff.FakeDiffHandler;
import cloudypeer.store.persistence.BasicCloudPersistenceHandler;
import cloudypeer.store.persistence.InMemoryPersistenceHandler;
import cloudypeer.store.simple.SimpleStore;
import cloudypeer.store.simple.StoreEntryDiffHandler;
import org.apache.log4j.Logger;

/**
 * Simple test application
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class SimpleApp {

  static Logger logger = Logger.getLogger(SimpleApp.class);

  private NetworkHelper netHelper;

  private CloudURI peerSamplingCloudURI;
  private CloudURI storeCloudURI;
  private StorageCloud storeCloud;

  private PeerNode localNode;

  private RandomPeerSelector peerSelectorAE;
  private RandomPeerSelector peerSelectorRM;

  private Store simpleStore;
  private Store cloudStore;

  private CloudCast cloudCast;
  private CloudEnabledAntiEntropyBroadcast antiEntropy;
  private RumorMongeringBroadcast rumorMongering;



  public SimpleApp(String ip, int port, String cloudProvider, URI psURI, URI storeURI) throws Exception
  {
    /* Setup the net-helper */
    this.netHelper = NetworkHelper.configureDefaultInstance(InetAddress.getByName(ip), port);

    this.localNode = netHelper.getLocalNode();
    this.peerSamplingCloudURI = CloudURI.getInstance(cloudProvider, psURI);
    this.cloudCast = CloudCast.getDefaultInstance(localNode, peerSamplingCloudURI);

    this.peerSelectorAE = new RandomPeerSelector(cloudCast);
    this.peerSelectorAE.getExcludedPeers().add(localNode);
    this.peerSelectorRM = new RandomPeerSelector(cloudCast);
    this.peerSelectorRM.getExcludedPeers().add(localNode);
    this.peerSelectorRM.excludeCloud(true);

    StoreEntryDiffHandler diffHandler = new FakeDiffHandler();
    this.simpleStore = new SimpleStore(new InMemoryPersistenceHandler(), diffHandler);
    this.simpleStore.addUpdateHandler(new SimpleStoreUpdateHandler("local"));

    this.storeCloudURI = CloudURI.getInstance(cloudProvider, storeURI);
    this.storeCloud = StorageCloud.getInstance(cloudProvider, storeCloudURI);
    BasicCloudPersistenceHandler cloudPersistence = new BasicCloudPersistenceHandler(storeCloud, "store/");
    cloudPersistence.setKeysRefreshThreshold(10);
    cloudPersistence.setMetadataRefreshThreshold(2);
    this.cloudStore = new SimpleStore(cloudPersistence, diffHandler);
    this.cloudStore.addUpdateHandler(new SimpleStoreUpdateHandler("cloud"));


    this.antiEntropy = CloudEnabledAntiEntropyBroadcast.getDefaultInstance(localNode, peerSelectorAE,
                                                                    simpleStore, cloudStore);
    this.antiEntropy.setPeriod(15);
    this.rumorMongering = RumorMongeringBroadcast.getDefaultInstance(localNode, peerSelectorRM,
                                                                simpleStore, 5);
    this.rumorMongering.setPeriod(4);
  }

  public void run() throws Exception {
    netHelper.start();
    cloudCast.start();
    antiEntropy.start();
    rumorMongering.start();

    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    int count = 0;
    String entry = null;
    InputStream in;
    while(true) {
      count++;
      try {
        System.out.println("<Press enter to assign entry 'test'> ");
        reader.readLine();
        entry = String.format("%s - entry %d (%s)", localNode, count, new Date());
        logger.info(String.format("Putting entry: '%s'", entry));
      } catch (IOException e) {
        e.printStackTrace();
        continue;
      }

      in = new ByteArrayInputStream(entry.getBytes());
      simpleStore.putStoreEntry("test", in, "string", null);
    }
  }

  public static void help() {
    System.err.println("Usage: SimpleApp ip port cloudProvider psCloudURI storeCloudURI");
  }

  public static void main(String args[]) {
    String ip;
    int port;
    String cloudProvider;
    URI psURI;
    URI storeURI;

    try {
      if (args.length < 1) {
        help();
        throw new IllegalArgumentException("Missing ip configuration");
      } else ip = args[0];

      if (args.length < 2) {
        help();
        throw new IllegalArgumentException("Missing port configuration");
      } else port = Integer.parseInt(args[1]);

      if (args.length < 3) {
        help();
        throw new IllegalArgumentException("Missing cloud provider configuration");
      } else cloudProvider = args[2];

      if (args.length < 4) {
        help();
        throw new IllegalArgumentException("Missing peer sampler cloud uri");
      } else psURI = new URI(args[3]);

      if (args.length < 5) {
        help();
        throw new IllegalArgumentException("Missing store cloud uri");
      } else storeURI = new URI(args[4]);

      SimpleApp app = new SimpleApp(ip, port, cloudProvider, psURI, storeURI);
      app.run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
