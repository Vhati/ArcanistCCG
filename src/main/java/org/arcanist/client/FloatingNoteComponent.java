package org.arcanist.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;


/**
 * This is a text field for floating notes.
 * It has a serial number for uniqueness.
 */
public class FloatingNoteComponent extends JPanel implements GuiInterruptListener {

  public static final String ACTION_EDITING_STARTED = "Editing Started";
  public static final String ACTION_EDITING_ENDED = "Editing Ended";
  public static final String ACTION_CHANGED_TEXT = "Changed Text";

  private ArcanistCCGFrame frame = null;

  private FloatingNoteComponent pronoun = this;

  private boolean editing = false;
  private boolean paintLock = false;

  private int id = -1;
  private FloatingNote parentNote;
  private JTextField displayField = null;
  private JTextField editField = null;
  private JPanel glassPane = null;


  public FloatingNoteComponent(ArcanistCCGFrame f, String string, FloatingNote parent) {
    super();
    frame = f;
    parentNote = parent;
    this.setLayout(new OverlayLayout(this));

    glassPane = new NoteGlassPanel(new BorderLayout());
      glassPane.setOpaque(false);
      this.add(glassPane);

    editField = new JTextField();
      editField.setVisible(false);
      editField.setBackground(new Color(245, 245, 200));
      this.add(editField);

    displayField = new JTextField();
      displayField.setEnabled(false);
      displayField.setDisabledTextColor(Color.BLACK);
      this.add(displayField);

    //No need for built-in drag selection...
    MouseMotionListener[] doomedListeners = (MouseMotionListener[])(displayField.getListeners(MouseMotionListener.class));
    for (int i=0; i < doomedListeners.length; i++) {
      displayField.removeMouseMotionListener(doomedListeners[i]);
    }

    FocusListener alteredText = new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (!editing) return;

        fireActionPerformed(new ActionEvent(pronoun, ActionEvent.ACTION_PERFORMED, ACTION_EDITING_STARTED));
      }

      @Override
      public void focusLost(FocusEvent e) {
        if (!editing) return;

        editField.setVisible(false);
        editing = false;
        if (editField.getText().equals(displayField.getText()) == false) {
          //setText(editField.getText());
          fireActionPerformed(new ActionEvent(pronoun, ActionEvent.ACTION_PERFORMED, ACTION_CHANGED_TEXT));
        }
        fireActionPerformed(new ActionEvent(pronoun, ActionEvent.ACTION_PERFORMED, ACTION_EDITING_ENDED));
      }
    };
    editField.addFocusListener(alteredText);

    setText(string);
  }


  /**
   * Adds an ActionListener.
   */
  public void addActionListener(ActionListener l) {
    listenerList.add(ActionListener.class, l);
  }

  /**
   * Removes an ActionListener.
   */
  public void removeActionListener(ActionListener l) {
    listenerList.remove(ActionListener.class, l);
  }

  /**
   * Returns an array of all the ActionListeners added
   * to this with addActionListener().
   *
   * @return all of the ActionListeners added or an empty
   *         array if no listeners have been added
   */
  public ActionListener[] getActionListeners() {
    return (ActionListener[])(listenerList.getListeners(ActionListener.class));
  }

  /**
   * Notifies all listeners that have registered interest for
   * notification on this event type. The event instance
   * is lazily created using the event parameter.
   *
   */
  protected void fireActionPerformed(ActionEvent event) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    ActionEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i=listeners.length-2; i >= 0; i-=2) {
      if (listeners[i]==ActionListener.class) {
        // Lazily create the event:
        if (e == null) {
          String actionCommand = event.getActionCommand();
          e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand, event.getWhen(), event.getModifiers());
        }
        ((ActionListener)listeners[i+1]).actionPerformed(e);
      }
    }
  }


  @Override
  public void guiInterrupted() {
    if (!editing) return;

    editField.setVisible(false);
    editing = false;
    fireActionPerformed(new ActionEvent(pronoun, ActionEvent.ACTION_PERFORMED, ACTION_EDITING_ENDED));
  }


  /**
   * Sets the serial number.
   *
   * @param n id.
   */
  public void setId(int n) {
    id = n;
  }


  /**
   * Returns the serial number.
   */
  public int getId() {
    return id;
  }


  /**
   * Sets whether to paint a lock.
   *
   * @param b true to paint a lock, false otherwise
   */
  public void setPaintLock(boolean b) {paintLock = b;}

  /**
   * Gets whether to paint a lock.
   *
   * @return true to paint a lock, false otherwise
   */
  public boolean getPaintLock() {return paintLock;}


  public JPanel getGlassPane() {return glassPane;}


  /**
   * Handle a mouse click.
   * Use a sparate MouseListener to catch the
   * event and decide whether to pass it here.
   *
   * @param e a MOUSE_CLICKED event in the glassPane's coords
   */
  public void handleClick(MouseEvent e) {
    if (!editField.isVisible()) {
      editing = true;
      editField.setText(displayField.getText());
      editField.setVisible(true);
    }
    Point editSpace = SwingUtilities.convertPoint(glassPane, e.getPoint(), editField);
    editField.dispatchEvent(new MouseEvent(editField, MouseEvent.MOUSE_PRESSED, e.getWhen(), e.getModifiers(), editSpace.x, editSpace.y, 1, e.isPopupTrigger()));
    editField.dispatchEvent(new MouseEvent(editField, MouseEvent.MOUSE_RELEASED, e.getWhen(), e.getModifiers(), editSpace.x, editSpace.y, 1, e.isPopupTrigger()));
  }


  public void setText(String s) {
    displayField.setText(s);

    if (s.length() > 0) {
      this.setPreferredSize(displayField.getPreferredSize());

      if (this.getParent() != null) {
        this.setSize(displayField.getPreferredSize());
        this.revalidate();
        this.repaint();
      }
    }
  }

  public String getText() {return displayField.getText();}

  public String getEditedText() {return editField.getText();}


  public void setEditable(boolean b) {displayField.setEditable(b);}

  public boolean isEditable() {return displayField.isEditable();}


  public javax.swing.text.Document getDocument() {
    return displayField.getDocument();
  }


  private class NoteGlassPanel extends JPanel {
    public NoteGlassPanel(LayoutManager layout) {super(layout);}


    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = null;

      if (frame.isDragging() == false && paintLock && !editing) {
        // Draw the lock
        g2 = (Graphics2D)(g.create());
        g2.setColor(Color.ORANGE);
        g2.fillRect(16, 4, 2, 6);
        g2.fillRect(16, 4, 6, 2);
        g2.fillRect(20, 4, 2, 5);
        g2.fillRect(14, 9, 10, 9);
      }
    }
  }
}