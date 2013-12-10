package org.arcanist.util;


/**
 * An interface for objects that require an object.
 */
public interface DockableParent {

  /**
   * Get the DockableChild objects attached to this.
   *
   * @return an array of children
   */
  public DockableChild[] getDockableChildren();

  /**
   * Determines whether a DockableChild is present here.
   */
  public boolean hasDockableChild(DockableChild c);


  /**
   * Embed a DockableChild into this.
   */
  public void addDockableChild(DockableChild c);

  /**
   * Remove a DockableChild from this.
   */
  public void removeDockableChild(DockableChild c);

  /**
   * Test if a given DockableChild is allowed here.
   * This should test if the child is null, already present, is the of right class, etc.
   *
   * @return true if it can be added, false otherwise
   */
  public boolean isDockableChildValid(DockableChild c);
}
