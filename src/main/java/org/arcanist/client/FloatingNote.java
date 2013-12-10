package org.arcanist.client;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;


/**
 * This is a draggable textfield.
 * It notifies opponents when focus is lost.
 */
public class FloatingNote implements ActionListener, GuiInterruptListener {

  private ArcanistCCGFrame frame = null;

  private FloatingNote pronoun = this;

  private String prevText;
  private boolean editing = false;


  /** The note's visible component. */
  private FloatingNoteComponent textComp;

  /** Should this be inert. */
  private boolean inUse = false;

  /** Context menu. */
  private JPopupMenu popup = null;

  private JMenuItem duplicateMenuItem = new JMenuItem("Duplicate");
  private JMenuItem removeMenuItem = new JMenuItem("Remove");

  private GuiInterruptMouseAdapter floatingNoteDrag = null;


  public FloatingNote(ArcanistCCGFrame f, final String text) {
    frame = f;

    textComp = new FloatingNoteComponent(frame, text, this);


    //Setup Popup menu
    popup = new JPopupMenu();
      popup.add(duplicateMenuItem);
      popup.add(removeMenuItem);


    // Set up listeners
    duplicateMenuItem.addActionListener(this);
    removeMenuItem.addActionListener(this);
    textComp.addActionListener(this);


    floatingNoteDrag = new GuiInterruptMouseAdapter() {
      boolean wasBusy = true;                                //Remembers not to unlock on release
      int xdist=0, ydist=0;
      JComponent dragObj = textComp.getGlassPane();

      @Override
      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        if (e.getButton() == 1) {
          xdist = e.getX();
          ydist = e.getY();
          ArcanistCCG.NetManager.noteUse(arrayID());
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

        ArcanistCCG.NetManager.noteUnuse(arrayID());

        if (moved) {
          e = SwingUtilities.convertMouseEvent(dragObj, e, textComp.getParent());  //Convert note coords to table coords

          ArcanistCCG.NetManager.noteMove(arrayID(), (e.getX()-xdist), (e.getY()-ydist));
        }

        frame.getTablePane().repaint();
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (wasBusy == true) return;

        textComp.handleClick(e);
        //actionPerformed() below will handle use/unuse during editing
      }

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

          ArcanistCCG.NetManager.noteUnuse(arrayID());
        }
      }
    };
    textComp.getGlassPane().addMouseListener(floatingNoteDrag);

    textComp.getGlassPane().addMouseListener(frame.getFocusOnEnterListener());
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    //textField tracks inUse internally, and toggles via ActionEvents

    Object source = e.getSource();

    if (source == textComp) {
      if (e.getActionCommand() != null) {
        if (pronoun.existsOnTable() == true) {
          if (e.getActionCommand().equals(FloatingNoteComponent.ACTION_EDITING_STARTED)) {
            ArcanistCCG.NetManager.noteUse(arrayID());
          }
          else if (e.getActionCommand().equals(FloatingNoteComponent.ACTION_CHANGED_TEXT)) {
            ArcanistCCG.NetManager.noteAlter(arrayID(), textComp.getEditedText());
          }
          else if (e.getActionCommand().equals(FloatingNoteComponent.ACTION_EDITING_ENDED)) {
            ArcanistCCG.NetManager.noteUnuse(arrayID());
          }
        }
      }
    }

    if (isInUse() == true) return;

    if (source == duplicateMenuItem) {
      if (existsOnTable()) {
        Rectangle tableView = frame.getTableView();
        ArcanistCCG.NetManager.noteAdd(tableView.x, tableView.y, getText());
      }
    }
    else if (source == removeMenuItem) {
      if (existsOnTable()) {
        ArcanistCCG.NetManager.noteRemove(arrayID());
      }
    }
  }


  @Override
  public void guiInterrupted() {
    floatingNoteDrag.guiInterrupted();
    textComp.guiInterrupted();
  }


  public String toString() {
    return "Note" + textComp.getId() +
    "\nArrayID: "+ arrayID();
  }


  /**
   * Returns this object's table component.
   */
  public FloatingNoteComponent getComponent() {
    return textComp;
  }


  /**
   * Gets this note's ID.
   *
   * @return the ID
   * @see FloatingNoteComponent#getId()
   */
  public int getId() {return textComp.getId();}

  /**
   * Sets this note's id.
   *
   * @param n new id
   */
  public void setId(int n) {textComp.setId(n);}


  /**
   * Gets this card's index within the ArcanistCCGFrame's note array.
   *
   * @return the index, or -1 if absent
   */
  public int arrayID() {
    return frame.getNoteIndex(this);
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
    textComp.setPaintLock(state);
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
    Dimension fieldSize = textComp.getPreferredSize();

    textComp.setVisible(false);
    frame.addNote(this);
    frame.getTablePane().add(textComp, Prefs.noteLayer, 0);
    textComp.setBounds(x, y, fieldSize.width, fieldSize.height);
    textComp.setVisible(true);
  }

  /**
   * Removes this from the table.
   * Attributes are not reset.
   * The table will need to be repainted afterward.
   */
  public void removeFromTable() {
    popup.setVisible(false);
    frame.removeNote(arrayID());
    frame.getTablePane().remove(textComp);
  }

  /**
   * Determines whether this is present on the table.
   * @return true if it exists, false otherwise
   */
  public boolean existsOnTable() {
    return frame.hasNote(this);
  }


  /**
   * Sets this object's location.
   */
  public void setLocation(int x, int y) {
    textComp.setLocation(x, y);
  }

  /**
   * Returns this object's location.
   */
  public Point getLocation() {
    return textComp.getLocation();
  }

  /**
   * Returns this object's x position.
   */
  public int getX() {
    return textComp.getX();
  }

  /**
   * Returns this object's y position.
   */
  public int getY() {
    return textComp.getY();
  }


  /**
   * Sets the text.
   * @param input
   */
  public void setText(String input) {textComp.setText(input);}

  /**
   * Gets the text.
   * @return the text
   */
  public String getText() {return textComp.getText();}


  /**
   * Sets ability to edit.
   * @param state
   */
  public void setEditable(boolean state) {textComp.setEditable(state);}

  /**
   * Gets ability to edit.
   * @return true if editable, false otherwise
   */
  public boolean isEditable() {return textComp.isEditable();}
}
