package org.arcanist.client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.arcanist.client.*;
import org.arcanist.util.*;


public class Hand extends JPanel implements ActionListener, GuiInterruptListener, CardListContainer, Dependent, HoverListener, DragGhostListener {

  public static final int DEFAULT_WIDTH = 180;
  public static final int DEFAULT_HEIGHT = 190;
  private static final int MINIMUM_WIDTH = 100;
  private static final int MINIMUM_HEIGHT = 100;
  private static final String PANEL_LIST = "List";
  private static final String PANEL_SETTINGS = "Settings";
  private static final String PANEL_HIDDEN = "Hidden";

  private ArcanistCCGFrame frame = null;

  private Hand pronoun = this;
  private int width=1, height=1;

  private int id = -1;
  private List<Card> cardList = new ArrayList<Card>();

  private String currentPanel = PANEL_LIST;
  private String title = "";
  private boolean revealed = true;
  private boolean hovering = false;
  private Point hoverOffset = new Point(0, 0);
  private Deck drawDeck = null;
  private Deck discardDeck = null;

  private JPanel glassPanel = null;
  private JPanel stretchRightPanel = null;
  private JPanel stretchBottomPanel = null;
  private JPanel titlePanel = null;
  private JPanel centerPanel = null;
  private JPanel actionsPanel = null;
  private JLabel countLbl = null;
  private JButton showSettingsBtn = null;
  private JButton randomBtn = null;
  private JButton discardBtn = null;
  private JButton drawBtn = null;
  private JButton drawXBtn = null;
  private ReticlePanel drawDeckReticleBox = null;
  private ReticlePanel discardDeckReticleBox = null;
  private JList handList = null;
  private LongListModel handListModel = new LongListModel();
  private JButton showListBtn = null;

  private JMenuItem remoteRevealMenuItem = new JMenuItem("Reveal to Others");
  private JMenuItem remoteHideMenuItem = new JMenuItem("Hide from Others");
  private JMenuItem sortMenuItem = new JMenuItem("Sort");
  private JMenuItem discardAllMenuItem = new JMenuItem("Discard All");
  private JMenuItem revealedMenuItem = new JCheckBoxMenuItem("Revealed Locally", true);
  private JMenuItem hoverMenuItem = new JCheckBoxMenuItem("Hover Locally", false);
  private JMenuItem removeMenuItem = new JMenuItem("Remove");

  /** Should a lock be overlaid on this and should the hand be inert. */
  public boolean inUse = false;

  /** Context menu */
  private JPopupMenu popup = null;

  private GuiInterruptMouseInputAdapter cardDrag = null;
  private GuiInterruptMouseAdapter handDrag = null;
  private GuiInterruptMouseAdapter handStretchRight = null;
  private GuiInterruptMouseAdapter handStretchBottom = null;
  private GuiInterruptMouseAdapter drawDeckReticleDrag = null;
  private GuiInterruptMouseAdapter discardDeckReticleDrag = null;


  public Hand(ArcanistCCGFrame f, String title, int sizeW, int sizeH) {
    super();
    frame = f;
    setTitle(title);

    this.setOpaque(true);
    this.setBorder(BorderFactory.createLineBorder(new Color(102, 102, 153)));
    this.setLayout(new BorderLayout());
    //After 1.5, setLocation/setSize > setBounds > reshape (needs parent)
    //This class overrode setSize
    this.setSize(sizeW, sizeH);

    //Setup Popup menu
    discardAllMenuItem.setEnabled(false);

    popup = new JPopupMenu();
      popup.add(remoteRevealMenuItem);
      popup.add(remoteHideMenuItem);
      popup.add(sortMenuItem);
      popup.add(discardAllMenuItem);
      popup.add(revealedMenuItem);
      popup.add(hoverMenuItem);
      popup.add(removeMenuItem);

    JPanel northPanel = new JPanel();
      northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
      this.add(northPanel, BorderLayout.NORTH);

      JPanel titleHolderPanel = new JPanel(new BorderLayout());
        titlePanel = new HandTitlePanel(frame, this);
          titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
          titlePanel.setBackground(new Color(204, 204, 255));
          titlePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(102, 102, 128)));
          JLabel titleLabel = new JLabel(title);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            titlePanel.add(titleLabel);
          titlePanel.add(titleLabel, BorderLayout.NORTH);
          titlePanel.add(Box.createHorizontalGlue());
          countLbl = new JLabel("0");
            countLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            titlePanel.add(countLbl);
        titleHolderPanel.add(titlePanel, BorderLayout.NORTH);
        northPanel.add(titleHolderPanel);

      actionsPanel = new JPanel(new CardLayout());
        northPanel.add(actionsPanel);

        JPanel listActionsPanel = new JPanel();
          listActionsPanel.setLayout(new BoxLayout(listActionsPanel, BoxLayout.X_AXIS));
          listActionsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(102, 102, 128)));
          showSettingsBtn = new JButton("...");
            showSettingsBtn.setMargin(new Insets(0, 0, 0, 0));
            listActionsPanel.add(showSettingsBtn);
          listActionsPanel.add(Box.createHorizontalGlue());
          randomBtn = new JButton("Random");
            randomBtn.setMargin(new Insets(0, 0, 0, 0));
            listActionsPanel.add(randomBtn);
          discardBtn = new JButton("Discard");
            discardBtn.setMargin(new Insets(0, 0, 0, 0));
            discardBtn.setEnabled(false);
            listActionsPanel.add(discardBtn);
          drawBtn = new JButton("Draw");
            drawBtn.setMargin(new Insets(0, 0, 0, 0));
            drawBtn.setEnabled(false);
            listActionsPanel.add(drawBtn);
          drawXBtn = new JButton("X");
            drawXBtn.setMargin(new Insets(0, 0, 0, 0));
            drawXBtn.setEnabled(false);
            listActionsPanel.add(drawXBtn);
          actionsPanel.add(listActionsPanel, PANEL_LIST);

        JPanel settingsActionsPanel = new JPanel();
          settingsActionsPanel.setLayout(new BoxLayout(settingsActionsPanel, BoxLayout.X_AXIS));
          settingsActionsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(102, 102, 128)));
          showListBtn = new JButton("<");
            showListBtn.setMargin(new Insets(0, 0, 0, 0));
            settingsActionsPanel.add(showListBtn);
          actionsPanel.add(settingsActionsPanel, PANEL_SETTINGS);

        JPanel hiddenActionsPanel = new JPanel();
          hiddenActionsPanel.setBorder(BorderFactory.createEtchedBorder());
          actionsPanel.add(hiddenActionsPanel, PANEL_HIDDEN);

    centerPanel = new JPanel(new CardLayout());
      this.add(centerPanel, BorderLayout.CENTER);

      JPanel listMainPanel = new JPanel(new BorderLayout());
        JPanel glassPanelHolder = new JPanel();
          glassPanelHolder.setLayout(new OverlayLayout(glassPanelHolder));
          glassPanel = new JPanel();
            glassPanel.setOpaque(false);
            glassPanelHolder.add(glassPanel);
          handList = new JList(handListModel);
            handList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            final JScrollPane handListScrollPane = new JScrollPane(handList);
              //handList.addNotify();
              glassPanelHolder.add(handListScrollPane);
          listMainPanel.add(glassPanelHolder, BorderLayout.CENTER);

          MouseListener[] doomedMouseListeners = handList.getMouseListeners();
          for (int i=doomedMouseListeners.length-1; i >= 0; i--) handList.removeMouseListener(doomedMouseListeners[i]);

          MouseMotionListener[] doomedMotionListeners = handList.getMouseMotionListeners();
          for (int i=doomedMotionListeners.length-1; i >= 0; i--) handList.removeMouseMotionListener(doomedMotionListeners[i]);
        centerPanel.add(listMainPanel, PANEL_LIST);

      JPanel settingsMainPanel = new JPanel();
        GridBagConstraints gc = new GridBagConstraints();
          gc.gridwidth = GridBagConstraints.REMAINDER;
          gc.weightx = 1;
          gc.fill = GridBagConstraints.HORIZONTAL;
        settingsMainPanel.setLayout(new GridBagLayout());
        settingsMainPanel.setBorder(BorderFactory.createEtchedBorder());
        JLabel reticleInfoLbl = new JLabel("Drag targets below to set");
          reticleInfoLbl.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
          settingsMainPanel.add(reticleInfoLbl, gc);
        JPanel drawReticleHolder = new JPanel(new BorderLayout());
          drawReticleHolder.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 5));
          drawReticleHolder.add(new JLabel("Draw deck:"), BorderLayout.WEST);
          drawDeckReticleBox = new ReticlePanel();
            drawReticleHolder.add(drawDeckReticleBox, BorderLayout.EAST);
          settingsMainPanel.add(drawReticleHolder, gc);
        JPanel discardReticleHolder = new JPanel(new BorderLayout());
          discardReticleHolder.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 5));
          discardReticleHolder.add(new JLabel("Discard deck:"), BorderLayout.WEST);
          discardDeckReticleBox = new ReticlePanel();
            discardReticleHolder.add(discardDeckReticleBox, BorderLayout.EAST);
          settingsMainPanel.add(discardReticleHolder, gc);
        gc.weighty = 1;
        settingsMainPanel.add(Box.createVerticalGlue(), gc);
        centerPanel.add(settingsMainPanel, PANEL_SETTINGS);

      JPanel hiddenMainPanel = new JPanel(new BorderLayout());
        hiddenMainPanel.setBorder(BorderFactory.createEtchedBorder());
        centerPanel.add(hiddenMainPanel, PANEL_HIDDEN);

    stretchRightPanel = new JPanel() {
      public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension s = this.getSize();
        int margin = 25;
        if (s.height-margin*2 <= 0) return;
        int pos = s.width/2;
        g.setColor(this.getForeground());
        g.drawLine(pos, margin, pos, s.height-margin );
      }
    };
      stretchRightPanel.setPreferredSize(new Dimension(7, 1));
      //stretchRightPanel.setBackground(Color.WHITE);
      stretchRightPanel.setForeground(new Color(102, 102, 128));
      stretchRightPanel.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
      this.add(stretchRightPanel, BorderLayout.EAST);

    JPanel stretchBottomHolderPanel = new JPanel(new BorderLayout());
      stretchBottomPanel = new JPanel() {
        public void paintComponent(Graphics g) {
          super.paintComponent(g);
          Dimension s = this.getSize();
          int margin = 25;
          if (s.width-margin*2 <= 0) return;
          int pos = s.height/2;
          g.setColor(this.getForeground());
          g.drawLine(margin, pos, s.width-margin, pos);
        }
      };
        stretchBottomPanel.setPreferredSize(new Dimension(1, 7));
        //stretchBottomPanel.setBackground(Color.WHITE);
        stretchBottomPanel.setForeground(new Color(102, 102, 128));
        stretchBottomPanel.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
        stretchBottomHolderPanel.add(stretchBottomPanel, BorderLayout.CENTER);
        stretchBottomHolderPanel.add(Box.createHorizontalStrut(stretchRightPanel.getPreferredSize().width), BorderLayout.EAST);
      this.add(stretchBottomHolderPanel, BorderLayout.SOUTH);


    remoteRevealMenuItem.addActionListener(this);
    remoteHideMenuItem.addActionListener(this);
    sortMenuItem.addActionListener(this);
    discardAllMenuItem.addActionListener(this);
    revealedMenuItem.addActionListener(this);
    hoverMenuItem.addActionListener(this);
    removeMenuItem.addActionListener(this);
    showSettingsBtn.addActionListener(this);
    showListBtn.addActionListener(this);
    randomBtn.addActionListener(this);
    discardBtn.addActionListener(this);
    drawBtn.addActionListener(this);
    drawXBtn.addActionListener(this);


    MouseWheelListener listScroll = new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        if (!handListScrollPane.isWheelScrollingEnabled()) return;
        if (e.getScrollAmount() == 0) return;

        JScrollBar vb = handListScrollPane.getVerticalScrollBar();
        if (!vb.isVisible()) return;

        int notches = e.getWheelRotation();
        int direction = (notches<0 ? -1 : 1);

        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
          int delta = vb.getUnitIncrement(direction) * notches;
          vb.setValue(vb.getValue() + delta);
        }
        else if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
          int delta = vb.getBlockIncrement(direction) * notches;
          vb.setValue(vb.getValue() + delta);
        }
      }
    };
    glassPanel.addMouseWheelListener(listScroll);


    cardDrag = new GuiInterruptMouseInputAdapter() {
      JComponent[] dispatchTargets = new JComponent[] {
        handListScrollPane.getVerticalScrollBar(),
        handListScrollPane.getHorizontalScrollBar()
      };
      JComponent redispatching = null;
      boolean wasBusy = false;                               //Remembers not to unlock on release
      int xdist=0, ydist=0;
      JComponent dragObj = glassPanel;
      int nearestRow = -1;

      @Override
      public void mousePressed(MouseEvent e) {
        if (redispatch(e, true) != null) return;

        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        if (e.getButton() == 1) {
          Point handListSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), handList);
          nearestRow = handList.locationToIndex(handListSpace);
          if (nearestRow == -1) return;
          Rectangle cellBounds = handList.getCellBounds(nearestRow, nearestRow);
          if (cellBounds.contains(handListSpace) == false) return;

          handList.setSelectedIndex(nearestRow);
          Point rowLoc = SwingUtilities.convertPoint(handList, new Point(cellBounds.x, cellBounds.y), handListScrollPane);
          xdist = e.getX() - rowLoc.x;
          ydist = e.getY() - rowLoc.y;
          ArcanistCCG.NetManager.handUse(arrayID());
          frame.setDragging(true);

          DragGhost dragGhost = frame.getDragGhost();
          dragGhost.setOffset(xdist, ydist);
          dragGhost.setSourceObject(dragObj, pronoun, cellBounds.width, cellBounds.height);
          frame.addDragGhost();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (redispatching != null) {
          redispatch(e, false);
          redispatching = null;
          return;
        }

        if (wasBusy == true) return;
        if (nearestRow == -1) return;
                                                             //A Click triggers all 3: P,R,C
                                                             //So clicking briefly adds the listener
        DragGhost dragGhost = frame.getDragGhost();
        boolean moved = dragGhost.hasMoved();
        dragGhost.setSourceObject(null);
        frame.removeDragGhost();
        frame.setDragging(false);
        frame.getTablePane().revalidate();
        frame.getTablePane().repaint();

        ArcanistCCG.NetManager.handUnuse(arrayID());

        Point tableSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), pronoun.getParent());

        boolean dropped = false;
        if (moved && !dropped) {
          if (pronoun.getBounds().contains(tableSpace)) {
            Point handSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), pronoun);
            int dstRow = getAddCardIndex(handSpace);
            ArcanistCCG.NetManager.handReorderCard(arrayID(), cardList.get(nearestRow).getId(), dstRow);
            dropped = true;
          }
        }
        if (moved && !dropped) {
          Hand tmpHand = null;
          int handsCount = frame.getHandCount();
          for (int i=0; i < handsCount; i++) {
            tmpHand = frame.getHand(i);
            if (tmpHand == pronoun) continue;
            Point handSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), tmpHand);
            if ( tmpHand.contains(handSpace.x, handSpace.y) && tmpHand.isInUse() == false ) {
              int dstRow = tmpHand.getAddCardIndex(handSpace);
              ArcanistCCG.NetManager.handHand(arrayID(), i, cardList.get(nearestRow).getId(), dstRow);

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
              //Set AddToBottom if shift is held
              if (e.isShiftDown()) {
                if (prevToBottom == false) {
                  ArcanistCCG.NetManager.deckSetToBottom(i, true);
                }
              }

              ArcanistCCG.NetManager.handDiscard(arrayID(), i, cardList.get(nearestRow).getId());

              //Revert AddToBottom if necessary
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
          Point snapPoint = frame.getTablePane().getNearestGridPoint( new Point(tableSpace.x-xdist, tableSpace.y-ydist) );
          ArcanistCCG.NetManager.handGetCard(arrayID(), cardList.get(nearestRow).getId(), snapPoint.x, snapPoint.y, !e.isShiftDown());
          dropped = true;
        }
        if (!moved) handList.requestFocusInWindow();
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        if (redispatch(e, false) != null) return;

        JumboView jumboFrame = frame.getJumboFrame();
        if (jumboFrame != null && jumboFrame.isJumboActive()) {
          if (!frame.isDragging()) {
            Point handListSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), handList);
            nearestRow = handList.locationToIndex(handListSpace);
            if (nearestRow == -1) return;
            Rectangle cellBounds = handList.getCellBounds(nearestRow, nearestRow);
            if (cellBounds.contains(handListSpace) == false) return;

            Card tmpCard = cardList.get(nearestRow);
            if (jumboFrame.isShownInJumbo(tmpCard.getFrontPath(), tmpCard.getFrontName(), tmpCard.getSetAbbrev()) == false) {
              boolean preferText = jumboFrame.getPreferText();
              if (e.isShiftDown()) preferText = !preferText;

              if (tmpCard.isText() == true || preferText)
                jumboFrame.updateJumboText(new CardTextPanel(tmpCard.getFrontName(), tmpCard.getSetAbbrev(), false));
              else
                jumboFrame.updateJumboImage(tmpCard.getFrontPath());
            }
          }
        }
      }

      @Override
      public void mouseClicked(MouseEvent e) {redispatch(e, false);}
      @Override
      public void mouseDragged(MouseEvent e) {redispatch(e, false);}
      @Override
      public void mouseEntered(MouseEvent e) {redispatch(e, false);}
      @Override
      public void mouseExited(MouseEvent e) {redispatch(e, false);}

      /**
       * If an inner component should get these events,
       * redispatch and don't bother further in this listener.
       */
      private JComponent redispatch(MouseEvent e, boolean checkBounds) {
        JComponent target = null;
        if (checkBounds && redispatching == null) {
          Point targetParentSpace = null;
          for (int i=0; i < dispatchTargets.length; i++) {
            target = dispatchTargets[i];
            targetParentSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), target.getParent());
            if (target.getBounds().contains(targetParentSpace)) {
              redispatching = target;
              break;
            }
          }
        }
        if (redispatching == null) return null;

        for (int i=0; i < dispatchTargets.length; i++) {
          target = dispatchTargets[i];
          if (target.isVisible() && redispatching == target) {
            MouseEvent targetEvent = SwingUtilities.convertMouseEvent(dragObj, e, target);
            target.dispatchEvent(targetEvent);
            return target;
          }
        }

        redispatching = null;
        return null;
      }

      @Override
      public void guiInterrupted() {
        popup.setVisible(false);
        wasBusy = true;
        redispatching = null;

        DragGhost dragGhost = frame.getDragGhost();
        if (dragObj.equals(dragGhost.getSourceObject())) {
          dragGhost.setSourceObject(null);
          frame.removeDragGhost();
          frame.setDragging(false);
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();

          ArcanistCCG.NetManager.handUnuse(arrayID());
        }
      }
    };
    glassPanel.addMouseListener(cardDrag);
    glassPanel.addMouseMotionListener(cardDrag);
    glassPanel.addMouseListener(frame.getFocusOnEnterListener());


    handDrag = new GuiInterruptMouseAdapter() {
      boolean wasBusy = false;                               //Remembers not to unlock on release
      int xdist=0, ydist=0;
      JComponent dragObj = titlePanel;

      @Override
      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        if (e.getButton() == 1) {
          xdist = e.getX();
          ydist = e.getY();
          ArcanistCCG.NetManager.handUse(arrayID());
          frame.setDragging(true);

          DragGhost dragGhost = frame.getDragGhost();
          dragGhost.setOffset(xdist, ydist);
          dragGhost.setSourceObject(dragObj, pronoun.getSize().width, pronoun.getSize().height);
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

        ArcanistCCG.NetManager.handUnuse(arrayID());

        boolean dropped = false;
        if (moved && !dropped) {
          e = SwingUtilities.convertMouseEvent(dragObj, e, pronoun.getParent());  //Convert to table coords

          ArcanistCCG.NetManager.handMove(arrayID(), (e.getX()-xdist), (e.getY()-ydist));
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

          ArcanistCCG.NetManager.handUnuse(arrayID());
        }
      }
    };
    titlePanel.addMouseListener(handDrag);
    titlePanel.addMouseListener(frame.getFocusOnEnterListener());


    handStretchRight = new GuiInterruptMouseAdapter() {
      boolean wasBusy = false;                               //Remembers not to unlock on release
      MouseMotionListener dragability = null;
      int xdist=0, ydist=0;
      JComponent dragObj = stretchRightPanel;
      int newWidth=0, newHeight=0;
      boolean moved = false;

      @Override
      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        if (e.getButton() == 1) {
          moved = false;
          xdist = e.getX();
          ydist = e.getY();
          ArcanistCCG.NetManager.handUse(arrayID());
          frame.setDragging(true);

          dragability = new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
              e = SwingUtilities.convertMouseEvent(dragObj, e, pronoun);  //Convert to hand coords
              if (e.getX()+(dragObj.getWidth()-xdist) < MINIMUM_WIDTH) return;
              newWidth = e.getX()+(dragObj.getWidth()-xdist);
              newHeight = pronoun.getHeight();

              pronoun.setSize(newWidth, newHeight);
              pronoun.revalidate();
              moved = true;
            }
          };
          dragObj.addMouseMotionListener(dragability);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (wasBusy == true) return;
                                                             //A Click triggers all 3: P,R,C
                                                             //So clicking briefly adds the listener
        dragObj.removeMouseMotionListener(dragability);
        frame.setDragging(false);
        ArcanistCCG.NetManager.handUnuse(arrayID());

        if (moved) {
          e = SwingUtilities.convertMouseEvent(dragObj, e, stretchRightPanel.getParent().getParent());  //Convert to table coords

          ArcanistCCG.NetManager.handResize(arrayID(), newWidth, newHeight);
        }
      }

      @Override
      public void guiInterrupted() {
        popup.setVisible(false);
        wasBusy = true;

        boolean found = false;
        EventListener[] mmlArray = dragObj.getListeners(MouseMotionListener.class);
        for (int i=0; i < mmlArray.length; i++) {
          if (mmlArray[i] == dragability) {found = true; break;}
        }

        if (found) {
          dragObj.removeMouseMotionListener(dragability);
          frame.setDragging(false);

          ArcanistCCG.NetManager.handUnuse(arrayID());
        }
      }
    };
    stretchRightPanel.addMouseListener(handStretchRight);
    stretchRightPanel.addMouseListener(frame.getFocusOnEnterListener());


    handStretchBottom = new GuiInterruptMouseAdapter() {
      boolean wasBusy = false;                               //Remembers not to unlock on release
      MouseMotionListener dragability = null;
      int xdist=0, ydist=0;
      JComponent dragObj = stretchBottomPanel;
      int newWidth=0, newHeight=0;
      boolean moved = false;

      @Override
      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        if (e.getButton() == 1) {
          moved = false;
          xdist = e.getX();
          ydist = e.getY();
          ArcanistCCG.NetManager.handUse(arrayID());
          frame.setDragging(true);

          dragability = new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
              e = SwingUtilities.convertMouseEvent(dragObj, e, pronoun);  //Convert to hand coords
              if (e.getY()+(dragObj.getHeight()-ydist) < MINIMUM_HEIGHT) return;
              newWidth = pronoun.getWidth();
              newHeight = e.getY()+(dragObj.getHeight()-ydist);

              pronoun.setSize(newWidth, newHeight);
              pronoun.revalidate();
              moved = true;
            }
          };
          dragObj.addMouseMotionListener(dragability);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (wasBusy == true) return;
                                                             //A Click triggers all 3: P,R,C
                                                             //So clicking briefly adds the listener
        dragObj.removeMouseMotionListener(dragability);
        frame.setDragging(false);
        ArcanistCCG.NetManager.handUnuse(arrayID());

        if (moved) {
          e = SwingUtilities.convertMouseEvent(dragObj, e, stretchBottomPanel.getParent().getParent());  //Convert to table coords

          ArcanistCCG.NetManager.handResize(arrayID(), newWidth, newHeight);
        }
      }

      @Override
      public void guiInterrupted() {
        popup.setVisible(false);
        wasBusy = true;

        boolean found = false;
        EventListener[] mmlArray = dragObj.getListeners(MouseMotionListener.class);
        for (int i=0; i < mmlArray.length; i++) {
          if (mmlArray[i] == dragability) {found = true; break;}
        }

        if (found) {
          dragObj.removeMouseMotionListener(dragability);
          frame.setDragging(false);

          ArcanistCCG.NetManager.handUnuse(arrayID());
        }
      }
    };
    stretchBottomPanel.addMouseListener(handStretchBottom);
    stretchBottomPanel.addMouseListener(frame.getFocusOnEnterListener());


    drawDeckReticleDrag = new GuiInterruptMouseAdapter() {
      boolean wasBusy = false;                               //Remembers not to unlock on release
      int xdist=0, ydist=0;
      JComponent dragObj = drawDeckReticleBox;

      @Override
      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        if (e.getButton() == 1) {
          xdist = e.getX();
          ydist = e.getY();
          ArcanistCCG.NetManager.handUse(arrayID());
          frame.setDragging(true);

          DragGhost dragGhost = frame.getDragGhost();
          dragGhost.setOffset(xdist, ydist);
          dragGhost.setSourceObject(dragObj, 50, 50);
          dragGhost.setPaintReticle(true);
          frame.addDragGhost();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
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

        ArcanistCCG.NetManager.handUnuse(arrayID());

        boolean dropped = false;
        if (moved && !dropped) {
          Point tableSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), pronoun.getParent());
          Deck tmpDeck = null;
          int decksCount = frame.getDeckCount();
          Point deckSpace = null;
          boolean foundDeck = false;
          for (int i=0; i < decksCount; i++) {
            tmpDeck = frame.getDeck(i);
            deckSpace = SwingUtilities.convertPoint(pronoun.getParent(), tableSpace, tmpDeck.getComponent());
            if ( tmpDeck.getComponent().contains(deckSpace.x, deckSpace.y) && tmpDeck.isInUse() == false ) {
              foundDeck = true;
              ArcanistCCG.NetManager.handSetDrawDeck(arrayID(), i);
              break;
            }
          }
          if (!foundDeck) ArcanistCCG.NetManager.handSetDrawDeck(arrayID(), -1);

          dropped = true;
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

          ArcanistCCG.NetManager.handUnuse(arrayID());
        }
      }
    };
    drawDeckReticleBox.addMouseListener(drawDeckReticleDrag);


    discardDeckReticleDrag = new GuiInterruptMouseAdapter() {
      boolean wasBusy = false;                               //Remembers not to unlock on release
      int xdist=0, ydist=0;
      JComponent dragObj = discardDeckReticleBox;

      @Override
      public void mousePressed(MouseEvent e) {
        wasBusy = false;                                     //Reset the var
        if (pronoun.isInUse()) {wasBusy = true; return;}

        if (e.getButton() == 1) {
          xdist = e.getX();
          ydist = e.getY();
          ArcanistCCG.NetManager.handUse(arrayID());
          frame.setDragging(true);

          DragGhost dragGhost = frame.getDragGhost();
          dragGhost.setOffset(xdist, ydist);
          dragGhost.setSourceObject(dragObj, 50, 50);
          dragGhost.setPaintReticle(true);
          frame.addDragGhost();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
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

        ArcanistCCG.NetManager.handUnuse(arrayID());

        boolean dropped = false;
        if (moved && !dropped) {
          Point tableSpace = SwingUtilities.convertPoint(dragObj, e.getPoint(), pronoun.getParent());
          Deck tmpDeck = null;
          int decksCount = frame.getDeckCount();
          Point deckSpace = null;
          boolean foundDeck = false;
          for (int i=0; i < decksCount; i++) {
            tmpDeck = frame.getDeck(i);
            deckSpace = SwingUtilities.convertPoint(pronoun.getParent(), tableSpace, tmpDeck.getComponent());
            if ( tmpDeck.getComponent().contains(deckSpace.x, deckSpace.y) && tmpDeck.isInUse() == false ) {
              foundDeck = true;
              ArcanistCCG.NetManager.handSetDiscardDeck(arrayID(), i);
              break;
            }
          }
          if (!foundDeck) ArcanistCCG.NetManager.handSetDiscardDeck(arrayID(), -1);

          dropped = true;
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

          ArcanistCCG.NetManager.handUnuse(arrayID());
        }
      }
    };
    discardDeckReticleBox.addMouseListener(discardDeckReticleDrag);


    handListModel.addListDataListener(new ListDataListener() {
      @Override
      public void contentsChanged(ListDataEvent e) {updateCountLabel();}

      @Override
      public void intervalAdded(ListDataEvent e) {updateCountLabel();}

      @Override
      public void intervalRemoved(ListDataEvent e) {updateCountLabel();}
    });
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    if (isInUse() == true) return;

    Object source = e.getSource();

    if (source == remoteRevealMenuItem) {
      boolean state = true;
      ArcanistCCG.NetManager.handSetRemoteRevealed(arrayID(), state);

      String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
      ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" "+ (state?"revealed hand to":"hid hand from") +" others--");
    }
    else if (source == remoteHideMenuItem) {
      boolean state = false;
      ArcanistCCG.NetManager.handSetRemoteRevealed(arrayID(), state);

      String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
      ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" "+ (state?"revealed hand to":"hid hand from") +" others--");
    }
    else if (source == sortMenuItem) {
      if (cardList.size() == 0) return;

      ArcanistCCG.NetManager.handUse(arrayID());
      List<Integer> newOrder = genSortedOrder();
      ArcanistCCG.NetManager.handReorder(arrayID(), newOrder);
      ArcanistCCG.NetManager.handUnuse(arrayID());
    }
    else if (source == discardAllMenuItem) {
      if (cardList.size() == 0) return;

      Deck tmpDeck = getDiscardDeck();
      if (tmpDeck != null) {
        if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Really discard the entire hand?", "Discard All?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        for (int i=cardList.size()-1; i >= 0; i--) {
          ArcanistCCG.NetManager.handDiscard(arrayID(), tmpDeck.arrayID(), cardList.get(i).getId());
        }
        String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
        ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE2, "--"+ chatAlias +" discarded all cards from hand--");
      }
    }
    else if (source == revealedMenuItem) {
      boolean state = !getRevealed();
      ArcanistCCG.NetManager.handSetLocalRevealed(arrayID(), state);

      String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
      ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" locally "+ (state?"revealed":"hid") +" hand--");
    }
    else if (source == hoverMenuItem) {
      boolean state = !getHovering();
      setHovering(state);
    }
    else if (source == removeMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are You SURE You Want To Remove This?", "Remove?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        if (pronoun.existsOnTable()) {
          ArcanistCCG.NetManager.handRemove(arrayID());
        }
      }
    }
    else if (source == showSettingsBtn) {
      ((CardLayout)actionsPanel.getLayout()).show(actionsPanel, PANEL_SETTINGS);
      ((CardLayout)centerPanel.getLayout()).show(centerPanel, PANEL_SETTINGS);
      currentPanel = PANEL_SETTINGS;
    }
    else if (source == showListBtn) {
      ((CardLayout)actionsPanel.getLayout()).show(actionsPanel, PANEL_LIST);
      ((CardLayout)centerPanel.getLayout()).show(centerPanel, PANEL_LIST);
      currentPanel = PANEL_LIST;
    }
    else if (source == randomBtn) {
      int cardNum = (int) (Math.random() * handListModel.size());
      ArcanistCCG.NetManager.handCardSelect(arrayID(), cardNum);
    }
    else if (source == discardBtn) {
      Deck tmpDeck = getDiscardDeck();
      if (tmpDeck != null) {
        int index = handList.getSelectedIndex();
        if (handListModel.size() > 0 && index != -1) {
          ArcanistCCG.NetManager.handDiscard(arrayID(), tmpDeck.arrayID(), cardList.get(index).getId());

          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
          ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE2, "--"+ chatAlias +" discarded a card from hand--");
        }
      }
    }
    else if (source == drawBtn) {
      Deck tmpDeck = getDrawDeck();
      if (tmpDeck != null && tmpDeck.getCardCount() > 0) {
        ArcanistCCG.NetManager.handDraw(arrayID(), tmpDeck.arrayID(), 1);

        String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
        ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE2, "--"+ chatAlias +" drew a card into hand--");
      }
    }
    else if (source == drawXBtn) {
      Deck tmpDeck = getDrawDeck();
      if (tmpDeck != null && tmpDeck.getCardCount() > 0) {
        String userinput = "";
        int x = -1;
        do {
          userinput = JOptionPane.showInternalInputDialog(frame.getDesktop(), "Draw How Many Cards?", "Draw X Cards", JOptionPane.QUESTION_MESSAGE);
          if (userinput == null || userinput.length() == 0 || existsOnTable() == false || tmpDeck.existsOnTable() == false)
            break;
          else {
            x = Integer.parseInt(userinput);
            if (x > 0 && x <= tmpDeck.getCardCount()) {
              ArcanistCCG.NetManager.handDraw(arrayID(), tmpDeck.arrayID(), x);

              String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
              ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE2, "--"+ chatAlias +" drew "+ x +" cards into hand--");
            }
          }
        } while (x < 0);
      }
    }
  }


  @Override
  public void guiInterrupted() {
    cardDrag.guiInterrupted();
    handDrag.guiInterrupted();
    handStretchRight.guiInterrupted();
    handStretchBottom.guiInterrupted();
    drawDeckReticleDrag.guiInterrupted();
    discardDeckReticleDrag.guiInterrupted();
  }


  /**
   * Notifies a deck was removed.
   * Performs cleanup operations.
   */
  @Override
  public void sourceRemoved(Object source) {
    if (source instanceof Deck) {
      if (source == getDrawDeck()) setDrawDeck(null);
      if (source == getDiscardDeck()) setDiscardDeck(null);
    }
  }

  public void setHoverOffset(int x, int y) {hoverOffset.x = x; hoverOffset.y = y;}

  public Point getHoverOffset() {return hoverOffset;}

  public void hoverTo(int x, int y) {
    ArcanistCCG.NetManager.handMove(arrayID(), x, y);
  }


  /**
   * Syncs the count label with the list of cards.
   */
  private void updateCountLabel() {
    countLbl.setText(""+ getCardCount());
  }


  /**
   * Sets the serial number.
   *
   * @param n id.
   */
  public void setId(int n) {id = n;}

  /**
   * Returns the serial number.
   */
  public int getId() {
    return id;
  }


  /**
   * Gets this card's index within the ArcanistCCGFrame's hand array.
   *
   * @return the index, or -1 if absent
   */
  public int arrayID() {
    return frame.getHandIndex(this);
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
    this.setVisible(false);
    frame.addHand(this);
    frame.getTablePane().add(this, Prefs.handLayer, 0);
    this.setBounds(x, y, width, height);
    this.setVisible(true);
  }

  /**
   * Removes this from the table.
   * It will be removed from the ArcanistCCGFrame's HoverListeners.
   * Attributes like autoface are not reset.
   * The table will need to be repainted afterward.
   */
  public void removeFromTable() {
    popup.setVisible(false);
    frame.removeHoverListener(this);
    frame.removeHand(arrayID());
    frame.getTablePane().remove(this);
  }

  /**
   * Determines whether this is present on the table.
   * @return true if it exists, false otherwise
   */
  public boolean existsOnTable() {
    return frame.hasHand(this);
  }


  /**
   * Resizes this component so that it has width w and height h.
   * If it has no parent, the size will be cached until
   * addToTable() is called.
   */
  public void setSize(int w, int h) {
    width = w; height = h;
    if (this.getParent() != null) super.setSize(w, h);
  }


  /**
   * Sets this object's location.
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void setLocation(int x, int y) {
    super.setLocation(x, y);
    Rectangle tableView = frame.getTableView();
    hoverOffset.x = x - tableView.x;
    hoverOffset.y = y - tableView.y;
  }

  /**
   * Gets this object's location if it is on the table.
   *
   * @return a point with the coordinates, or null if not present
   */
  public Point getLocation() {
    if (this.getParent() != null) {
      return super.getLocation(null);
    } else {
      return null;
    }
  }



  /**
   * Sets the title.
   */
  public void setTitle(String s) {title = s;}

  /**
   * Gets the title.
   */
  public String getTitle() {return title;}


  /**
   * Sets the hovering state.
   * When the table is scrolled, this will
   * maintain its position in the viewport. if
   * outside the viewport, no movement will happen.
   * This method will register/unregister as one
   * of the ArcanistCCGFrame's HoverListeners.
   */
  public void setHovering(boolean state) {
    if (state == true) {
      Rectangle tableView = frame.getTableView();
      Point curPos = pronoun.getLocation();
      setHoverOffset(curPos.x - tableView.x, curPos.y - tableView.y);
      frame.addHoverListener(this);
      hovering = true;
    } else {
      frame.removeHoverListener(this);
      hovering = false;
    }
  }

  /**
   * Gets the hovering state.
   */
  public boolean getHovering() {return hovering;}


  /**
   * Sets the revealed state.
   */
  public void setRevealed(boolean state) {
    revealed = state;
    String panelName;
    if (state == true) panelName = PANEL_LIST;
    else panelName = PANEL_HIDDEN;

    ((CardLayout)actionsPanel.getLayout()).show(actionsPanel, panelName);
    ((CardLayout)centerPanel.getLayout()).show(centerPanel, panelName);
    currentPanel = panelName;

    revealedMenuItem.setSelected(state);
    sortMenuItem.setEnabled(state);
  }

  /**
   * Gets the revealed state.
   */
  public boolean getRevealed() {return revealed;}


  /**
   * Sets the the draw deck.
   * Relevant buttons will be enabled/disabled.
   *
   * @param deck the deck, or null to remove
   */
  public void setDrawDeck(Deck deck) {
    drawDeck = deck;
    boolean isNotNull = deck != null;
    drawBtn.setEnabled(isNotNull);
    drawXBtn.setEnabled(isNotNull);
    if (isNotNull) {
      drawDeck.addDependent(pronoun);
      drawDeckReticleBox.setLit(true);
    } else {
      drawDeckReticleBox.setLit(false);
    }
  }

  /**
   * Gets the the draw deck.
   */
  public Deck getDrawDeck() {return drawDeck;}


  /**
   * Sets the the discard deck.
   * Relevant buttons will be enabled/disabled.
   *
   * @param deck the deck, or null to remove
   */
  public void setDiscardDeck(Deck deck) {
    discardDeck = deck;
    boolean isNotNull = deck != null;
    discardAllMenuItem.setEnabled(isNotNull);
    discardBtn.setEnabled(isNotNull);
    if (isNotNull) {
      discardDeck.addDependent(pronoun);
      discardDeckReticleBox.setLit(true);
    } else {
      discardDeckReticleBox.setLit(false);
    }
  }

  /**
   * Gets the the discard deck.
   */
  public Deck getDiscardDeck() {return discardDeck;}


  /**
   * Selects a card.
   */
  public void selectCard(int cardNum) {
    if (cardNum < 0 || cardNum >= handListModel.size()) return;

    handList.setSelectedIndex(cardNum);
    handList.ensureIndexIsVisible(cardNum);
  }


  /**
   * Derives an index to insert into, from a point.
   * If the card list is not visible, -1 is returned.
   *
   * @param p drop point in this hand's coords
   * @return an index for addCard(Card, int), or -1 for addCard(Card)
   */
  public int getAddCardIndex(Point p) {
    if (currentPanel.equals(PANEL_LIST) == false) return -1;

    Point listSpace = SwingUtilities.convertPoint(pronoun, p, handList);
    int index = handList.locationToIndex(listSpace);

    int lastIndex = handListModel.size()-1;
    if (index == lastIndex && handListModel.size() > 0) {
      Rectangle lastCellBounds = handList.getCellBounds(lastIndex, lastIndex);
      if (lastCellBounds.contains(listSpace) == false) index = -1;
      //Normally, dragging under the last row, would insert above the last row
    }

    return index;
  }

  /**
   * Inserts a card into the hand.
   * The card's attributes will be reset.
   *
   * @param newCard the card to add
   * @param index where to put it
   * @see Card#reset()
   */
  public void addCard(Card newCard, int index) {
    if (index > cardList.size()) index = cardList.size();
    cardList.add(index, newCard);
    handListModel.addElement(index, newCard.getFrontName());
    newCard.reset();
  }

  /**
   * Adds a card to the hand.
   * The card's attributes will be reset.
   *
   * @param newCard the card to add
   * @see Card#reset()
   */
  public void addCard(Card newCard) {
    cardList.add(newCard);
    handListModel.addElement(newCard.getFrontName());
    newCard.reset();
  }

  /**
   * Adds cards to the hand.
   * The cards' attributes will be reset.
   *
   * @param newCards the card objects to add
   * @see Card#reset()
   */
  public void addCards(List<Card> newCards) {
    String[] newNames = new String[newCards.size()];
    for (int i=0; i < newCards.size(); i++) {
      Card tmpCard = newCards.get(i);
      newNames[i] = tmpCard.getFrontName();
      tmpCard.reset();
    }
    cardList.addAll(newCards);
    handListModel.addAll(newNames);
  }


  /**
   * Returns the row occupied by a card.
   *
   * @return a row, or -1 if not present
   */
  private int getRowById(int cardId) {
    for (int i=cardList.size()-1; i >= 0; i--) {
      if (cardList.get(i).getId() == cardId) {
        return i;
      }
    }
    return -1;
  }


  /**
   * Removes and returns a card.
   * This is a frontend for takeCard(), using card ids.
   *
   * @return the card, or null
   */
  public Card takeCardById(int id) {
    int index = getRowById(id);
    if (index == -1) return null;

    Card result = takeCard(index);
    return result;
  }

  /**
   * Removes and returns a card.
   * If the card was selected, its row will remain selected.
   * If the row no longer exists, the next row up will be selected.
   *
   * @return the card, or null
   */
  public Card takeCard(int index) {
    if (index < 0 || index >= cardList.size()) return null;
    int selectedRow = handList.getSelectedIndex();

    Card result = cardList.remove(index);
    handListModel.removeElement(index);

    if (selectedRow != -1) {
      if (selectedRow < handListModel.size()) handList.setSelectedIndex(selectedRow);
      else if (index-1 >= 0) handList.setSelectedIndex(selectedRow-1);
    }

    return result;
  }


  /**
   * Returns the card at an index.
   * It is left in place.
   *
   * @return the card, or null
   */
  public Card getCard(int index) {
    if (index < 0 || index >= cardList.size()) return null;
    return cardList.get(index);
  }


  /**
   * Returns the card count.
   * The top card is 0.
   */
  public int getCardCount() {
    return handListModel.size();
  }


  /**
   * Takes the top n cards from a deck.
   */
  public void draw(Deck srcDeck, int amount) {
    if (srcDeck != null && srcDeck.getCardCount() >= amount) {
      List<Card> newCards = srcDeck.removeCards(srcDeck.getCardCount()-amount, srcDeck.getCardCount());
      Collections.reverse(newCards);
      addCards(newCards);
      srcDeck.refresh();
    }
  }


  /**
   * Generates a sorted order.
   *
   * @return a list of Integers of the original indeces in the new order
   */
  public List<Integer> genSortedOrder() {
    List<Integer> resultList = new ArrayList<Integer>(cardList.size());
    if (cardList.size() == 0) return resultList;

    List<Card> tmpList = new ArrayList<Card>(cardList);

    Collections.sort(tmpList, new Comparator<Card>() {
      @Override
      public int compare(Card a, Card b) {
        return a.getFrontName().compareTo(b.getFrontName());
      }

      @Override
      public boolean equals(Object obj) {
        return this.equals(obj);
      }
    });

    for (int i=0; i < tmpList.size(); i++) {
      resultList.add(new Integer( cardList.indexOf(tmpList.get(i)) ));
    }
    return resultList;
  }

  /**
   * Reorders the hand.
   *
   * @param indeces a list of Integers of the original indeces in the new order
   * @see Hand#genSortedOrder()
   */
  public void reorder(List<Integer> indeces) {
    if (indeces.size() != cardList.size()) return;

    List<Card> newCardList = new ArrayList<Card>(indeces.size());
    List<String> newNameList = new ArrayList<String>(indeces.size());

    for (Integer index : indeces) {
      Card tmpCard = cardList.get(index.intValue());
      newCardList.add(tmpCard);
      newNameList.add(tmpCard.getFrontName());
    }

    handListModel.clear();
    cardList.clear();
    cardList.addAll(newCardList);
    handListModel.addAll(newNameList.toArray());
  }


  /**
   * Saves the hand.
   * Generates tab-separated lines for each card in the hand.
   *
   * @return a String for ascii serialization.
   */
  public String save() {
    StringBuffer dupHand = new StringBuffer();
    String tmp;

    int max = cardList.size();
    for (int i=0; i < max; i++) {
      Card tmpCard = cardList.get(i);
      tmp = tmpCard.getFrontName();
      if (tmp != null) {
        //Find adjacent duplicates
        int j;
        for (j=1; i+j < max; j++) {
          Card tmpOtherCard = cardList.get(i+j);
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

        dupHand.append((1+j)); dupHand.append("\t"); dupHand.append(tmp);

        dupHand.append("\t");
        tmp = Prefs.textDat.getSetNameFromAbbrev(tmpCard.getSetAbbrev());
        dupHand.append(tmp);

        dupHand.append("\t");
        dupHand.append(tmpCard.getFrontFile());

        dupHand.append("\t");
        dupHand.append(tmpCard.getBackName());

        dupHand.append("\t");
        dupHand.append(tmpCard.getBackFile());
        dupHand.append("\n");

        i += j;                                              //Skip the duplicates
      }
    }
    return dupHand.toString();
  }


  /**
   * Clears this object's hotkeys and reapplies current global ones.
   * Cards inside will be updated as well.
   */
  public void updateHotkeys() {
    for (int i=cardList.size()-1; i >= 0; i--) {
      cardList.get(i).updateHotkeys();
    }
  }


  /**
   * A callback to modify a DragGhost mid-drag.
   *
   * @param g the DragGhost
   * @param e an event in the ghost's sourceComponent coords
   */
  @Override
  public void ghostDragged(DragGhost g, MouseEvent e) {
    JComponent sourceComponent = g.getSourceObject();

    if (sourceComponent == glassPanel) {
      // See the cardDrag listener above
      if (sourceComponent.getBounds().contains(e.getPoint()) == false) {
        g.setSize(Prefs.defaultCardWidth, Prefs.defaultCardHeight);
      } else {
        int index = handList.getSelectedIndex();
        if (handListModel.size() > 0 && index != -1) {
          Rectangle cellBounds = handList.getCellBounds(index, index);
          g.setSize(cellBounds.width, cellBounds.height);
        }
      }
    }
  }



  private static class HandTitlePanel extends JPanel {
    private ArcanistCCGFrame frame = null;
    private Hand parentHand = null;

    public HandTitlePanel(ArcanistCCGFrame f, Hand parent) {super(); frame = f; parentHand = parent;}

    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Dimension size = getSize();

      if ( frame.isDragging() == false && (parentHand != null && parentHand.isInUse() == true) ) {
        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(Color.orange);
        g2d.fillRect(size.width/2-3, 1, 2, 6);
        g2d.fillRect(size.width/2-3, 1, 6, 2);
        g2d.fillRect(size.width/2+1, 1, 2, 5);
        g2d.fillRect(size.width/2-5, 6, 10, 9);
      }
    }
  }


  private static class ReticlePanel extends JPanel {
    private final Color LIT = new Color(20, 150, 20);
    private final Color DARK = Color.BLACK;
    private JLabel reticleLbl = null;

    public ReticlePanel() {
      super(new BorderLayout());
      super.setBorder(BorderFactory.createEtchedBorder());
      reticleLbl = new JLabel();
        reticleLbl.setFont(new Font("monospaced", Font.BOLD, 12));
        reticleLbl.setText("(+)");
        reticleLbl.setForeground(DARK);
        reticleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        reticleLbl.setVerticalAlignment(SwingConstants.CENTER);
        this.add(reticleLbl);
        this.setPreferredSize(new Dimension(this.getPreferredSize().width, this.getPreferredSize().width));
    }

    public void setLit(boolean b) {
      if (b) reticleLbl.setForeground(LIT);
      else reticleLbl.setForeground(DARK);
    }
  }
}
