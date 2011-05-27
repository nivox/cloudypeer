/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package statistics;

import java.io.File;
import cloudypeer.PeerNode;
import cloudypeer.store.Store;
import cloudypeer.store.StoreEntry;
import cloudypeer.store.StoreEntryMetadata;
import java.io.FileWriter;
import cloudypeer.store.StoreUpdateHandler;
import org.apache.log4j.Logger;
import java.io.PrintWriter;

/**
 * Describe class StatisticsUpdateHandler here.
 *
 *
 * Created: Thu May 26 16:54:45 2011
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public class StatisticsUpdateHandler implements StoreUpdateHandler {

  static Logger logger = Logger.getLogger(StatisticsUpdateHandler.class);

  private String node;
  private File logFile;
  private PrintWriter out;

  public StatisticsUpdateHandler(PeerNode node) throws Exception{
    String fileName = null;
    if (node != null) {
      this.node = node.toString();
      fileName = String.format("out-%s_%d.log",
                               node.getInetAddress().getHostAddress(),
                               node.getPort());
    } else {
      this.node = "cloud";
      fileName = "out-cloud.log";
    }

    File baseDirectory = Statistics.getInstance().getBaseDirectory();
    this.logFile = new File(baseDirectory.getPath() + File.separator + fileName);
    this.logFile.createNewFile();
    this.out = new PrintWriter(new FileWriter(logFile));
  }

  public synchronized void notifyUpdate(String[] keys, Store store) {
    for (String key: keys) {
      StoreEntry entry = store.getStoreEntry(key);
      StoreEntryMetadata meta = entry.getMetadata();
      logger.info(String.format("%s received update for entry %s (%s: %s)",
                                node, key, meta.getModifiedTimestamp(),
                                meta.getContentMD5()));

      out.println(String.format("@ time=%s, key=%s, modified=%s, md5=%s",
                              System.currentTimeMillis() / 1000,
                              key,
                              meta.getModifiedTimestamp().getTime() / 1000,
                              meta.getContentMD5()));
    }
    out.flush();
  }

  public void close() {
    this.out.close();
  }
}
