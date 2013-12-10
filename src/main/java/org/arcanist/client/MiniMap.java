package org.arcanist.client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.arcanist.client.*;


public class MiniMap extends JPanel {

  private static final int IDLE = 0;
  private static final int VIEW_DRAGGING = 1;
  private static final int GROUP_CREATING = 2;

  private ArcanistCCGFrame frame = null;

  private MiniMap pronoun = this;
  private Rectangle area = new Rectangle(0,0,1,1);
  private Rectangle mapRect = new Rectangle();
  private Rectangle viewRect = new Rectangle();
  private Point groupOrigin = new Point();
  private Rectangle groupRect = new Rectangle();
  private int status = IDLE;
  private float scale = 1;
  private float[] dashes = {5f,10f};
  private BasicStroke groupStroke = new BasicStroke(2,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_ROUND,5,dashes,4f);

  private ActionListener paintListener = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      pronoun.revalidate();
      updateScale();
      pronoun.repaint(pronoun.getBounds());
    }
  };
  private Timer repainter = new Timer(500, paintListener);


  public MiniMap(ArcanistCCGFrame f) {
    super();
    frame = f;

    this.setLayout(null);

    MouseListener mapClick = new MouseAdapter() {
      boolean moved = false;

      MouseMotionListener viewDragability = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          int pendX = e.getX() - viewRect.width/2;
          int pendY = e.getY() - viewRect.height/2;
          setViewPosition(pendX, pendY);
          moved = true;
        }
      };

      MouseMotionListener groupSelectability = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          int pendX = e.getX();
          int pendY = e.getY();
          if (pendX < 0) pendX = 0;
          if (pendY < 0) pendY = 0;
          if (pendX > mapRect.width) pendX = mapRect.width;
          if (pendY > mapRect.height) pendY = mapRect.height;
          if (pendX > groupOrigin.x) groupRect.width = pendX - groupOrigin.x;
          else {
            groupRect.width = groupOrigin.x - pendX;
            groupRect.x = pendX;
          }
          if (pendY > groupOrigin.y) groupRect.height = pendY - groupOrigin.y;
          else {
            groupRect.height = groupOrigin.y - pendY;
            groupRect.y = pendY;
          }

          pronoun.repaint();
          moved = true;
        }
      };

      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getButton() == 1) {
          int pendX = e.getX() - viewRect.width/2;
          int pendY = e.getY() - viewRect.height/2;
          setViewPosition(pendX, pendY);
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {               // Dragging freaks out if frame isn't selected
        if (frame.getJumboFrame().isSelected() == false) {
          try {
            frame.getJumboFrame().setSelected(true);
            frame.getJumboFrame().moveToBack();
          }
          catch (java.beans.PropertyVetoException exception) {
            ArcanistCCG.LogManager.write(exception, "Couldn't move frame to back for drag safety.");
          }
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        if (e.getButton() == 1) {
          moved = false;
          if (e.isShiftDown() == true) {
            status = GROUP_CREATING;
            groupOrigin.setLocation(e.getPoint());
            groupRect.setLocation(groupOrigin);
            groupRect.setSize(0, 0);
            pronoun.addMouseMotionListener(groupSelectability);
          }
          else if (viewRect.contains(e.getPoint())) {
            status = VIEW_DRAGGING;
            pronoun.addMouseMotionListener(viewDragability);
          }
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
                                                             //A Click triggers all 3: P,R,C
                                                             //So clicking briefly adds the listener
        if (status == VIEW_DRAGGING) {
          pronoun.removeMouseMotionListener(viewDragability);
          status = IDLE;
          if (moved) {
            int pendX = e.getX() - viewRect.width/2;
            int pendY = e.getY() - viewRect.height/2;
            setViewPosition(pendX, pendY);
          }
        }
        else if (status == GROUP_CREATING) {
          pronoun.removeMouseMotionListener(groupSelectability);
          status = IDLE;
          pronoun.repaint();

          if (!groupRect.isEmpty()) {
            Rectangle groupBounds = new Rectangle((int)(groupRect.x / scale), (int)(groupRect.y / scale), (int)(groupRect.width / scale), (int)(groupRect.height / scale));
            MassDragger newMD = new MassDragger(groupBounds, frame);
            newMD.addToTable();
          }
        }
      }
    };
    this.addMouseListener(mapClick);

    // Update immediately when the MiniMap is revealed
    repainter.setInitialDelay(0);
  }


  /**
   * Sets the upper-left corner of the minimap's view rectangle.
   */
  private void setViewPosition(int x, int y) {
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (x > mapRect.width-viewRect.width) x = mapRect.width-viewRect.width;
    if (y > mapRect.height-viewRect.height) y = mapRect.height-viewRect.height;
    viewRect.setLocation(x, y);
    pronoun.repaint();
    frame.getTablePane().scrollRectToVisible(new Rectangle((int)(viewRect.x / scale), (int)(viewRect.y / scale), (int)(viewRect.width / scale), (int)(viewRect.height / scale)));
  }


  public void startRepainter() {repainter.start();}
  public void stopRepainter() {repainter.stop();}


  public void updateScale() {
    JComponent t = frame.getTablePane();
    float aspect = (float)t.getWidth() / (float)t.getHeight();
    if (aspect < 1) scale = (float)this.getHeight() / (float)t.getHeight();
    else scale = (float)this.getWidth() / (float)t.getWidth();

    area.setSize(this.getSize());
    //Rectangle r = g.getClipBounds();  //This is for use in paintComponent()
    if (aspect > (float)area.width / (float)area.height) mapRect.setSize(area.width-1, (int)((float)area.width/aspect));
    else mapRect.setSize((int)((float)area.height*aspect), area.height-1);

    if (status == IDLE) {
      Rectangle v = frame.getTableView();
      viewRect.setLocation((int)(v.x*scale), (int)(v.y*scale));
      viewRect.setSize((int)(v.width*scale), (int)(v.height*scale));
    }
  }


  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    g.setColor(Color.black);
    for (int i=0; i < frame.getCardCount(); i++) {
      Component child = frame.getCard(i).getComponent();
      g.drawRect((int)(child.getX()*scale), (int)(child.getY()*scale), (int)(child.getWidth()*scale), (int)(child.getHeight()*scale));
    }
    for (int i=0; i < frame.getDeckCount(); i++) {
      Component child = frame.getDeck(i).getComponent();
      g.drawRect((int)(child.getX()*scale), (int)(child.getY()*scale), (int)(child.getWidth()*scale), (int)(child.getHeight()*scale));
    }
    for (int i=0; i < frame.getTokenCount(); i++) {
      Component child = frame.getToken(i).getComponent();
      g.drawRect((int)(child.getX()*scale), (int)(child.getY()*scale), (int)(child.getWidth()*scale), (int)(child.getHeight()*scale));
    }
    for (int i=0; i < frame.getNoteCount(); i++) {
      Component child = frame.getNote(i).getComponent();
      g.drawRect((int)(child.getX()*scale), (int)(child.getY()*scale), (int)(child.getWidth()*scale), (int)(child.getHeight()*scale));
    }
    for (int i=0; i < frame.getHandCount(); i++) {
      Component child = frame.getHand(i);
      g.drawRect((int)(child.getX()*scale), (int)(child.getY()*scale), (int)(child.getWidth()*scale), (int)(child.getHeight()*scale));
    }
    g.drawRect(mapRect.x, mapRect.y, mapRect.width, mapRect.height);

    g.setColor(Color.red);
    g.drawRect(viewRect.x, viewRect.y, viewRect.width, viewRect.height);

    if (status == GROUP_CREATING) {
      ((Graphics2D)g).setColor(Prefs.dragColor);
      Stroke previous = ((Graphics2D)g).getStroke();
      ((Graphics2D)g).setStroke(groupStroke);
      ((Graphics2D)g).draw(groupRect);
      ((Graphics2D)g).setStroke(previous);
    }
  }

}
