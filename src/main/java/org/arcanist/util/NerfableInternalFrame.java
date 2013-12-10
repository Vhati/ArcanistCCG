package org.arcanist.util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.IllegalComponentStateException;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;

import org.arcanist.util.*;


public class NerfableInternalFrame extends JInternalFrame implements Nerfable {

  private NerfableInternalFrame pronoun = this;

  private Container contentPaneHolder = new JPanel(new BorderLayout());
  private Container contentPane = new JPanel(new BorderLayout());
  private NerfableGlassPane glassPane = null;


  public NerfableInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
    super();
    super.setTitle(title);
    super.setResizable(resizable);
    super.setClosable(closable);
    super.setMaximizable(maximizable);
    super.setIconifiable(iconifiable);

    JPanel realContentPane = new JPanel(new BorderLayout());
      JPanel overlayPanel = new JPanel();
        overlayPanel.setLayout(new OverlayLayout(overlayPanel));
        realContentPane.add(overlayPanel);

      glassPane = new NerfableGlassPane();
        overlayPanel.add(glassPane);

      contentPaneHolder.add(new JPanel());
        overlayPanel.add(contentPaneHolder);

      contentPaneHolder.add(contentPane);

    super.setContentPane(realContentPane);
  }


  /**
   * Toggles user interaction within this window.
   */
  @Override
  public void setNerfed(boolean b) {
    glassPane.setNerfed(b);
    super.repaint();
  }

  public boolean isNerfed() {return glassPane.isNerfed();}


  /**
   * Sets the content pane.
   * Or rather, nests a given panel in the content pane.
   * The JRootPane method is not invoked.
   *
   * @throws IllegalComponentStateException (a runtime exception) if the content pane parameter is null
   */
  public void setContentPane(Container c) {
    if (c != null) {
      contentPaneHolder.removeAll();
      contentPane = c;
      contentPaneHolder.add(contentPane);
    } else {
      throw new IllegalComponentStateException("contentPane cannot be set to null.");
    }
  }

  /**
   * Gets the content pane.
   */
  public Container getContentPane() {
    return contentPane;
  }
}
