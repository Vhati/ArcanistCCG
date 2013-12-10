// http://weblogs.java.net/blog/2006/09/20/well-behaved-glasspane
// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4222821

package org.arcanist.util;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;


public class NerfableGlassPane extends JPanel implements Nerfable {

  private boolean nerfed = false;

  private MouseListener ml = new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {e.consume();}
    @Override
    public void mouseReleased(MouseEvent e) {e.consume();}
  };

  private KeyListener kl = new KeyListener() {
    @Override
    public void keyPressed(KeyEvent e) {e.consume();}
    @Override
    public void keyReleased(KeyEvent e) {e.consume();}
    @Override
    public void keyTyped(KeyEvent e) {e.consume();}
  };

  private AncestorListener al = new AncestorListener() {
    @Override
    public void ancestorAdded(AncestorEvent e) {
      if (nerfed) requestFocusInWindow();
    }
    @Override
    public void ancestorRemoved(AncestorEvent e) {}
    @Override
    public void ancestorMoved(AncestorEvent e) {}
  };


  public NerfableGlassPane() {
    super();
    super.setVisible(false);
    super.setOpaque(false);
    super.setFocusTraversalKeysEnabled(false);
  }


  @Override
  public void setNerfed(boolean b) {
    if (nerfed == b) return;
    nerfed = b;

    if (b) {
      super.addMouseListener(ml);
      super.addKeyListener(kl);
      super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      super.addAncestorListener(al);
      super.setVisible(true);
      //super.requestFocusInWindow();
    } else {
      super.setVisible(false);
      super.removeAncestorListener(al);
      super.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      super.removeKeyListener(kl);
      super.removeMouseListener(ml);
    }
    super.repaint();
  }

  public boolean isNerfed() {return nerfed;}


  /**
   * Overridden to honor cursors when invisible.
   */
  public boolean contains(int x, int y) {
    if (!nerfed) return false;
    return super.contains(x, y);
  }
}
