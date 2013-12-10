package org.arcanist.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A popup window that visually spoils the contents of a deck.
 * Thumbnails are updated as the deck is changed. Clicking a
 * thumbnail triggers a draw. The window nerfs until the drawn
 * card is removed.
 */
public class DeckListWindow extends NerfableInternalFrame implements Dependent, ListDataListener {
  private static final int ALL = 0;
  private static final int TOP = 1;
  private static final int BOTTOM = 2;

  private ArcanistCCGFrame frame = null;

  private DeckListWindow pronoun = this;
  private Deck deck = null;

  private int section = ALL;
  private int depth = 0;  // Offset from the deck's bottom
  private int lastDeckSize = 0;
  private int awaitedDraw = -1;

  private Map<String,ImageIcon> thumbCacheMap = new HashMap<String,ImageIcon>();
  private LongListModel thumbListModel = null;
  private JList thumbList = null;

  private JPanel pane = new JPanel(new BorderLayout());


  public DeckListWindow(ArcanistCCGFrame f, Deck inDeck) {
    super("DeckList",
      true, //resizable
      true, //closable
      true, //maximizable
      false); //iconifiable

    deck = inDeck;
    frame = f;

    deck.addDependent((Dependent)pronoun);
    deck.addListDataListener((ListDataListener)pronoun);
    ArcanistCCG.NetManager.deckUse(deck.arrayID());
    frame.getTablePane().repaint();

    this.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosing(InternalFrameEvent e) {
        if (deck.existsOnTable()) {
          deck.removeListDataListener((ListDataListener)pronoun);
          deck.removeDependent((Dependent)pronoun);
          ArcanistCCG.NetManager.deckUnuse(deck.arrayID());
          frame.getTablePane().repaint();
        }
      }

      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        //Break references so they can be garbage collected
        //If a model lingers, it keeps its GUI on life-support
        if (thumbList != null) thumbList.setModel(new DefaultListModel());
      }
    });

    int width = 440, height = 235;

    JPanel depthHolderPanel = new JPanel(new GridBagLayout());
      GridBagConstraints gridC = new GridBagConstraints();
        gridC.fill = GridBagConstraints.NONE;
        gridC.weightx = 0;
        gridC.weighty = 0;
        gridC.gridwidth = GridBagConstraints.REMAINDER;  //End Row

      JPanel depthPanel = new JPanel(new GridLayout(4, 3));
        ButtonGroup btnGrp = new ButtonGroup();
        final JRadioButton topBtn = new JRadioButton("Top", true);
          btnGrp.add(topBtn);
          depthPanel.add(topBtn);
        final JRadioButton bottomBtn = new JRadioButton("Bottom", false);
          btnGrp.add(bottomBtn);
          depthPanel.add(bottomBtn);
        final JTextField depthField = new JTextField(new RegexDocument("[0-9]*"), "5", 3);
          depthField.setHorizontalAlignment(JTextField.CENTER);
          depthField.setMaximumSize(depthField.getPreferredSize());
          depthPanel.add(depthField);
        //
          depthPanel.add(new JLabel());
        final JRadioButton allBtn = new JRadioButton("All", false);
          btnGrp.add(allBtn);
          depthPanel.add(allBtn);
          depthPanel.add(new JLabel());
        //
          depthPanel.add(new JLabel());
          depthPanel.add(new JLabel());
          depthPanel.add(new JLabel());
        //
          depthPanel.add(new JLabel());
          depthPanel.add(new JLabel());
        JButton okBtn = new JButton("List");
          depthPanel.add(okBtn);
        depthPanel.setMaximumSize(depthPanel.getPreferredSize());

        depthHolderPanel.add(depthPanel, gridC);
        pane.add(depthHolderPanel);

    okBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

        if (allBtn.isSelected()) {
          section = ALL;
          depth = 0;
          ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" listed a deck--");
        } else {
          int n = 0;
          try {n = Integer.parseInt(depthField.getText());}
          catch (NumberFormatException f) {return;}
          if (n > deck.getCardCount()) n = deck.getCardCount();
          depth = n;

          if (topBtn.isSelected()) {
            section = TOP;
            ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" listed top "+ depth +" cards of a deck--");
          }
          else if (bottomBtn.isSelected()) {
            section = BOTTOM;
            ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" listed bottom "+ depth +" cards of a deck--");
          }
        }
        pane.removeAll();
        pane.add(getThumbPanel());
        pane.revalidate();
        pane.repaint();
      }
    });

    this.setContentPane(pane);
    frame.getDesktop().add(this);

    if (frame.getTableFrame().getSize().width/2-width/2 > 0 && frame.getTableFrame().getSize().height/2-height/2 > 0)
      this.reshape(frame.getTableFrame().getSize().width/2-width/2, frame.getTableFrame().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();
  }


  /**
   * Notifies that the deck was removed.
   * Closes the window.
   */
  public void sourceRemoved(Object source) {
    deck.removeListDataListener((ListDataListener)pronoun);
    pronoun.setVisible(false);
    pronoun.dispose();
    deck = null;
  }


  /**
   * Scale card images fill a wrapping list,
   * and return a scrollpane holding it.
   *
   * @return the scrollable deck list
   */
  private JScrollPane getThumbPanel() {
    MouseMotionListener jumboListener = new MouseMotionAdapter() {
      public void mouseMoved(MouseEvent e) {
        JumboView jumboFrame = frame.getJumboFrame();
        int nearestRow = -1;
        if (jumboFrame != null && jumboFrame.isJumboActive()) {
          if (!frame.isDragging()) {
            nearestRow = thumbList.locationToIndex(e.getPoint());
            if (nearestRow == -1) return;
            Rectangle cellBounds = thumbList.getCellBounds(nearestRow, nearestRow);
            if (cellBounds.contains(e.getPoint()) == false) return;

            int index = -1;
            if (section == ALL) index = deck.getCardCount()-1 - nearestRow;
            else if (section == TOP) index = deck.getCardCount()-1 - nearestRow;
            else if (section == BOTTOM) index = (Math.min(depth, thumbListModel.size())-1) - nearestRow;

            if (index < 0 || index >= deck.getCardCount()) {
              pronoun.doDefaultCloseAction();
              pronoun.setVisible(false);
              pronoun.dispose();
              return;
            }

            Card tmpCard = deck.getCard(index);
            if (jumboFrame.isShownInJumbo(tmpCard.getFrontPath(), tmpCard.getFrontName(), tmpCard.getSetAbbrev()) == false) {
              boolean preferText = jumboFrame.getPreferText();
              if (e.isShiftDown()) preferText = !preferText;

              if (tmpCard.isText() == true || preferText) {
                jumboFrame.updateJumboText(new CardTextPanel(tmpCard.getFrontName(), tmpCard.getSetAbbrev(), false));
              } else {
                jumboFrame.updateJumboImage(tmpCard.getFrontPath());
              }
            }
          }
        }
      }
    };

    MouseListener drawListener = new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent e) {
        if (awaitedDraw != -1) return;  // Req'd a draw, awaiting removal

        int nearestRow = -1;
        nearestRow = thumbList.locationToIndex(e.getPoint());
        if (nearestRow == -1) return;
        Rectangle cellBounds = thumbList.getCellBounds(nearestRow, nearestRow);
        if (cellBounds.contains(e.getPoint()) == false) return;

        int index = -1;
        if (section == ALL) index = deck.getCardCount()-1 - nearestRow;
        else if (section == TOP) index = deck.getCardCount()-1 - nearestRow;
        else if (section == BOTTOM) index = (Math.min(depth, thumbListModel.size())-1) - nearestRow;

        awaitedDraw = index;
        setNerfed(true);
        //thumbListModel.removeElement(nearestRow);
        ArcanistCCG.NetManager.deckTableCard(deck.arrayID(), index, true);
/*
        if (thumbListModel.size() == 0) {
          pronoun.doDefaultCloseAction();
          pronoun.setVisible(false);
          pronoun.dispose();
        }
*/
      }
    };

    BufferedImage blankImage = Prefs.Cache.getImg(Prefs.defaultBlankPath);
    blankImage = Prefs.Cache.getScaledInstance(blankImage, Prefs.defaultCardWidth/2, Prefs.defaultCardHeight/2, false);
    ImageIcon blankIcon = new ImageIcon(blankImage);
    thumbCacheMap.put(Prefs.defaultBlankPath, blankIcon);

    BufferedImage errorImage = Prefs.Cache.getImg(Prefs.defaultErrorPath);
    errorImage = Prefs.Cache.getScaledInstance(errorImage, Prefs.defaultCardWidth/2, Prefs.defaultCardHeight/2, false);
    ImageIcon errorIcon = new ImageIcon(errorImage);
    thumbCacheMap.put(Prefs.defaultErrorPath, errorIcon);

    int max = 0;  // Topmost index (inclusive)
    int min = 0;  // Bottommost index (inclusive)
    if (section == ALL) {
      max = deck.getCardCount()-1;
      min = 0;
    } else if (section == TOP) {
      max = deck.getCardCount()-1;
      min = max - (depth-1);
    } else if (section == BOTTOM) {
      max = depth-1;
      min = 0;
    }
    lastDeckSize = deck.getCardCount();

    String[] thumbPaths = getImagePaths(min, max);

    ThumbRenderer thumbRenderer = new ThumbRenderer(thumbCacheMap);
    thumbListModel = new LongListModel();
      thumbListModel.addAll(thumbPaths);
    thumbList = new JList(thumbListModel);
      thumbList.setCellRenderer(thumbRenderer);
      thumbList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
      thumbList.setVisibleRowCount(-1);
      thumbList.setBackground(Prefs.tableBgColor);
      thumbList.addMouseListener(drawListener);
      thumbList.addMouseMotionListener(jumboListener);

    JScrollPane thumbsScrollPane = new JScrollPane(thumbList);
      thumbsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      //The default scroll speed is too slow
      thumbsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
      thumbList.addNotify();

    return thumbsScrollPane;
  }

  /**
   * Caches scaled thumbnails of cards in the deck and returns paths in reverse order.
   * Better precache blank and error thumbs first.
   *
   * @param min bottommost deck index (inclusive)
   * @param max topmost deck index (inclusive)
   */
  private String[] getImagePaths(int min, int max) {
    String[] result = new String[max+1-min];
    int ti = 0;
    for (int i=max; i >= min; i--) {
      Card tmpCard = deck.getCard(i);
      String frontPath = tmpCard.getFrontPath();
      String tmpPath = frontPath;
      ImageIcon tmpIcon = null;
      if (thumbCacheMap.containsKey(tmpPath)) {
        // tmpPath is fine, and the thumb is cached already
      }
      else if (Prefs.useTextOnly == false && new File(frontPath).exists() == true) {
        BufferedImage tmpImage = (BufferedImage)Prefs.Cache.getImg(frontPath);
          tmpImage = Prefs.Cache.getScaledInstance(tmpImage, Prefs.defaultCardWidth/2, Prefs.defaultCardHeight/2, true);
        tmpIcon = new ImageIcon(tmpImage);
        thumbCacheMap.put(tmpPath, tmpIcon);
      }
      else {
        tmpPath = Prefs.defaultBlankPath;
      }
      result[ti++] = tmpPath;
    }

    return result;
  }


  public void contentsChanged(ListDataEvent e) {
    if (thumbListModel == null) return;
    lastDeckSize = deck.getCardCount();  // Indeces refer to current size
    int deckIndex0 = e.getIndex0();
    int deckIndex1 = e.getIndex1();
    int listIndex0 = -1;
    int listIndex1 = -1;
    if (section == ALL) {
      // Reverse the indeces: 0th=Top, SizeMinusOneth=Bottom
      listIndex0 = lastDeckSize-1 - deckIndex1;
      listIndex1 = lastDeckSize-1 - deckIndex0;
      String[] addedThumbs = getImagePaths(deckIndex0, deckIndex1);
      thumbListModel.removeRange(listIndex0, listIndex1+1);
      thumbListModel.addAll(listIndex0, addedThumbs);
    }
    else if (section == TOP) {
      // Reverse the indeces: 0th=Top, SizeMinusOneth=Bottom
      listIndex0 = lastDeckSize-1 - deckIndex1;
      listIndex1 = lastDeckSize-1 - deckIndex0;
      // Out of view
      if (listIndex0 >= Math.min(depth, thumbListModel.size())) return;
      // Shrink to visible (depth or model.size())
      if (listIndex1 >= Math.min(depth, thumbListModel.size())) {
        deckIndex0 = lastDeckSize - (thumbListModel.size());
        listIndex1 = thumbListModel.size()-1;
      }
      String[] addedThumbs = getImagePaths(deckIndex0, deckIndex1);
      thumbListModel.removeRange(listIndex0, listIndex1+1);
      thumbListModel.addAll(listIndex0, addedThumbs);
    }
    else if (section == BOTTOM) {
      listIndex0 = thumbListModel.size()-1 - deckIndex1;
      listIndex1 = thumbListModel.size()-1 - deckIndex0;
      if (listIndex1 < 0) return;
      if (listIndex0 < 0) {
        deckIndex1 = deckIndex1 - (-listIndex0);
        listIndex0 = 0;
      }
      String[] addedThumbs = getImagePaths(deckIndex0, deckIndex1);
      thumbListModel.removeRange(listIndex0, listIndex1+1);
      thumbListModel.addAll(listIndex0, addedThumbs);
    }
  }

  public void intervalAdded(ListDataEvent e) {
    if (thumbListModel == null) return;
    lastDeckSize = deck.getCardCount();  // Indeces refer to current size
    int deckIndex0 = e.getIndex0();
    int deckIndex1 = e.getIndex1();
    int listIndex0 = -1;
    int listIndex1 = -1;

    if (section == ALL) {
      // Reverse the indeces: 0th=Top, SizeMinusOneth=Bottom
      listIndex0 = lastDeckSize-1 - deckIndex1;
      listIndex1 = lastDeckSize-1 - deckIndex0;
      String[] addedThumbs = getImagePaths(deckIndex0, deckIndex1);
      thumbListModel.addAll(listIndex0, addedThumbs);
    }
    else if (section == TOP) {
      // Reverse the indeces: 0th=Top, SizeMinusOneth=Bottom
      listIndex0 = lastDeckSize-1 - deckIndex1;
      listIndex1 = lastDeckSize-1 - deckIndex0;
      // Out of view
      if (listIndex0 >= depth) return;
      else if (listIndex0 > thumbListModel.size()) {
        listIndex0 = thumbListModel.size();
      }
      // Shrink to visible (depth or model.size())
      if (listIndex1 >= Math.min(depth, thumbListModel.size())) {
        deckIndex0 = lastDeckSize - (thumbListModel.size());
        listIndex1 = thumbListModel.size()-1;
      }
      String[] addedThumbs = getImagePaths(deckIndex0, deckIndex1);
      thumbListModel.addAll(listIndex0, addedThumbs);
      if (thumbListModel.size() > depth) thumbListModel.removeRange(depth, (thumbListModel.size()));
    }
    else if (section == BOTTOM) {
      listIndex0 = thumbListModel.size() - deckIndex1;
      listIndex1 = thumbListModel.size() - deckIndex0;
      if (listIndex1 < 0) return;
      if (listIndex0 < 0) {
        deckIndex1 = deckIndex1 - (-listIndex0);
        listIndex0 = 0;
      }
      String[] addedThumbs = getImagePaths(deckIndex0, deckIndex1);
      thumbListModel.addAll(listIndex0, addedThumbs);
      if (thumbListModel.size() > depth) thumbListModel.removeRange(0, (thumbListModel.size()-depth));
    }
  }

  public void intervalRemoved(ListDataEvent e) {
    if (thumbListModel == null) return;
    int prevDeckSize = lastDeckSize;  // Indeces refer to prev size
    lastDeckSize = deck.getCardCount();
    int deckIndex0 = e.getIndex0();
    int deckIndex1 = e.getIndex1();
    int listIndex0 = -1;
    int listIndex1 = -1;

    if (section == ALL) {
      // Reverse the indeces: 0th=Top, SizeMinusOneth=Bottom
      listIndex0 = prevDeckSize-1 - deckIndex1;
      listIndex1 = prevDeckSize-1 - deckIndex0;
    }
    else if (section == TOP) {
      // Reverse the indeces: 0th=Top, SizeMinusOneth=Bottom
      listIndex0 = prevDeckSize-1 - deckIndex1;
      listIndex1 = prevDeckSize-1 - deckIndex0;
      // Out of view
      if (listIndex0 >= Math.min(depth, thumbListModel.size())) return;
      // Shrink to visible (depth or model.size())
      if (listIndex1 >= Math.min(depth, thumbListModel.size())) listIndex1 = thumbListModel.size()-1;
    }
    else if (section == BOTTOM) {
      listIndex0 = thumbListModel.size()-1 - deckIndex1;
      listIndex1 = thumbListModel.size()-1 - deckIndex0;
      if (listIndex1 < 0) return;
      if (listIndex0 < 0) listIndex1 = 0;
    }
    thumbListModel.removeRange(listIndex0, listIndex1+1);

    if (awaitedDraw == deckIndex0) {
      awaitedDraw = -1;
      setNerfed(false);
    }

    if (thumbListModel.size() == 0) {
      pronoun.doDefaultCloseAction();
      pronoun.setVisible(false);
      pronoun.dispose();
    }
  }



  private static class ThumbRenderer extends DefaultListCellRenderer {
    private Map<String,ImageIcon> cacheMap = null;

    public ThumbRenderer(Map<String,ImageIcon> cacheMap) {
      this.cacheMap = cacheMap;
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
      Component result = null;
      if (cacheMap.containsKey(value)) result = super.getListCellRendererComponent(list, cacheMap.get(value), index, isSelected, hasFocus);
      else result = super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
      return result;
    }
  }
}
