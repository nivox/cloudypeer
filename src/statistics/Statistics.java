/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package statistics;

import java.util.Random;
import java.net.InetAddress;
import java.io.File;
import java.util.ArrayList;
import java.io.Serializable;
import java.net.URI;
import org.apache.log4j.Logger;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.util.Date;
import jgrapes.JGrapesException;


class StatisticsNews implements Serializable {
  private static int counter = 0;

  private String content;
  private int id;

  public StatisticsNews() {
    id = counter++;
    content = "This is the content of news " + id;
  }

  public String getName() {
    return "news" + id;
  }

}

/**
 * Statistics tests controller
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class Statistics {

  static Logger logger = Logger.getLogger(Statistics.class);

  private static Statistics instance;
  private static StatisticsUpdateHandler cloudUpdateHandlerInstance;
  private static PrintStream out;

  private final File baseLogDirectory;
  private int networkSize;
  private InetAddress ip;
  private int basePort;
  private int duration;
  private int iterations;
  private int newsPeriod;
  private int aePeriod;
  private int rmPeriod;
  private String cloudProvider;
  private URI cloudURI;
  private ArrayList<StatisticsNode> nodeList = new ArrayList<StatisticsNode>();
  private Random random = new Random();

  private String expName;
  private File logFile;

  private PrintStream cloudPrintStream;

  public static Statistics getInstance() {
    return instance;
  }

  public static PrintStream getPrintStream() {
    return out;
  }

  public static StatisticsUpdateHandler getCloudUpdateHandlerInstance() {
    return cloudUpdateHandlerInstance;
  }

  public Statistics(String logDirectoryPath, String ip, int basePort, String cloudProvider,
                    String cloudURI, int networkSize, int duration, int iterations,
                    int newsPeriod, int aePeriod, int rmPeriod) throws Exception
  {
    Statistics.instance = this;
    baseLogDirectory = new File(logDirectoryPath);

    if (!baseLogDirectory.isDirectory())
      throw new IllegalArgumentException("Log directory not found: " + logDirectoryPath);

    expName = baseLogDirectory.getName();

    File cloudLogFile = new File(baseLogDirectory.getPath() + File.separator +
                                 String.format("out-cloud_%s_%d.log", ip, basePort));

    cloudLogFile.createNewFile();
    this.cloudPrintStream = new PrintStream(cloudLogFile);
    cloudUpdateHandlerInstance = new StatisticsUpdateHandler(null, cloudPrintStream);

    this.ip = InetAddress.getByName(ip);
    this.basePort = basePort;
    this.cloudProvider = cloudProvider;
    this.cloudURI = new URI(cloudURI);

    this.networkSize = networkSize;
    this.duration = duration;
    this.iterations = iterations;
    this.newsPeriod = newsPeriod;
    this.aePeriod = aePeriod;
    this.rmPeriod = rmPeriod;

    this.logFile = new File(baseLogDirectory.getPath() + File.separator +
                            String.format("out-controller-%s_%d.log", ip, basePort));
    this.logFile.createNewFile();
    this.out = new PrintStream(logFile);
  }

  private void print(String msg) {
    logger.info(msg);
    out.println(msg);
  }


  private int netSizeAtTimeSin(int currentTime, int length) {
    double x;
    x = (((double) currentTime) / length) * 180;
    return (int)Math.round((Math.sin(Math.toRadians(x)) * networkSize));
  }

  private int netSizeAtTimeConst(long currentTime, long length) {
    long base = 5000;
    int nodes;

    if (length < base * networkSize) {
      base = length/networkSize;
    }

    nodes = (int) (currentTime/base + 1);
    if (nodes > networkSize) nodes = networkSize;
    return nodes;
  }


  public void run(boolean newsController) throws Exception {
    long nextNewsTime = System.currentTimeMillis() + (random.nextInt(newsPeriod) * 60000);
    print("# first news addition at " + new Date(nextNewsTime));
    for (int j=1; j<=iterations; j++) {
      logger.info("Starting iteration " + j);

      long now;
      int nodesToAddRemove;
      long startTime =  System.currentTimeMillis();
      long endTime = startTime + (duration * 60000);
      int lengthTime = (int) (endTime - startTime);
      int currentNodes = 0;
      int progress = 0;
      int tmp;

      while ((now = System.currentTimeMillis()) < endTime) {
        int relativeTime = (int) (now - startTime);

        nodesToAddRemove = netSizeAtTimeConst(relativeTime, lengthTime) - currentNodes;
        tmp = (int)(((double)(relativeTime) / lengthTime) * 100);

        // Log informations
        if ((nodesToAddRemove > 0) || (tmp > progress)) {
          progress = tmp;
          print(String.format("@ time=%d progress=%d%% current_nodes=%d, after_nodes=%d (%d)",
                              (System.currentTimeMillis() / 1000),
                              progress,
                              currentNodes,
                              (currentNodes + nodesToAddRemove), nodesToAddRemove));
        }

        // Add remove nodes
        if (nodesToAddRemove > 0) {
          int temp = 0;

          try {
            for (int i=0; i<nodesToAddRemove; i++) {
              StatisticsNode n = new StatisticsNode(ip, basePort, cloudProvider, cloudURI, expName);
              n.setAntiEntropyPeriod(aePeriod);
              n.setRumorMongeringPeriod(rmPeriod);
              n.start();
              nodeList.add(n);

              print("@ adding node=" + n.getNode().toString());
              temp++;
            }
          } catch (JGrapesException e) {
            print("# error instantiating node: " + e.getMessage());
            currentNodes += temp;
            continue;
          }
        } else {
          for (int i=0; i<-nodesToAddRemove; i++) {
            int index = random.nextInt(nodeList.size());

            StatisticsNode n = nodeList.remove(index);
            print("@ removing node=" + n.getNode().toString());
            n.terminate();
          }
        }
        currentNodes += nodesToAddRemove;


        // Add news
        if (newsController && now > nextNewsTime) {
          if (nodeList.size() > 0) {
            nextNewsTime = now + newsPeriod * 60000;
            StatisticsNews news = new StatisticsNews();

            int index = random.nextInt(nodeList.size());
            StatisticsNode n = nodeList.get(index);
            print(String.format("@ time=%d adding news_name=%s", System.currentTimeMillis()/1000, news.getName()));
            n.addNews(news.getName(), StatisticsNews.class.getName(), news);
            print("# next news addition at " + new Date(nextNewsTime));
          } else {
            nextNewsTime = now + (random.nextInt(newsPeriod) * 60000);
            print("# postponing news addition to " + new Date(nextNewsTime));
          }
        }

        Thread.currentThread().sleep(1000);
      }
    }
    print("# Finished, quitting");
    out.close();
    cloudPrintStream.close();
    System.exit(0);
  }


  public File getBaseDirectory() {
    return baseLogDirectory;
  }

  public static void usage() {
    System.err.println("Usage: statistics <logdir> <ip> <baseport> <cloud-provider> " +
                       "<cloudURI> <netsize> <duration min> <iterations> <news-period min> " +
                       "<ae-period sec> <rm-period sec>");
  }

  public static void main(String args[]) throws Exception {
    String logDirectoryPath = null;
    String ip = null;
    int basePort = -1;
    String cloudProvider = null;
    String cloudURI = null;
    int netsize = -1;
    int duration = -1;
    int iterations = -1;
    int newsPeriod = -1;
    int aePeriod = -1;
    int rmPeriod = -1;
    boolean newsController = false;

    int count = 0;
    if (args.length <= count) {
      usage();
      System.err.println("Missing log directory");
      System.exit(1);
    } else logDirectoryPath = args[count];

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing ip address");
      System.exit(1);
    } else ip = args[count];

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing base port");
      System.exit(1);
    } else basePort = Integer.parseInt(args[count]);

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing cloud provider");
      System.exit(1);
    } else cloudProvider = args[count];

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing cloud URI");
      System.exit(1);
    } else cloudURI = args[count];

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing network size");
      System.exit(1);
    } else netsize = Integer.parseInt(args[count]);

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing duration (minutes)");
      System.exit(1);
    } else duration = Integer.parseInt(args[count]);

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing iterations");
      System.exit(1);
    } else iterations = Integer.parseInt(args[count]);

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing news period (minutes)");
      System.exit(1);
    } else newsPeriod = Integer.parseInt(args[count]);

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing anti entropy period (seconds)");
      System.exit(1);
    } else aePeriod = Integer.parseInt(args[count]);

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing rumor mongering period (seconds)");
      System.exit(1);
    } else rmPeriod = Integer.parseInt(args[count]);

    count++;
    if (args.length <= count) {
      usage();
      System.err.println("Missing news controller setting");
      System.exit(1);
    } else newsController = Boolean.parseBoolean(args[count]);

    logger.info("Initializing test");
    Statistics stats = new Statistics(logDirectoryPath, ip, basePort, cloudProvider, cloudURI,
                                      netsize, duration, iterations, newsPeriod, aePeriod,
                                      rmPeriod);

    logger.info("Running test");

    try {
      stats.run(newsController);
    } catch (Exception e){
      logger.error("Main received uncatched exception! Exiting");
      e.printStackTrace();
      System.exit(666);
    }
  }
}
