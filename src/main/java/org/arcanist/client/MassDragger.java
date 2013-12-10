package org.arcanist.client;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.*;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A draggable object that affects things under it.
 */
public class MassDragger extends JComponent implements ActionListener, GuiInterruptListener {

  private ArcanistCCGFrame frame = null;

  private TexturePaint texturePaint = null;

  private MassDragger pronoun = this;
  private boolean filterCard = true;
  private boolean filterDeck = true;
  private boolean filterToken = true;
  private boolean filterNote = true;
  private List<Card> cardGroup = new ArrayList<Card>(0);
    private List<Point> cardFudge = new ArrayList<Point>(0);
  private List<Deck> deckGroup = new ArrayList<Deck>(0);
    private List<Point> deckFudge = new ArrayList<Point>(0);
  private List<Token> tokenGroup = new ArrayList<Token>(0);
    private List<Point> tokenFudge = new ArrayList<Point>(0);
  private List<FloatingNote> noteGroup = new ArrayList<FloatingNote>(0);
    private List<Point> noteFudge = new ArrayList<Point>(0);
  private Rectangle selection = null;

  /** Should this be inert. */
  public boolean inUse = false;

  /** Context menu */
  private JPopupMenu popup = null;


  private JMenuItem cardLocalFlipMenuItem = new JMenuItem("Local Flip");
  private JMenuItem cardRemoteFlipMenuItem = new JMenuItem("Remote Flip");
  private JMenuItem cardFlipMenuItem = new JMenuItem("Flip");
  private JMenuItem cardTurn0MenuItem = new JMenuItem("Reset Rotation");
  private JMenuItem cardTurn90MenuItem = new JMenuItem("=90");
    private JMenuItem cardTurnMinus90MenuItem = new JMenuItem("-90");
    private JMenuItem cardTurnPlus90MenuItem = new JMenuItem("+90");
  private JMenuItem cardTurn180MenuItem = new JMenuItem("=180");
    private JMenuItem cardTurnMinus180MenuItem = new JMenuItem("-180");
    private JMenuItem cardTurnPlus180MenuItem = new JMenuItem("+180");
  private JMenuItem cardTurn270MenuItem = new JMenuItem("=270");
    private JMenuItem cardTurnMinus270MenuItem = new JMenuItem("-270");
    private JMenuItem cardTurnPlus270MenuItem = new JMenuItem("+270");
  private JMenuItem cardRandomMenuItem = new JMenuItem("Pick Random");
  private JMenuItem cardRemoveMenuItem = new JMenuItem("Remove");

  private JMenuItem deckLockTrueMenuItem = new JMenuItem("Set");
  private JMenuItem deckLockFalseMenuItem = new JMenuItem("Unset");
  private JMenuItem deckFacingTrueMenuItem = new JMenuItem("Set");
  private JMenuItem deckFacingFalseMenuItem = new JMenuItem("Unset");
  private JMenuItem deckToBottomTrueMenuItem = new JMenuItem("Set");
  private JMenuItem deckToBottomFalseMenuItem = new JMenuItem("Unset");
  private JMenuItem deckDirUMenuItem = new JMenuItem("Up");
  private JMenuItem deckDirDMenuItem = new JMenuItem("Down");
  private JMenuItem deckDirLMenuItem = new JMenuItem("Left");
  private JMenuItem deckDirRMenuItem = new JMenuItem("Right");
  private JMenuItem deckSaveMenuItem = new JMenuItem("Save...");
  private JMenuItem deckRemoveMenuItem = new JMenuItem("Remove");

  private JMenuItem tokenRemoveMenuItem = new JMenuItem("Remove");

  private JMenuItem noteRemoveMenuItem = new JMenuItem("Remove");

  private JMenuItem allRemoveMenuItem = new JMenuItem("Remove");

  private JRadioButtonMenuItem filterCardMenuItem = new JRadioButtonMenuItem("Cards", true);
  private JRadioButtonMenuItem filterDeckMenuItem = new JRadioButtonMenuItem("Decks", true);
  private JRadioButtonMenuItem filterTokenMenuItem = new JRadioButtonMenuItem("Tokens", true);
  private JRadioButtonMenuItem filterNoteMenuItem = new JRadioButtonMenuItem("Notes", true);

  private JMenuItem countMenuItem = new JMenuItem("Count");
  private JMenuItem ungroupMenuItem = new JMenuItem("Ungroup");

  private GuiInterruptMouseAdapter groupDrag = null;


  public MassDragger(Rectangle bounds, ArcanistCCGFrame f) {
    selection = bounds;
    frame = f;

    //Declare Popup
    popup = new JPopupMenu();
      JMenu cardSubMenu = new JMenu("Cards");
        // cardLocalFlipMenuItem
        // cardRemoteFlipMenuItem
        // cardFlipMenuItem
        // cardTurn0MenuItem
        // cardTurn90MenuItem
          // cardTurnMinus90MenuItem
          // cardTurnPlus90MenuItem
        // cardTurn180MenuItem
          // cardTurnMinus180MenuItem
          // cardTurnPlus180MenuItem
        // cardTurn270MenuItem
          // cardTurnMinus270MenuItem
          // cardTurnPlus270MenuItem
        // cardRandomMenuItem
        // cardRemoveMenuItem
      JMenu deckSubMenu = new JMenu("Decks");
        JMenu deckLockSubMenu = new JMenu("Position Lock");
          // deckLockTrueMenuItem
          // deckLockFalseMenuItem
        JMenu deckFacingSubMenu = new JMenu("FaceUp");
          // deckFacingTrueMenuItem
          // deckFacingFalseMenuItem
        JMenu deckToBottomSubMenu = new JMenu("Add to Bottom");
          // deckToBottomTrueMenuItem
          // deckToBottomFalseMenuItem
        JMenu deckDirSubMenu = new JMenu("Card Offset");
          // deckDirUMenuItem
          // deckDirDMenuItem
          // deckDirLMenuItem
          // deckDirRMenuItem
        // deckSaveMenuItem
        // deckRemoveMenuItem
      JMenu tokenSubMenu = new JMenu("Tokens");
        // tokenRemoveMenuItem
      JMenu noteSubMenu = new JMenu("Floating Notes");
        // noteRemoveMenuItem
      JMenu allSubMenu = new JMenu("All");
        // allRemoveMenuItem
      JMenu filterSubMenu = new JMenu("Drag Filter");
        // filterCardMenuItem
        // filterDeckMenuItem
        // filterTokenMenuItem
        // filterNoteMenuItem
      // countMenuItem
      // ungroupMenuItem


      cardSubMenu.getPopupMenu().setLayout(new GridBagLayout());
      GridBagConstraints cardSubMenuC = new GridBagConstraints();
      cardSubMenuC.fill = GridBagConstraints.HORIZONTAL;
      cardSubMenuC.weightx = 0.0;
      cardSubMenuC.insets = new Insets(0,0,0,0);
      cardSubMenuC.gridwidth = GridBagConstraints.REMAINDER;

      //Build Popup
      popup.add(cardSubMenu);
        cardSubMenu.getPopupMenu().add(cardRemoteFlipMenuItem, cardSubMenuC);
        cardSubMenu.getPopupMenu().add(cardLocalFlipMenuItem, cardSubMenuC);
        cardSubMenu.getPopupMenu().add(cardFlipMenuItem, cardSubMenuC);
        cardSubMenu.getPopupMenu().add(cardTurn0MenuItem, cardSubMenuC);
        //They think they're 45x21, but they're 111x21 (as big as the fattest item), so I use negative padding
        cardSubMenuC.ipadx = -65;
        cardSubMenuC.gridwidth = 1;
        cardSubMenu.getPopupMenu().add(cardTurn90MenuItem, cardSubMenuC);
        cardSubMenu.getPopupMenu().add(cardTurnMinus90MenuItem, cardSubMenuC);
        cardSubMenuC.gridwidth = GridBagConstraints.REMAINDER;
        cardSubMenu.getPopupMenu().add(cardTurnPlus90MenuItem, cardSubMenuC);

        cardSubMenuC.gridwidth = 1;
        cardSubMenu.getPopupMenu().add(cardTurn180MenuItem, cardSubMenuC);
        cardSubMenu.getPopupMenu().add(cardTurnMinus180MenuItem, cardSubMenuC);
        cardSubMenuC.gridwidth = GridBagConstraints.REMAINDER;
        cardSubMenu.getPopupMenu().add(cardTurnPlus180MenuItem, cardSubMenuC);

        cardSubMenuC.gridwidth = 1;
        cardSubMenu.getPopupMenu().add(cardTurn270MenuItem, cardSubMenuC);
        cardSubMenu.getPopupMenu().add(cardTurnMinus270MenuItem, cardSubMenuC);
        cardSubMenuC.gridwidth = GridBagConstraints.REMAINDER;
        cardSubMenu.getPopupMenu().add(cardTurnPlus270MenuItem, cardSubMenuC);

        cardSubMenuC.weightx = 1;
        cardSubMenuC.weighty = 1;
        cardSubMenuC.ipadx = 0;
        cardSubMenu.getPopupMenu().add(cardRandomMenuItem, cardSubMenuC);
        cardSubMenu.getPopupMenu().add(cardRemoveMenuItem, cardSubMenuC);
//System.out.println(cardSubMenu.getPreferredSize());
      popup.add(deckSubMenu);
        deckSubMenu.add(deckLockSubMenu);
          deckLockSubMenu.add(deckLockTrueMenuItem);
          deckLockSubMenu.add(deckLockFalseMenuItem);
        deckSubMenu.add(deckFacingSubMenu);
          deckFacingSubMenu.add(deckFacingTrueMenuItem);
          deckFacingSubMenu.add(deckFacingFalseMenuItem);
        deckSubMenu.add(deckToBottomSubMenu);
          deckToBottomSubMenu.add(deckToBottomTrueMenuItem);
          deckToBottomSubMenu.add(deckToBottomFalseMenuItem);
        deckSubMenu.add(deckDirSubMenu);
          deckDirSubMenu.add(deckDirUMenuItem);
          deckDirSubMenu.add(deckDirDMenuItem);
          deckDirSubMenu.add(deckDirLMenuItem);
          deckDirSubMenu.add(deckDirRMenuItem);
        deckSubMenu.add(deckSaveMenuItem);
        deckSubMenu.add(deckRemoveMenuItem);
      popup.add(tokenSubMenu);
        tokenSubMenu.add(tokenRemoveMenuItem);
      popup.add(noteSubMenu);
        noteSubMenu.add(noteRemoveMenuItem);
      popup.add(allSubMenu);
        allSubMenu.add(allRemoveMenuItem);
      popup.addSeparator();
      popup.add(filterSubMenu);
        filterSubMenu.add(filterCardMenuItem);
        filterSubMenu.add(filterDeckMenuItem);
        filterSubMenu.add(filterTokenMenuItem);
        filterSubMenu.add(filterNoteMenuItem);
      popup.add(countMenuItem);
      popup.add(ungroupMenuItem);


    // Set up listeners
    cardRemoteFlipMenuItem.addActionListener(this);
    cardLocalFlipMenuItem.addActionListener(this);
    cardFlipMenuItem.addActionListener(this);
    cardTurn0MenuItem.addActionListener(this);
    cardTurn90MenuItem.addActionListener(this);
    cardTurn180MenuItem.addActionListener(this);
    cardTurn270MenuItem.addActionListener(this);
    cardTurnMinus90MenuItem.addActionListener(this);
    cardTurnPlus90MenuItem.addActionListener(this);
    cardTurnMinus180MenuItem.addActionListener(this);
    cardTurnPlus180MenuItem.addActionListener(this);
    cardTurnMinus270MenuItem.addActionListener(this);
    cardTurnPlus270MenuItem.addActionListener(this);
    cardRandomMenuItem.addActionListener(this);
    cardRemoveMenuItem.addActionListener(this);
    deckLockTrueMenuItem.addActionListener(this);
    deckLockFalseMenuItem.addActionListener(this);
    deckFacingTrueMenuItem.addActionListener(this);
    deckFacingFalseMenuItem.addActionListener(this);
    deckToBottomTrueMenuItem.addActionListener(this);
    deckToBottomFalseMenuItem.addActionListener(this);
    deckDirUMenuItem.addActionListener(this);
    deckDirDMenuItem.addActionListener(this);
    deckDirLMenuItem.addActionListener(this);
    deckDirRMenuItem.addActionListener(this);
    deckSaveMenuItem.addActionListener(this);
    deckRemoveMenuItem.addActionListener(this);
    tokenRemoveMenuItem.addActionListener(this);
    noteRemoveMenuItem.addActionListener(this);
    allRemoveMenuItem.addActionListener(this);
    filterCardMenuItem.addActionListener(this);
    filterDeckMenuItem.addActionListener(this);
    filterTokenMenuItem.addActionListener(this);
    filterNoteMenuItem.addActionListener(this);
    countMenuItem.addActionListener(this);
    ungroupMenuItem.addActionListener(this);


    groupDrag = new GuiInterruptMouseAdapter() {
      boolean wasBusy = true;                                //Remembers not to unlock on release
      int xsnapDist=0, ysnapDist=0;
      int xdist=0, ydist=0;
      JComponent dragObj = pronoun;

      @Override
      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        if (e.getButton() == 1) {
          Point snapPoint = frame.getTablePane().getNearestGridPoint( new Point(pronoun.getBounds().x, pronoun.getBounds().y) );
          xsnapDist = pronoun.getBounds().x - (int)snapPoint.getX();
          ysnapDist = pronoun.getBounds().y - (int)snapPoint.getY();
          xdist = e.getX();
          ydist = e.getY();
          pronoun.setInUse(true);
          frame.setDragging(true);

          DragGhost dragGhost = frame.getDragGhost();
          dragGhost.setOffset(xdist, ydist, xsnapDist, ysnapDist);
          dragGhost.setSourceObject(dragObj);
          dragGhost.setSnap(true);
          frame.addDragGhost();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();

          cardGroup.clear(); cardFudge.clear();
          deckGroup.clear(); deckFudge.clear();
          tokenGroup.clear(); tokenFudge.clear();
          noteGroup.clear(); noteFudge.clear();

          if (filterCard) {
            for (int i=frame.getCardCount()-1; i >= 0; i--) {
              Card tmpCard = frame.getCard(i);
              Point tmpPoint = tmpCard.getLocation();
              if (selection.contains(tmpPoint)) {
                if ( tmpCard.isInUse() ) continue;
                cardGroup.add(tmpCard);
                cardFudge.add(new Point(tmpPoint.x-pronoun.getBounds().x, tmpPoint.y-pronoun.getBounds().y));
                ArcanistCCG.NetManager.cardUse(i);
              }
            }
          }
          if (filterDeck) {
            for (int i=frame.getDeckCount()-1; i >= 0; i--) {
              Deck tmpDeck = frame.getDeck(i);
              Point tmpPoint = tmpDeck.getLocation();
              if (selection.contains(tmpPoint)) {
                if (tmpDeck.isInUse()) continue;
                deckGroup.add(tmpDeck);
                deckFudge.add(new Point(tmpPoint.x-pronoun.getBounds().x, tmpPoint.y-pronoun.getBounds().y));
                ArcanistCCG.NetManager.deckUse(i);
              }
            }
          }
          if (filterToken) {
            for (int i=frame.getTokenCount()-1; i >= 0; i--) {
              Token tmpToken = frame.getToken(i);
              Point tmpPoint = tmpToken.getLocation();
              if ( selection.contains(tmpToken.getLocation()) ) {
                if (tmpToken.isInUse()) continue;
                tokenGroup.add(tmpToken);
                tokenFudge.add(new Point(tmpPoint.x-pronoun.getBounds().x, tmpPoint.y-pronoun.getBounds().y));
                ArcanistCCG.NetManager.tokenUse(i);
              }
            }
          }
          if (filterNote) {
            for (int i=frame.getNoteCount()-1; i >= 0; i--) {
              FloatingNote tmpNote = frame.getNote(i);
              Point tmpPoint = tmpNote.getLocation();
              if ( selection.contains(tmpPoint) ) {
                if (tmpNote.isInUse()) continue;
                noteGroup.add(tmpNote);
                noteFudge.add(new Point(tmpPoint.x-pronoun.getBounds().x, tmpPoint.y-pronoun.getBounds().y));
                ArcanistCCG.NetManager.noteUse(i);
              }
            }
          }
        }
        //Right-Click Popup menu
        if (e.getButton() == 3 ||  e.isPopupTrigger()) {
          cardLocalFlipMenuItem.setEnabled(ArcanistCCG.NetManager.isOnline());
          cardRemoteFlipMenuItem.setEnabled(ArcanistCCG.NetManager.isOnline());
          popup.show(e.getComponent(), e.getX(), e.getY());
        }
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

        pronoun.setInUse(false);

        if (moved) {
          e = SwingUtilities.convertMouseEvent(dragObj, e, pronoun.getParent());  //Convert to table coords
          Point snapPoint = frame.getTablePane().getNearestGridPoint( new Point(e.getX()-xdist, e.getY()-ydist) );

          pronoun.setLocation((int)snapPoint.getX()+xsnapDist, (int)snapPoint.getY()+ysnapDist);

          for (int i = cardGroup.size()-1; i >= 0; i--) {
            ArcanistCCG.NetManager.cardMove(((Card)(cardGroup.get(i))).arrayID(), (pronoun.getBounds().x+((Point)cardFudge.get(i)).x), (pronoun.getBounds().y+((Point)cardFudge.get(i)).y));
            ArcanistCCG.NetManager.cardUnuse(((Card)(cardGroup.get(i))).arrayID());
          }
          for (int i = deckGroup.size()-1; i >= 0; i--) {
            ArcanistCCG.NetManager.deckMove(((Deck)(deckGroup.get(i))).arrayID(), (pronoun.getBounds().x+((Point)deckFudge.get(i)).x), (pronoun.getBounds().y+((Point)deckFudge.get(i)).y));
            ArcanistCCG.NetManager.deckUnuse(((Deck)(deckGroup.get(i))).arrayID());
          }
          for (int i = tokenGroup.size()-1; i >= 0; i--) {
            ArcanistCCG.NetManager.tokenMove(((Token)(tokenGroup.get(i))).arrayID(), (pronoun.getBounds().x+((Point)tokenFudge.get(i)).x), (pronoun.getBounds().y+((Point)tokenFudge.get(i)).y));
            ArcanistCCG.NetManager.tokenUnuse(((Token)(tokenGroup.get(i))).arrayID());
          }
          for (int i = noteGroup.size()-1; i >= 0; i--) {
            ArcanistCCG.NetManager.noteMove(((FloatingNote)(noteGroup.get(i))).arrayID(), (pronoun.getBounds().x+((Point)noteFudge.get(i)).x), (pronoun.getBounds().y+((Point)noteFudge.get(i)).y));
            ArcanistCCG.NetManager.noteUnuse(((FloatingNote)(noteGroup.get(i))).arrayID());
          }

          selection = pronoun.getBounds();
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

          pronoun.setInUse(false);
        }
      }
    };
    this.addMouseListener(groupDrag);
    this.addMouseListener(frame.getFocusOnEnterListener());

    this.setBorder(BorderFactory.createEtchedBorder());
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    if (isInUse() == true) return;

    Object source = e.getSource();
    boolean ungroupAfter = false;

    if (source == cardRemoteFlipMenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          ArcanistCCG.NetManager.cardFlip(i);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardLocalFlipMenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          boolean peeked = false;
          if (tmpCard.getFlipState() == false) peeked = true;

          ArcanistCCG.NetManager.cardLocalFlip(i, peeked);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardFlipMenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          ArcanistCCG.NetManager.cardFlip(i);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurn0MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          ArcanistCCG.NetManager.cardRotate(i, 0);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurn90MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          ArcanistCCG.NetManager.cardRotate(i, -90);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurn180MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          ArcanistCCG.NetManager.cardRotate(i, -180);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurn270MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          ArcanistCCG.NetManager.cardRotate(i, -270);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurnMinus90MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          int newRotation = tmpCard.getRotation()+90;

          ArcanistCCG.NetManager.cardRotate(i, newRotation);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurnPlus90MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          int newRotation = tmpCard.getRotation()-90;

          ArcanistCCG.NetManager.cardRotate(i, newRotation);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurnMinus180MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          int newRotation = tmpCard.getRotation()+180;

          ArcanistCCG.NetManager.cardRotate(i, newRotation);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurnPlus180MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          int newRotation = tmpCard.getRotation()-180;

          ArcanistCCG.NetManager.cardRotate(i, newRotation);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurnMinus270MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          int newRotation = tmpCard.getRotation()+270;

          ArcanistCCG.NetManager.cardRotate(i, newRotation);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardTurnPlus270MenuItem) {
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          int newRotation = tmpCard.getRotation()-270;

          ArcanistCCG.NetManager.cardRotate(i, newRotation);
        }
      }
      ungroupAfter = true;
    }
    else if (source == cardRandomMenuItem) {
      cardGroup.clear();

      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          cardGroup.add(tmpCard);
        }
      }
      if (cardGroup.size() > 0) {
        int index = (int) (Math.random() * cardGroup.size());
        ((Card)cardGroup.get(index)).focus();
      }

      cardGroup.clear();
      ungroupAfter = true;
    }
    else if (source == cardRemoveMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are you SURE you want to remove the cards?", "Remove?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          ArcanistCCG.NetManager.cardRemove(i);
        }
      }
      ungroupAfter = true;
    }
    else if (source == deckLockTrueMenuItem || source == deckLockFalseMenuItem) {
      boolean state = true;
      if (source == deckLockFalseMenuItem) state = false;

      for (int i=frame.getDeckCount()-1; i >= 0; i--) {
        Deck tmpDeck = frame.getDeck(i);
        if ( selection.contains(tmpDeck.getLocation()) ) {
          if (tmpDeck.isInUse()) continue;
          ArcanistCCG.NetManager.deckSetLocked(i, state);
        }
      }
      ungroupAfter = true;
    }
    else if (source == deckFacingTrueMenuItem || source == deckFacingFalseMenuItem) {
      boolean state = true;
      if (source == deckFacingFalseMenuItem) state = false;

      for (int i=frame.getDeckCount()-1; i >= 0; i--) {
        Deck tmpDeck = frame.getDeck(i);
        if ( selection.contains(tmpDeck.getLocation()) ) {
          if (tmpDeck.isInUse()) continue;
          ArcanistCCG.NetManager.deckSetFacing(i, state);
        }
      }
      ungroupAfter = true;
    }
    else if (source == deckToBottomTrueMenuItem || source == deckToBottomFalseMenuItem) {
      boolean state = true;
      if (source == deckToBottomFalseMenuItem) state = false;

      for (int i=frame.getDeckCount()-1; i >= 0; i--) {
        Deck tmpDeck = frame.getDeck(i);
        if ( selection.contains(tmpDeck.getLocation()) ) {
          if (tmpDeck.isInUse()) continue;
          ArcanistCCG.NetManager.deckSetToBottom(i, state);
        }
      }
      ungroupAfter = true;
    }
    else if (source == deckDirUMenuItem || source == deckDirDMenuItem || source == deckDirLMenuItem || source == deckDirRMenuItem) {
      char dir = 'D';
      if (source == deckDirUMenuItem) dir = 'U';
      else if (source == deckDirDMenuItem) dir = 'D';
      else if (source == deckDirLMenuItem) dir = 'L';
      else if (source == deckDirRMenuItem) dir = 'R';

      for (int i=frame.getDeckCount()-1; i >= 0; i--) {
        Deck tmpDeck = frame.getDeck(i);
        if ( selection.contains(tmpDeck.getLocation()) ) {
          if (tmpDeck.isInUse()) continue;
          ArcanistCCG.NetManager.deckSetOffset(i, dir);
        }
      }
      ungroupAfter = true;
    }
    else if (source == deckSaveMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = frame.fileChooser(JFileChooser.SAVE_DIALOG, Prefs.homePath +"decks", filter);
      if (file == null) return;

      StringBuffer deckStringBuf = new StringBuffer();

      for (int i=frame.getDeckCount()-1; i >= 0; i--) {
        Deck tmpDeck = frame.getDeck(i);
        if ( selection.contains(tmpDeck.getLocation()) ) {
          if (tmpDeck.isInUse()) continue;
          if (deckStringBuf.length() > 0) deckStringBuf.append(";\t\n");
          deckStringBuf.append(frame.getDeck(i).save());
        }
      }

      try {
        FileWriter fr = new FileWriter(file);
        BufferedWriter outFile = new BufferedWriter(fr);
        outFile.write(deckStringBuf.toString());
        outFile.close();
        fr.close();
      }
      catch (FileNotFoundException exception) {
        ArcanistCCG.LogManager.write(exception, "Couldn't save deck.");
      }
      catch (IOException exception) {
        ArcanistCCG.LogManager.write(exception, "Couldn't save deck.");
      }
      ungroupAfter = true;
    }
    else if (source == deckRemoveMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are you SURE you want to remove the decks?", "Remove?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;
      for (int i=frame.getDeckCount()-1; i >= 0; i--) {
        Deck tmpDeck = frame.getDeck(i);
        if ( selection.contains(tmpDeck.getLocation()) ) {
          if (tmpDeck.isInUse()) continue;
          ArcanistCCG.NetManager.deckRemove(i);
        }
      }
      ungroupAfter = true;
    }
    else if (source == tokenRemoveMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are you SURE you want to remove the tokens?", "Remove?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;
      for (int i=frame.getTokenCount()-1; i >= 0; i--) {
        Token tmpToken = frame.getToken(i);
        if ( selection.contains(tmpToken.getLocation()) ) {
          if (tmpToken.isInUse()) continue;
          ArcanistCCG.NetManager.tokenRemove(i);
        }
      }
      ungroupAfter = true;
    }
    else if (source == noteRemoveMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are you SURE you want to remove the notes?", "Remove?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;
      for (int i=frame.getNoteCount()-1; i >= 0; i--) {
        FloatingNote tmpNote = frame.getNote(i);
        if ( selection.contains(tmpNote.getLocation()) ) {
          if (tmpNote.isInUse()) continue;
          ArcanistCCG.NetManager.noteRemove(i);
        }
      }
      ungroupAfter = true;
    }
    else if (source == allRemoveMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are you SURE you want to remove the everything?", "Remove?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;
      for (int i=frame.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = frame.getCard(i);
        if ( selection.contains(tmpCard.getLocation()) ) {
          if (tmpCard.isInUse()) continue;
          ArcanistCCG.NetManager.cardRemove(i);
        }
      }
      for (int i=frame.getDeckCount()-1; i >= 0; i--) {
        Deck tmpDeck = frame.getDeck(i);
        if ( selection.contains(tmpDeck.getLocation()) ) {
          if (tmpDeck.isInUse()) continue;
          ArcanistCCG.NetManager.deckRemove(i);
        }
      }
      for (int i=frame.getTokenCount()-1; i >= 0; i--) {
        Token tmpToken = frame.getToken(i);
        if ( selection.contains(tmpToken.getLocation()) ) {
          if (tmpToken.isInUse()) continue;
          ArcanistCCG.NetManager.tokenRemove(i);
        }
      }
      for (int i=frame.getNoteCount()-1; i >= 0; i--) {
        FloatingNote tmpNote = frame.getNote(i);
        if ( selection.contains(tmpNote.getLocation()) ) {
          if (tmpNote.isInUse()) continue;
          ArcanistCCG.NetManager.noteRemove(i);
        }
      }
      ungroupAfter = true;
    }
    else if (source == filterCardMenuItem) {
      if (filterCard == true) filterCard = false;
      else filterCard = true;
    }
    else if (source == filterDeckMenuItem) {
      if (filterDeck == true) filterDeck = false;
      else filterDeck = true;
    }
    else if (source == filterTokenMenuItem) {
      if (filterToken == true) filterToken = false;
      else filterToken = true;
    }
    else if (source == filterNoteMenuItem) {
      if (filterNote == true) filterNote = false;
      else filterNote = true;
    }
    else if (source == countMenuItem) {
      int cardCount = countCards();
      int deckCount = countDecks();
      int tokenCount = countTokens();
      int noteCount = countNotes();
      String countTxt = "Cards: "+ cardCount +"\nDecks: "+ deckCount +"\nTokens: "+ tokenCount +"\nNotes: "+ noteCount +"\nTotal: "+ (cardCount+deckCount+tokenCount+noteCount);
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), countTxt, "Group Count", JOptionPane.PLAIN_MESSAGE);
    }
    else if (source == ungroupMenuItem) {
      ungroupAfter = true;
    }

    if (ungroupAfter && pronoun.existsOnTable()) {
      pronoun.removeFromTable();
      frame.getTablePane().revalidate();
      frame.getTablePane().repaint();
    }
  }


  @Override
  public void guiInterrupted() {
    groupDrag.guiInterrupted();
  }


  /**
   * Gets this card's index within the ArcanistCCGFrame's group array.
   *
   * @return the index, or -1 if absent
   */
  public int arrayID() {
    return frame.getGroupIndex(this);
  }


  /**
   * Sets the used state.
   * Objects that are in use are busy with an opponent.
   * They will not respond to user input.
   *
   * @param state true if in use, false otherwise
   */
  public void setInUse(boolean state) {inUse = state;}

  /**
   * Gets the used state.
   * Objects that are in use are busy with an opponent.
   * They will not respond to user input.
   *
   * @return true if in use, false otherwise
   */
  public boolean isInUse() {return inUse;}


  /**
   * Add this to the table.
   * The table will need to be repainted afterward.
   */
  public void addToTable() {
    this.setVisible(false);
    frame.addGroup(this);
    frame.getTablePane().add(this, Prefs.groupLayer, 0);
    this.setBounds(selection);
    this.setVisible(true);
  }

  /**
   * Remove this from the table.
   * Attributes are not reset.
   * The table will need to be repainted afterward.
   */
  public void removeFromTable() {
    popup.setVisible(false);
    frame.removeGroup(arrayID());
    frame.getTablePane().remove(this);
  }

  /**
   * Determine whether this is present on the table.
   * @return true if it exists, false otherwise
   */
  public boolean existsOnTable() {
    return frame.hasGroup(this);
  }


  /**
   * Count cards currently within this group.
   *
   * @return the total
   */
  public int countCards() {
    int cardCount = 0;
    for (int i=frame.getCardCount()-1; i >= 0; i--) {
      if ( selection.contains(frame.getCard(i).getLocation()) ) {
        cardCount++;
      }
    }
    return cardCount;
  }

  /**
   * Count decks currently within this group.
   *
   * @return the total
   */
  public int countDecks() {
    int deckCount = 0;
    for (int i=frame.getDeckCount()-1; i >= 0; i--) {
      if ( selection.contains(frame.getDeck(i).getLocation()) ) {
        deckCount++;
      }
    }
    return deckCount;
  }

  /**
   * Count tokens currently within this group.
   *
   * @return the total
   */
  public int countTokens() {
    int tokenCount = 0;
    for (int i=frame.getTokenCount()-1; i >= 0; i--) {
      if ( selection.contains(frame.getToken(i).getLocation()) ) {
        tokenCount++;
      }
    }
    return tokenCount;
  }

  /**
   * Count notes currently within this group.
   *
   * @return the total
   */
  public int countNotes() {
    int noteCount = 0;
    for (int i=frame.getNoteCount()-1; i >= 0; i--) {
      if ( selection.contains(frame.getNote(i).getLocation()) ) {
        noteCount++;
      }
    }
    return noteCount;
  }


  /**
  * Standard debug info.
  */
  public String toString() {
    String result = "MassDragger(unnumbered)" + "\nContains: ";
    for (int i=0; i < cardGroup.size(); i++) {
      result += (i>0?", ":"") + "Card" + ((Card)cardGroup.get(i)).arrayID();
    }
    for (int i=0; i < deckGroup.size(); i++) {
      result += (i>0?", ":"") + "Deck" + ((Deck)deckGroup.get(i)).arrayID();
    }
    for (int i=0; i < tokenGroup.size(); i++) {
      result += (i>0?", ":"") + "Token" + ((Token)tokenGroup.get(i)).arrayID();
    }
    for (int i=0; i < noteGroup.size(); i++) {
      result += (i>0?", ":"") + "Note" + ((FloatingNote)noteGroup.get(i)).arrayID();
    }
    return result;
  }


  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (texturePaint == null) {
      int textureW = 15; int textureH = 15;
      BufferedImage textureImage = new BufferedImage(textureW, textureH, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2Texture = textureImage.createGraphics();
      g2Texture.setColor(new Color(150,150,150));
      g2Texture.fillRect(0, 0, textureW, textureH);
      g2Texture.setColor(Color.black);
      //g2Texture.fillRect(3, 3, 2, 2);
      g2Texture.fillRect(7, 7, 2, 2);
      texturePaint = new TexturePaint(textureImage, new Rectangle(textureW, textureH));
    }

    Graphics2D g2d = (Graphics2D)g;
    Composite prevComposite = g2d.getComposite();
    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
    g2d.setPaint(texturePaint);
    g2d.fillRect(0, 0, this.getWidth()-1, this.getHeight()-1);
    g2d.setComposite(prevComposite);
  }
}
