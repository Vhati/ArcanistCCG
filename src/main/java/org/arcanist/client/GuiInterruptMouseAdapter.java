package org.arcanist.client;

import java.awt.event.MouseAdapter;


/**
 * Cancels in-progress mouse operations.
 */
public class GuiInterruptMouseAdapter extends MouseAdapter implements GuiInterruptListener {


  public GuiInterruptMouseAdapter() {
    super();
  }


  @Override
  public void guiInterrupted() {
  }
}
