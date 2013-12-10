package org.arcanist.client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JLayeredPane;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * This is a layered pane in which objects are played.
 */
public class TableComponent extends JLayeredPane implements Nerfable {

  private NerfableGlassPane glassPane = new NerfableGlassPane();

  //Grid Snap
  private int gridSnap = 1;
  //Background
  private Color bgColor = null;
  private Image bgImage = null;
  private boolean tiled = true;
  private int bgImageWidth=0, bgImageHeight=0;
  //Multiselect
  private boolean drawSelection = false;
  private int fromX=0, fromY=0;
  private Rectangle selectionRect = null;
  private float[] dashes = {10f,20f};
  private BasicStroke selectionStroke = new BasicStroke(2,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_ROUND,5,dashes,4f);


  public TableComponent() {
    super();
    setLayout(null);
    add(glassPane, Prefs.glassLayer);
    glassPane.setBounds(0, 0, 1, 1);

    ComponentListener glassStretcher = new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        Component c = e.getComponent();
        glassPane.setSize(c.getWidth(), c.getHeight());
      }
    };
    this.addComponentListener(glassStretcher);
  }


  public void setNerfed(boolean b) {
    glassPane.setNerfed(b);
    super.repaint();
  }


  /**
   * Gets grid snap increment.
   */
  public int getGridSnap() {
    return gridSnap;
  }

  /**
   * Sets grid snap increment.
   *
   * @param i pixel increment (1 is essentially no snap)
   * @throws IllegalArgumentException if the increment is less than 1
   */
  public void setGridSnap(int i) {
    if (i < 1) throw new IllegalArgumentException("Grid snap "+ i +" is less than 1");
    gridSnap = i;
  }

  /**
   * Gets the nearest grid point.
   *
   * @param p a point to snap
   */
  public Point getNearestGridPoint(Point p) {
    if (gridSnap == 1) return p;

    int w = this.getWidth();
    int h = this.getHeight();

    int x = (((int)p.getX()-w/2) / gridSnap)*gridSnap + w/2;
    int y = (((int)p.getY()-h/2) / gridSnap)*gridSnap + h/2;

    return new Point(x, y);
  }


  /**
   * Sets the panel's background.
   *
   * @param path path to the desired image or null to clear the image.
   * @param tile if true and the image exists, tiles the image.
   * @throws IOException if an error occurs during reading
   */
  public void setBackgroundImage(String path, boolean tile) throws IOException {
    if (path == null) {
      bgImage = null;
      bgImageWidth = 0;
      bgImageHeight = 0;
      repaint();
      return;
    }
    try {
      bgImage = javax.imageio.ImageIO.read(new File(path));
      bgImageWidth = bgImage.getWidth(null);
      bgImageHeight = bgImage.getHeight(null);
      tiled = tile;
      repaint();
    } catch (IOException e) {
      throw new IOException("Unable to read "+ path);
    }
  }


  /**
   * Sets the bounds of a dashed rectangle when shift-selecting.
   * This function determines which corner it's been given.
   *
   * @param x the X coord.
   * @param y the Y coord.
   * @see TableComponent#getSelection
   * @see TableComponent#clearSelection
   */
  public void setSelectionCorner(int x, int y) {
    if (selectionRect == null) {
      fromX = x; fromY = y;
      selectionRect = new Rectangle();
      selectionRect.setLocation(x, y);
    } else {
      drawSelection = true;
      if (x > fromX) selectionRect.width = x - fromX;
      else {
        selectionRect.width = fromX - x;
        selectionRect.x = x;
      }
      if (y > fromY) selectionRect.height = y - fromY;
      else {
        selectionRect.height = fromY - y;
        selectionRect.y = y;
      }
      repaint();
    }
  }


  /**
   * Gets the current selection rectangle.
   *
   * @return the current selection.
   * @see TableComponent#setSelectionCorner
   * @see TableComponent#clearSelection
   */
  public Rectangle getSelection() {
    return selectionRect;
  }


  /**
   * Clears the selection rectangle.
   *
   * @see TableComponent#setSelectionCorner
   * @see TableComponent#getSelection
   */
  public void clearSelection() {
    drawSelection = false;
    selectionRect = null;
    repaint();
  }


  public void paintComponent(Graphics g) {
    super.paintComponent(g);                                 //paint normally

    ((Graphics2D)g).drawRect(0, 0, this.getPreferredSize().width, this.getPreferredSize().height);

    if (bgImage != null) {
      Dimension size = this.getSize();
      if (tiled == true) {
        int xsegments = size.width / bgImageWidth + 1;
        int ysegments = size.height / bgImageHeight + 1;
        for (int y=0; y < ysegments; y++) {
          for (int x=0; x < xsegments; x++) {
            ((Graphics2D)g).drawImage(bgImage, x*bgImageWidth, y*bgImageHeight, null);
          }
        }
      } else {
        ((Graphics2D)g).drawImage(bgImage, size.width/2-bgImageWidth/2, size.height/2-bgImageHeight/2, null);
      }
    }

    if (drawSelection == true) {
      ((Graphics2D)g).setColor(Prefs.dragColor);
      Stroke previous = ((Graphics2D)g).getStroke();
      ((Graphics2D)g).setStroke(selectionStroke);
      ((Graphics2D)g).draw(selectionRect);
      ((Graphics2D)g).setStroke(previous);
    }
  }
}