package org.arcanist.client;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;


/**
 * A temporary draggable stand-in.
 * Appears in place of other things, and they move after dragging is complete.
 */
public class DragGhost extends JComponent {

  private ArcanistCCGFrame frame = null;

  private DragGhost pronoun = this;

  private int offsetX = 0;
  private int offsetY = 0;
  private int snapOffsetX = 0;
  private int snapOffsetY = 0;
  private JComponent sourceComponent = null;
  private DragGhostListener ghostListener = null;
  private boolean paintReticle = false;
  private boolean snap = false;
  private boolean moved = false;

  private RenderingHints renderHints = null;


  private MouseMotionListener dragRelayListener = new MouseMotionAdapter() {
    @Override
    public void mouseDragged(MouseEvent e) {
      if (sourceComponent == null || pronoun.getParent() == null) return;
      if (ghostListener != null) ghostListener.ghostDragged(pronoun, e);

      Point ghostSpace = SwingUtilities.convertPoint(sourceComponent, e.getPoint(), pronoun);
      pronoun.dispatchEvent(new MouseEvent(pronoun, e.getID(), e.getWhen(), e.getModifiers(), ghostSpace.x, ghostSpace.y, e.getClickCount(), e.isPopupTrigger()));
      if (!moved) pronoun.setVisible(true);
      moved = true;
    }
  };


  public DragGhost(ArcanistCCGFrame f) {
    super();
    frame = f;

    renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    renderHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);


    MouseMotionListener dragability = new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        e = SwingUtilities.convertMouseEvent(pronoun, e, pronoun.getParent());  //Convert ghost coords to table coords

        int newX = e.getX()-(paintReticle?pronoun.getSize().width/2+3:offsetX);
        int newY = e.getY()-(paintReticle?pronoun.getSize().height/2+3:offsetY);
        if (snap == true) {
          Point snapPoint = frame.getTablePane().getNearestGridPoint(new Point(newX, newY));
          newX = (int)snapPoint.getX() + snapOffsetX;
          newY = (int)snapPoint.getY() + snapOffsetY;
        }
        pronoun.setLocation(newX, newY);
      }
    };
    pronoun.addMouseMotionListener(dragability);


    //super.setBorder(BorderFactory.createLineBorder(Color.BLACK));
  }


  /**
   * Standard debug info.
   */
  public String toString() {
    return "DragGhost" +
    "\nHasMoved: "+ moved +
    "\nOffset: "+ offsetX +","+ offsetY +
    "\nSourceObject: "+ (sourceComponent==null?"null":sourceComponent.toString());
  }


  /**
   * Sets initial drag point (from object's top-left).
   *
   * @param oX horizontalal initial offset
   * @param oY vertical initial offset
   */
  public void setOffset(int oX, int oY) {
    setOffset(oX, oY, 0, 0);
  }

  /**
   * Sets initial drag point (from object's top-left)
   * and offset from snap grid.
   *
   * @param oX horizontalal initial offset
   * @param oY vertical initial offset
   * @param sX horizontalal snap offset
   * @param sY vertical snap offset
   */
  public void setOffset(int oX, int oY, int sX, int sY) {
    offsetX = oX; offsetY = oY;
    snapOffsetX = sX; snapOffsetY = sY;
  }


  /**
   * Sets what will be dragged.
   * Its size and location will be copied, and a
   * MouseMotionListener will be added to it to
   * relay events to the ghost.
   * 'moved' and 'snap' will be reset to false.
   *
   * @param c the object, or null to release
   */
  public void setSourceObject(JComponent c) {
    if (c == null) {
      setSourceObject(null, 1, 1);
    } else {
      setSourceObject(c, c.getSize().width, c.getSize().height);
    }
  }

  /**
   * Sets what will be dragged.
   * Its size will be custom, its location copied,
   * and a MouseMotionListener will be added to it
   * to relay events to the ghost.
   * 'moved', 'snap', and 'reticle' will be reset to false.
   *
   * @param c the object, or null to release
   * @param w custom width
   * @param h custom height
   */
  public void setSourceObject(JComponent c, int w, int h) {
    setSourceObject(c, null, w, h);
  }

  /**
   * Sets what will be dragged.
   * Its size will be custom, its location copied,
   * and a MouseMotionListener will be added to it
   * to relay events to the ghost.
   * 'moved', 'snap', and 'reticle' will be reset to false.
   *
   * @param c the object, or null to release
   * @param dgl a callback during movement, or null for none
   * @param w custom width
   * @param h custom height
   */
  public void setSourceObject(JComponent c, DragGhostListener dgl, int w, int h) {
    if (sourceComponent != null) {
      sourceComponent.removeMouseMotionListener(dragRelayListener);
    }
    sourceComponent = c;
    ghostListener = dgl;
    moved = false;
    snap = false;
    paintReticle = false;
    pronoun.setVisible(false);
    if (sourceComponent != null) {
      super.setSize(w, h);
      super.setLocation(sourceComponent.getLocation());
      sourceComponent.addMouseMotionListener(dragRelayListener);
    }
  }

  public JComponent getSourceObject() {return sourceComponent;}


  public void setSnap(boolean b) {snap = b;}


  public void setPaintReticle(boolean b) {paintReticle = b;}


  public boolean hasMoved() {return moved;}


  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (sourceComponent == null) return;
    int w = this.getSize().width;
    int h = this.getSize().height;
    Graphics2D g2 = null;

    if (paintReticle == false) {
      // Draw the outline
      g.drawRect(0, 0, w-1, h-1);
    }
    else {
      // Draw the reticle
      int sdim = (w<h?w:h);
      int margin = 5;

      g2 = (Graphics2D)g.create();
      Stroke oldStroke = g2.getStroke();
      RenderingHints oldHints = g2.getRenderingHints();

      //g2.setStroke(new BasicStroke(1.5f));
      g2.drawLine(w/2, h/2-sdim/7*2, w/2, h/2-sdim/7);
      g2.drawLine(w/2, h/2+sdim/7, w/2, h/2+sdim/7*2);

      g2.drawLine(w/2-sdim/7*2, h/2, w/2-sdim/7, h/2);
      g2.drawLine(w/2+sdim/7, h/2, w/2+sdim/7*2, h/2);

      g2.setStroke(new BasicStroke(1.4f));
      g2.setRenderingHints(renderHints);
      //g2.draw(new java.awt.geom.Arc2D.Double(margin, margin, w-margin*2, h-margin*2, 70, -140, java.awt.geom.Arc2D.OPEN));
      //g2.draw(new java.awt.geom.Arc2D.Double(margin, margin, w-margin*2, h-margin*2, 110, 140, java.awt.geom.Arc2D.OPEN));
      g2.draw(new java.awt.geom.Ellipse2D.Double(margin, margin, w-margin*2, h-margin*2));
      g2.setRenderingHints(oldHints);

      g2.setStroke(oldStroke);
    }
  }
}
