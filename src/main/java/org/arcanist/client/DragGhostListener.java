package org.arcanist.client;

import java.awt.event.MouseEvent;

import org.arcanist.client.*;



public interface DragGhostListener {


  /**
   * A callback to modify a DragGhost mid-drag.
   *
   * @param g the DragGhost
   * @param e an event in the ghost's sourceComponent coords
   */
  public void ghostDragged(DragGhost g, MouseEvent e);
}
