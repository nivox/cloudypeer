/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package statistics;

import cloudypeer.network.NetworkHelper;
import java.net.URI;
import java.io.PipedInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import cloudypeer.store.persistence.BasicCloudPersistenceHandler;
import cloudypeer.store.persistence.InMemoryPersistenceHandler;
import cloudypeer.store.simple.SimpleStore;
import cloudypeer.store.diff.FakeDiffHandler;
import cloudypeer.store.simple.StoreEntryDiffHandler;
import java.net.InetAddress;
import cloudypeer.epidemicbcast.rumormongering.RumorMongeringBroadcast;
import cloudypeer.epidemicbcast.antientropy.CloudEnabledAntiEntropyBroadcast;
import cloudypeer.peersampling.cloudcast.CloudCast;
import cloudypeer.store.Store;
import cloudypeer.peersampling.RandomPeerSelector;
import cloudypeer.PeerNode;
import cloudypeer.cloud.StorageCloud;
import cloudypeer.cloud.CloudURI;
import org.apache.log4j.Logger;
import java.io.Serializable;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;

/**
 * Describe class StatisticsNode here.
 *
 *
 * Created: Thu May 26 16:28:33 2011
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class StatisticsNode {

  static Logger logger = Logger.getLogger(StatisticsNode.class);

  private NetworkHelper netHelper;

  private CloudURI peerSamplingCloudURI;
  private CloudURI storageCloudCloudURI;

  private StorageCloud storageCloud;

  private PeerNode localNode;

  private RandomPeerSelector peerSelectorAE;
  private RandomPeerSelector peerSelectorRM;

  private Store localStore;
  private Store cloudStore;

  private CloudCast cloudCast;
  private CloudEnabledAntiEntropyBroadcast antiEntropy;
  private RumorMongeringBroadcast rumorMongering;

  private StatisticsUpdateHandler updateHandler;

  private PrintStream out;

  public StatisticsNode(InetAddress ip, int baseport, String cloudProvider, URI cloudURI, String name)
    throws Exception
  {
    this.netHelper = NetworkHelper.getDefaultInstance(ip, NetworkHelper.findFreePort(baseport, 0));
    this.localNode = netHelper.getLocalNode();

    File logFile = new File(Statistics.getInstance().getBaseDirectory().getPath() + File.separator +
                            String.format("out-%s_%d.log",
                                          localNode.getInetAddress().getHostAddress(),
                                          localNode.getPort()));
    logFile.createNewFile();
    this.out = new PrintStream(new FileOutputStream(logFile, true));

    this.peerSamplingCloudURI = CloudURI.getInstance(cloudProvider, new URI(cloudURI.toString() + "/" + name +".view"));
    this.cloudCast = CloudCast.getDefaultInstance(localNode, peerSamplingCloudURI);

    this.peerSelectorAE = new RandomPeerSelector(cloudCast);
    this.peerSelectorAE.getExcludedPeers().add(localNode);
    this.peerSelectorRM = new RandomPeerSelector(cloudCast);
    this.peerSelectorRM.getExcludedPeers().add(localNode);
    this.peerSelectorRM.excludeCloud(true);

    StoreEntryDiffHandler diffHandler = new FakeDiffHandler();
    this.localStore = new SimpleStore(new InMemoryPersistenceHandler(), diffHandler);
    this.updateHandler = new StatisticsUpdateHandler(localNode, out);
    this.localStore.addUpdateHandler(updateHandler);

    this.storageCloudCloudURI = CloudURI.getInstance(cloudProvider, cloudURI);
    this.storageCloud = StorageCloud.getInstance(cloudProvider, storageCloudCloudURI);
    BasicCloudPersistenceHandler cloudPersistence = new BasicCloudPersistenceHandler(storageCloud, name + "/");
    cloudPersistence.setKeysRefreshThreshold(10);
    cloudPersistence.setMetadataRefreshThreshold(2);
    this.cloudStore = new SimpleStore(cloudPersistence, diffHandler);
    this.cloudStore.addUpdateHandler(Statistics.getCloudUpdateHandlerInstance());

    this.antiEntropy = CloudEnabledAntiEntropyBroadcast.getDefaultInstance(localNode, peerSelectorAE,
                                                                    localStore, cloudStore);
    this.rumorMongering = RumorMongeringBroadcast.getDefaultInstance(localNode, peerSelectorRM,
                                                                localStore, 5);

    this.antiEntropy.setProtocolData("nethelper", netHelper);
    this.rumorMongering.setProtocolData("nethelper", netHelper);

    this.antiEntropy.setProtocolData("printstream", Statistics.getPrintStream());
    this.rumorMongering.setProtocolData("printstream", Statistics.getPrintStream());
  }

  public void setAntiEntropyPeriod(int period) {
    this.antiEntropy.setPeriod(period);
  }

  public void setRumorMongeringPeriod(int period) {
    this.rumorMongering.setPeriod(period);
  }

  public PeerNode getNode() {
    return this.localNode;
  }

  public void start() throws Exception{
    out.println("@ starting time=" + System.currentTimeMillis() / 1000);
    netHelper.start();
    cloudCast.start();
    antiEntropy.start();
    rumorMongering.start();
  }

  public void terminate() {
    out.println("@ terminating time=" + System.currentTimeMillis() / 1000);
    out.flush();
    out.close();
    rumorMongering.terminate();
    antiEntropy.terminate();
    cloudCast.terminate();
    netHelper.terminate();
  }

  public synchronized void addNews(String name, String contentType, Serializable o) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ByteArrayInputStream bin = null;
    ObjectOutputStream out = new ObjectOutputStream(bout);

    out.writeObject(o);
    out.close();

    bin = new ByteArrayInputStream(bout.toByteArray());
    try {
      localStore.putStoreEntry(name, bin, contentType, null);
    } finally {
      bin.close();
    }
  }
}
