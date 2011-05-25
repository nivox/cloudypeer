/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.peersampling.cloudcast;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;

import cloudypeer.CloudNode;
import cloudypeer.Metadata;
import cloudypeer.Node;
import cloudypeer.PeerNode;
import cloudypeer.cloud.CloudURI;
import cloudypeer.peersampling.PeerSampler;
import cloudypeer.peersampling.PeerSamplerException;
import cloudypeer.peersampling.View;
import jgrapes.CloudHelper;
import jgrapes.JGrapes;
import jgrapes.JGrapesException;
import jgrapes.JGrapesHelper;
import jgrapes.NetworkHelper;
import jgrapes.NodeID;
import jgrapes.ReceivedData;

/**
 * Implementation of the cloudcast peer sampling protocol based on the jGRAPES library.
 *
 * Besides the standard cloudcast parameters this implementation allows the following configuration
 * via the setProperties method:
 *
 * <ul>
 *  <li><b>max_silence:</b> numer of active cycle without global cloud contact before forcely
 *  readd a cloud descriptor</li>
 *  <li><b>cloud_respawn_prob:<b> probability that a node will generate a cloud descriptor after
 *  max_silence</li>
 * </ul>
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 * @see https://github.com/nivox/jGRAPES
 *
 */
public class JGrapesCloudCast extends CloudCast {

  private NetworkHelper netHelper;
  private CloudHelper cloudHelper;
  private jgrapes.PeerSampler peerSampler;
  private Random random = new Random();

  /**
   * Creates a new <code>JGrapesCloudCast</code> instance with the default parameters.
   *
   * @param localNode The node descriptor to be used as local node
   * @param cloud The URI that identifies the cloud path to be used as view
   */
  public JGrapesCloudCast(PeerNode localNode, CloudURI cloud) {
    super(localNode, cloud);
  }

  /**
   * Creates a new <code>JGrapesCloudCast</code> instance.
   *
   * @param localNode The node descriptor to be used as local node
   * @param cloud The CloudURI that identifies the cloud path to be used as view
   * @param viewSize The maximum size of the network view
   * @param partialViewSize The size of the partial view send to to remote nodes
   * @param period Period of the active thread of cloudcast
   * @param threshold Cloudcast's threshold parameter value
   */
  public JGrapesCloudCast(PeerNode localNode, CloudURI cloud, int viewSize, int partialViewSize,
                          int period, int threshold) {
    super(localNode, cloud, viewSize, partialViewSize, period, threshold);
  }


  /* *********************************************************************
   * Implementation of util methods
   ***********************************************************************/

  private static String getNativeLibraryName(String libraryBaseName) {
    String osname = System.getProperty("os.name").toLowerCase();

    String libraryName;
    if (osname.contains("windows"))
      libraryName = String.format("%s.dll", libraryBaseName);
    else if (osname.contains("os x"))
      libraryName = String.format("%s.dylib", libraryBaseName);
    else libraryName = String.format("%s.so", libraryBaseName);

    return libraryName;
  }


  private static String[] parsePath(String path, int n) {
    if (path == null || path.equals("")) return new String[n];
    if (!path.startsWith("/")) throw new IllegalArgumentException("Path is not absolute!");

    String paths[] = new String[n];
    String components[] = path.split("/");

    /* The first component of the split is always "" */
    for (int i=1; i<=n && i<components.length; i++)
      if (!components[i].equals("")) paths[i-1] = components[i];

    return paths;
  }



  /**
   * Parse the user info string. <br>
   * Supported form: <code
   *
   * @param userInfo a <code>String</code> value
   * @return a <code>String[]</code> value
   */
  private static String[] parseUserInfo(String userInfo) {
    String credentials[] = new String[2];
    if (userInfo != null) {
      String parsed[] = userInfo.split(":");

      if (parsed[0] != null) parsed[0].trim();
      else parsed[0] = "";

      if (parsed[1] != null) parsed[1].trim();
      else parsed[1] = "";

      if (!parsed[0].equals("")) credentials[0] = parsed[0];
      if (!parsed[1].equals("")) credentials[1] = parsed[1];
    }

    return credentials;
  }

  /**
   * Assembles the configuration for the MySQL delegate helper of grapes. <br>
   * <br>
   * The cloud uri associated with this instance must contains the following parameters:
   * <ul>
   *  <li> supported mysql scheme </li>
   *  <li> MySQL server host </li>
   *  <li> MySQL database as first path element </li>
   *  <li> MySQL bucket table as second path element </li>
   *  <li> MySQL user </li>
   *  <li> optional MySQL password </li>
   * </ul>
   *
   * @return MySQL delegate helper configuration
   * @exception IllegalArgumentException Raised if one of the required parameters is missing
   */
  private String genJGrapesCloudHelperMysqlConfString() {
    String host;
    String user;
    String pass;
    String db;
    String bucket;

    URI baseCloudURI = cloudURI.getBaseURI();
    host = baseCloudURI.getHost();

    String userInfo[] = cloudURI.getAuthenticationInfo();
    if (userInfo == null || userInfo.length == 0)
      throw new IllegalArgumentException("MySQL user not specified");
    user = userInfo[0];
    if (userInfo.length >= 2) pass = userInfo[1];
    else pass = null;

    String pathComponents[] = parsePath(baseCloudURI.getPath(), 1);
    db = pathComponents[0];

    bucket = cloudURI.getBucket();
    if (db == null || bucket == null)
      throw new IllegalArgumentException("MySQL database or table not specified");

    String sharedLibrary = getNativeLibraryName("mysql_delegate_helper");

    String conf = "provider=delegate,delegate_lib=" + sharedLibrary;
    conf += ",mysql_host=" + host;
    conf += ",mysql_user=" + user;
    conf += ",mysql_pass=" + ((pass != null)? pass: "");
    conf += ",mysql_db=" + db;
    conf += ",mysql_table=" + bucket;

    return conf;
  }


  /**
   * Assembles the configuration for the libs3 delegate helper of grapes. <br>
   * <br>
   * The cloud uri associated with this instance must contains the following parameters:
   * <ul>
   *  <li> supported amazon s3 scheme </li>
   *  <li> bucket name </li>
   *  <li> s3 access key </li>
   *  <li> s3 secret access key </li>
   * </ul>
   *
   * @return libs3 delegate helper configuration
   * @exception IllegalArgumentException Raised if one of the required parameters is missing or
   * invalid
   */
  private String genJGrapesCloudHelperAmazonConfString() {
    String protocol;
    String bucket;
    String accessKey;
    String secretKey;

    URI baseURI = cloudURI.getBaseURI();

    /* Parsing protocol */
    protocol = baseURI.getScheme();

    /* Parsing bucket */
    bucket = cloudURI.getBucket();
    if (bucket == null) throw new IllegalArgumentException("Bucket not specified");

    /* Parsing access keys */
    String userInfo[] = cloudURI.getAuthenticationInfo();
    if (userInfo == null || userInfo.length < 2)
      throw new IllegalArgumentException("Access keys not specified");
    accessKey = userInfo[0];
    secretKey = userInfo[1];

    String sharedLibrary = getNativeLibraryName("libs3_delegate_helper");

    /* Assembling configuration string for grapes delegate cloud helper libs3_delegate_helper */
    return String.format("provider=delegate,delegate_lib=%s,s3_access_key=%s,s3_secret_key=%s," +
                         "s3_bucket_name=%s,s3_protocol=%s", sharedLibrary, accessKey, secretKey,
                         bucket, protocol);
  }

  /**
   * Generates the grapes cloud helper configuration string.
   *
   * @return Grapes cloud helper configuration
   * @exception IllegalArgumentException Raised if one of the required parameters is missing or
   * invalid
   */
  private String genJGrapesCloudHelperConfString() {
    String conf;
    URI cloudProvider = cloudURI.getBaseURI();

    if (cloudProvider.getScheme().equals("mysql"))
      conf = genJGrapesCloudHelperMysqlConfString();
    else if (cloudProvider.getHost().equals("s3.amazonaws.com"))
      conf = genJGrapesCloudHelperAmazonConfString();
    else throw new IllegalArgumentException("Cloud provider not supported: " + cloudProvider);

    return conf;
  }


  /**
   * Generates the grapes peer sampler configuration string.
   *
   * @return Grapes peer sampler configuration
   * @exception IllegalArgumentException Raised if one of the required parameters is missing or
   * invalid.
   */
  private String genJGrapesPeerSamplerConfString() {
    String conf = "protocol=cloudcast";

    conf += ",period=" + period;
    conf += ",cache_size=" + viewSize;
    conf += ",sent_entries=" + partialViewSize;

    if (cloudURI.getKey() != null && !cloudURI.getKey().equals(""))
      conf += ",view_key=" + cloudURI.getKey();

    String maxSilenceString = protocolConfiguration.getProperty("max_silence");
    if (maxSilenceString != null) {
      try {
        conf += String.format(",max_silence=%d", Integer.parseInt(maxSilenceString));
      } catch (NumberFormatException e) {
        System.err.format("JGrapesCloudCast warning: Expected an int value for parameter " +
                          "max_silence, got '%s'", maxSilenceString);
      }
    }

    String cloudRespawnProbString = protocolConfiguration.getProperty("cloud_respawn_prob");
    if (cloudRespawnProbString != null) {
      try {
        conf += String.format(",cloud_respawn_prob=%d", Double.parseDouble(cloudRespawnProbString));
      } catch (NumberFormatException e) {
        System.err.format("JGrapesCloudCast warning: Expected a double value for parameter " +
                          "cloud_respawn_prob, got '%s'", cloudRespawnProbString);
      }
    }

    return conf;
  }

  /**
   * Converts a jGrapes node in a CloudyPeer node.
   *
   * @param node A jGrapes node
   * @return A CloudyPeer node
   */
  private Node convertToNode(NodeID node, int port) throws UnknownHostException {
    if (cloudHelper.isCloudNode(node)) {
      return new CloudNode(cloudURI);
    } else {
      return new PeerNode(node.getAddress(), port);
    }
  }

  /**
   * Converts a CloudyPeer node in a jGrapes node.
   *
   * @param node A CloudyPeer node
   * @return A jGrapes node
   * @exception PeerSamplerException if an error error occurs
   */
  private NodeID convertToNodeID(Node node) throws PeerSamplerException{
    if (node.isCloud()) {
      return cloudHelper.getCloudNode();
    } else {
      try {
      PeerNode n = (PeerNode) node;
      return new NodeID(String.format("%s:%d", n.getInetAddress().getHostAddress(), n.getPort()));
      } catch (ClassCastException e) {
        throw new PeerSamplerException("Unsupported node type", e);
      } catch (UnknownHostException e) {
        throw new PeerSamplerException("Unknown host for peer", e);
      }
    }
  }

  /* *********************************************************************
   * Implementation of GossipProtocol's protected methods
   ***********************************************************************/
  private int findFreeUDPPort(int start) throws PeerSamplerException {
    boolean found = false;
    int count = 0;
    int targetPort;
    DatagramSocket sock = null;
    while (!found && count < (65535 - start)) {
      count++;
      targetPort = random.nextInt(65535 - start) + start;

      try {
        sock = new DatagramSocket(targetPort);
        return targetPort;
      } catch (SocketException e) {
      } finally {
        if (sock != null) sock.close();
      }
    }

    throw new PeerSamplerException("Cannot find a free port to bind jGRAPES network helper. Base port: " + start);
  }

  @Override
  public void init() throws PeerSamplerException {
    String localIp = localNode.getInetAddress().getHostAddress();
    int localPort = findFreeUDPPort(localNode.getPort());
    String cloudConf = genJGrapesCloudHelperConfString();
    String psConf = genJGrapesPeerSamplerConfString();

    try {
      netHelper = JGrapes.newNetworkHelperInstance(localIp, localPort, null);
    } catch (JGrapesException e) {
      throw new PeerSamplerException("Error initializing jgrapes network helper", e);
    }

    try {
      cloudHelper = JGrapes.newCloudHelperInstance(netHelper, cloudConf);
    } catch (JGrapesException e) {
      throw new PeerSamplerException("Error initializing jgrapes cloud helper", e);
    }

    try {
      byte[] localMeta = ByteBuffer.allocate(4).putInt(localNode.getPort()).array();
      peerSampler = JGrapes.newPeerSamplerInstance(netHelper, localMeta, psConf);
    } catch (JGrapesException e) {
      throw new PeerSamplerException("Error initializing jgrapes peer sampler", e);
    }
  }

  private static byte[] receiveFromPeer(NetworkHelper netHelper) throws JGrapesException {
    /* Here we should probably do something smarter */
    ReceivedData rec = netHelper.recvFromPeer(1024);
    return rec.getData();
  }

  private static byte[] receiveFromCloud(CloudHelper cloudHelper) throws JGrapesException {
    /* Here we should probably do something smarter */
    byte buffer[] = cloudHelper.recvFromCloud(1024);
    return buffer;
  }

  /*
   * Implementation of abstract method runActiveThread
   */
  protected void runActiveThread() {
    NetworkHelper netHelpers[] = new NetworkHelper[]{netHelper};
    CloudHelper cloudHelpers[] = new CloudHelper[]{cloudHelper};

    while(!isTerminated()) {
      Object resource;
      byte data[] = null;

      try {
        resource = JGrapesHelper.waitForAny(netHelpers, cloudHelpers, 1);
        if (resource instanceof NetworkHelper)
          data = receiveFromPeer((NetworkHelper) resource);
        else if (resource instanceof CloudHelper)
          data = receiveFromCloud((CloudHelper) resource);
      } catch (JGrapesException e) {
        System.err.println("JGrapesCloudCast: error receiving data");
        e.printStackTrace();
      }

      if (data != null) { /* passive cycle */
        try {
          synchronized (peerSampler) {
            peerSampler.parseData(data);
          }
        } catch (JGrapesException e) {
          System.err.println("JGrapesCloudCast: error parsing remote data");
          e.printStackTrace();
        }
      } else { /* active cycle */
        try {
          synchronized (peerSampler) {
            peerSampler.parseData(null);
          }
        } catch (JGrapesException e) {
          System.err.println("JGrapesCloudCast: error performing active cycle");
          e.printStackTrace();
        }
      }
    }
  }

  /*
   * Implementation of abstract method runPassiveThread and startPassiveThread.
   * Since jgrapes is structured to use a single thread there's no need to start another one.
   */
  protected void runPassiveThread() {}
  protected void startPassiveThread() {}

  /* *********************************************************************
   * Implementation of PeerSampler's public accessible methods
   ***********************************************************************/

  /*
   * Implementation of abstract method addNode
   */
  public boolean addNode(Node node) throws PeerSamplerException {
    if (!wasStarted() || isTerminated())
      throw new IllegalStateException("Cloudcast protocol not active");

    boolean status = false;
    try {
        peerSampler.addPeer(convertToNodeID(node), null);
      status = true;
    } catch (JGrapesException e) {
      /* this is thrown to signal that the node could not be added */
    }

    return status;
  }

  /*
   * Implementation of abstract method removeNode
   */
  public boolean removeNode(Node node) throws PeerSamplerException {
    if (!wasStarted() || isTerminated())
      throw new IllegalStateException("Cloudcast protocol not active");

    try {
      peerSampler.removePeer(convertToNodeID(node));
    } catch (JGrapesException e) {
      throw new PeerSamplerException("Error removing peer", e);
    }

    return true;
  }

  /*
   * Implementation of abstract method getView
   */
  public View getView() throws PeerSamplerException {
    if (!wasStarted() || isTerminated())
      throw new IllegalStateException("Cloudcast protocol not active");

    NodeID cache[];
    byte meta[][];

    synchronized (peerSampler) {
      cache = peerSampler.getCache();
      meta = peerSampler.getMetadata();
    }

    HashMap<Node, Metadata> view = new HashMap<Node, Metadata>();

    Node n;
    int i = 0;
    for (NodeID node: cache) {
      try {
        n = convertToNode(node, ByteBuffer.wrap(meta[i]).getInt(0));
        i++;
      } catch (UnknownHostException e) {
        /* This should not be happening... */
        e.printStackTrace();
        continue;
      } catch (ArrayIndexOutOfBoundsException e) {
        /* Whoops, problem with the metadata */
        e.printStackTrace();
        continue;
      }

      Metadata m = new Metadata();
      m.put("nodeid", node.getPort());
      view.put(n, m);
    }

    return new View(view);
  }
}
