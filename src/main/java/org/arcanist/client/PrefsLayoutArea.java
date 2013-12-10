package org.arcanist.client;

import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


/**
 * A draggable/stretchable textarea for text layout.
 */
public class PrefsLayoutArea extends JTextArea implements ActionListener {

  private int number = -1;
  public int[] proposedArray;
  private PrefsLayoutArea pronoun = this;
  private JPanel rightZone = new JPanel();
  private JPanel bottomZone = new JPanel();

  private JRadioButtonMenuItem hiddenMenuItem = new JRadioButtonMenuItem("Hidden");
  private JRadioButtonMenuItem fieldMenuItem = new JRadioButtonMenuItem("Field");
  private JRadioButtonMenuItem areaMenuItem = new JRadioButtonMenuItem("Area");


  public PrefsLayoutArea(int n, int[] tempArray) {
    number = n;
    proposedArray = new int[tempArray.length];
    for (int i=0; i < tempArray.length; i++) {
      proposedArray[i] = tempArray[i];
      if (i>2 && proposedArray[i]==0)
        proposedArray[i]=20;
    }

    switch (proposedArray[0]) {
      case(0):
        setText(number +"H");
        setBounds(new Rectangle(proposedArray[1], proposedArray[2], proposedArray[3], 20));
        break;
      case(1):
        setText(number +"F");
        setBounds(new Rectangle(proposedArray[1], proposedArray[2], proposedArray[3], 20));
        break;
      case(2):
        setText(number +"A");
        setBounds(new Rectangle(proposedArray[1], proposedArray[2], proposedArray[3], proposedArray[4]));
        this.add(bottomZone);
        break;
    }
    setEditable(false);
    setMargin(new Insets(2,2,2,2));


    this.add(rightZone);
      rightZone.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
    bottomZone.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
    updateZones();

    //Setup Popup menu
    final JPopupMenu popup = new JPopupMenu();
      ButtonGroup typeGroup = new ButtonGroup();
        // hiddenMenuItem
          if (proposedArray[0]==0) hiddenMenuItem.setSelected(true);
          typeGroup.add(hiddenMenuItem);
          popup.add(hiddenMenuItem);
        // fieldMenuItem
          if (proposedArray[0]==1) fieldMenuItem.setSelected(true);
          typeGroup.add(fieldMenuItem);
          popup.add(fieldMenuItem);
        // areaMenuItem
          if (proposedArray[0]==2) areaMenuItem.setSelected(true);
          typeGroup.add(areaMenuItem);
          popup.add(areaMenuItem);

    //Setup listeners
    MouseListener AreaDrag = new MouseAdapter() {
      MouseMotionListener dragability;
      int xdist=0, ydist=0;

      @Override
      public void mousePressed(MouseEvent e) {
        if (e.getButton() == 1) {
          xdist = SwingUtilities.convertMouseEvent(pronoun, e, pronoun).getX();
          ydist = SwingUtilities.convertMouseEvent(pronoun, e, pronoun).getY();

          dragability = new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
              e = SwingUtilities.convertMouseEvent(pronoun, e, pronoun.getParent());  //Convert coords
              if (e.getX()-xdist > 0)
                proposedArray[1] = e.getX()-xdist;
              if (e.getY()-ydist > 0)
                proposedArray[2] = e.getY()-ydist;
              pronoun.setBounds(new Rectangle(proposedArray[1], proposedArray[2], pronoun.getWidth(), pronoun.getHeight()));
            }
          };
          pronoun.addMouseMotionListener(dragability);
        }
        //Right-Click Popup menu
        //  Technically This should be in both pressed and released
        //  Mac checks the trigger on press, Win on release
        //  But only macs have 1-button mice, which need the trigger check ;)
        if (e.getButton() == 3 || e.isPopupTrigger()) {popup.show(e.getComponent(), e.getX(), e.getY());}
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        pronoun.removeMouseMotionListener(dragability);
        pronoun.getParent().repaint();
      }
    };
    this.addMouseListener(AreaDrag);


    //rightZone listener
    MouseListener rightResize = new MouseAdapter() {
      MouseMotionListener dragability;

      @Override
      public void mousePressed(MouseEvent e) {
        if (e.getButton() == 1) {
          dragability = new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
              e = SwingUtilities.convertMouseEvent(rightZone, e, rightZone.getParent());  //Convert coords
              if (e.getX()>10) {
                proposedArray[3] = e.getX();
                rightZone.getParent().setBounds(new Rectangle(proposedArray[1], proposedArray[2], proposedArray[3], rightZone.getParent().getHeight()));
              }
            }
          };
          rightZone.addMouseMotionListener(dragability);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        rightZone.removeMouseMotionListener(dragability);
        rightZone.getParent().repaint();
        updateZones();
      }
    };
    rightZone.addMouseListener(rightResize);


    //bottomZone listener
    MouseListener bottomResize = new MouseAdapter() {
      MouseMotionListener dragability;

      @Override
      public void mousePressed(MouseEvent e) {
        if (e.getButton() == 1) {
          dragability = new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
              e = SwingUtilities.convertMouseEvent(bottomZone, e, bottomZone.getParent());  //Convert coords
              if (e.getY()>10) {
                proposedArray[4] = e.getY();
                bottomZone.getParent().setBounds(new Rectangle(proposedArray[1], proposedArray[2], bottomZone.getParent().getWidth(), proposedArray[4]));
              }
            }
          };
          bottomZone.addMouseMotionListener(dragability);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        bottomZone.removeMouseMotionListener(dragability);
        bottomZone.getParent().repaint();
        updateZones();
      }
    };
    bottomZone.addMouseListener(bottomResize);

    hiddenMenuItem.addActionListener(this);
    fieldMenuItem.addActionListener(this);
    areaMenuItem.addActionListener(this);
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == hiddenMenuItem) {
      if (proposedArray[0]==2)
        pronoun.remove(bottomZone);
      proposedArray[0] = 0;
      pronoun.setText(number +"H");
      pronoun.setBounds(new Rectangle(proposedArray[1], proposedArray[2], proposedArray[3], 20));
      updateZones();
      pronoun.getParent().repaint();
    }
    else if (source == fieldMenuItem) {
      if (proposedArray[0]==2)
        pronoun.remove(bottomZone);
      proposedArray[0] = 1;
      pronoun.setText(number +"F");
      pronoun.setBounds(new Rectangle(proposedArray[1], proposedArray[2], proposedArray[3], 20));
      updateZones();
      pronoun.getParent().repaint();
    }
    else if (source == areaMenuItem) {
      if (proposedArray[0]==0 || proposedArray[0]==1)
        pronoun.add(bottomZone);
      proposedArray[0] = 2;
      pronoun.setText(number +"A");
      pronoun.setBounds(new Rectangle(proposedArray[1], proposedArray[2], proposedArray[3], proposedArray[4]));
      updateZones();
      pronoun.getParent().repaint();
    }
  }


  private void updateZones() {
    if (proposedArray[0]==2) {
      rightZone.setBounds(new Rectangle(proposedArray[3]-5, 4, 5, proposedArray[4]-8));
      bottomZone.setBounds(new Rectangle(4, proposedArray[4]-5, proposedArray[3]-8, 5));
    }
    else {
      rightZone.setBounds(new Rectangle(proposedArray[3]-5, 4, 5, 20-8));
      bottomZone.setBounds(new Rectangle(4, 20-5, proposedArray[3]-8, 5));
    }
  }
}
