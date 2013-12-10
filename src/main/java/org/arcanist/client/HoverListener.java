package org.arcanist.client;

import java.awt.Point;
import java.awt.Rectangle;


/**
 * An interface for objects that move in response to something else.
 */
public interface HoverListener {

  /**
   * Sets the offset to maintain from the trigger object.
   */
  public void setHoverOffset(int x, int y);

  /**
   * Gets the offset to maintain from the trigger object.
   */
  public Point getHoverOffset();

  /**
   * Gets the bounds of this component in the form of a Rectangle object.
   */
  public Rectangle getBounds();

  /**
   * Notifies this listener to move.
   */
  public void hoverTo(int x, int y);
}
