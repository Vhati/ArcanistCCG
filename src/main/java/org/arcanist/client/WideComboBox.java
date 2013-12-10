package org.arcanist.client;

import java.awt.Dimension;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;


/**
 * A bugfix class for too-narrow ComboBox popups.
 * Discovered by Mark_McLaren, for the following bug:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607
 */
public class WideComboBox extends JComboBox {

  private boolean layingOut = false;
  private int cachedWidth = 0;


  public WideComboBox() {}

  public WideComboBox(final Object items[]) {
    super(items);
    cachedWidth = this.getPreferredSize().width;
  }

  public WideComboBox(Vector items) {
    super(items);
    cachedWidth = this.getPreferredSize().width;
  }

  public WideComboBox(ComboBoxModel aModel) {
    super(aModel);
    cachedWidth = this.getPreferredSize().width;
  }


  public void recachePopupWidth() {
    cachedWidth = this.getPreferredSize().width;
  }


  @Override
  public void doLayout(){
    try {
      layingOut = true;
      super.doLayout();
    }
    finally {
      layingOut = false;
    }
  }

  @Override
  public Dimension getSize() {
    Dimension dim = super.getSize();
    if (!layingOut)
      dim.width = Math.max(dim.width, cachedWidth);
    return dim;
  }
}
