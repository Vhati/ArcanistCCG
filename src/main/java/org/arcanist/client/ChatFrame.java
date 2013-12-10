package org.arcanist.client;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A chat window.
 */
public class ChatFrame extends JInternalFrame implements DockableParent {

  private ArcanistCCGFrame frame = null;

  private JPanel pane = new JPanel(new BorderLayout());

  private List<DockableChild> dockableChildren = new ArrayList<DockableChild>(1);


  /**
   * Constructs and shows the window.
   * The global ChatPanel will be removed from the JumboView, and added here.
   */
  public ChatFrame(ArcanistCCGFrame f) {
    super("Chat",
      true, //resizable
      true, //closable
      true, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);
    if (Prefs.usePaletteFrames) this.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
    frame = f;

    int width = 325, height = 300;

    if (frame.getChatPanel() == null) return;

    if (frame.getJumboFrame() != null) {
      //if for some reason, it's not docked there, nothing bad will happen
      frame.getJumboFrame().removeDockableChild(frame.getChatPanel());
    }
    addDockableChild(frame.getChatPanel());

    this.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosing(InternalFrameEvent e) {
        removeDockableChild(frame.getChatPanel());
        if (frame.getJumboFrame() != null) {
          frame.getJumboFrame().addDockableChild(frame.getChatPanel());
        }
      }
    });

    this.setContentPane(pane);
    frame.getDesktop().add(this);

    if (frame.getDesktop().getSize().width/2-width/2 > 0 && frame.getDesktop().getSize().height/2-height/2 > 0)
      this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();
  }


  /**
   * Get the DockableChild objects attached to this.
   *
   * @return an array of children
   * @see DockableParent#getDockableChildren()
   */
  @Override
  public DockableChild[] getDockableChildren() {
    return dockableChildren.toArray(new DockableChild[dockableChildren.size()]);
  }

  /**
   * Determines whether a DockableChild is present here.
   *
   * @see DockableParent#hasDockableChild(DockableChild)
   */
  @Override
  public boolean hasDockableChild(DockableChild c) {
    return dockableChildren.contains(c);
  }

  /**
   * Test if a given DockableChild is allowed here.
   * It cannot already be present, and must be a ChatPanel.
   * And there cannot already be a ChatPanel.
   *
   * @return true if it can be added, false otherwise
   * @see DockableParent#isDockableChildValid(DockableChild)
   */
  @Override
  public boolean isDockableChildValid(DockableChild c) {
    if (c == null || dockableChildren.contains(c)) return false;
    if ((c instanceof JComponent) == false) return false;

    if (c.getClass() == ChatPanel.class) {
      for (int i=0; i < dockableChildren.size(); i++) {
        if (dockableChildren.get(i).getClass() == ChatPanel.class) return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Embed a DockableChild into this.
   * If it is a ChatPanel, it will be added within the Chat tab, which will be enabled.
   *
   * @throws IllegalArgumentException if the child is invalid
   * @see ChatFrame#isDockableChildValid(DockableChild)
   * @see DockableParent#addDockableChild(DockableChild)
   */
  @Override
  public void addDockableChild(DockableChild c) {
    if (!isDockableChildValid(c)) throw new IllegalArgumentException("Attempted to add an invalid DockableChild");

    if (c.getClass() == ChatPanel.class) {
      dockableChildren.add(c);
      pane.add((JComponent)c, BorderLayout.CENTER);
      pane.revalidate();
      pane.repaint();
    }
  }

  /**
   * Remove a DockableChild from this.
   *
   * @see DockableParent#removeDockableChild(DockableChild)
   */
  @Override
  public void removeDockableChild(DockableChild c) {
    if (c == null || !dockableChildren.contains(c)) return;

    if (c.getClass() == ChatPanel.class) {
      dockableChildren.remove(c);
      pane.remove((JComponent)c);
      pane.revalidate();
      pane.repaint();
    }
  }
}
