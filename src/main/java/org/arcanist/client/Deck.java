package org.arcanist.client;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * An array of cards.
 * A deck uses the image of one of its
 * members to represent itself on the table.
 */
public class Deck implements ActionListener, GuiInterruptListener, CardListContainer {
  private ArcanistCCGFrame frame = null;

  private Deck pronoun = this;

  /** Deck image. */
  private DeckComponent deckComp;

  private List<Card> deckList;


  private boolean faceUp = true;
  private boolean locked = false;
  private boolean autoface = true;
  private boolean bottom = false;
  private char drawOffset = 'D';
  private int newCardOffsetH = Prefs.defaultNewCardOffsetH;
  private int newCardOffsetV = Prefs.defaultNewCardOffsetV;


  /** Should this be inert. */
  private boolean inUse = false;

  /** An array of Dependent objects. */
  private List<Dependent> dependentArray = new ArrayList<Dependent>();

  protected EventListenerList listenerList = new EventListenerList();

  /** Context menu. */
  private JPopupMenu popup = null;

  private JCheckBoxMenuItem lockMenuItem = new JCheckBoxMenuItem("Position Lock");
  private JCheckBoxMenuItem autofaceMenuItem = new JCheckBoxMenuItem("AutoFace");
  private JCheckBoxMenuItem facingMenuItem = new JCheckBoxMenuItem("FaceUp");
  private JCheckBoxMenuItem addBottomMenuItem = new JCheckBoxMenuItem("Add to Bottom");
  private JRadioButtonMenuItem settingsDirUMenuItem = new JRadioButtonMenuItem("Up");
  private JRadioButtonMenuItem settingsDirDMenuItem = new JRadioButtonMenuItem("Down");
  private JRadioButtonMenuItem settingsDirLMenuItem = new JRadioButtonMenuItem("Left");
  private JRadioButtonMenuItem settingsDirRMenuItem = new JRadioButtonMenuItem("Right");

  private JMenuItem shuffleMenuItem = new JMenuItem("Shuffle");
  private JMenuItem drawXMenuItem = new JMenuItem("Draw X Cards...");
  private JMenuItem randomMenuItem = new JMenuItem("Grab a Random Card");
  private JMenuItem searchMenuItem = new JMenuItem("Search...");
  private JMenuItem listMenuItem = new JMenuItem("List Contents...");
  private JMenuItem dupMenuItem = new JMenuItem("Duplicate");
  private JMenuItem saveMenuItem = new JMenuItem("Save...");
  private JMenuItem reverseMenuItem = new JMenuItem("Reverse");
  private JMenuItem removeMenuItem = new JMenuItem("Remove");

  private GuiInterruptMouseAdapter deckListener = null;


  public Deck(ArcanistCCGFrame f, List<Card> newDeckList, int positionX, int positionY, boolean facing) {
    frame = f;

    deckList = newDeckList;

    BufferedImage image = Prefs.Cache.getImg(Prefs.defaultEmptyDeckPath);
    deckComp = new DeckComponent(frame, image);

    faceUp = facing;
    deckComp.setBounds(positionX, positionY, image.getWidth()+4, image.getHeight()+4);
    deckComp.setToolTipText("Deck/Discard ("+ deckList.size() +" cards)");

    ButtonGroup radioDirGroup = new ButtonGroup();
      radioDirGroup.add(settingsDirUMenuItem);
      radioDirGroup.add(settingsDirDMenuItem);
      radioDirGroup.add(settingsDirLMenuItem);
      radioDirGroup.add(settingsDirRMenuItem);

    //Setup Popup menu
    popup = new JPopupMenu();
      // See top: shuffleMenuItem
      // See top: drawXMenuItem
      // See top: randomMenuItem
      // See top: searchMenuItem
      // See top: listMenuItem
      JMenu popupSettingsSubMenu = new JMenu("Settings");
        // See top: lockMenuItem (setLocked accesses it)
        // See top: autofaceMenuItem (setAutoFace accesses it)
          autofaceMenuItem.setSelected(autoface);
        // See top: facingMenuItem (setFacing accesses it)
          facingMenuItem.setSelected(facing);
        // See top: addBottomMenuItem (setToBottom accesses it)
        JMenu popupSettingsDirSubMenu = new JMenu("Card Offset");
          // See top: settingsDirUMenuItem
          // See top: settingsDirDMenuItem
            settingsDirDMenuItem.setSelected(true);
          // See top: settingsDirLMenuItem
          // See top: settingsDirRMenuItem
      // See top: dupMenuItem
      // See top: saveMenuItem
      // See top: reverseMenuItem
      // See top: removeMenuItem
    popup.add(shuffleMenuItem);
    popup.add(drawXMenuItem);
    popup.add(randomMenuItem);
    popup.add(searchMenuItem);
    popup.add(listMenuItem);
    popup.add(popupSettingsSubMenu);
      popupSettingsSubMenu.add(lockMenuItem);
      popupSettingsSubMenu.add(autofaceMenuItem);
      popupSettingsSubMenu.add(facingMenuItem);
      popupSettingsSubMenu.add(addBottomMenuItem);
      popupSettingsSubMenu.add(popupSettingsDirSubMenu);
        popupSettingsDirSubMenu.add(settingsDirUMenuItem);
        popupSettingsDirSubMenu.add(settingsDirDMenuItem);
        popupSettingsDirSubMenu.add(settingsDirLMenuItem);
        popupSettingsDirSubMenu.add(settingsDirRMenuItem);
    popup.add(dupMenuItem);
    popup.add(saveMenuItem);
    popup.add(reverseMenuItem);
    popup.add(removeMenuItem);


    shuffleMenuItem.addActionListener(this);
    drawXMenuItem.addActionListener(this);
    randomMenuItem.addActionListener(this);
    searchMenuItem.addActionListener(this);
    listMenuItem.addActionListener(this);
    lockMenuItem.addActionListener(this);
    autofaceMenuItem.addActionListener(this);
    facingMenuItem.addActionListener(this);
    addBottomMenuItem.addActionListener(this);
    settingsDirUMenuItem.addActionListener(this);
    settingsDirDMenuItem.addActionListener(this);
    settingsDirLMenuItem.addActionListener(this);
    settingsDirRMenuItem.addActionListener(this);
    dupMenuItem.addActionListener(this);
    saveMenuItem.addActionListener(this);
    reverseMenuItem.addActionListener(this);
    removeMenuItem.addActionListener(this);

    deckListener = new GuiInterruptMouseAdapter() {
      boolean wasBusy = true;                                // Remembers not to unlock on release
      MouseMotionListener dragability;
      int xdist=0, ydist=0;
      JComponent dragObj = deckComp;

      public void mouseEntered(MouseEvent e) {               // Update JumboView
        JumboView jumboFrame = frame.getJumboFrame();
        if (jumboFrame != null && jumboFrame.isJumboActive()) {
          if (!frame.isDragging() && deckList.size() > 0) {
            Card topCard = getCard(deckList.size()-1);
            if ( getFacing() == true || topCard.getBackName().length() > 0 ) {
              boolean preferText = jumboFrame.getPreferText();
              if (e.isShiftDown()) preferText = !preferText;

              if (topCard.isText() == true || preferText) {
                if (getFacing() == true)
                  frame.getJumboFrame().updateJumboText(new CardTextPanel(topCard.getFrontName(), topCard.getSetAbbrev(), false));
                else if (topCard.getBackName().length() > 0)
                  frame.getJumboFrame().updateJumboText(new CardTextPanel(topCard.getBackName(), topCard.getSetAbbrev(), false));
              }
              else {
                if (getFacing() == true)
                  frame.getJumboFrame().updateJumboImage(topCard.getFrontPath());
                else if (topCard.getBackName().length()>0)
                  frame.getJumboFrame().updateJumboImage(topCard.getBackPath());
              }
            }
          }
        }
      }

      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        deckComp.grabFocus();

        if (e.getButton() == 1) {
          if (getLocked() == false) {
            xdist = e.getX();
            ydist = e.getY();
            ArcanistCCG.NetManager.deckUse(arrayID());
            frame.setDragging(true);

            DragGhost dragGhost = frame.getDragGhost();
            dragGhost.setOffset(xdist, ydist);
            dragGhost.setSourceObject(dragObj);
            dragGhost.setSnap(true);
            frame.addDragGhost();
            frame.getTablePane().revalidate();
            frame.getTablePane().repaint();
          }
        }
        //Right-Click Popup menu
        //  Technically This should be in both pressed and released
        //  Mac checks the trigger on press, Win on release
        //  But only macs have 1-button mice, which need the trigger check ;)
        if (e.getButton() == 3 || e.isPopupTrigger()) {popup.show(e.getComponent(), e.getX(), e.getY());}
      }

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

        ArcanistCCG.NetManager.deckUnuse(arrayID());

        boolean dropped = false;
        if (moved && !dropped) {
          Hand tmpHand = null;
          int handsCount = frame.getHandCount();
          // Add this deck to a hand if dragged onto one
          for (int i = 0; i < handsCount; i++) {
            tmpHand = frame.getHand(i);
            Point handSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), tmpHand);
            if ( tmpHand.contains(handSpace.x, handSpace.y) && tmpHand.isInUse() == false ) {
              ArcanistCCG.NetManager.deckHand(arrayID(), i);

              dropped = true;
              break;
            }
          }
        }
        if (moved && !dropped) {
          Deck tmpDeck = null;
          int decksCount = frame.getDeckCount();
          // Add this deck to another if dragged onto one
          for (int i = 0; i < decksCount; i++) {
            tmpDeck = frame.getDeck(i);
            Point deckSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), tmpDeck.getComponent());
            if ( tmpDeck.getComponent().contains(deckSpace.x, deckSpace.y) && tmpDeck.isInUse() == false ) {
              if (tmpDeck.equals(pronoun)) continue;

              boolean prevToBottom = tmpDeck.getToBottom();
              // Set AddToBottom if shift is held
              if (e.isShiftDown()) {
                if (prevToBottom == false) {
                  ArcanistCCG.NetManager.deckSetToBottom(i, true);
                }
              }
              if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are You SURE You Want To Combine Decks?", "Combine Decks?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) break;

              ArcanistCCG.NetManager.deckDeck(arrayID(), i);
              // //pronoun.removeFromTable();
              // //tmpDeck.addDeck(pronoun);
              // //tmpDeck.refresh();

              // Revert AddToBottom if necessary
              if (e.isShiftDown()) {
                if (prevToBottom == false) {
                  ArcanistCCG.NetManager.deckSetToBottom(i, false);
                }
                String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
                ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" added a deck under a deck--");
              }

              dropped = true;
              break;
            }
          }
        }
        if (moved && !dropped) {
          e = SwingUtilities.convertMouseEvent(dragObj, e, deckComp.getParent());  //Convert deck coords to table coords
          Point snapPoint = frame.getTablePane().getNearestGridPoint( new Point(e.getX()-xdist, e.getY()-ydist) );

          ArcanistCCG.NetManager.deckMove(arrayID(), (int)snapPoint.getX(), (int)snapPoint.getY());
          moved = false;
        }

        frame.getTablePane().repaint();
      }

      public void mouseClicked(MouseEvent e) {
        if (wasBusy) return;

        // A check for ctrl-clicking on Mac to avoid drawing
        if (e.getButton() == 1 && !e.isControlDown()) {
          ArcanistCCG.NetManager.deckTableCard(arrayID(), (deckList.size()-1), !e.isShiftDown());
        }
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

          ArcanistCCG.NetManager.deckUnuse(arrayID());
        }
      }
    };
    deckComp.addMouseListener(deckListener);


    deckComp.addMouseListener(frame.getFocusOnEnterListener());
  }


  public void actionPerformed(ActionEvent e) {
    if (isInUse() == true) return;

    Object source = e.getSource();

    if (source == shuffleMenuItem) {
      if (deckList.size() == 0) return;

      // //setInUse(true);
      ArcanistCCG.NetManager.deckUse(arrayID());

      List<Integer> newOrder = genRandomOrder();
      ArcanistCCG.NetManager.deckShuffle(arrayID(), newOrder);
      // //reorder(newOrder);

      // //setInUse(false);
      ArcanistCCG.NetManager.deckUnuse(arrayID());

      String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
      ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" shuffled a deck--");
    }
    else if (source == drawXMenuItem) {
      String userinput = "";
      int x = -1;
      do {
        userinput = JOptionPane.showInternalInputDialog(frame.getDesktop(), "Draw How Many Cards?", "Draw X Cards", JOptionPane.QUESTION_MESSAGE);
        if (userinput == null || userinput.length() == 0 || existsOnTable() == false)
          break;
        else {
          x = Integer.parseInt(userinput);
          if (x > 0 && x <= deckList.size()) {
            ArcanistCCG.NetManager.deckTableXCards(arrayID(), x);

            String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
            ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE2, "--"+ chatAlias +" drew "+ x +" cards--");
          }
        }
      } while (x < 0);
    }
    else if (source == randomMenuItem) {
      if (deckList.size()-1 >= 0) {
        int randCard = (int) (Math.random() * (deckList.size()));
        ArcanistCCG.NetManager.deckTableCard(arrayID(), randCard, true);

        String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
        ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE2, "--"+ chatAlias +" drew a random card--");
      }
    }
    else if (source == searchMenuItem) {
      ArcanistCCG.NetManager.deckSearch(arrayID());
      //new DeckSearchWindow(frame, pronoun);

      String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
      ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" searched a deck--");
    }
    else if (source == listMenuItem) {
      ArcanistCCG.NetManager.deckList(arrayID());
      //new DeckListWindow(frame, pronoun);
    }
    else if (source == lockMenuItem) {
      boolean state = !getLocked();
      ArcanistCCG.NetManager.deckSetLocked(arrayID(), state);
      // //setLocked(state);
    }
    else if (source == autofaceMenuItem) {
      boolean state = !getAutoFace();
      ArcanistCCG.NetManager.deckSetAutoFace(arrayID(), state);
      // //setAutoFace(state);
    }
    else if (source == facingMenuItem) {
      boolean state = !getFacing();
      ArcanistCCG.NetManager.deckSetFacing(arrayID(), state);
      // //setFacing(state);
    }
    else if (source == addBottomMenuItem) {
      boolean state = !getToBottom();
      ArcanistCCG.NetManager.deckSetToBottom(arrayID(), state);
      // //setToBottom(state);

      String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
      ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE2, "--"+ chatAlias +" set a deck's AddToBottom to "+ state +"--");
    }
    else if (source == settingsDirUMenuItem) {
      ArcanistCCG.NetManager.deckSetOffset(arrayID(), 'U');
      // //setOffset('U');
    }
    else if (source == settingsDirDMenuItem) {
      ArcanistCCG.NetManager.deckSetOffset(arrayID(), 'D');
      // //setOffset('D');
    }
    else if (source == settingsDirLMenuItem) {
      ArcanistCCG.NetManager.deckSetOffset(arrayID(), 'L');
      // //setOffset('L');
    }
    else if (source == settingsDirRMenuItem) {
      ArcanistCCG.NetManager.deckSetOffset(arrayID(), 'R');
      // //setOffset('R');
    }
    else if (source == dupMenuItem) {
      String dupDeck = save().replaceAll("\\n","|") +"|";
      Rectangle tableView = frame.getTableView();

      // Leave the duplicate unlocked; You're gonna move it anyway.
      ArcanistCCG.NetManager.deckAdd(tableView.x, tableView.y, false, getAutoFace(), getFacing(), getToBottom(), getOffset(), dupDeck);
      //ArcanistCCG.NetManager.deckAdd(tableView.x, tableView.y, dupDeck);
      // //DeckParser newDeckParser = new DeckParser(frame, true, dupDeck, tableView.x, tableView.y);
      // //newDeckParser.startThread();
    }
    else if (source == saveMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = frame.fileChooser(JFileChooser.SAVE_DIALOG, Prefs.homePath +"decks", filter);
      if (file == null) return;

      String dupDeck = save();

      try {
        FileWriter fr = new FileWriter(file);
        BufferedWriter outFile = new BufferedWriter(fr);
        outFile.write(dupDeck);
        outFile.close();
        fr.close();
      }
      catch (FileNotFoundException exception) {
        ArcanistCCG.LogManager.write(exception, "Couldn't save deck.");
      }
      catch (IOException exception) {
        ArcanistCCG.LogManager.write(exception, "Couldn't save deck.");
      }
    }
    else if (source == reverseMenuItem) {
      ArcanistCCG.NetManager.deckReverse(arrayID());

      String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
      ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" reversed a deck--");
      // //reverse();
    }
    else if (source == removeMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are You SURE You Want To Remove This?", "Remove?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        if (pronoun.existsOnTable()) {
          ArcanistCCG.NetManager.deckRemove(arrayID());
          // //pronoun.removeFromTable();
          // //frame.getTablePane().repaint();
        }
      }
    }
  }


  public void guiInterrupted() {
    deckListener.guiInterrupted();
  }


  /**
  * Standard debug info.
  */
  public String toString() {
    return "Deck" + deckComp.getId() +
    "\nArrayID: " + arrayID();
  }


  /**
   * Returns this object's table component.
   */
  public DeckComponent getComponent() {
    return deckComp;
  }


  /**
   * Generates a random order for shuffling.
   * Technically this doesn't randomize enough.
   *
   * @return a list of Integers of the original indeces in the nre order
   */
  public List<Integer> genRandomOrder() {
    List<Integer> resultList = new ArrayList<Integer>(deckList.size());
    if (deckList.size() == 0) return resultList;

    for (int i=0; i < deckList.size(); i++) {
      resultList.add(new Integer(i));
    }

    int randCard;
    Random rng = new Random();
    List<Integer> tmpList = new ArrayList<Integer>(deckList.size());

    for (int i=1; i <= Prefs.shuffles; i++) {
      while(resultList.size() > 0) {
        rng.setSeed(rng.nextLong()^(long)deckList.size()^System.currentTimeMillis());
        randCard = rng.nextInt(resultList.size());
        tmpList.add(resultList.remove(randCard));
      }
      resultList.addAll(tmpList);
      tmpList.clear();
    }
    return resultList;
  }

  /**
   * Reorders the deck.
   *
   * @param indeces a list of Integers of the original indeces in the nre order
   * @see Deck#genRandomOrder()
   */
  public void reorder(List<Integer> indeces) {
    if (indeces.size() != deckList.size()) return;

    List<Card> tmpList = new ArrayList<Card>(deckList.size());

    for (Integer index : indeces) {
      tmpList.add(deckList.get( index.intValue() ));
    }
    deckList.clear();
    deckList.addAll(tmpList);
    refresh();
    fireContentsChanged(this, 0, deckList.size()-1);
  }


  /**
   * Updates the deck image.
   * This is usually done when the top card changes.
   * The table is repainted in the process.
   */
  public void refresh() {
    if (deckList.size()-1 >= 0) {
      deckComp.setCardShown(true);
      if (getFacing() == true) deckComp.setCardImage(getCard(deckList.size()-1).getComponent().getFrontImage());
      else deckComp.setCardImage(getCard(deckList.size()-1).getComponent().getBackImage());
    }
    else deckComp.setCardShown(false);

    frame.getTablePane().repaint();
    deckComp.setToolTipText("Deck/Discard ("+ deckList.size() +" cards)");
  }


  /**
   * Gets this deck's ID.
   *
   * @return the ID
   * @see DeckComponent#getId()
   */
  public int getId() {return deckComp.getId();}

  /**
   * Sets this deck's id.
   *
   * @param n new id
   */
  public void setId(int n) {deckComp.setId(n);}


  /**
   * Gets this card's index within the ArcanistCCGFrame's deck array.
   *
   * @return the index, or -1 if absent
   */
  public int arrayID() {
    return frame.getDeckIndex(this);
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
    deckComp.setPaintLock(state);
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
   * The table will need to be repainted afterward.
   */
  public void addToTable() {
    frame.addDeck(this);
    frame.getTablePane().add(deckComp, Prefs.deckLayer, 0);
  }

  /**
   * Removes this from the table.
   * Attributes like autoface are not reset.
   * The table will need to be repainted afterward.
   */
  public void removeFromTable() {
    popup.setVisible(false);
    frame.removeDeck(arrayID());
    frame.getTablePane().remove(deckComp);
    for (int i=dependentArray.size()-1; i >= 0; i--) {
      ((Dependent)dependentArray.get(i)).sourceRemoved(pronoun);
      removeDependent( ((Dependent)dependentArray.get(i)) );
    }
  }

  /**
   * Determines whether this is present on the table.
   *
   * @return true if it exists, false otherwise
   */
  public boolean existsOnTable() {
    return frame.hasDeck(this);
  }

  /**
   * Registers an object which needs this on the table.
   */
  public void addDependent(Dependent d) {
    if (!dependentArray.contains(d)) dependentArray.add(d);
  }

  /**
   * Unregisters an object which needs this on the table.
   */
  public void removeDependent(Dependent d) {
    dependentArray.remove(d);
  }


  /**
   * Sets this object's location.
   */
  public void setLocation(int x, int y) {
    deckComp.setLocation(x, y);
  }

  /**
   * Returns this object's location.
   */
  public Point getLocation() {
    return deckComp.getLocation();
  }

  /**
   * Returns this object's x position.
   */
  public int getX() {
    return deckComp.getX();
  }

  /**
   * Returns this object's y position.
   */
  public int getY() {
    return deckComp.getY();
  }


  /**
   * Inserts a card into the deck.
   * The card's attributes will be reset.
   * The deck will need a refresh() afterward.
   *
   * @param newCard the card to add
   * @param index where to put it
   * @see Card#reset()
   * @see Deck#refresh()
   */
  public void addCard(Card newCard, int index) {
    deckList.add(index, newCard);
    fireIntervalAdded(this, index, index);
    newCard.reset();
  }

  /**
   * Adds a card to the deck.
   * The card's attributes will be reset.
   * If this deck is set to add to bottom,
   * the card will be added to the bottom.
   * The deck will need a refresh() afterward.
   *
   * @param newCard the card to add
   * @see Card#reset()
   * @see Deck#refresh()
   */
  public void addCard(Card newCard) {
    if (getToBottom() == true) {
      deckList.add(0, newCard);
      fireIntervalAdded(this, 0, 0);
    } else {
      deckList.add(newCard);
      fireIntervalAdded(this, deckList.size()-1, deckList.size()-1);
    }

    newCard.reset();
  }

  /**
   * Adds a deck's cards to the deck.
   * The cards' attributes will not be reset.
   * If AddToBottom for this deck is set,
   * that deck will be added to the bottom.
   * The added deck's order will be maintained.
   * This deck will need a refresh() afterward.
   *
   * @param srcDeck the deck to add from
   * @see Card#reset()
   * @see Deck#refresh()
   */
  public void addDeck(Deck srcDeck) {
    List<Card> srcList = srcDeck.removeCards(0, srcDeck.getCardCount());
    if (getToBottom() == true) {
      deckList.addAll(0, srcList);
      fireIntervalAdded(this, 0, srcList.size()-1);
    } else {
      deckList.addAll(srcList);
      fireIntervalAdded(this, deckList.size()-srcList.size(), deckList.size()-1);
    }
  }


  /**
   * Returns the table location at which cards drawn should appear.
   */
  public Point getRallyPoint() {
    return new Point(deckComp.getLocation().x + newCardOffsetH, deckComp.getLocation().y + newCardOffsetV);
  }


  /**
   * Apply autofacing rules to a Card.
   * If this Deck is facedown and autoface is set: faceup.
   * Otherwise the Card's facing will match this Deck's facing.
   */
  public void applyAutoFaceToCard(Card c) {
    if (getFacing() == false && getAutoFace() == true) {
      c.setFlipState(true);
    } else {
      c.setFlipState(getFacing());
    }
  }

  /**
   * Removes and returns a card.
   * Garbage collection is triggered when
   * the deck total is a multiple of 10.
   *
   * @param index index within the deck's array
   * @return the card, or null
   */
  public Card takeCard(int index) {
    if (index < 0 || index >= deckList.size()) return null;

    Card result = (Card)deckList.remove(index);
    refresh();
    fireIntervalRemoved(this, index, index);

    if (deckList.size()%10 == 0) {
      System.gc();                                           //Collect garbage (card rot/jumbo leftovers)
    }
    return result;
  }


  /**
   * Returns the card at an index, from the bottom.
   * It is left in place.
   *
   * @return the card, or null
   */
  public Card getCard(int index) {
    if (index < 0 || index >= deckList.size()) return null;
    return (Card)deckList.get(index);
  }


  /**
   * Returns the card count.
   * The top card is this total-1.
   */
  public int getCardCount() {
    return deckList.size();
  }


  /**
   * Removes an arbitrary card.
   * The deck will need a refresh() afterward.
   *
   * @param index index within the deck's array
   * @return the removed card, or null
   */
  public Card removeCard(int index) {
    if (index < 0 || index >= deckList.size()) return null;
    Card result = (Card)deckList.remove(index);
    fireIntervalRemoved(this, index, index);
    return result;
  }

  /**
   * Removes a range of cards.
   * The deck will need a refresh() afterward.
   *
   * @param startIndex (inclusive)
   * @param endIndex (exclusive) less than or equal to getCardCount()
   * @return the removed cards, or an empty list
   */
  public List<Card> removeCards(int startIndex, int endIndex) {
    if (startIndex < 0 || startIndex >= deckList.size()) return new ArrayList<Card>(0);
    if (endIndex <= startIndex || endIndex < 0 || endIndex > deckList.size()) return new ArrayList<Card>(0);

    List<Card> result = new ArrayList<Card>(endIndex - startIndex);
    List<Card> tmpList = deckList.subList(startIndex, endIndex);
    result.addAll(tmpList);
    tmpList.clear();
    fireIntervalRemoved(this, startIndex, endIndex-1);

    return result;
  }


  /**
   * Sets the drag-locked state.
   * While locked, the deck will not be draggable.
   */
  public void setLocked(boolean state) {
    locked = state;
    lockMenuItem.setState(state);
  }

  /**
   * Gets the drag-locked state.
   * While locked, the deck will not be draggable.
   *
   * @return true if locked, false otherwise
   */
  public boolean getLocked() {return locked;}


  /**
   * Sets automatic local flipping upon drawing.
   * If on when you draw a card, it flips for you alone automatically.
   */
  public void setAutoFace(boolean state) {
    autoface = state;
    autofaceMenuItem.setState(state);
  }

  /**
   * Gets automatic local flipping upon drawing.
   * If facedown when you draw, the card will be
   * locally flipped to be face up.
   */
  public boolean getAutoFace() {return autoface;}


  /**
   * Sets the deck's faceup/facedown status.
   */
  public void setFacing(boolean state) {
    faceUp = state;
    facingMenuItem.setState(state);
    refresh();
  }

  /**
   * Gets the deck's faceup/facedown status.
   *
   * @return true if face up, false otherwise
   */
  public boolean getFacing() {return faceUp;}


  /**
   * Sets drawing from the bottom of the deck.
   */
  public void setToBottom(boolean state) {
    bottom = state;
    addBottomMenuItem.setState(state);
  }

  /**
   * Sets drawing from the bottom of the deck.
   *
   * @return true if set, false otherwise
   */
  public boolean getToBottom() {return bottom;}


  /**
   * Changes the offset where cards appear when drawn.
   *
   * @param dir U-Up, D-Down, L-Left, R-Right
   */
  public void setOffset(char dir) {
    if (dir == 'U') {
      drawOffset = dir;
      newCardOffsetH = 0; newCardOffsetV = -121;
      settingsDirUMenuItem.setSelected(true);
    } else if (dir == 'D') {
      drawOffset = dir;
      newCardOffsetH = 0; newCardOffsetV = 121;
      settingsDirDMenuItem.setSelected(true);
    } else if (dir == 'L') {
      drawOffset = dir;
      newCardOffsetH = -88; newCardOffsetV = 0;
      settingsDirLMenuItem.setSelected(true);
    } else if (dir =='R') {
      drawOffset = dir;
      newCardOffsetH = 88; newCardOffsetV = 0;
      settingsDirRMenuItem.setSelected(true);
    }
  }

  /**
   * Returns this deck's drawn card offset.
   *
   * @return U, D, L, or R
   */
  public char getOffset() {
    return drawOffset;
  }


  /**
   * Reverses the deck.
   */
  public void reverse() {
    List<Card> tempList = new ArrayList<Card>(deckList.size());

    for (int i = deckList.size()-1; i >= 0; i--) {
      tempList.add(deckList.get(i));
    }
    deckList = tempList;
    refresh();
    fireContentsChanged(this, 0, deckList.size()-1);
  }

  /**
   * Saves the deck.
   * Generates tab-separated lines for each card in the deck.
   *
   * @return a String for ascii serialization.
   */
  public String save() {
    StringBuffer dupDeck = new StringBuffer();
    String tmp;

    for (int i=deckList.size()-1; i >= 0; i--) {
      Card tmpCard = getCard(i);
      tmp = tmpCard.getFrontName();
      if (tmp != null) {
        //Find adjacent duplicates
        int j;
        for (j=1; j <= i; j++) {
          Card tmpOtherCard = getCard(i-j);
          if ( tmpCard.getSetAbbrev().equals( tmpOtherCard.getSetAbbrev() ) ) {
            if ( tmpCard.getFrontPath().equals( tmpOtherCard.getFrontPath() ) ) {
              if ( tmpCard.getBackPath().equals( tmpOtherCard.getBackPath() ) ) {
                continue;
              }
            }
          }
          break;
        }
        j--;

        dupDeck.append(1+j);
        dupDeck.append("\t").append(tmp);
        dupDeck.append("\t").append(Prefs.textDat.getSetNameFromAbbrev(tmpCard.getSetAbbrev()));
        dupDeck.append("\t").append(tmpCard.getFrontFile());
        dupDeck.append("\t").append(tmpCard.getBackName());
        dupDeck.append("\t").append(tmpCard.getBackFile());
        dupDeck.append("\n");

        i -= j;                                              //Skip the duplicates
      }
    }
    return dupDeck.toString();
  }


  /**
   * Clears this object's hotkeys and reapplies current global ones.
   * Cards inside will be updated as well.
   */
  public void updateHotkeys() {
    for (int i=deckList.size()-1; i >= 0; i--) {
      ((Card)deckList.get(i)).updateHotkeys();
    }
  }


  protected void fireContentsChanged(Object source, int index0, int index1) {
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i=listeners.length-2; i >= 0; i -= 2) {
      if (listeners[i] == ListDataListener.class) {
        if (e == null) {
          e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
        }
        ((ListDataListener)listeners[i+1]).contentsChanged(e);
      }	       
    }
  }

  protected void fireIntervalAdded(Object source, int index0, int index1) {
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i=listeners.length-2; i >= 0; i -= 2) {
      if (listeners[i] == ListDataListener.class) {
        if (e == null) {
          e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0, index1);
        }
        ((ListDataListener)listeners[i+1]).intervalAdded(e);
      }	       
    }
  }

  protected void fireIntervalRemoved(Object source, int index0, int index1) {
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i=listeners.length-2; i >= 0; i -= 2) {
      if (listeners[i] == ListDataListener.class) {
        if (e == null) {
          e = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, index0, index1);
        }
        ((ListDataListener)listeners[i+1]).intervalRemoved(e);
      }	       
    }
  }

  public void addListDataListener(ListDataListener l) {
    listenerList.add(ListDataListener.class, l);
  }

  public void removeListDataListener(ListDataListener l) {
    listenerList.remove(ListDataListener.class, l);
  }

  public ListDataListener[] getListDataListeners() {
    return (ListDataListener[])listenerList.getListeners(ListDataListener.class);
  }
}
