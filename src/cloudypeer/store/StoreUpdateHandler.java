/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer.store;

/**
 * Interface used by a Store instance to notify about updates.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public interface StoreUpdateHandler {

  /**
   * Method call whenever the store is updated. <br>
   * This method should be quick to return control.
   *
   * @param keys Keys that were updated
   * @param source The store that was updated
   */
  public void notifyUpdate(String keys[], Store source);
}
