package org.arcanist.util;


/**
 * An interface for objects that require an object.
 */
public interface Dependent {

  /**
   * The source object was removed.
   * Perform cleanup operations.
   */
  public void sourceRemoved(Object source);
}
