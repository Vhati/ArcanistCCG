package org.arcanist.client;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A draggable, flippable, rotatable image.
 * It includes subpanels that react to the mouse.
 */
public class Card implements ActionListener, GuiInterruptListener {
  private Card pronoun = this;

  private ArcanistCCGFrame frame = null;

  /** Card image. */
  private CardComponent cardComp;

  /** Path to image file for face. */
  private String frontPath = "";

  /** Path to image file for back. */
  private String backPath = "";


  /** Current rotation (degrees). */
  private int rotation = 0;

  /** Actual name of face. */
  private String frontName = "";

  /** Actual name of back. */
  private String backName = "";

  /** Expansion the card came from. */
  private String setAbbrev = "";


  /** Is this card text-only. */
  private boolean isText = false;

  /** Should this be inert. */
  private boolean inUse = false;

  // Reference to rotation mouse hotspots.
  List<CardSpinPanel> hotspotArray = new ArrayList<CardSpinPanel>(0);

  /** Context menu */
  private JPopupMenu popup = null;

  private JMenuItem remoteFlipMenuItem = new JMenuItem("Remote Flip");
  private JMenuItem localFlipMenuItem = new JMenuItem("Local Flip");
  private JMenuItem flipMenuItem = new JMenuItem("Flip");
  private JMenuItem toTopMenuItem = new JMenuItem("Move to Top");
  private JMenuItem toBottomMenuItem = new JMenuItem("Move to Bottom");
  private JMenuItem dupMenuItem = new JMenuItem("Duplicate");
  private JMenuItem removeMenuItem = new JMenuItem("Remove");

  private GuiInterruptMouseAdapter cardListener = null;


  /**
   * Creates a valid card from suggested paths.
   * Example: deasciifying a deck line.
   *
   * @param logPrefix string to prepend to log messages for missing images
   * @param frame the window to add this card to
   * @param frontPath path to card front image
   * @param backPath path to card back image
   * @param flipState face up? (T/F)
   */
  public static Card createValidCard(String logPrefix, ArcanistCCGFrame frame, String frontPath, String backPath, boolean flipState) {
    File tmpFile = null;
    String frontName = null;
    if (frontPath != null && frontPath.length() > 0) {
      tmpFile = new File(frontPath);
      frontName = tmpFile.getName();
      if (!tmpFile.exists()) frontPath = null;
    } else {
      frontName = "Error";
    }

    if (backPath != null && backPath.length() > 0) {
      tmpFile = new File(backPath);
      if (!tmpFile.exists()) backPath = null;
    } else {
      backPath = null;
    }


    if (frontPath == null) {
      if (Prefs.useTextOnly == false && Prefs.suppressLostCards == false)
        ArcanistCCG.LogManager.write(LogManager.INFO_LEVEL, logPrefix +"Image not found for \""+ frontName +"\".");
      frontPath = Prefs.defaultErrorPath;
    }
    if (backPath == null) {
      if (Prefs.suppressLostCards == false)
        ArcanistCCG.LogManager.write(LogManager.INFO_LEVEL, logPrefix +"Back image not found for \""+ frontName +"\".");
      backPath = Prefs.defaultBackPath;
    }

    Card newCard = new Card(frame, frontPath, backPath, flipState);
    return newCard;
  }

  /**
   * Creates a valid card from suggested paths.
   * Example: deasciifying a deck line.
   *
   * @param logPrefix string to prepend to log messages for missing images
   * @param frame the window to add this card to
   * @param frontName name of front
   * @param backName name of back
   * @param setName full set name
   * @param frontFile front image file name
   * @param backFile back image file name
   * @param flipState face up? (T/F)
   */
  public static Card createValidCard(String logPrefix, ArcanistCCGFrame frame, String frontName, String backName, String setName, String frontFile, String backFile, boolean flipState) {
    Card newCard = null;

    String[] actualPaths = CardImagePathParser.getPaths(setName, frontName, frontFile, backName, backFile);
      setName = actualPaths[0];
      String path = actualPaths[1];
      String pathBack = actualPaths[2];

    String setAbbrev = Prefs.textDat.getSetAbbrevFromName(setName);

    if (pathBack == null && backName.length() == 0 && backFile.length() == 0) {
      pathBack = Prefs.defaultBackPath;
    }

    if (Prefs.useTextOnly == false && (path != null && pathBack != null)) {
      if (frontName.length() > 0)
        newCard = new Card(frame, frontName, backName, setAbbrev, path, pathBack, flipState);
      else
        newCard = new Card(frame, path, pathBack, flipState);
    }
    else {
      if (path == null) {
        if (Prefs.useTextOnly == false && Prefs.suppressLostCards == false)
          ArcanistCCG.LogManager.write(LogManager.INFO_LEVEL, logPrefix +"Image not found for \""+ frontName +"\".");
      }
      if (Prefs.useTextOnly == true || path == null) {
        if (frontName.length() > 0) frontName = Prefs.textDat.isItThere(frontName);

        if (frontName.length() > 0) path = Prefs.defaultBlankPath;
        else {
          frontName = "Error";
          path = Prefs.defaultErrorPath;
        }
      }
      if (pathBack == null) {
        if (Prefs.suppressLostCards == false && (backName.length() > 0 || backFile.length() > 0))
          ArcanistCCG.LogManager.write(LogManager.INFO_LEVEL, logPrefix +"Back image not found for \""+ frontName +"\".");

        pathBack = Prefs.defaultBackPath;
      }
      newCard = new Card(frame, frontName, backName, setAbbrev, path, pathBack, flipState);
    }

    return newCard;
  }


  /**
   * Constructs a duplicate of a card.
   */
  public Card(ArcanistCCGFrame frame, Card c) {
    this.frame = frame;
    this.frontName = c.getFrontName();
    this.backName = c.getBackName();
    this.setAbbrev = c.getSetAbbrev();
    Create(c.getFrontPath(), c.getBackPath(), c.getFlipState());
  }

  /**
   * Constructor for when text is known.
   *
   * @param f the window to add this card to
   * @param frontName name of front
   * @param backName name of back
   * @param setAbbrev abbreviated set name
   * @param frontPath path to front image file
   * @param backPath path to back image file
   * @param flipState face up? (T/F)
   */
  public Card(ArcanistCCGFrame f, String frontName, String backName, String setAbbrev, String frontPath, final String backPath, boolean flipState) {
    frame = f;
    this.frontName = frontName;
    this.backName = backName;
    this.setAbbrev = setAbbrev;
    Create(frontPath, backPath, flipState);
  }

  /**
   * Basic Constructor for when only images are known.
   *
   * @param f the window to add this card to
   * @param frontPath path to front image file
   * @param backPath path to back image file
   * @param flipState face up? (T/F)
   */
  public Card(ArcanistCCGFrame f, String frontPath, String backPath, boolean flipState) {
    frame = f;
    frontName = (new File(frontPath)).getName();
    String[] tmp = frontPath.split("/");
    if (tmp != null && tmp.length > 1) {
      String setName = tmp[tmp.length-1-1];
      setAbbrev = Prefs.textDat.getSetAbbrevFromName(setName);
    }
    Create(frontPath, backPath, flipState);
  }

  /**
   * Initializes a Card object.
   *
   * @param fp path to card front image
   * @param bp path to card back image
   * @param flipState face up? (T/F)
   */
  private void Create(String fp, final String bp, boolean flipState) {
    frontPath = fp;
    backPath = bp;

    if (frontPath.equals(Prefs.defaultBlankPath) == true || frontPath.equals(Prefs.defaultErrorPath) == true)
      isText = true;
    else if (Prefs.useTextOnly == true || (new File(frontPath)).exists() == false) {
      isText = true;
      frontPath = Prefs.defaultBlankPath;
    }

    BufferedImage frontImage = Prefs.Cache.getImg(frontPath);

    cardComp = new CardComponent(frame, frontImage);
      hotspotArray.add(new CardSpinPanel(frame, this, 1));
      hotspotArray.add(new CardSpinPanel(frame, this, 2));
      hotspotArray.add(new CardSpinPanel(frame, this, 3));
      hotspotArray.add(new CardSpinPanel(frame, this, 4));
      cardComp.setBackImage(Prefs.Cache.getImg(backPath));
      cardComp.setBounds(0, 0, frontImage.getWidth(), frontImage.getHeight());
      cardComp.setFlipState(flipState);
      if (flipState == true) {cardComp.setToolTipText(frontName);}
      cardComp.setFocusable(true);
      cardComp.setShowText(isText);
      cardComp.setNameText(getFrontName(), getBackName());


    //Setup Popup menu
    popup = new JPopupMenu();
      // See top: remoteFlipMenuItem
      // See top: localFlipMenuItem
      // See top: flipMenuItem
      // See top: toTopMenuItem
      // See top: toBottomMenuItem
      // See top: dupMenuItem
      // See top: removeMenuItem
    popup.add(remoteFlipMenuItem);
    popup.add(localFlipMenuItem);
    popup.add(flipMenuItem);
    popup.add(toTopMenuItem);
    popup.add(toBottomMenuItem);
    popup.add(dupMenuItem);
    popup.add(removeMenuItem);


    // Set up listeners
    remoteFlipMenuItem.addActionListener(this);
    localFlipMenuItem.addActionListener(this);
    flipMenuItem.addActionListener(this);
    toTopMenuItem.addActionListener(this);
    toBottomMenuItem.addActionListener(this);
    dupMenuItem.addActionListener(this);
    removeMenuItem.addActionListener(this);


    cardListener = new GuiInterruptMouseAdapter() {
      boolean wasBusy = true;                                // Remembers not to unlock on release
      int xdist=0, ydist=0;
      JComponent dragObj = cardComp;

      @Override
      public void mouseEntered(MouseEvent e) {               // Update JumboView
        JumboView jumboFrame = frame.getJumboFrame();
        if (jumboFrame != null && jumboFrame.isJumboActive()) {
          if (!frame.isDragging() && (getFlipState() == true || backName.length() > 0) ) {
            boolean preferText = jumboFrame.getPreferText();
            if (e.isShiftDown()) preferText = !preferText;

            if (isText == true || preferText) {
              if (getFlipState() == true)
                jumboFrame.updateJumboText(new CardTextPanel(frontName, setAbbrev, false));
              else if (backName.length() > 0)
                jumboFrame.updateJumboText(new CardTextPanel(backName, setAbbrev, false));
            }
            else {
              if (getFlipState() == true)
                jumboFrame.updateJumboImage(frontPath);
              else if (backName.length()>0)
                jumboFrame.updateJumboImage(backPath);
            }
          }
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        cardComp.grabFocus();                                   //Focus this window, or dragging goes nuts

        if (e.getButton() == 1) {
          xdist = e.getX();
          ydist = e.getY();
          ArcanistCCG.NetManager.cardUse(arrayID());
          frame.setDragging(true);

          DragGhost dragGhost = frame.getDragGhost();
          dragGhost.setOffset(xdist, ydist);
          dragGhost.setSourceObject(dragObj);
          dragGhost.setSnap(true);
          frame.addDragGhost();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
        }
        //Right-Click Popup menu
        //  Technically This should be in both pressed and released

        //  Mac checks the trigger on press, Win on release
        //  But only macs have 1-button mice, which need the trigger check ;)
        if (e.getButton() == 3 || e.isPopupTrigger()) {
          localFlipMenuItem.setVisible(ArcanistCCG.NetManager.isOnline());
          remoteFlipMenuItem.setVisible(ArcanistCCG.NetManager.isOnline());
          popup.show(e.getComponent(), e.getX(), e.getY());
          return;
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

        ArcanistCCG.NetManager.cardUnuse(arrayID());

        boolean dropped = false;
        if (moved && !dropped) {
          Hand tmpHand = null;
          int handsCount = frame.getHandCount();
          for (int i=0; i < handsCount; i++) {
            tmpHand = frame.getHand(i);
            Point handSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), tmpHand);
            if ( tmpHand.contains(handSpace.x, handSpace.y) && tmpHand.isInUse() == false ) {
              int dstRow = tmpHand.getAddCardIndex(handSpace);
              ArcanistCCG.NetManager.cardHand(arrayID(), i, dstRow);

              dropped = true;
              break;
            }
          }
        }
        if (moved && !dropped) {
          Deck tmpDeck = null;
          int decksCount = frame.getDeckCount();
          for (int i=0; i < decksCount; i++) {
            tmpDeck = frame.getDeck(i);
            Point deckSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), tmpDeck.getComponent());
            if ( tmpDeck.getComponent().contains(deckSpace.x, deckSpace.y) && tmpDeck.isInUse() == false ) {
              boolean prevToBottom = tmpDeck.getToBottom();
              // Set AddToBottom if shift is held
              if (e.isShiftDown()) {
                if (prevToBottom == false) {
                  ArcanistCCG.NetManager.deckSetToBottom(i, true);
                }
              }

              ArcanistCCG.NetManager.cardDeck(arrayID(), i);

              // Revert AddToBottom if necessary
              if (e.isShiftDown()) {
                if (prevToBottom == false) {
                  ArcanistCCG.NetManager.deckSetToBottom(i, false);
                }
                String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
                ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" added a card under a deck--");
              }

              dropped = true;
              break;
            }
          }
        }
        if (moved && !dropped) {
          e = SwingUtilities.convertMouseEvent(dragObj, e, cardComp.getParent());  //Convert card coords to table coords
          Point snapPoint = frame.getTablePane().getNearestGridPoint( new Point(e.getX()-xdist, e.getY()-ydist) );

          ArcanistCCG.NetManager.cardMove(arrayID(), (int)snapPoint.getX(), (int)snapPoint.getY());
          dropped = true;
        }

        frame.getTablePane().repaint();
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

          ArcanistCCG.NetManager.cardUnuse(arrayID());
        }
      }
    };
    cardComp.addMouseListener(cardListener);


    FocusListener selectme = new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        cardComp.setBorder(BorderFactory.createLineBorder(Prefs.highlightColor));
      }

      @Override
      public void focusLost(FocusEvent e) {
        cardComp.setBorder(BorderFactory.createEmptyBorder());
      }
    };
    cardComp.addFocusListener(selectme);


    Action keyFlipRemoteAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (pronoun.isInUse()) return;
        if (ArcanistCCG.NetManager.isOnline() == false) return;

        ArcanistCCG.NetManager.cardRemoteFlip(arrayID());
      }
    };
    cardComp.getActionMap().put(Prefs.ACTION_CARD_FLIP_REMOTE, keyFlipRemoteAction);

    Action keyRotateLeftAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (pronoun.isInUse()) return;

        int newRotation = pronoun.getRotation()+90;
        ArcanistCCG.NetManager.cardRotate(arrayID(), newRotation);
      }
    };
    cardComp.getActionMap().put(Prefs.ACTION_CARD_ROTATE_LEFT, keyRotateLeftAction);

    Action keyRotateRightAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (pronoun.isInUse()) return;

        int newRotation = pronoun.getRotation()-90;
        ArcanistCCG.NetManager.cardRotate(arrayID(), newRotation);
      }
    };
    cardComp.getActionMap().put(Prefs.ACTION_CARD_ROTATE_RIGHT, keyRotateRightAction);

    Action keyRepoUpAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (pronoun.isInUse()) return;

        int current = frame.getTablePane().getPosition(cardComp);
        double carddiag = Math.sqrt(Math.pow(Prefs.defaultCardWidth,2) + Math.pow(Prefs.defaultCardHeight,2));

        if (current == 0) return;
        int candidate = -1;
        int cardsCount = frame.getCardCount();
        for(int i=0; i < cardsCount; i++) {
          CardComponent tmpComp = frame.getCard(i).getComponent();
          int tmpPos = frame.getTablePane().getPosition(tmpComp);
          if (tmpPos < current && tmpPos > candidate) {
            if (java.awt.geom.Point2D.distance(tmpComp.getBounds().x, tmpComp.getBounds().y, cardComp.getBounds().x, cardComp.getBounds().y) < carddiag) {
              candidate = tmpPos;
            }
          }
        }
        if (candidate == -1) candidate = frame.getTablePane().getPosition(cardComp)-1;

        ArcanistCCG.NetManager.cardReposition(arrayID(), candidate);
      }
    };
    cardComp.getActionMap().put(Prefs.ACTION_CARD_REPO_UP, keyRepoUpAction);

    Action keyRepoDownAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (pronoun.isInUse()) return;

        int current = frame.getTablePane().getPosition(cardComp);
        double carddiag = Math.sqrt(Math.pow(Prefs.defaultCardWidth,2) + Math.pow(Prefs.defaultCardHeight,2));

        int cardsCount = frame.getCardCount();
        int candidate = cardsCount-1;
        if (current == candidate) return;
        for(int i=0; i < cardsCount; i++) {
          CardComponent tmpComp = frame.getCard(i).getComponent();
          int tmpPos = frame.getTablePane().getPosition(tmpComp);
          if (tmpPos > current && tmpPos < candidate) {
            if (java.awt.geom.Point2D.distance(tmpComp.getBounds().x, tmpComp.getBounds().y, cardComp.getBounds().x, cardComp.getBounds().y) < carddiag) {
              candidate = tmpPos;
            }
          }
        }
        // If no nearby cards are lower
        if (candidate == cardsCount-1) candidate = frame.getTablePane().getPosition(cardComp)+1;

        ArcanistCCG.NetManager.cardReposition(arrayID(), candidate);
      }
    };
    cardComp.getActionMap().put(Prefs.ACTION_CARD_REPO_DOWN, keyRepoDownAction);

    cardComp.addMouseListener(frame.getFocusOnEnterListener());
    updateHotkeys();
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    if (isInUse() == true) return;

    Object source = e.getSource();

    if (source == remoteFlipMenuItem) {
      ArcanistCCG.NetManager.cardRemoteFlip(arrayID());
    }
    else if (source == localFlipMenuItem) {
      //Nag if we're peeking at a card
      boolean peeked = false;
      if (getFlipState() == false) peeked = true;

      ArcanistCCG.NetManager.cardLocalFlip(arrayID(), peeked);
      // //flip();
    }
    else if (source == flipMenuItem) {
      ArcanistCCG.NetManager.cardFlip(arrayID());
      // //flip();
    }
    else if (source == toTopMenuItem) {
      ArcanistCCG.NetManager.cardToFront(arrayID());
      // //moveToFront();
    }
    else if (source == toBottomMenuItem) {
      ArcanistCCG.NetManager.cardToBack(arrayID());
      // //moveToBack();
    }
    else if (source == dupMenuItem) {
      Rectangle tableView = frame.getTableView();

      ArcanistCCG.NetManager.cardAdd(tableView.x, tableView.y, frontName, backName, Prefs.textDat.getSetNameFromAbbrev(setAbbrev), getFrontFile(), getBackFile(), getFlipState(), getRotation());
    }
    else if (source == removeMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are You SURE You Want To Remove This?", "Remove?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        if (pronoun.existsOnTable()) {
          ArcanistCCG.NetManager.cardRemove(arrayID());
        }
      }
    }
  }


  public void guiInterrupted() {
    cardListener.guiInterrupted();
  }


  /**
  * Standard debug info.
  */
  public String toString() {
    return "Card" + cardComp.getId() +
      "\nName: "+ frontName +
      "\nSet: "+ setAbbrev +
      "\nFrontFile: " + frontPath +
      "\nBackName: " + backName +
      "\nBackFile: " + backPath +
      "\nArrayID: " + arrayID();
  }


  /**
   * Returns this object's table component.
   */
  public CardComponent getComponent() {
    return cardComp;
  }


  /**
   * Sets the card's front image.
   *
   * @param path path to card's front image
   */
  public void setFrontImage(String path) {
    cardComp.setFrontImage(Prefs.Cache.getImg(path));
    frontPath = path;
  }

  /**
   * Sets the card's back image.
   *
   * @param path path to card's back image
   */
  public void setBackImage(String path) {
    cardComp.setBackImage(Prefs.Cache.getImg(path));
    backPath = path;
  }


  /**
   * Rotates a card's front/back images.
   *
   * @param rot new angle in degrees
   */
  public void setRotation(int rot) {
    while (rot <= -360) rot += 360;
    while (rot >= 360) rot -= 360;
    if (rot == rotation) return;
    rotation = rot;
    double angle = Math.toRadians(rotation);

    BufferedImage thumbImage = Prefs.Cache.getImg(frontPath);
    if (angle != 0) {
      thumbImage = Prefs.Cache.getRotatedInstance(thumbImage, (double)-angle);
    }
    cardComp.setFrontImage(thumbImage);

    thumbImage = Prefs.Cache.getImg(backPath);
    if (angle != 0) {
      thumbImage = Prefs.Cache.getRotatedInstance(thumbImage, (double)-angle);
    }
    cardComp.setBackImage(thumbImage);

    cardComp.setSize(thumbImage.getWidth(), thumbImage.getHeight());
      for (int i=0; i < hotspotArray.size(); i++) {
        ((CardSpinPanel)hotspotArray.get(i)).rot(thumbImage.getWidth(), thumbImage.getHeight());
      }

    cardComp.setRotation(rotation);

    frame.getTablePane().repaint();
  }

  /**
   * Gets this card's rotation.
   * @return rotation in degrees
   */
  public int getRotation() {
    return (int)rotation;
  }


  /**
   * Gets this card's Id.
   *
   * @return the ID
   * @see CardComponent#getId()
   */
  public int getId() {return cardComp.getId();}

  /**
   * Sets this card's id.
   *
   * @param n new id
   */
  public void setId(int n) {cardComp.setId(n);}


  /**
   * Gets this card's index within the ArcanistCCGFrame's card array.
   *
   * @return the index, or -1 if absent
   */
  public int arrayID() {
    return frame.getCardIndex(this);
  }


  /**
   * Sets the used state.
   * Objects that are in use are busy with an opponent.
   * They will not respond to user input.
   * @param state true if in use, false otherwise
   */
  public void setInUse(boolean state) {
    this.inUse = state;
    cardComp.setPaintLock(state);
  }

  /**
   * Gets the used state.
   * Objects that are in use are busy with an opponent.
   * They will not respond to user input.
   * @return true if in use, false otherwise
   */
  public boolean isInUse() {
    return this.inUse;
  }


  /**
   * Adds this to the table.
   * The table will need to be validated and repainted afterward.
   */
  public void addToTable(int x, int y) {
    cardComp.setVisible(false);
    if (isText() || Prefs.textOnImages == true) updateOverlayText();
    frame.addCard(this);
    frame.getTablePane().add(cardComp, Prefs.cardLayer, 0);
    setLocation(x, y);
    cardComp.setVisible(true);
  }

  /**
   * Removes this from the table.
   * Attributes like rotation and flipState are not reset.
   * The table will need to be repainted afterward.
   */
  public void removeFromTable() {
    popup.setVisible(false);
    frame.removeCard(arrayID());
    frame.getTablePane().remove(cardComp);
  }

  /**
   * Determines whether this is present on the table.
   *
   * @return true if it exists, false otherwise
   */
  public boolean existsOnTable() {
    return frame.hasCard(this);
  }


  /**
   * Sets this object's location if it is on the table.
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void setLocation(int x, int y) {
    cardComp.setLocation(x, y);
  }

  /**
   * Returns this object's location.
   */
  public Point getLocation() {
    return cardComp.getLocation();
  }

  /**
   * Returns this object's x position.
   */
  public int getX() {
    return cardComp.getX();
  }

  /**
   * Returns this object's y position.
   */
  public int getY() {
    return cardComp.getY();
  }


  /**
   * Flips the card.
   * This also (en/dis)ables the card name tooltip.
   * The table will need to be repainted afterward.
   */
  public void flip() {
    if (getFlipState() == false) {
      cardComp.setFlipState(true);
      cardComp.setToolTipText(frontName);
    }
    else {
      cardComp.setFlipState(false);
      cardComp.setToolTipText(null);
    }
  }

  /**
   * Sets the card's flip state.
   * This ensures the card is face up or face down
   * by calling flip() if different from the argument.
   * @param flipState true for face up, false for face down
   */
  public void setFlipState(boolean flipState) {
    if (getFlipState() != flipState) {
      flip();
    }
  }

  /**
   * Gets the card's flip state.
   * @return true for face up, false for face down
   */
  public boolean getFlipState() {
    return cardComp.getFlipState();
  }


  /**
   * Moves this to the front, if on the table.
   */
  public void moveToFront() {
    if (frame.hasCard(this)) frame.getTablePane().moveToFront(cardComp);
  }

  /**
   * Moves this to the back, if on the table.
   */
  public void moveToBack() {
    if (frame.hasCard(this)) frame.getTablePane().moveToBack(cardComp);
  }


  /**
   * Gets this card's name.
   * @return the name
   */
  public String getFrontName() {
    return frontName;
  }

  /**
   * Gets this card's back name.
   * @return the name
   */
  public String getBackName() {
    return backName;
  }

  /**
   * Gets this card's abbreviated set name.
   */
  public String getSetAbbrev() {
    return setAbbrev;
  }

  /**
   * Gets this card's path to the front image.
   *
   * @return the path
   */
  public String getFrontPath() {
    return frontPath;
  }

  /**
   * Gets this card's front image filename.
   *
   * @return the filename
   */
  public String getFrontFile() {
    String[] tmpPath = frontPath.split("/");
    if (tmpPath != null && tmpPath.length > 0)
      return tmpPath[tmpPath.length-1];

    return "";
  }

  /**
   * Gets this card's path to the back image.
   *
   * @return the path
   */
  public String getBackPath() {
    return backPath;
  }

  /**
   * Gets this card's back image filename.
   *
   * @return the filename, the backPath if outside the expansion, or "" if it was the game default
   */
  public String getBackFile() {
    if (backPath.equals(Prefs.defaultBackPath)) return "";

    String[] tmpPath = backPath.split("/");
    if (tmpPath != null) {
      if (tmpPath.length > 1) {
        String fullSetName = Prefs.textDat.getSetNameFromAbbrev(setAbbrev);
        if (tmpPath[tmpPath.length-2].equals(fullSetName) == false) {
          return backPath;
        }
      }
      if (tmpPath.length > 0)
        return tmpPath[tmpPath.length-1];
    }

    return "";
  }


  /**
   * Returns true if this card is text-only.
   *
   * @return true if this card is text-only
   */
  public boolean isText() {
    return isText;
  }


  /**
   * Requests that this card get focus.
   */
  public void focus() {
    cardComp.requestFocusInWindow();
  }


  /**
   * Resets attributes to a consistent state.
   * Call this when adding to a container.
   * Rotation = 0. FlipState = true.
   */
  public void reset() {
    if (getRotation() != 0) setRotation(0);
    if (getFlipState() != true) setFlipState(true);
  }


  /**
   * Clears this object's hotkeys and reapplies current global ones.
   */
  public void updateHotkeys() {
    InputMap map = cardComp.getInputMap();
    KeyStroke[] keys = map.keys();
    if (keys != null) {
      for (int i=0; i < keys.length; i++) {
        Object o = map.get(keys[i]);
        if (o == Prefs.ACTION_CARD_FLIP_REMOTE) map.remove(keys[i]);
        else if (o == Prefs.ACTION_CARD_ROTATE_LEFT) map.remove(keys[i]);
        else if (o == Prefs.ACTION_CARD_ROTATE_RIGHT) map.remove(keys[i]);
        else if (o == Prefs.ACTION_CARD_REPO_UP) map.remove(keys[i]);
        else if (o == Prefs.ACTION_CARD_REPO_DOWN) map.remove(keys[i]);
      }
    }

    synchronized (Prefs.hotkeyLock) {
      Object tmpStroke = null;
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CARD_FLIP_REMOTE);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CARD_FLIP_REMOTE);
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CARD_ROTATE_LEFT);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CARD_ROTATE_LEFT);
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CARD_ROTATE_RIGHT);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CARD_ROTATE_RIGHT);
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CARD_REPO_UP);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CARD_REPO_UP);
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CARD_REPO_DOWN);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CARD_REPO_DOWN);
    }

    // This would underline the nth letter in the menu item
    //remoteFlipMenuItem.setDisplayedMnemonicIndex(8);
  }

  public void updateOverlayText() {
    int statsField = Prefs.cardTextOverlayStatsField;

    String frontStats = null;
    String backStats = null;
    if (statsField >= 0 && statsField < Prefs.textDat.getFieldCount()) {
      int frontDatIndex = Prefs.textDat.findCard(getFrontName(), getSetAbbrev());
      if (frontDatIndex != -1) {
        String[] frontText = Prefs.textDat.getCardText(frontDatIndex);
        if (frontText != null && frontText[statsField].length() > 0) frontStats = frontText[statsField];
      }
      int backDatIndex = Prefs.textDat.findCard(getBackName(), getSetAbbrev());
      if (backDatIndex != -1) {
        String[] backText = Prefs.textDat.getCardText(backDatIndex);
        if (backText != null && backText[statsField].length() > 0) backStats = backText[statsField];
      }
    }
    cardComp.setStatsText(frontStats, backStats);
  }



  /**
   * A card-rotating hotspot.
   * This is an invisible JPanel.
   */
  private static class CardSpinPanel extends JPanel {
    private ArcanistCCGFrame frame = null;

    private Card parentCard = null;
    private CardSpinPanel pronoun = this;
    private int size=15, corner=1;


    public CardSpinPanel (ArcanistCCGFrame f, Card card, int icorner) {
      super();
      parentCard = card; corner = icorner;
      frame = f;

      card.getComponent().add(this);

      switch (corner) {
        case 1:
          this.setBounds(new Rectangle(0, 0, size, size));
          break;
        case 2:
          this.setBounds(new Rectangle(Prefs.defaultCardWidth-size, 0, size, size));
          break;
        case 3:
          this.setBounds(new Rectangle(0, Prefs.defaultCardHeight-size, size, size));
          break;
        case 4:
          this.setBounds(new Rectangle(Prefs.defaultCardWidth-size, Prefs.defaultCardHeight-size, size, size));
          break;
      }

      this.setCursor(new Cursor(Cursor.HAND_CURSOR));


      MouseListener spinner = new MouseAdapter() {
        MouseMotionListener spinnability;
        int initialX = 0, initialY = 0;
        int initialrot = 0;
        int lastRotation = 0;

        public void mousePressed(MouseEvent e) {
          if (parentCard.isInUse() == true)
            return;

          Point cardSpace = SwingUtilities.convertPoint(pronoun, e.getPoint(), pronoun.getParent());
          initialX = cardSpace.x;
          initialY = cardSpace.y;

          initialrot = parentCard.getRotation();
          lastRotation = parentCard.getRotation();

          if (e.getButton() == 1) {
            spinnability = new MouseMotionAdapter() {
              public void mouseDragged(MouseEvent e) {
                e = SwingUtilities.convertMouseEvent(pronoun, e, pronoun.getParent());  //Convert pronoun coords to card coords
                boolean validAngle = false;
                int angle = 0;
                int newRotation = 0;
                if (e.getX() >= initialX && e.getY() < initialY) {
                  angle = (int)Math.toDegrees(Math.atan(Math.abs((double)(initialY-e.getY())/(double)(e.getX()-initialX))));
                }
                if (e.getX() >= initialX && e.getY() >= initialY) {
                  angle = (int)Math.toDegrees(Math.atan(Math.abs((double)(e.getY()-initialY)/(double)(e.getX()-initialX))));
                  angle = 360 - angle;
                }
                if (e.getX() < initialX && e.getY() < initialY) {
                  angle = (int)Math.toDegrees(Math.atan(Math.abs((double)(initialY-e.getY())/(double)(initialX-e.getX()))));
                  angle = 180 - angle;
                }
                if (e.getX() < initialX && e.getY() >= initialY) {
                  angle = (int)Math.toDegrees(Math.atan(Math.abs((double)(e.getY()-initialY)/(double)(initialX-e.getX()))));
                  angle = 180 + angle;
                }
                if (angle >= 315 || angle <= 45) {
                  validAngle = true; newRotation = -90;
                } else if (angle > 45 && angle <= 135) {
                  validAngle = true; newRotation = 0;
                } else if (angle > 135 && angle <= 225) {
                  validAngle = true; newRotation = -270;
                } else if (angle > 225 && angle < 315) {
                  validAngle = true; newRotation = -180;
                }

                if (validAngle && lastRotation != newRotation) {
                  lastRotation = newRotation;
                  ArcanistCCG.NetManager.cardRotate(parentCard.arrayID(), newRotation);
                  // //parentCard.setRotation(newRotation);
                }
              }
            };
            pronoun.addMouseMotionListener(spinnability);
            frame.setDragging(true);
            // //parentCard.setInUse(true);
            ArcanistCCG.NetManager.cardUse(parentCard.arrayID());
          }
        }

        public void mouseReleased(MouseEvent e) {
          if (frame.isDragging() == false)
            return;

          //if (lastRotation != initialrot) ArcanistCCG.NetManager.cardRotate(parentCard.arrayID(), lastRotation);

          pronoun.removeMouseMotionListener(spinnability);
          // //parentCard.setInUse(false);
          ArcanistCCG.NetManager.cardUnuse(parentCard.arrayID());

          frame.setDragging(false);
          frame.getTablePane().repaint();
        }
      };
      this.addMouseListener(spinner);
    }


    /**
     * Repositions hotspot when the parent card is rotated.
     * The card's rotImage function calls this.
     * @param width New card width
     * @param height New card height
     */
    public void rot(int width, int height) {
      switch (corner) {
        case 1:
          break;
        case 2:
          this.setBounds(new Rectangle(width-size, 0, size, size));
          break;
        case 3:
          this.setBounds(new Rectangle(0, height-size, size, size));
          break;
        case 4:
          this.setBounds(new Rectangle(width-size, height-size, size, size));
          break;
      }
    }

    public void paintComponent(Graphics g) {                 //setVisible() turns off cursor checking
      //super.paintComponent(g);
    }
  }
}