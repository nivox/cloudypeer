/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store;

import java.io.InputStream;
import java.io.Serializable;


/**
 * Defines an entry of a Store instance.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface StoreEntry extends Serializable {


  /**
   * Returns the key of this entry.
   *
   * @return Entry key
   */
  public String getKey();

  /**
   * Returns the metadata associated with this entry.
   *
   * @return Entry metadata
   */
  public StoreEntryMetadata getMetadata();

  /**
   * Returns the input stream of the content. Be careful using the input stream and close it as
   * quick as possible as it may be backed by a network stream.
   *
   * @return Content input stream
   */
  public InputStream getInputStream();
}
