package org.arcanist.client;

import javax.swing.event.MouseInputAdapter;


/**
 * Cancels in-progress mouse operations.
 */
public class GuiInterruptMouseInputAdapter extends MouseInputAdapter implements GuiInterruptListener {


  public GuiInterruptMouseInputAdapter() {
    super();
  }


  public void guiInterrupted() {
  }
}
