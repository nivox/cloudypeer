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
import java.io.PrintStream;

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
  private PrintStream out;

  public StatisticsUpdateHandler(PeerNode node, PrintStream out) throws Exception{
    this.out = out;

    if (node != null)
      this.node = node.toString();
    else
      this.node = "cloud";
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
  }
}
