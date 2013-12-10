package org.arcanist.client;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import org.arcanist.client.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


public class ArcanistCCGFrame extends JFrame implements ActionListener, Nerfable {

  private ArcanistCCGFrame pronoun = this;

  /** The main game window. */
  private JDesktopPane desktop = new JDesktopPane();

  /**
   * Available to Cards and Decks for a dragging glitch workaround.
   * Table components must activate the table frame on mouseOver.
   * It has something to do with InternalFrames and confused mouse coordinate systems.
   */
  private JInternalFrame tableFrame = new JInternalFrame("Table",
    true,  //resizable
    false, //closable
    true,  //maximizable
    false); //iconifiable

  /** The table in which everything is draggable. */
  private TableComponent tablePane = new TableComponent();

  /** The JScrollPane that contains tablePane. */
  private JScrollPane myScrollPane;

  /** Allows other objects to reach the JumboView. */
  private JumboView jumboFrame = null;

  /** Allows other objects to reach the ChatPanel. */
  private ChatPanel ChatPanel = new ChatPanel();

  /** Allows other objects to reach the ChatFrame. */
  private ChatFrame chatFrame = null;


  /** Suppresses JumboView update when dragging.
   *  A Card changes this value while glued to mouse,
   *  so other Cards know not to react to mouseEntered events.
   */
  private volatile boolean dragging = false;

  /** Stand-in graphic while moving */
  private volatile DragGhost dragGhost = null;


  /** Master list of cards on table. */
  private List<Card> cardArray = new ArrayList<Card>(0);
  /** Master list of decks on table. */
  private List<Deck> deckArray = new ArrayList<Deck>(0);
  /** Master list of notes on table. */
  private List<FloatingNote> noteArray = new ArrayList<FloatingNote>(0);
  /** Master list of tokens on table. */
  private List<Token> tokenArray = new ArrayList<Token>(0);
  /** Master list of hands on table. */
  private List<Hand> handArray = new ArrayList<Hand>(0);

  /** Master list of card groups on table. */
  private List<MassDragger> groupArray = new ArrayList<MassDragger>(0);

  /** List of objects to notify on scroll events */
  private List<HoverListener> hoverListeners = new ArrayList<HoverListener>();


  private JMenuItem fileOpenDeckMenuItem = new JMenuItem("Open Deck...");
  private JMenuItem fileDeckBuilderMenuItem = new JMenuItem("Build Deck...");
  private JMenuItem fileClearMenuItem = new JMenuItem("Clear Table");
  private JMenuItem fileConnectMenuItem = new JMenuItem("NetPlay...");
  private JMenuItem fileLobbyMenuItem = new JMenuItem("Lobby...");
  private JMenuItem fileDisconnectMenuItem = new JMenuItem("Disconnect");
  private JMenuItem fileSendStateMenuItem = new JMenuItem("Send State");
  private JMenuItem fileLoadStateMenuItem = new JMenuItem("Load State...");
  private JMenuItem fileSaveStateMenuItem = new JMenuItem("Save State...");
  private JMenuItem fileExitMenuItem = new JMenuItem("Exit");
  private JMenuItem settingsAppearanceMenuItem = new JMenuItem("Appearance");
  private JMenuItem settingsKeyboardMenuItem = new JMenuItem("Keyboard");
  private JMenuItem settingsPathsMenuItem = new JMenuItem("Paths");
  private JMenuItem settingsTextOnImagesMenuItem = new JCheckBoxMenuItem("TextOnCards");
  private JMenuItem settingsTableSizeMenuItem = new JMenuItem("Table Size...");
  private JMenuItem addCounterMenuItem = new JMenuItem("Counter");
  private JMenuItem addRollerMenuItem = new JMenuItem("Die Roller");
  private JMenuItem addTimerMenuItem = new JMenuItem("Timer");
  private JMenuItem addTokenMenuItem = new JMenuItem("Tokens...");
  private JMenuItem addNoteMenuItem = new JMenuItem("Floating Note");
  private JMenuItem addDiscardMenuItem = new JMenuItem("Discard Pile");
  private JMenuItem addHandMenuItem = new JMenuItem("Hand");
  private JMenuItem addJumboMenuItem = new JMenuItem("Jumbo Card View");
  private JMenuItem helpHelpMenuItem = new JMenuItem("Help");
  private JMenuItem helpAboutMenuItem = new JMenuItem("About");


  private MouseListener focusOnEnterListener = null;
  AdjustmentListener hoverwatchListener = null;


  public ArcanistCCGFrame(String title) {
    super(title);

    DatParser newDat = new DatParser();
      newDat.setup(this, Prefs.gameloc, Prefs.gamedat, Prefs.gameset);
    Prefs.textDat = newDat;

    //Declare menus
    JMenuBar menuBar = new JMenuBar();
      JMenu fileMenu = new JMenu("File");
        // fileOpenDeckMenuItem
        // fileDeckBuilderMenuItem
        // fileClearMenuItem
        // fileConnectMenuItem
        // fileLobbyMenuItem
        // fileDisconnectMenuItem
        // fileSendStateMenuItem
        // fileLoadStateMenuItem
        // fileSaveStateMenuItem
        // fileExitMenuItem
      JMenu settingsMenu = new JMenu("Settings");
        // settingsAppearanceMenuItem
        // settingsKeyboardMenuItem
        // settingsPathsMenuItem
        // settingsTextOnImagesMenuItem
        // settingsTableSizeMenuItem
      JMenu addMenu = new JMenu("Add");
        // addCounterMenuItem
        // addRollerMenuItem
        // addTimerMenuItem
        // addTokenMenuItem
        // addNoteMenuItem
        // addDiscardMenuItem
        // addHandMenuItem
        // addJumboMenuItem
      JMenu helpMenu = new JMenu("Help");
        // helpHelpMenuItem
        // helpAboutMenuItem

      //Build menus
      menuBar.add(fileMenu);
        fileMenu.add(fileOpenDeckMenuItem);
        fileMenu.add(fileDeckBuilderMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(fileClearMenuItem);
        fileMenu.add(fileConnectMenuItem);
        fileMenu.add(fileLobbyMenuItem);
        fileMenu.add(fileDisconnectMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(fileSendStateMenuItem);
        fileMenu.add(fileLoadStateMenuItem);
        fileMenu.add(fileSaveStateMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(fileExitMenuItem);
      menuBar.add(settingsMenu);
        settingsMenu.add(settingsAppearanceMenuItem);
        settingsMenu.add(settingsKeyboardMenuItem);
        settingsMenu.add(settingsPathsMenuItem);
        settingsMenu.addSeparator();
        settingsMenu.add(settingsTextOnImagesMenuItem);
        settingsMenu.add(settingsTableSizeMenuItem);
      menuBar.add(addMenu);
        addMenu.add(addCounterMenuItem);
        addMenu.add(addRollerMenuItem);
        addMenu.add(addTimerMenuItem);
        addMenu.add(addTokenMenuItem);
        addMenu.add(new JSeparator());
        addMenu.add(addNoteMenuItem);
        addMenu.add(addDiscardMenuItem);
        addMenu.add(addHandMenuItem);
        addMenu.add(new JSeparator());
        addMenu.add(addJumboMenuItem);
      menuBar.add(helpMenu);
        helpMenu.add(helpHelpMenuItem);
        helpMenu.add(helpAboutMenuItem);
    this.setJMenuBar(menuBar);


    //Declare the table
    tablePane.setPreferredSize(new Dimension(3000,3000));

    //Put the table inside a scrollable pane
    myScrollPane = new JScrollPane(tablePane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      //The default scroll speed is too slow
      myScrollPane.getHorizontalScrollBar().setUnitIncrement(25);
      myScrollPane.getVerticalScrollBar().setUnitIncrement(25);

    desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);     //slow painting otherwise

    tableFrame.setFrameIcon(null);
    tableFrame.getContentPane().add(myScrollPane);
      desktop.add(tableFrame);
      tableFrame.show();

    this.setContentPane(desktop);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    if (Prefs.appIconPositive != null) this.setIconImage(new ImageIcon(Prefs.appIconPositive).getImage());

    dragGhost = new DragGhost(this);
    setConnectedState(false);


    // Set up listeners
    Action keyFocusChatAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (ChatPanel != null) {
          if (jumboFrame != null && jumboFrame.hasDockableChild(ChatPanel)) {
            jumboFrame.switchToChatTab();
          }
          else if (ChatPanel.getParent() != null) {
            ChatPanel.getInputField().requestFocusInWindow();
          }
        }
      }
    };
    tableFrame.getActionMap().put(Prefs.ACTION_TABLE_FOCUS_CHAT, keyFocusChatAction);


    //Begin MultiSelect
    tablePane.addMouseListener(new MouseAdapter() {
      MouseMotionListener multiselect;

      @Override
      public void mousePressed(MouseEvent e) {
        if (e.isShiftDown() == true) {
          tablePane.setSelectionCorner(e.getX(), e.getY());
          dragging = true;
          multiselect = new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
              tablePane.setSelectionCorner(e.getX(), e.getY());
            }
          };
          tablePane.addMouseMotionListener(multiselect);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        tablePane.removeMouseMotionListener(multiselect);
        Rectangle selection = tablePane.getSelection();
        if (selection != null && !selection.isEmpty()) {
          MassDragger newMD = new MassDragger(tablePane.getSelection(), pronoun);
          newMD.addToTable();
        }
        tablePane.clearSelection();
        dragging = false;
      }
    }); //End MultiSelect

    tableFrame.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameActivated(InternalFrameEvent e) {
        tableFrame.moveToBack();
      }
    });

    fileOpenDeckMenuItem.addActionListener(this);
    fileDeckBuilderMenuItem.addActionListener(this);
    fileClearMenuItem.addActionListener(this);
    fileConnectMenuItem.addActionListener(this);
    fileLobbyMenuItem.addActionListener(this);
    fileDisconnectMenuItem.addActionListener(this);
    fileSendStateMenuItem.addActionListener(this);
    fileLoadStateMenuItem.addActionListener(this);
    fileSaveStateMenuItem.addActionListener(this);
    fileExitMenuItem.addActionListener(this);
    settingsAppearanceMenuItem.addActionListener(this);
    settingsKeyboardMenuItem.addActionListener(this);
    settingsPathsMenuItem.addActionListener(this);
    settingsTextOnImagesMenuItem.addActionListener(this);
    settingsTableSizeMenuItem.addActionListener(this);
    addCounterMenuItem.addActionListener(this);
    addRollerMenuItem.addActionListener(this);
    addTimerMenuItem.addActionListener(this);
    addTokenMenuItem.addActionListener(this);
    addNoteMenuItem.addActionListener(this);
    addDiscardMenuItem.addActionListener(this);
    addHandMenuItem.addActionListener(this);
    addJumboMenuItem.addActionListener(this);
    helpHelpMenuItem.addActionListener(this);
    helpAboutMenuItem.addActionListener(this);


    //Dragging freaks out if frame isn't selected
    focusOnEnterListener = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        if (getTableFrame().isSelected() == false) {
          try {
            getTableFrame().setSelected(true);
            getTableFrame().moveToBack();
          }
          catch (java.beans.PropertyVetoException exception) {
            ArcanistCCG.LogManager.write(exception, "Couldn't move frame to back for drag safety.");
          }
        }
      }
    };

    hoverwatchListener = new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent e) {
        if (hoverListeners.size() == 0) return;

        JScrollBar src = (JScrollBar)e.getSource();
        if (src.getValueIsAdjusting()) return;

        Rectangle tableView = getTableView();
        int hoverCount = hoverListeners.size();
        for (int i=0; i < hoverCount; i++) {
          HoverListener h = (HoverListener)hoverListeners.get(i);
          Point offset = h.getHoverOffset();
          Rectangle oldBounds = h.getBounds();
          Rectangle newBounds = new Rectangle(tableView.x + offset.x, tableView.y + offset.y, oldBounds.width, oldBounds.height);
          if (tableView.contains(newBounds)) {
            h.hoverTo(tableView.x + offset.x, tableView.y + offset.y);
          } else {
            h.setHoverOffset(oldBounds.x - tableView.x, oldBounds.y - tableView.y);
          }
        }
      }
    };
    myScrollPane.getHorizontalScrollBar().addAdjustmentListener(hoverwatchListener);
    myScrollPane.getVerticalScrollBar().addAdjustmentListener(hoverwatchListener);

    updateHotkeys();
    loadKeyboard();
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == fileOpenDeckMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = fileChooser(JFileChooser.OPEN_DIALOG, Prefs.homePath +"decks", filter);
      if (file != null) {
        Rectangle tableView = getTableView();

        String deckString = DeckParser.readDeckStringFromFile(pronoun, file);
        if (deckString != null) {
          ArcanistCCG.NetManager.deckAdd(tableView.x, tableView.y, deckString);
        }
        // //DeckParser newDeckParser = new DeckParser(pronoun, true, file, tableView.x, tableView.y);
        // //newDeckParser.startThread();
      }
    }
    else if (source == fileDeckBuilderMenuItem) {
      new DeckBuilder(this);
    }
    else if (source == fileClearMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(desktop, "Are you SURE you want to remove EVERYTHING?", "Remove?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;

      ArcanistCCG.NetManager.tableClear();
    }
    else if (source == fileConnectMenuItem) {
      new NetPlayFrame(this);
    }
    else if (source == fileLobbyMenuItem) {
      ServerThread st = ArcanistCCG.getServerThread();
      if (st == null) {return;}
      new LobbyFrame(this, st);
    }
    else if (source == fileDisconnectMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(desktop, "Are you SURE you want to disconnect?", "Disconnect?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;
      ArcanistCCG.NetManager.setClientThread(null);
    }
    else if (source == fileSendStateMenuItem) {
      String state = XmlUtils.saveState(pronoun);
      ArcanistCCG.NetManager.tableStateLoad(state, true);
    }
    else if (source == fileLoadStateMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("XML Files (*.xml)", new String[] {".xml"});
      File file = fileChooser(JFileChooser.OPEN_DIALOG, Prefs.homePath +"states", filter);
      if (file != null) {
        Document doc = XmlUtils.readXML(file);
        if (doc == null) {
          ArcanistCCG.NetManager.notify(ChatPanel.STYLE_ERROR, "--Error loading state. Check the log for details.--");
        } else {
          boolean neutralView = false;
          int userinput = JOptionPane.showInternalConfirmDialog(desktop, "Should locally-visible things (cards, hands, etc) in this saved state be hidden?\nOtherwise everyone will see whatever was visible at the time it was saved.", "Neutral View?", JOptionPane.YES_NO_OPTION);
            if (userinput == JOptionPane.YES_OPTION) neutralView = true;

          XMLOutputter outputter = new XMLOutputter(Format.getRawFormat());
          String stateString = outputter.outputString(doc);

          ArcanistCCG.NetManager.tableStateLoad(stateString, neutralView);
        }
      }
    }
    else if (source == fileSaveStateMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("XML Files (*.xml)", new String[] {".xml"});
      File file = fileChooser(JFileChooser.SAVE_DIALOG, Prefs.homePath +"states", filter);
      if (file != null) {
        XmlUtils.saveState(pronoun, file.getAbsolutePath());
      }
    }
    else if (source == fileExitMenuItem) {
      System.exit(0);
    }
    else if (source == settingsAppearanceMenuItem) {
      new PrefsAppearanceFrame(this);
    }
    else if (source == settingsKeyboardMenuItem) {
      new PrefsKeyboardFrame(this);
    }
    else if (source == settingsPathsMenuItem) {
      new PrefsPathsFrame(this);
    }
    else if (source == settingsTextOnImagesMenuItem) {
      Prefs.textOnImages = ((JCheckBoxMenuItem)settingsTextOnImagesMenuItem).getState();
      if (Prefs.textOnImages == true) updateOverlayText();
      tablePane.repaint();
    }
    else if (source == settingsTableSizeMenuItem) {
      Dimension prevSize = tablePane.getPreferredSize();
      int userinput; int tableWidth = prevSize.width, tableHeight = prevSize.height;

      JPanel sizePanel = new JPanel();
        JTextField widthField = new JTextField(new NumberDocument(), tableWidth+"", 3);
          //widthField.setPreferredSize(new Dimension(40,20));
          sizePanel.add(widthField);
        sizePanel.add(new JLabel("x"));
        JTextField heightField = new JTextField(new NumberDocument(), tableHeight+"", 3);
          //heightField.setPreferredSize(new Dimension(40,20));
          sizePanel.add(heightField);
      Object[] options = {"OK", "Cancel"};
      do {
        userinput = JOptionPane.showInternalOptionDialog(desktop, sizePanel, "Table Size ("+ tableWidth +"x"+ tableHeight +")", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (userinput == 1) return;                     //User hit 'cancel'
        else {
          try {
            tableWidth = Integer.parseInt(widthField.getText());
            tableHeight = Integer.parseInt(heightField.getText());
          }
          catch (NumberFormatException exception) {return;}
        }
      } while (tableWidth <= 0 || tableHeight <= 0);


      ArcanistCCG.NetManager.tableResize(tableWidth, tableHeight);
      // //resizeTable(tableWidth, tableHeight);
    }
    else if (source == addCounterMenuItem) {
      new CounterFrame(pronoun);
    }
    else if (source == addRollerMenuItem) {
      new DieRollerFrame(pronoun);
    }
    else if (source == addTimerMenuItem) {
      new TimerFrame(pronoun);
    }
    else if (source == addTokenMenuItem) {
      new TokensFrame(pronoun);
    }
    else if (source == addNoteMenuItem) {
      Rectangle tableView = getTableView();
      ArcanistCCG.NetManager.noteAdd(tableView.x, tableView.y, " Note ");
    }
    else if (source == addDiscardMenuItem) {
      Rectangle tableView = getTableView();
      ArcanistCCG.NetManager.deckAdd(tableView.x, tableView.y, false, true, true, false, 'D', "");
    }
    else if (source == addHandMenuItem) {
      Rectangle tableView = getTableView();
      ArcanistCCG.NetManager.handAdd(tableView.x, tableView.y, "Hand", Hand.DEFAULT_WIDTH, Hand.DEFAULT_HEIGHT);
    }
    else if (source == addJumboMenuItem) {
      if (addJumboMenuItem.isEnabled()) {                    //It's still clickable when disabled!
        createJumboFrame();
      }
    }
    else if (source == helpHelpMenuItem) {
      new HelpWindow(this);
    }
    else if (source == helpAboutMenuItem) {
      String message = "ArcanistCCG "+ ArcanistCCG.VERSION +"\n";
        message += "Copyright (C) 2011 David Millis\n";
        message += ArcanistCCG.WEBSITE +"\n\n";
        message += "ArcanistCCG comes with ABSOLUTELY NO WARRANTY.\n";
        message += "Distributed under the terms of the GNU General Public License.";
        message += "\n\nIncludes rotation code based on Image Processing Filters\n";
        message += "Copyright (C) Jerry Huxtable 1998\n";
        message += "http://www.lhlabs.com";
      JTextArea aboutTxt = new JTextArea(message);
        aboutTxt.setEditable(false);
      JScrollPane aboutScrollPane = new JScrollPane(aboutTxt);
        aboutScrollPane.setPreferredSize(new Dimension(350, 175));
      JOptionPane.showInternalMessageDialog(desktop, aboutScrollPane, "About", JOptionPane.PLAIN_MESSAGE);
    }
  }


  public void guiInterrupted() {
    for (int i=getCardCount()-1; i >= 0; i--) {
      getCard(i).guiInterrupted();
    }
    for (int i=getDeckCount()-1; i >= 0; i--) {
      getDeck(i).guiInterrupted();
    }
    for (int i=getNoteCount()-1; i >= 0; i--) {
      getNote(i).guiInterrupted();
    }
    for (int i=getTokenCount()-1; i >= 0; i--) {
      getToken(i).guiInterrupted();
    }
    for (int i=getHandCount()-1; i >= 0; i--) {
      getHand(i).guiInterrupted();
    }
    for (int i=getGroupCount()-1; i >= 0; i--) {
      getGroup(i).guiInterrupted();
    }
  }


  /**
   * Toggles user interaction within this window.
   * Various menu items and all nerfable jinternalframes are affected.
   */
  @Override
  public void setNerfed(boolean b) {
    //button mnemonics will still work
    tablePane.setNerfed(b);

    fileOpenDeckMenuItem.setEnabled(!b);
    fileClearMenuItem.setEnabled(!b);
    fileLoadStateMenuItem.setEnabled(!b);
    settingsTableSizeMenuItem.setEnabled(!b);
    addTokenMenuItem.setEnabled(!b);
    addNoteMenuItem.setEnabled(!b);
    addDiscardMenuItem.setEnabled(!b);
    addHandMenuItem.setEnabled(!b);
    JInternalFrame[] frames = desktop.getAllFrames();
    for (int i=0; i < frames.length; i++) {
      if (frames[i] instanceof Nerfable) ((Nerfable)frames[i]).setNerfed(b);
    }

    guiInterrupted();
  }


  public void setDragging(boolean b) {dragging = b;}
  public boolean isDragging() {return dragging;}
  public DragGhost getDragGhost() {return dragGhost;}
  public DragGhost addDragGhost() {if (dragGhost.getParent() == null) tablePane.add(dragGhost, Prefs.ghostLayer, 0); return dragGhost;}
  public void removeDragGhost() {tablePane.remove(dragGhost);}

  public void setJumboFrame(JumboView f) {jumboFrame = f;}
  public JumboView getJumboFrame() {return jumboFrame;}

  public MouseListener getFocusOnEnterListener() {return focusOnEnterListener;}

  public Card getCard(int index) {return cardArray.get(index);}
  public Deck getDeck(int index) {return deckArray.get(index);}
  public FloatingNote getNote(int index) {return noteArray.get(index);}
  public Token getToken(int index) {return tokenArray.get(index);}
  public Hand getHand(int index) {return handArray.get(index);}
  public MassDragger getGroup(int index) {return groupArray.get(index);}

  public void addCard(Card c) {cardArray.add(c);}
  public void addDeck(Deck d) {deckArray.add(d);}
  public void addNote(FloatingNote n) {noteArray.add(n);}
  public void addToken(Token t) {tokenArray.add(t);}
  public void addHand(Hand h) {handArray.add(h);}
  public void addGroup(MassDragger g) {groupArray.add(g);}

  public boolean hasCard(Card c) {return cardArray.contains(c);}
  public boolean hasDeck(Deck d) {return deckArray.contains(d);}
  public boolean hasNote(FloatingNote n) {return noteArray.contains(n);}
  public boolean hasToken(Token t) {return tokenArray.contains(t);}
  public boolean hasHand(Hand h) {return handArray.contains(h);}
  public boolean hasGroup(MassDragger g) {return groupArray.contains(g);}

  public void removeCard(int index) {cardArray.remove(index);}
  public void removeDeck(int index) {deckArray.remove(index);}
  public void removeNote(int index) {noteArray.remove(index);}
  public void removeToken(int index) {tokenArray.remove(index);}
  public void removeHand(int index) {handArray.remove(index);}
  public void removeGroup(int index) {groupArray.remove(index);}

  public int getCardCount() {return cardArray.size();}
  public int getDeckCount() {return deckArray.size();}
  public int getNoteCount() {return noteArray.size();}
  public int getTokenCount() {return tokenArray.size();}
  public int getHandCount() {return handArray.size();}
  public int getGroupCount() {return groupArray.size();}

  public int getCardIndex(Card c) {
    int id = c.getId();
    for (int i=getCardCount()-1; i >= 0; i--) {
      if (getCard(i).getId() == id) return i;
    }
    return -1;
  }

  public int getDeckIndex(Deck d) {
    int id = d.getId();
    for (int i=getDeckCount()-1; i >= 0; i--) {
      if (getDeck(i).getId() == id) return i;
    }
    return -1;
  }

  public int getNoteIndex(FloatingNote n) {
    int id = n.getId();
    for (int i=getNoteCount()-1; i >= 0; i--) {
      if (getNote(i).getId() == id) return i;
    }
    return -1;
  }

  public int getTokenIndex(Token t) {
    int id = t.getId();
    for (int i=getTokenCount()-1; i >= 0; i--) {
      if (getToken(i).getId() == id) return i;
    }
    return -1;
  }

  public int getHandIndex(Hand h) {
    int id = h.getId();
    for (int i=getHandCount()-1; i >= 0; i--) {
      if (getHand(i).getId() == id) return i;
    }
    return -1;
  }

  public int getGroupIndex(MassDragger g) {
    for (int i=getGroupCount()-1; i >= 0; i--) {
      if (getGroup(i) == g) return i;
    }
    return -1;
  }


  public JDesktopPane getDesktop() {return desktop;}
  public JInternalFrame getTableFrame() {return tableFrame;}
  public TableComponent getTablePane() {return tablePane;}
  public JMenuItem getAddJumboMenuItem() {return addJumboMenuItem;}

  public ChatPanel getChatPanel() {return ChatPanel;}
  public void setChatPanel(ChatPanel c) {ChatPanel = c;}


  public void addHoverListener(HoverListener h) {
    if (!hoverListeners.contains(h)) hoverListeners.add(h);
  }

  public void removeHoverListener(HoverListener h) {
    hoverListeners.remove(h);
  }


  /**
   * Sets a new size for the table.
   * Existing objects and view will be moved centerward
   * from the top-left, so added space appears at the border.
   */
  public void resizeTable(int tableWidth, int tableHeight) {
    guiInterrupted();

    Dimension prevSize = tablePane.getPreferredSize();
    tablePane.setPreferredSize(new Dimension(tableWidth, tableHeight));

    int fudgeX = tableWidth/2 - prevSize.width/2;
    int fudgeY = tableHeight/2 - prevSize.height/2;
    int i=0;
    for (i=getCardCount()-1; i >= 0; i--) {
      Card tmpCard = getCard(i);
      tmpCard.setLocation(tmpCard.getX()+fudgeX, tmpCard.getY()+fudgeY);
    }

    for (i=getDeckCount()-1; i >= 0; i--) {
      Deck tmpDeck = getDeck(i);
      tmpDeck.setLocation(tmpDeck.getX()+fudgeX, tmpDeck.getY()+fudgeY);
    }

    for (i=getNoteCount()-1; i >= 0; i--) {
      FloatingNote tmpNote = getNote(i);
      tmpNote.setLocation(tmpNote.getX()+fudgeX, tmpNote.getY()+fudgeY);
    }

    for (i=getTokenCount()-1; i >= 0; i--) {
      Token tmpToken = getToken(i);
      tmpToken.setLocation(tmpToken.getX()+fudgeX, tmpToken.getY()+fudgeY);
    }

    for (i=getHandCount()-1; i >= 0; i--) {
      getHand(i).setLocation((getHand(i).getX()+fudgeX), (getHand(i).getY()+fudgeY));
    }

    for (i=getGroupCount()-1; i >= 0; i--) {
      getGroup(i).setLocation((getGroup(i).getX()+fudgeX), (getGroup(i).getY()+fudgeY));
    }

    tablePane.revalidate();
    //myScrollPane.revalidate();

    Rectangle newView = myScrollPane.getViewport().getViewRect();
    newView.x += fudgeX; newView.y += fudgeY;
    tablePane.scrollRectToVisible(newView);
    tablePane.repaint();
  }


  /**
   * Returns visible region in table coords.
   */
  public Rectangle getTableView() {
    return myScrollPane.getViewport().getViewRect();
  }

  /**
   * Scrolls the table view to the center.
   */
  public void centerTableView() {
    Rectangle newView = myScrollPane.getViewport().getViewRect();
      newView.x += (tablePane.getSize().width-newView.width)/2; newView.y += (tablePane.getSize().height-newView.height)/2;
    tablePane.scrollRectToVisible(newView);
  }


  /**
   * Removes all objects from table.
   * Also clears the image cache, and empties the used ids list.
  */
  public void clearTable() {
    for (int i=getCardCount()-1; i >= 0; i--) {
      getCard(i).removeFromTable();
    }
    for (int i=getDeckCount()-1; i >= 0; i--) {
      getDeck(i).removeFromTable();
    }
    for (int i=getTokenCount()-1; i >= 0; i--) {
      getToken(i).removeFromTable();
    }
    for (int i=getNoteCount()-1; i >= 0; i--) {
      getNote(i).removeFromTable();
    }
    for (int i=getHandCount()-1; i >= 0; i--) {
      getHand(i).removeFromTable();
    }
    for (int i=getGroupCount()-1; i >= 0; i--) {
      getGroup(i).removeFromTable();
    }
    tablePane.repaint();
    ArcanistCCG.setUsedIds(null);
    Prefs.Cache.setup();
  }


  /**
   * Unuses all objects on the table.
   */
  public void unuseEverything() {
    for (int i=getCardCount()-1; i >= 0; i--) {
      getCard(i).setInUse(false);
    }
    for (int i=getDeckCount()-1; i >= 0; i--) {
      getDeck(i).setInUse(false);
    }
    for (int i=getTokenCount()-1; i >= 0; i--) {
      getToken(i).setInUse(false);
    }
    for (int i=getNoteCount()-1; i >= 0; i--) {
      getNote(i).setInUse(false);
    }
    for (int i=getHandCount()-1; i >= 0; i--) {
      getHand(i).setInUse(false);
    }
    for (int i=getGroupCount()-1; i >= 0; i--) {
      getGroup(i).setInUse(false);
    }
  }


  /**
   * Toggles visibility of menu items depending on connected state.
   * NetPlay/Disconnect/Lobby/SendState
   *
   * @param state true if connected, false if not connected
   */
  public void setConnectedState(boolean state) {
    if (state == false) {
      fileConnectMenuItem.setVisible(true);
      fileConnectMenuItem.setEnabled(true);
      fileDisconnectMenuItem.setVisible(false);
      fileDisconnectMenuItem.setEnabled(false);

      fileSendStateMenuItem.setVisible(false);
      fileSendStateMenuItem.setEnabled(false);

      fileLobbyMenuItem.setVisible(false);
      fileLobbyMenuItem.setEnabled(false);
    } else {
      fileConnectMenuItem.setVisible(false);
      fileConnectMenuItem.setEnabled(false);
      fileDisconnectMenuItem.setVisible(true);
      fileDisconnectMenuItem.setEnabled(true);

      fileSendStateMenuItem.setVisible(true);
      fileSendStateMenuItem.setEnabled(true);

      ServerThread st = ArcanistCCG.getServerThread();
      fileLobbyMenuItem.setVisible(st != null);
      fileLobbyMenuItem.setEnabled(st != null);
    }
  }


  /**
   * Clears objects' hotkeys and reapplies current global ones.
   */
  public void updateHotkeys() {
    InputMap tableInputMap = tableFrame.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    KeyStroke[] tableInputKeys = tableInputMap.keys();
    if (tableInputKeys != null) {
      for (int i=0; i < tableInputKeys.length; i++) {
        Object o = tableInputMap.get(tableInputKeys[i]);
        if (o == Prefs.ACTION_TABLE_FOCUS_CHAT) tableInputMap.remove(tableInputKeys[i]);
      }
    }
    synchronized (Prefs.hotkeyLock) {
      Object tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_TABLE_FOCUS_CHAT);
      if (tmpStroke != null) tableInputMap.put((KeyStroke)tmpStroke, Prefs.ACTION_TABLE_FOCUS_CHAT);
    }

    ChatPanel chatPanel = getChatPanel();
    if (chatPanel != null) chatPanel.updateHotkeys();

    for (int i=getCardCount()-1; i >= 0; i--) {
      getCard(i).updateHotkeys();
    }
    for (int i=getDeckCount()-1; i >= 0; i--) {
      getDeck(i).updateHotkeys();
    }
    for (int i=getHandCount()-1; i >= 0; i--) {
      getHand(i).updateHotkeys();
    }
  }


  /**
   * Applies overlay text to cards in play.
   */
  public void updateOverlayText() {
    for (int i=getCardCount()-1; i >= 0; i--) {
      getCard(i).updateOverlayText();
    }
  }


  /**
   * Creates and displays a JumboView.
   */
  public JumboView createJumboFrame() {
    if (jumboFrame != null) return jumboFrame;

    jumboFrame = new JumboView(this);
    return jumboFrame;
  }


  /**
   * Displays a file chooser dialog.
   * When saving, a file filter was active, the chosen file
   * doesn't exist, and the filter doesn't accept the proposed
   * file, the filter's primary suffix will be appended to the
   * filename.
   *
   * @param chooserType JFileChooser.OPEN_DIALOG or JFileChooser.SAVE_DIALOG
   * @param path initial directory
   * @param filter a file filter, or null
   * @return the chosen file, or null
   */
  public File fileChooser(int chooserType, String path, ExtensionFileFilter filter) {
    if (chooserType != JFileChooser.OPEN_DIALOG && chooserType != JFileChooser.SAVE_DIALOG) return null;

    JFileChooser chooser = new JFileChooser(path);
      chooser.setDialogType(chooserType);
      if (filter != null) chooser.addChoosableFileFilter(filter);
      //int status = chooser.showOpenDialog(desktop);
      int status = chooser.showDialog(desktop, null);
      if (status != JFileChooser.APPROVE_OPTION) return null;

      File result = chooser.getSelectedFile();

      if (chooserType == JFileChooser.SAVE_DIALOG) {
        if (filter != null) {
          if (!result.exists() && chooser.getFileFilter() == filter && !filter.accept(result)) {
            result = new File(result.getAbsolutePath() + filter.getPrimarySuffix());
          }
        }
        if (result.exists()) {
          if (JOptionPane.showInternalConfirmDialog(desktop, result.getName() +" already exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return null;
          }
        }
      }
      return result;
  }


  /**
   * Loads appearance settings.
   */
  public boolean loadAppearance() {
    File appearanceFile = new File(Prefs.appearanceFilePath);
    if (!appearanceFile.exists()) return false;

    ArcanistCCG.LogManager.write(LogManager.INFO_LEVEL, "Loading appearance settings ("+ Prefs.appearanceFilePath +").");

    Document doc = XmlUtils.readXML(appearanceFile);
    if (doc == null) return false;
    Element rootNode = doc.getRootElement();

    Element tableElement = rootNode.getChild("table");
    if (tableElement != null) {
      Element mainBgColorElement = tableElement.getChild("main-bgcolor");
      Prefs.mainBgColor = XmlUtils.parseRGBAttribs(mainBgColorElement, null);
      if (Prefs.mainBgColor != null) pronoun.getContentPane().setBackground(Prefs.mainBgColor);

      Element tableBgColorElement = tableElement.getChild("table-bgcolor");
      Prefs.tableBgColor = XmlUtils.parseRGBAttribs(tableBgColorElement, null);
      if (Prefs.tableBgColor != null) tablePane.getParent().setBackground(Prefs.tableBgColor);

      Element tableBgImageElement = tableElement.getChild("table-bgimage");
      if (tableBgImageElement != null) {
        try {
          Prefs.tableBgTiled = XmlUtils.parseString(tableBgImageElement.getAttributeValue("tiled"), "false").equals("true");
          Prefs.tableBgImage = tableBgImageElement.getText();  // Defaults to ""
          if (Prefs.tableBgImage.length() > 0) {
            tablePane.setBackgroundImage(Prefs.tableBgImage, Prefs.tableBgTiled);
          } else {
            tablePane.setBackgroundImage(null, Prefs.tableBgTiled);
          }
        } catch (IOException f) {
          JOptionPane.showInternalMessageDialog(desktop, "Couldn't load background image.", "Error", JOptionPane.PLAIN_MESSAGE);
          Prefs.tableBgImage = "";
          Prefs.tableBgTiled = false;
        }
      }

      Element gridElement = tableElement.getChild("table-grid");
      tablePane.setGridSnap(Math.max(1, XmlUtils.parseInt(gridElement.getAttributeValue("value"), 1)));
    }

    Element playerElement = rootNode.getChild("player");
    if (playerElement != null) {
      Element chatAliasElement = playerElement.getChild("chat-alias");
      if (chatAliasElement != null) {
        String savedAlias = chatAliasElement.getText();
        String prevAlias = ArcanistCCG.NetManager.getPlayerAlias();
        Prefs.setPlayerAlias(savedAlias);
        String newAlias = ArcanistCCG.NetManager.getPlayerAlias();
        if (prevAlias.equals(newAlias) == false) {
          ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE2, "--"+ prevAlias +" set alias to "+ newAlias +"--");
        }
      }

      Element chatTimestmpsElement = playerElement.getChild("chat-timestamps");
      Prefs.chatTimestamps = XmlUtils.parseString(chatTimestmpsElement.getAttributeValue("value"), "false").equals("true");
    }

    boolean tableSized = false;
    Element windowsElement = rootNode.getChild("windows");
    if (windowsElement != null) {
      List<Element> windowList = windowsElement.getChildren("window");
      Iterator<Element> it = windowList.iterator();
      while (it.hasNext()) {
        Element windowElement = it.next();
        JInternalFrame newFrame = null;

        String windowType = windowElement.getAttributeValue("type");
        if (windowType == null) continue;
        else if (windowType.equals("table")) {
          newFrame = tableFrame;
          tableSized = true;
        }
        else if (windowType.equals("jumbo")) {
          jumboFrame = createJumboFrame();
          newFrame = jumboFrame;
        }
        else if (windowType.equals("chat")) {
          if (chatFrame == null) chatFrame = new ChatFrame(pronoun);
          newFrame = chatFrame;
        }
        else if (windowType.equals("tokens")) {
          newFrame = new TokensFrame(pronoun);
        }
        else if (windowType.equals("counter")) {
          newFrame = new CounterFrame(pronoun);
          String counterName = XmlUtils.parseString(windowElement.getChildText("name"), "Amount");
          String counterValue = XmlUtils.parseString(windowElement.getAttributeValue("value"), "0");
          ((CounterFrame)newFrame).setName(counterName);
          ((CounterFrame)newFrame).setValue(counterValue);
        }
        else if (windowType.equals("dieroller")) {
          newFrame = new DieRollerFrame(pronoun);
          String dieCount = XmlUtils.parseString(windowElement.getAttributeValue("count"), "1");
          String dieSides = XmlUtils.parseString(windowElement.getAttributeValue("sides"), "20");
          ((DieRollerFrame)newFrame).setDice(dieCount, dieSides);
        }
        else if (windowType.equals("timer")) {
          newFrame = new TimerFrame(pronoun);
          ((TimerFrame)newFrame).setCountdown(XmlUtils.parseInt(windowElement.getAttributeValue("countdown"), 0));
        }
        else continue;

        if (newFrame != null) {
          Rectangle newBounds = new Rectangle();
          newBounds.x = XmlUtils.parseInt(windowElement.getAttributeValue("x"), 0);
          newBounds.y = XmlUtils.parseInt(windowElement.getAttributeValue("y"), 0);
          newBounds.width = XmlUtils.parseInt(windowElement.getAttributeValue("w"), 100);
          newBounds.height = XmlUtils.parseInt(windowElement.getAttributeValue("h"), 100);
          newFrame.reshape(newBounds.x, newBounds.y, newBounds.width, newBounds.height);
        }
      }
    }
    return tableSized;
  }

  /**
   * Saves appearance settings.
   */
  public void saveAppearance(boolean saveWindowLayout) {
    ArcanistCCG.LogManager.write(LogManager.INFO_LEVEL, "Saving appearance settings.");

    StringBuffer buf = new StringBuffer();
    buf.append("<?xml version=\"1.0\"?>\n");
    buf.append("<appearance version=\"1\">\n");

    buf.append("  <table>\n");
    if (Prefs.mainBgColor != null) {
      buf.append("    <main-bgcolor");
      buf.append(" r=\"").append(Prefs.mainBgColor.getRed()).append("\"");
      buf.append(" g=\"").append(Prefs.mainBgColor.getGreen()).append("\"");
      buf.append(" b=\"").append(Prefs.mainBgColor.getBlue()).append("\"");
      buf.append(" />\n");
    }
    if (Prefs.tableBgColor != null) {
      buf.append("    <table-bgcolor");
      buf.append(" r=\"").append(Prefs.tableBgColor.getRed()).append("\"");
      buf.append(" g=\"").append(Prefs.tableBgColor.getGreen()).append("\"");
      buf.append(" b=\"").append(Prefs.tableBgColor.getBlue()).append("\"");
      buf.append(" />\n");
    }
    if (Prefs.tableBgImage.length() > 0) {
      buf.append("    <table-bgimage");
      buf.append(" tiled=\"").append(Prefs.tableBgTiled).append("\"");
      buf.append(">");
      buf.append("<![CDATA[").append(Prefs.tableBgImage).append("]]>");
      buf.append("</table-bgimage>\n");
    }
    if (tablePane.getGridSnap() > 1) {
      buf.append("    <table-grid");
      buf.append(" value=\"").append(tablePane.getGridSnap()).append("\"");
      buf.append(" />\n");
    }
    buf.append("  </table>\n");

    buf.append("  <player>\n");
    if (Prefs.savedAlias.length() > 0) {
      buf.append("    <chat-alias>");
      buf.append("<![CDATA[").append(Prefs.savedAlias).append("]]>");
      buf.append("</chat-alias>\n");
    }
    if (Prefs.chatTimestamps != false) {
      buf.append("    <chat-timestamps");
      buf.append(" value=\"").append(Prefs.chatTimestamps).append("\"");
      buf.append(" />\n");
    }
    buf.append("  </player>\n");

    if (saveWindowLayout) {
      JInternalFrame[] windows = desktop.getAllFrames();
      buf.append("  <windows>\n");
      for (int i=0; i < windows.length; i++) {
        Rectangle bounds = windows[i].getNormalBounds();
        StringBuffer boundsBuf = new StringBuffer();
          boundsBuf.append(" x=\"").append(bounds.x).append("\"");
          boundsBuf.append(" y=\"").append(bounds.y).append("\"");
          boundsBuf.append(" w=\"").append(bounds.width).append("\"");
          boundsBuf.append(" h=\"").append(bounds.height).append("\"");

        if (windows[i].getTitle().equals("Table")) {
          buf.append("    <window type=\"table\"").append(boundsBuf).append(" />\n");
        }
        else if (windows[i].getClass() == JumboView.class) {
          buf.append("    <window type=\"jumbo\"").append(boundsBuf).append(" />\n");
        }
        else if (windows[i].getClass() == ChatFrame.class) {
          buf.append("    <window type=\"chat\"").append(boundsBuf).append(" />\n");
        }
        else if (windows[i].getClass() == TokensFrame.class) {
          buf.append("    <window type=\"tokens\"").append(boundsBuf).append(" />\n");
        }
        else if (windows[i].getClass() == CounterFrame.class) {
          buf.append("    <window type=\"counter\"").append(boundsBuf);
          buf.append(" value=\"").append(((CounterFrame)windows[i]).getValue()).append("\"");
          buf.append(">");
          buf.append("<name><![CDATA[").append(((CounterFrame)windows[i]).getName()).append("]]></name>");
          buf.append("</window>\n");
        }
        else if (windows[i].getClass() == DieRollerFrame.class) {
          buf.append("    <window type=\"dieroller\"").append(boundsBuf);
          buf.append(" count=\"").append(((DieRollerFrame)windows[i]).getDice()).append("\"");
          buf.append(" sides=\"").append(((DieRollerFrame)windows[i]).getDie()).append("\"");
          buf.append(" />\n");
        }
        else if (windows[i].getClass() == TimerFrame.class) {
          buf.append("    <window type=\"timer\"").append(boundsBuf);
          buf.append(" countdown=\"").append(((TimerFrame)windows[i]).getCountdown()).append("\"");
          buf.append(" />\n");
        }
      }
      buf.append("  </windows>\n");
    }

    buf.append("</appearance>\n");

    BufferedWriter outFile = null;
    try {
      OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(Prefs.appearanceFilePath), "UTF-8");
      outFile = new BufferedWriter(fw);
      outFile.write(buf.toString());
    }
    catch (IOException e) {ArcanistCCG.LogManager.write(e, "Couldn't save appearance settings.");}
    finally {
      try {if (outFile != null) outFile.close();}
      catch (IOException e) {}
    }
  }


  /**
   * Loads keyboard settings.
   */
  public void loadKeyboard() {
    File keyboardFile = new File(Prefs.keyboardFilePath);
    if (!keyboardFile.exists()) return;

    ArcanistCCG.LogManager.write(LogManager.INFO_LEVEL, "Loading keyboard settings ("+ Prefs.keyboardFilePath +").");

    Map<String,KeyStroke> newStrokeMap = new HashMap<String,KeyStroke>();
    Map<String,String> newArgMap = new HashMap<String,String>();
    Iterator<Element> it = null;

    Document doc = XmlUtils.readXML(keyboardFile);
    if (doc == null) return;

    Element rootNode = doc.getRootElement();
    Element hotkeysElement = rootNode.getChild("hotkeys");
    if (hotkeysElement == null) return;

    List<Element> hotkeyList = hotkeysElement.getChildren("hotkey");
    it = hotkeyList.iterator();
    while(it.hasNext()) {
      Element hotkeyElement = it.next();
      String hotkeyAction = hotkeyElement.getChildText("action");
      String hotkeyString = hotkeyElement.getChildText("keystroke");
      if (hotkeyAction == null || hotkeyAction.length() == 0 || hotkeyString == null) continue;

      KeyStroke hotkeyStroke = null;  // Blank hotkeyString means null
      if (hotkeyString.length() > 0) {
        hotkeyStroke = KeyStroke.getKeyStroke(hotkeyString);
        if (hotkeyStroke == null) {
          ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Invalid keystroke for keyboard setting: '"+ hotkeyAction +"'.");
          continue;
        }
      }
      newStrokeMap.put(hotkeyAction, hotkeyStroke);

      String hotkeyArg = hotkeyElement.getChildText("arg");
      if (hotkeyArg == null || hotkeyArg.length() == 0) continue;

      newArgMap.put(hotkeyAction, hotkeyArg);
    }
    if (newStrokeMap.size() == 0) return;

    synchronized (Prefs.hotkeyLock) {
      for (String hotkeyAction : newStrokeMap.keySet()) {
        KeyStroke hotkeyStroke = newStrokeMap.get(hotkeyAction);

        if (Prefs.hotkeyStrokeMap.containsKey(hotkeyAction)) {
          Prefs.hotkeyStrokeMap.put(hotkeyAction, hotkeyStroke);
        }
        else {
          ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Unrecognied keyboard setting action: '"+ hotkeyAction +"'.");
          continue;
        }

        if (newArgMap.containsKey(hotkeyAction)) {
          String hotkeyArg = newArgMap.get(hotkeyAction);
          if (Prefs.hotkeyArgMap.containsKey(hotkeyAction)) {
            Prefs.hotkeyArgMap.put(hotkeyAction, hotkeyArg);
          }
          else {
            ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Unexpected keyboard setting arg for action: '"+ hotkeyAction +"'.");
            continue;
          }
        }
      }
    }
    updateHotkeys();
  }

  /**
   * Saves keyboard settings.
   */
  public void saveKeyboard() {
    ArcanistCCG.LogManager.write(LogManager.INFO_LEVEL, "Saving keyboard settings.");
    StringBuffer buf = new StringBuffer();

    synchronized (Prefs.hotkeyLock) {
      Iterator<Map.Entry<String,KeyStroke>> it = new TreeMap<String,KeyStroke>(Prefs.hotkeyStrokeMap).entrySet().iterator();

      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<keyboard version=\"1\">\n");
      buf.append("  <hotkeys>\n");
      while (it.hasNext()) {
        Map.Entry<String,KeyStroke> entry = it.next();
        String hotkeyAction = entry.getKey();
        String hotkeyString = "";
        String hotkeyArg = Prefs.hotkeyArgMap.get(hotkeyAction);
        if (hotkeyArg == null) hotkeyArg = "";

        if (entry.getValue() != null) hotkeyString = XmlUtils.keyStroke2String((KeyStroke)entry.getValue());
        if (hotkeyString == null || ((String)hotkeyString).indexOf("unknown") != -1) hotkeyString = "";

        buf.append("    <hotkey>\n");
        buf.append("      <action>").append(hotkeyAction).append("</action>\n");
        buf.append("      <keystroke><![CDATA[").append(hotkeyString).append("]]></keystroke>\n");
        if (hotkeyArg != null) {
          buf.append("      <arg><![CDATA[").append(hotkeyArg).append("]]></arg>\n");
        }
        buf.append("    </hotkey>\n");
      }
      buf.append("  </hotkeys>\n");
      buf.append("</keyboard>\n");
    }

    BufferedWriter outFile = null;
    try {
      OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(Prefs.keyboardFilePath), "UTF-8");
      outFile = new BufferedWriter(fw);
      outFile.write(buf.toString());
      outFile.close();
    }
    catch (IOException e) {ArcanistCCG.LogManager.write(e, "Couldn't save keyboard settings.");}
    finally {
      try {if (outFile != null) outFile.close();}
      catch (IOException e) {}
    }
  }
}
