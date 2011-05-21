/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store;

import java.io.Serializable;

/**
 * A class implementing this interface must provide all the data necessary for a remote party to
 * produce a StoreEntryDiff object.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface StoreEntryDiffData extends Serializable {

  /**
   * Returns the entry key this instance describe
   *
   * @return Store entry key described
   */
  public String getKey();
}
