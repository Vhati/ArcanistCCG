package org.arcanist.client;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;


/**
 * A draggable image.
 * Tokens are only capable of being dragged and removed.
 */
public class Token implements ActionListener, GuiInterruptListener {

  private ArcanistCCGFrame frame = null;

  private Token pronoun = this;

  /** Token image. */
  private TokenComponent tokenComp;

  private String imagePath = "";

  /** Should this be inert. */
  public boolean inUse = false;

  /** Context menu */
  private JPopupMenu popup = null;

  private JMenuItem removeMenuItem = new JMenuItem("Remove");

  private GuiInterruptMouseAdapter tokenDrag = null;


  public Token(ArcanistCCGFrame f, String path) {
    frame = f;
    imagePath = path;

    BufferedImage image = Prefs.Cache.getCachedOriginalImage(imagePath);
    tokenComp = new TokenComponent(frame, image);

    //Setup Popup menu
    popup = new JPopupMenu();
      popup.add(removeMenuItem);


    // Set up listeners
    removeMenuItem.addActionListener(this);


    tokenDrag = new GuiInterruptMouseAdapter() {
      boolean wasBusy = true;                                //Remembers not to unlock on release
      int xdist=0, ydist=0;
      JComponent dragObj = tokenComp;

      @Override
      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        if (e.getButton() == 1) {
          xdist = e.getX();
          ydist = e.getY();
          ArcanistCCG.NetManager.tokenUse(arrayID());
          frame.setDragging(true);

          DragGhost dragGhost = frame.getDragGhost();
          dragGhost.setOffset(xdist, ydist);
          dragGhost.setSourceObject(dragObj);
          frame.addDragGhost();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
        }
        //Right-Click Popup menu
        //  Technically This should be in both pressed and released
        //  Mac checks the trigger on press, Win on release
        //  But only macs have 1-button mice, which need the trigger check ;)
        if (e.getButton() == 3 || e.isPopupTrigger()) {popup.show(e.getComponent(), e.getX(), e.getY());}
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (wasBusy == true) return;
                                                             //A Click triggers all 3: P,R,C
                                                             //So clicking briefly adds the listener
        DragGhost dragGhost = frame.getDragGhost();
        boolean moved = dragGhost.hasMoved();
        dragGhost.setSourceObject(null);
        frame.removeDragGhost();
        frame.setDragging(false);
        frame.getTablePane().revalidate();
        frame.getTablePane().repaint();

        ArcanistCCG.NetManager.tokenUnuse(arrayID());

        if (moved) {
          e = SwingUtilities.convertMouseEvent(dragObj, e, tokenComp.getParent());  //Convert to table coords

          ArcanistCCG.NetManager.tokenMove(arrayID(), (e.getX()-xdist), (e.getY()-ydist));
        }
      }

      @Override
      public void guiInterrupted() {
        popup.setVisible(false);
        wasBusy = true;

        DragGhost dragGhost = frame.getDragGhost();
        if (dragObj.equals(dragGhost.getSourceObject())) {
          dragGhost.setSourceObject(null);
          frame.removeDragGhost();
          frame.setDragging(false);
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();

          ArcanistCCG.NetManager.tokenUnuse(arrayID());
        }
      }
    };
    tokenComp.addMouseListener(tokenDrag);
    tokenComp.addMouseListener(frame.getFocusOnEnterListener());
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    if (isInUse() == true) return;

    Object source = e.getSource();

    if (source == removeMenuItem) {
      if (pronoun.existsOnTable()) {
        ArcanistCCG.NetManager.tokenRemove(arrayID());
      }
    }
  }


  @Override
  public void guiInterrupted() {
    tokenDrag.guiInterrupted();
  }


  /**
  * Standard debug info.
  */
  public String toString() {
    return "Token" + tokenComp.getId() +
    "\nArrayID: "+ arrayID();
  }


  /**
   * Returns this object's table component.
   */
  public TokenComponent getComponent() {
    return tokenComp;
  }


  /**
   * Gets this token's ID.
   *
   * @return the ID
   * @see TokenComponent#getId()
   */
  public int getId() {return tokenComp.getId();}

  /**
   * Sets this token's id.
   *
   * @param n new id
   */
  public void setId(int n) {tokenComp.setId(n);}


  /**
   * Gets this card's index within the ArcanistCCGFrame's token array.
   *
   * @return the index, or -1 if absent
   */
  public int arrayID() {
    return frame.getTokenIndex(this);
  }


  /**
   * Sets the used state.
   * Objects that are in use are busy with an opponent.
   * They will not respond to user input.
   *
   * @param state true if in use, false otherwise
   */
  public void setInUse(boolean state) {
    inUse = state;
    tokenComp.setPaintLock(state);
  }

  /**
   * Gets the used state.
   * Objects that are in use are busy with an opponent.
   * They will not respond to user input.
   *
   * @return true if in use, false otherwise
   */
  public boolean isInUse() {return inUse;}


  /**
   * Adds this to the table.
   * The table will need to be validated and repainted afterward.
   */
  public void addToTable(int x, int y) {
    tokenComp.setVisible(false);
    frame.addToken(this);
    frame.getTablePane().add(tokenComp, Prefs.tokenLayer, 0);
    tokenComp.setBounds(x, y, tokenComp.getImage().getWidth(), tokenComp.getImage().getHeight());
    tokenComp.setVisible(true);
  }

  /**
   * Removes this from the table.
   * Attributes like autoface are not reset.
   * The table will need to be repainted afterward.
   */
  public void removeFromTable() {
    popup.setVisible(false);
    frame.removeToken(arrayID());
    frame.getTablePane().remove(tokenComp);
  }

  /**
   * Determines whether this is present on the table.
   * @return true if it exists, false otherwise
   */
  public boolean existsOnTable() {
    return frame.hasToken(this);
  }


  /**
   * Sets this object's location.
   */
  public void setLocation(int x, int y) {
    tokenComp.setLocation(x, y);
  }

  /**
   * Returns this object's location.
   */
  public Point getLocation() {
    return tokenComp.getLocation();
  }

  /**
   * Returns this object's x position.
   */
  public int getX() {
    return tokenComp.getX();
  }

  /**
   * Returns this object's y position.
   */
  public int getY() {
    return tokenComp.getY();
  }


  /**
   * Returns this tokens image path.
   *
   * @return this token's image path
   */
  public String getImagePath() {
    return imagePath;
  }
}
