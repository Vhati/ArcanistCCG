package org.arcanist.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * Deck building window.
 * This works closely with the DatParser class.
 */
public class DeckBuilder extends NerfableInternalFrame implements ActionListener {
  ArcanistCCGFrame frame = null;

  JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

  boolean dontReset = false;                                 //Prevents unfiltering

  List<String> fullCardList = new ArrayList<String>();
  List<String> resultList = new ArrayList<String>(0);
  JLabel resultsLabel = new JLabel("");
  JLabel deckCountLabel = new JLabel("");
  LongListModel cardListModel = new LongListModel(0);
  JList cardList = null;
  private boolean cardListFiltered = false;
  DefaultTableModel deckTableModel = new DefaultTableModel(new String[]{"#", "Name", "Set", "FrontFile", "BackName", "BackFile"}, 0);
  JTable deckTable = null;
  DefaultMutableTreeNode treeModel = new DefaultMutableTreeNode("Images");
  JTree treeList = null;
  private SearchPanel searchPanel = new SearchPanel();
  private boolean editingEnabled = false;                    //Editing menu state

  private JMenuItem fileNewDeckMenuItem = null;
  private JMenuItem fileOpenDeckMenuItem = null;
  private JMenuItem fileSaveDeckMenuItem = null;
  private JMenuItem fileImportLackeyMenuItem = null;
  private JMenuItem fileImportApprMenuItem = null;
  private JMenuItem fileExportApprMenuItem = null;
  //private JMenuItem fileExportSpoilerMenuItem = null;
  private JMenuItem editEnableMenuItem = null;
  private JMenuItem editNewMenuItem = null;
  private JMenuItem editEditMenuItem = null;
  private JMenuItem editDeleteMenuItem = null;
  private JMenuItem editSaveMenuItem = null;
  private JMenuItem editReopenMenuItem = null;

  private JButton saveResultsButton = null;
  private JButton tableButton = null;
  private JButton resetButton = null;
  private JButton filterButton = null;
  private JButton viewtxtButton = null;
  private JButton viewpicButton = null;
  private JButton addButton = null;
  private JButton remButton = null;
  private JButton upButton = null;
  private JButton downButton = null;
  private JButton blankButton = null;


  public DeckBuilder(ArcanistCCGFrame f) {
    super("DeckBuilder",
      true, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    super.setFrameIcon(null);
    frame = f;

    //Declare menus
    JMenuBar menuBar = new JMenuBar();
      JMenu fileMenu = new JMenu("File");
        fileNewDeckMenuItem = new JMenuItem("New Deck");
        fileOpenDeckMenuItem = new JMenuItem("Open Deck...");
        fileSaveDeckMenuItem = new JMenuItem("Save Deck...");
        fileImportLackeyMenuItem = new JMenuItem("Import LackeyCCG deck...");
        fileImportApprMenuItem = new JMenuItem("Import Apprentice deck...");
        fileExportApprMenuItem = new JMenuItem("Export Apprentice deck...");
//      fileExportSpoilerMenuItem = new JMenuItem("Export Deck Spoiler...");
      JMenu editMenu = new JMenu("Edit");
        editEnableMenuItem = new JMenuItem("Enable Editing");
        editNewMenuItem = new JMenuItem("New Card");
          editNewMenuItem.setEnabled(false);
        editEditMenuItem = new JMenuItem("Edit Card");
          editEditMenuItem.setEnabled(false);
        editDeleteMenuItem = new JMenuItem("Delete Card");
          editDeleteMenuItem.setEnabled(false);
        editSaveMenuItem = new JMenuItem("Save Dat");
          editSaveMenuItem.setEnabled(false);
        editReopenMenuItem = new JMenuItem("Reopen Dat");
          editReopenMenuItem.setEnabled(false);
      menuBar.add(fileMenu);
        fileMenu.add(fileNewDeckMenuItem);
        fileMenu.add(fileOpenDeckMenuItem);
        fileMenu.add(fileSaveDeckMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(fileImportLackeyMenuItem);
        fileMenu.add(fileImportApprMenuItem);
        fileMenu.add(fileExportApprMenuItem);
//      fileMenu.addSeparator();
//      fileMenu.add(fileExportSpoilerMenuItem);
      menuBar.add(editMenu);
        editMenu.add(editEnableMenuItem);
        editMenu.addSeparator();
        editMenu.add(editNewMenuItem);
        editMenu.add(editEditMenuItem);
        editMenu.add(editDeleteMenuItem);
        editMenu.addSeparator();
        editMenu.add(editSaveMenuItem);
        editMenu.add(editReopenMenuItem);
    this.setJMenuBar(menuBar);


    rebuildCardList();


    JPanel builderPanel = new JPanel();
      GridBagLayout builderGridbag = new GridBagLayout();
      GridBagConstraints builderC = new GridBagConstraints();
        builderC.fill = GridBagConstraints.BOTH;
        builderC.weightx = 0;
        builderC.weighty = 0;
        builderC.gridwidth = GridBagConstraints.REMAINDER;   //Whole Row
      builderPanel.setLayout(builderGridbag);
      JPanel buildOptionsPanel = new JPanel();
        buildOptionsPanel.setBounds(0,0,410,27);
        buildOptionsPanel.setPreferredSize(new Dimension(410,27));
        buildOptionsPanel.setMaximumSize(new Dimension(410,27));
        buildOptionsPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,0));
        buildOptionsPanel.setBorder(BorderFactory.createEtchedBorder());
        resultsLabel.setText("Results: "+ fullCardList.size());
          buildOptionsPanel.add(resultsLabel);
        buildOptionsPanel.add(Box.createHorizontalStrut(4));
        saveResultsButton = new JButton("Copy");
          buildOptionsPanel.add(saveResultsButton);
          saveResultsButton.setMargin(new Insets(0,2,0,2));
          saveResultsButton.setFocusable(false);
          saveResultsButton.setToolTipText("Copy results to clipboard");
        tableButton = new JButton("Table");
          buildOptionsPanel.add(tableButton);
          tableButton.setMargin(new Insets(0,2,0,2));
          tableButton.setFocusable(false);
          tableButton.setToolTipText("Add card to table");
        resetButton = new JButton("Reset");
          buildOptionsPanel.add(resetButton);
          resetButton.setMargin(new Insets(0,2,0,2));
          resetButton.setFocusable(false);
          resetButton.setToolTipText("Show all cards");
        filterButton = new JButton("Filter");
          buildOptionsPanel.add(filterButton);
          filterButton.setMargin(new Insets(0,2,0,2));
          filterButton.setFocusable(false);
          filterButton.setToolTipText("Filter search criteria");
        buildOptionsPanel.add(Box.createHorizontalStrut(4));
        deckCountLabel.setText("Deck: 0");
          buildOptionsPanel.add(deckCountLabel);
        builderPanel.add(buildOptionsPanel, builderC);

      builderC.gridwidth = 1;
      builderC.gridy = 1;
      builderC.weightx = .4;
      builderC.weighty = 1;
      cardList = new JList(cardListModel);
        cardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cardList.setFixedCellWidth(120);
        cardList.setVisibleRowCount(10);
        if (cardListModel.size() > 0) cardList.setSelectedIndex(0);
      JScrollPane cardListScrollPane = new JScrollPane(cardList);
        cardListScrollPane.setBounds(0,0,198,154);
        cardListScrollPane.setPreferredSize(new Dimension(198,154));
      builderPanel.add(cardListScrollPane, builderC);

      builderC.gridx = 1;
      builderC.weightx = 0;
      builderC.weighty = 0;
      JPanel buildCmdPanel = new JPanel();
        buildCmdPanel.setPreferredSize(new Dimension(45,154));
        buildCmdPanel.setMinimumSize(new Dimension(45,154));
        buildCmdPanel.setBorder(BorderFactory.createEtchedBorder());
        viewtxtButton = new JButton("Txt");
          buildCmdPanel.add(viewtxtButton);
          viewtxtButton.setMargin(new Insets(0,2,0,2));
          viewtxtButton.setFocusable(false);
        viewpicButton = new JButton("Pic");
          buildCmdPanel.add(viewpicButton);
          viewpicButton.setMargin(new Insets(0,2,0,2));
          viewpicButton.setFocusable(false);
        addButton = new JButton("Add");
          buildCmdPanel.add(addButton);
          addButton.setMargin(new Insets(0,2,0,2));
          addButton.setFocusable(false);
          addButton.setToolTipText("Add one card");
        remButton = new JButton("Rem");
          buildCmdPanel.add(remButton);
          remButton.setMargin(new Insets(0,2,0,2));
          remButton.setFocusable(false);
          remButton.setToolTipText("Remove all copies of selected card");
        upButton = new JButton("^");
          buildCmdPanel.add(upButton);
          upButton.setMargin(new Insets(0,1,0,1));
          upButton.setFocusable(false);
          upButton.setToolTipText("Move selected card up");
        downButton = new JButton("v");
          buildCmdPanel.add(downButton);
          downButton.setMargin(new Insets(0,1,0,1));
          downButton.setFocusable(false);
          downButton.setToolTipText("Move selected card down");
        blankButton = new JButton("");
          buildCmdPanel.add(blankButton);
          blankButton.setMargin(new Insets(1,14,1,14));
          blankButton.setFocusable(false);
          blankButton.setToolTipText("Add a blank line");
        builderPanel.add(buildCmdPanel, builderC);

      builderC.gridx = 2;
      builderC.weightx = .6;
      builderC.weighty = 1;
      treeList = new JTree(treeModel);
        treeList.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        treeList.setRootVisible(false);
      JScrollPane treeListScrollPane = new JScrollPane(treeList);
        treeListScrollPane.setBounds(0,0,228,154);
        treeListScrollPane.setPreferredSize(new Dimension(228,154));
      builderPanel.add(treeListScrollPane, builderC);



      deckTable = new JTable(deckTableModel);
        deckTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deckTable.getTableHeader().setReorderingAllowed(false);
        deckTable.getColumn("#").setPreferredWidth(16);
        deckTable.getColumn("Name").setPreferredWidth(162);
        deckTable.getColumn("Set").setPreferredWidth(85);
        deckTable.getColumn("FrontFile").setPreferredWidth(65);
        deckTable.getColumn("BackName").setPreferredWidth(59);
        deckTable.getColumn("BackFile").setPreferredWidth(47);
        deckTable.addNotify();
      JScrollPane deckListScrollPane = new JScrollPane(deckTable);
        deckListScrollPane.setBounds(0,0,480,154);

        deckListScrollPane.setPreferredSize(new Dimension(480,154));



    cardList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {              //Fires twice on a click, one firing's false
          updateSets();
        }
        if (cardList.isSelectionEmpty() == true || cardListModel.size()==0) return;

        cardList.ensureIndexIsVisible( cardList.getSelectedIndex() );
      }
    });

    deckTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (dontReset == true) return;

        if (deckTable.getRowCount() > 0 && deckTable.getSelectedRow() != -1) {
          updateDeckCount();
          if (cardListFiltered == true) {
            if ( ((String)deckTable.getValueAt(deckTable.getSelectedRow(), 0)).matches("\\p{Digit}") ) {
              if ( !resultList.contains(deckTable.getValueAt(deckTable.getSelectedRow(), 1)) ) {
                resetCardList();
              }
            }
          }
          cardList.setSelectedValue(deckTable.getValueAt(deckTable.getSelectedRow(), 1), true);
                                                             //This triggers the cardList listener
          updateSetsFromDeck();
        }
      }
    });

    deckTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getButton() == 1 && !e.isControlDown()) {
          if (deckTable.getRowCount() > 0) {
            Point p = e.getPoint();
            int clickedRow = deckTable.rowAtPoint(p);
            int currentRow = deckTable.getSelectedRow();
            if (clickedRow != -1 && clickedRow != currentRow) {
              deckTable.getSelectionModel().setSelectionInterval(clickedRow, clickedRow);
            }
            showPic();
          }
        }
      }
    });

    fileNewDeckMenuItem.addActionListener(this);
    fileOpenDeckMenuItem.addActionListener(this);
    fileSaveDeckMenuItem.addActionListener(this);
    fileImportLackeyMenuItem.addActionListener(this);
    fileImportApprMenuItem.addActionListener(this);
    fileExportApprMenuItem.addActionListener(this);
    //fileExportSpoilerMenuItem.addActionListener(this);
    editEnableMenuItem.addActionListener(this);
    editNewMenuItem.addActionListener(this);
    editEditMenuItem.addActionListener(this);
    editDeleteMenuItem.addActionListener(this);
    editSaveMenuItem.addActionListener(this);
    editReopenMenuItem.addActionListener(this);
    saveResultsButton.addActionListener(this);
    tableButton.addActionListener(this);
    resetButton.addActionListener(this);
    filterButton.addActionListener(this);


    Action searchAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (searchPanel.isBlank()) return;

        resultList = searchPanel.search();
        cardListModel.clear();
        resultsLabel.setText("Results: "+ resultList.size());
        cardListModel.addAll(resultList.toArray());
        cardListFiltered = true;
      }
    };
    searchPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("released ENTER"), "startSearch");
    searchPanel.getActionMap().put("startSearch", searchAction);


    viewtxtButton.addActionListener(this);
    viewpicButton.addActionListener(this);


    cardList.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        if (e.getButton() != 1 && e.getButton() != 3) return;
        if (e.getClickCount() != 1) return;
        if (cardList.isSelectionEmpty() == true || cardListModel.size() == 0) return;

        if (e.getButton() == 1) {
          boolean shown = showPic();
          if (!shown) showTxt();
        }
        else if (e.getButton() == 3 && cardList.locationToIndex(e.getPoint()) != -1) {
          cardList.setSelectedIndex(cardList.locationToIndex(e.getPoint()));
          showTxt();
        }
        System.gc();
      }
    });

    cardList.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (cardList.isSelectionEmpty() == true || cardListModel.size() == 0) return;
        if (e.getKeyCode() != KeyEvent.VK_ENTER && e.getKeyCode() != KeyEvent.VK_LEFT && e.getKeyCode() != KeyEvent.VK_RIGHT) return;

        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_LEFT) {
          boolean shown = showPic();
          if (!shown) showTxt();
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
          showTxt();
        }
        System.gc();
      }
    });

    treeList.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        if (cardList.isSelectionEmpty() == true || cardListModel.size() == 0) return;

        TreePath currentSelection = treeList.getSelectionPath();
        if (currentSelection != null) {
          DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(currentSelection.getLastPathComponent());
          if (currentNode.getLevel() > 1) {
            showPic();
          }
          else if (currentNode.getLevel() == 1) {
            showTxt();
          }
        }
        System.gc();
      }
    });

    // Show expansion dirs not present in Expan.dat as red
    // Show expansions that have no text as blue
    DefaultTreeCellRenderer treeRenderer = new DefaultTreeCellRenderer() {
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)value;
        int level = currentNode.getLevel();

        if (level == 1) {
          String setName = (String)currentNode.getUserObject();
          if (!Prefs.textDat.textSetExists(setName)) {
            super.setForeground(Color.red);
          }
          else if (!cardList.isSelectionEmpty() && cardListModel.size() > 0) {
            String cardName = cardListModel.elementAt(cardList.getSelectedIndex()).toString();
            String setAbbrev = Prefs.textDat.getSetAbbrevFromName(setName);
            if (Prefs.textDat.findCard(cardName, setAbbrev) == -1) {
              super.setForeground(Color.blue);
            }
          }
        }
        return this;
      }
    };
    treeList.setCellRenderer(treeRenderer);


    addButton.addActionListener(this);
    remButton.addActionListener(this);
    upButton.addActionListener(this);
    downButton.addActionListener(this);
    blankButton.addActionListener(this);


    this.addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameClosed(InternalFrameEvent e) {
        //Break references so they can be garbage collected
        //If a model lingers, it keeps its GUI on life-support
        cardList.setModel(new DefaultListModel());
        deckTable.setModel(new DefaultTableModel());
        treeList.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
      }
    });


    pane.setTopComponent(searchPanel);
    JSplitPane builderSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      builderSplitPane.setTopComponent(builderPanel);
      builderSplitPane.setBottomComponent(deckListScrollPane);
      pane.setBottomComponent(builderSplitPane);

    frame.getDesktop().add(this);
    this.setContentPane(pane);
    this.reshape(0, 0, 500, 519);
    this.show();
    updateSets();
  }


  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == fileNewDeckMenuItem) {
      resetCardList();
      searchPanel.reset();
      if (cardListModel.size() > 0) cardList.setSelectedIndex(0);
      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();
      deckTableModel.setRowCount(0);
      updateDeckCount();
    }
    else if (source == fileOpenDeckMenuItem) {
      File file = frame.fileChooser(JFileChooser.OPEN_DIALOG, Prefs.homePath +"decks", null);
      if (file == null) return;

      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();
      loadDeck(file);
    }
    else if (source == fileSaveDeckMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = frame.fileChooser(JFileChooser.SAVE_DIALOG, Prefs.homePath +"decks", filter);
      if (file == null) return;

      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();
      saveDeck(file);
    }
    else if (source == fileImportLackeyMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = frame.fileChooser(JFileChooser.OPEN_DIALOG, Prefs.homePath, filter);
      if (file == null) return;

      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();
      importLackeyDeck(file);
    }
    else if (source == fileImportApprMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = frame.fileChooser(JFileChooser.OPEN_DIALOG, Prefs.homePath, filter);
      if (file == null) return;

      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();
      importApprenticeDeck(file);
    }
    else if (source == fileExportApprMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = frame.fileChooser(JFileChooser.SAVE_DIALOG, Prefs.homePath, filter);
      if (file == null) return;

      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();
      exportApprenticeDeck(file);
    }
/*  else if (source == fileExportSpoilerMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = frame.fileChooser(JFileChooser.SAVE_DIALOG, Prefs.homePath, filter);
      if (file == null) return;

      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();
      exportSpoiler(file);
    } */
    else if (source == editEnableMenuItem) {
      if (editingEnabled) {return;}
      if (!Prefs.textDat.isWritable()) {return;}

      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Editing card data is dangerous.\nAre you sure you want to continue?", "Continue?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;

      editingEnabled = true;
      editEnableMenuItem.setEnabled(false);
      editNewMenuItem.setEnabled(true);
      editEditMenuItem.setEnabled(true);
      editDeleteMenuItem.setEnabled(true);
      editSaveMenuItem.setEnabled(true);
      editReopenMenuItem.setEnabled(true);
    }
    else if (source == editNewMenuItem) {
      if (!editingEnabled) {return;}
      if (frame.getJumboFrame() == null) {
        frame.createJumboFrame();
      }
      frame.getJumboFrame().updateJumboText(new CardTextPanel("", "", true));
      System.gc();
    }
    else if (source == editEditMenuItem) {
      if (!editingEnabled) {return;}
      if (cardList.isSelectionEmpty() == true || cardListModel.size()==0) return;

      if (frame.getJumboFrame() == null) {
        frame.createJumboFrame();
      }
                                                           //Find the expansion
      String expansion = "";
      TreePath currentSelection = treeList.getSelectionPath();
      if (currentSelection != null) {
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(currentSelection.getLastPathComponent());
        MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
        if (currentNode.getLevel() > 1) {
          expansion = currentSelection.getPathComponent(currentSelection.getPathCount()-2).toString();
        }
        else if (currentNode.getLevel() == 1) {
          expansion = currentSelection.getPathComponent(currentSelection.getPathCount()-1).toString();
        }
        expansion = Prefs.textDat.getSetAbbrevFromName(expansion);
      }
      frame.getJumboFrame().updateJumboText(new CardTextPanel((String) cardListModel.elementAt(cardList.getSelectedIndex()), expansion, true));
      System.gc();
    }
    else if (source == editDeleteMenuItem) {
      if (!editingEnabled) {return;}
      if (cardList.isSelectionEmpty() == true || cardListModel.size()==0) return;

      String expansion = "";
      TreePath currentSelection = treeList.getSelectionPath();
      if (currentSelection != null) {
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(currentSelection.getLastPathComponent());
        MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
        if (currentNode.getLevel() > 1) {
          expansion = currentSelection.getPathComponent(currentSelection.getPathCount()-2).toString();
        }
        else if (currentNode.getLevel() == 1) {
          expansion = currentSelection.getPathComponent(currentSelection.getPathCount()-1).toString();
        }
        expansion = Prefs.textDat.getSetAbbrevFromName(expansion);
      }

      int selectedIndex = cardList.getSelectedIndex();
      Prefs.textDat.delCard( Prefs.textDat.findCard( (String)cardListModel.elementAt(selectedIndex), expansion ) );
      rebuildCardList();
      if (selectedIndex < cardListModel.size())
        cardList.setSelectedIndex(selectedIndex);
      System.gc();
    }
    else if (source == editSaveMenuItem) {
      if (!editingEnabled) {return;}

      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "This will overwrite your dat file.\nAre you sure you want to continue?", "Overwrite?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;

      Prefs.textDat.save();
    }
    else if (source == editReopenMenuItem) {
      if (!editingEnabled) {return;}

      DatParser newDat = new DatParser();
        newDat.setup(frame, Prefs.gameloc, Prefs.gamedat, Prefs.gameset);
      Prefs.textDat = newDat;

      Prefs.Cache.setup();
      rebuildCardList();
    }
    else if (source == saveResultsButton) {
      String tempLine = "", results = "";

      for (int i=0; i < cardListModel.size(); i++) {
        tempLine = cardListModel.elementAt(i).toString();
        results += tempLine +"\n";
      }
      java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(results);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
    }
    else if (source == tableButton) {
      String frontFile = "", expansion = "", path = "";
      TreePath currentSelection = treeList.getSelectionPath();
      if (cardList.getSelectedIndex() == -1) return;

      String cardName = cardListModel.elementAt(cardList.getSelectedIndex()).toString();
      Rectangle tableView = frame.getTableView();

      if (currentSelection != null) {
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(currentSelection.getLastPathComponent());
        MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
        if (currentNode.getLevel() > 1) {
          expansion = currentSelection.getPathComponent(currentSelection.getPathCount()-2).toString();
          frontFile = currentSelection.getPathComponent(currentSelection.getPathCount()-1).toString();
        }
        else if (currentNode.getLevel() == 1) {
          expansion = currentSelection.getPathComponent(currentSelection.getPathCount()-1).toString();
        }
      }
      ArcanistCCG.NetManager.cardAdd(tableView.x, tableView.y, cardName, "", expansion, frontFile, "", true, 0);
/*
// //
      if (expansion.length()==0) {
        expansion = Prefs.textDat.availableImages(cardName);
      }
      if (expansion.length() > 0 && frontFile.length() > 0) {
        path = Prefs.gameloc +"/"+ expansion +"/"+ frontFile;
        if (!new File(path).exists()) {
          frontFile = "";
        }
      }
      if (expansion.length()>0 && frontFile.length()==0) {
        ArrayList picList = Prefs.textDat.availableImages(expansion, cardName);
        if (picList.size() > 0) {
          frontFile = (String)picList.get(0);
        }
      }
      if (expansion.length() > 0 && frontFile.length() > 0) {
        path = Prefs.gameloc +"/"+ expansion +"/"+ frontFile;
      }
      if (frontFile.length() == 0 || (new File(path)).exists() == false)
        path = Prefs.defaultBlankPath;

      Card dupCard = new Card(frame, cardName, "", Prefs.textDat.getSetAbbrevFromName(expansion), path, Prefs.defaultBackPath, true);
      dupCard.setId(ArcanistCCG.getNextUnusedId());
      dupCard.addToTable(tableView.x, tableView.y);
      frame.getTablePane().repaint();
// //
*/
    }
    else if (source == resetButton) {
      resetCardList();
      searchPanel.reset();
      if (cardListModel.size() > 0) cardList.setSelectedIndex(0);
    }
    else if (source == filterButton) {
      if (searchPanel.isBlank()) return;

      resultList = searchPanel.search();
      cardListModel.clear();
      resultsLabel.setText("Results: "+ resultList.size());

      //JRE 1.5 made this slooow
      //for (int i=0; i < resultList.size(); i++) {
      //  cardListModel.addElement(resultList.get(i));
      //}
      cardListModel.addAll(resultList.toArray());
      cardListFiltered = true;
    }
    else if (source == viewtxtButton) {
      showTxt();
      System.gc();
    }
    else if (source == viewpicButton) {
      showPic();
      System.gc();
    }
    else if (source == addButton) {
      if (cardList.isSelectionEmpty() == true || cardListModel.size()==0) return;
      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();

      String amount = "1", frontName = "", set = "";

      TreePath currentSelection = treeList.getSelectionPath();
      if (currentSelection != null) {
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(currentSelection.getLastPathComponent());
        MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
        if (currentNode.getLevel() > 1) {
          set = currentSelection.getPathComponent(currentSelection.getPathCount()-2).toString();
          frontName = currentSelection.getPathComponent(currentSelection.getPathCount()-1).toString();
        }
        else if (currentNode.getLevel() == 1) {
          set = currentSelection.getPathComponent(currentSelection.getPathCount()-1).toString();
        }
      }

      dontReset = true;
      if (cardList.getSelectedIndex() != -1) {
        int rowNum = deckTable.getSelectedRow();
        if (rowNum != -1) {
          if ( ((String)deckTable.getValueAt(rowNum, 1)).equals(cardListModel.elementAt(cardList.getSelectedIndex()).toString()) && ((String)deckTable.getValueAt(rowNum, 3)).equals(frontName) && ((String)deckTable.getValueAt(rowNum, 2)).equals(set)) {
            try {                                          //Increment card amount
              int newAmt = Integer.parseInt( ((String)deckTable.getValueAt(rowNum, 0)) )+1;
              deckTable.setValueAt(  (Object)new String(Integer.toString(newAmt)), rowNum, 0);
            }
            catch (NumberFormatException exception) {
            }
          }
          else {                                           //Insert new row
            deckTableModel.insertRow(rowNum+1, new String[]{amount, cardListModel.elementAt(cardList.getSelectedIndex()).toString(), set, frontName, "", ""});
            deckTable.changeSelection(rowNum+1, 0, false, false);
          }
        }
        else {                                             //Add to end
          deckTableModel.addRow(new String[]{amount, cardListModel.elementAt(cardList.getSelectedIndex()).toString(), set, frontName, "", ""});
          deckTable.changeSelection(deckTable.getRowCount()-1, 0, false, false);
        }
      }
      dontReset = false;
      updateDeckCount();
    }
    else if (source == remButton) {
      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();

      int currentRow = deckTable.getSelectedRow();
      if (currentRow != -1) {
        deckTableModel.removeRow(currentRow);
        if (currentRow < deckTable.getRowCount())
          deckTable.changeSelection(currentRow, 0, false, false);
        else if (deckTable.getRowCount() > 0)
          deckTable.changeSelection(deckTable.getRowCount()-1, 0, false, false);
        updateDeckCount();
      }
    }
    else if (source == upButton) {
      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();

      int currentRow = deckTable.getSelectedRow();
      if (currentRow > 0) {
        deckTableModel.moveRow(currentRow, currentRow, currentRow-1);
        deckTable.changeSelection(currentRow-1, 0, false, false);
      }
    }
    else if (source == downButton) {
      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();

      int currentRow = deckTable.getSelectedRow();
      if (currentRow < deckTable.getRowCount()-1) {
        deckTableModel.moveRow(currentRow, currentRow, currentRow+1);
        deckTable.changeSelection(currentRow+1, 0, false, false);
      }
    }
    else if (source == blankButton) {
      if (deckTable.getCellEditor() != null) deckTable.getCellEditor().cancelCellEditing();
      int rowNum = deckTable.getSelectedRow();

      dontReset = true;
      if (rowNum != -1) {                                  //Insert new row
        deckTableModel.insertRow(rowNum+1, new String[]{"", "", "", "", "", ""});
        deckTable.changeSelection(rowNum+1, 0, false, false);
      }
      else {                                               //Add to end
        deckTableModel.addRow(new String[]{"", "", "", "", "", ""});
        deckTable.changeSelection(deckTable.getRowCount()-1, 0, false, false);
      }
      dontReset = false;
    }
  }


  /**
   * Update the reprint tree to sync with the card list.
   */
  private void updateSets() {
    treeModel = new DefaultMutableTreeNode("Images");
    if (cardList.isSelectionEmpty() == true || cardListModel.size() == 0) {
      treeList.setModel(new DefaultTreeModel(treeModel));
      return;
    }

    String cardName = (String)cardListModel.elementAt(cardList.getSelectedIndex());
    List<String> textSetNameList = Prefs.textDat.getCardTextSets(cardName);
    List<String> noImageSetNameList = new ArrayList<String>(textSetNameList);

    String[] fullSetNames = Prefs.textDat.getTextSetNames();

    for (int i=0; i < fullSetNames.length; i++) {
      List<String> availableImages = Prefs.textDat.availableImages(fullSetNames[i], cardName);

      if (availableImages.size() > 0) {
        DefaultMutableTreeNode tmpNode = new DefaultMutableTreeNode(fullSetNames[i]);
        for (int j=0; j < availableImages.size(); j++) {
          tmpNode.add(new DefaultMutableTreeNode(availableImages.get(j)));
        }
        treeModel.add(tmpNode);
        noImageSetNameList.remove(fullSetNames[i]);
      }
    }

    for (int i=0; i < noImageSetNameList.size(); i++) {
      treeModel.add(new DefaultMutableTreeNode(noImageSetNameList.get(i)));
    }

    treeList.setModel(new DefaultTreeModel(treeModel));

    // Expand the nodes
    for (int i=0; i < treeModel.getRoot().getChildCount(); i++) {
      if (treeModel.getRoot().getChildAt(i).getChildCount() > 0) {
        treeList.expandPath(new TreePath( new Object[] {treeModel.getRoot(), treeModel.getRoot().getChildAt(i)} ));
      }
    }

    // Select a recent reprint
    for (int i=treeModel.getRoot().getChildCount()-1; i >= 0; i--) {
      DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)treeModel.getRoot().getChildAt(i);
      if (currentNode.getChildCount() > 0 && textSetNameList.contains(currentNode.getUserObject())) {
        int row = treeList.getRowForPath(new TreePath( new Object[] {treeModel.getRoot(), currentNode} ));
        treeList.scrollRowToVisible(row);
        treeList.scrollRowToVisible(row+1);
        treeList.getSelectionModel().setSelectionPath(new TreePath( new Object[] {treeModel.getRoot(), currentNode, currentNode.getChildAt(0)} ));
        break;
      }
    }
  }


  /**
   * Update the reprint tree to sync with the deck table.
   */
  @SuppressWarnings("unchecked")
  private void updateSetsFromDeck() {
    String set = (String) deckTable.getValueAt(deckTable.getSelectedRow(), 2);
    String frontFile = (String) deckTable.getValueAt(deckTable.getSelectedRow(), 3);

    for (int i=0; i < treeModel.getRoot().getChildCount(); i++) {
      DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)treeModel.getRoot().getChildAt(i);
      if ( ((String)currentNode.getUserObject()).equals(set)) {
        if (frontFile.length()>0 && currentNode.getChildCount() > 0) {
          for (int j=0; j < currentNode.getChildCount(); j++) {
            if ( ((String)((DefaultMutableTreeNode)currentNode.getChildAt(j)).getUserObject()).equals(frontFile) ) {
              int row = treeList.getRowForPath(new TreePath( new Object[] {treeModel.getRoot(), currentNode} ));
              treeList.scrollRowToVisible(row);
              treeList.scrollRowToVisible(row+j+1);
              treeList.getSelectionModel().setSelectionPath(new TreePath( new Object[] {treeModel.getRoot(), currentNode, currentNode.getChildAt(j)} ));
              break;
            }
          }
        }
        else {
          treeList.scrollRowToVisible(treeList.getRowForPath(new TreePath( new Object[] {treeModel.getRoot(), currentNode} )));
          treeList.getSelectionModel().setSelectionPath(new TreePath( new Object[] {treeModel.getRoot(), currentNode} ));
        }
        break;
      }
    }
  }


  /**
   * Revert to a complete cardlist.
   */
  private void resetCardList() {
    resultList.clear();
    cardListModel.clear();
    resultsLabel.setText("Results: "+ fullCardList.size());
    //JRE 1.5 made this slooow
    //for (int i=0; i <= fullCardList.size()-1; i++) {
    //  cardListModel.addElement((String)fullCardList.get(i));
    //}
    cardListModel.addAll(fullCardList.toArray());
    cardListFiltered = false;
  }


  /**
   * Recalculates deck size.
   */
  @SuppressWarnings("unchecked")
  private void updateDeckCount() {
    int totalcount = 0;
    int count = 0;
    String tipString = "";

    for (int i=0; i <= deckTableModel.getRowCount()-1; i++) {
      if (((String)deckTable.getValueAt(i, 0)).equals(";")) {
        if (tipString.length()==0)
          tipString = "Primary:";
        tipString += count +"  "+ ((String)deckTable.getValueAt(i, 1)) +":";
        count = 0;
      }
      else {
        try {                                          //Increment card amount
          int increment = Integer.parseInt( ((String)deckTable.getValueAt(i, 0)) );
          count += increment;
          totalcount += increment;
        }
        catch (NumberFormatException exception) {}
      }
    }
    if (tipString.length() > 0) {
      tipString += count;
      deckCountLabel.setToolTipText(tipString);
    }
    else {
      deckCountLabel.setToolTipText(null);
    }
    deckCountLabel.setText("Deck: "+ totalcount);
  }


  /**
   * Rebuilds the card list from the dat in memory.
   */
  private void rebuildCardList() {
    fullCardList.clear();
    fullCardList.addAll(Prefs.textDat.getAllCardNames());
    Collections.sort(fullCardList);
    resetCardList();
  }


  /**
   * Resyncs this DeckBuilder with the dat.
   * Call this after a new dat has been parsed.
   */
  public void datChanged() {
    searchPanel.datChanged();
    rebuildCardList();
    if (cardListModel.size() > 0) cardList.setSelectedIndex(0);
  }


  /**
   * Display the currently selected reprint in the JumboView.
   * Or the most recent fo nothing was selected.
   */
  private void showTxt() {
    if (cardList.isSelectionEmpty() == true || cardListModel.size() == 0) return;
    if (frame.getJumboFrame() == null) frame.createJumboFrame();

    String cardName = (String)cardListModel.elementAt(cardList.getSelectedIndex());

    // Find the expansion
    String setName = ""; String setAbbrev = "";
    TreePath currentSelection = treeList.getSelectionPath();
    if (currentSelection != null) {
      DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(currentSelection.getLastPathComponent());
      MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
      if (currentNode.getLevel() > 1) {
        setName = currentSelection.getPathComponent(currentSelection.getPathCount()-2).toString();
      }
      else if (currentNode.getLevel() == 1) {
        setName = currentSelection.getPathComponent(currentSelection.getPathCount()-1).toString();
      }
      setAbbrev = Prefs.textDat.getSetAbbrevFromName(setName);
    }

    frame.getJumboFrame().updateJumboText(new CardTextPanel(cardName, setAbbrev, false));
  }

  /**
   * Display the currently selected reprint in the JumboView.
   * Or the first contained image if a set was selected.
   *
   * @return true if successful, false otherwise
   */
  @SuppressWarnings("unchecked")
  private boolean showPic() {
    if (cardList.isSelectionEmpty() == true || cardListModel.size() == 0) return false;
    if (frame.getJumboFrame() == null) frame.createJumboFrame();
    boolean result = false;

    TreePath currentSelection = treeList.getSelectionPath();
    if (currentSelection != null) {
      DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(currentSelection.getLastPathComponent());
      MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
      if (currentNode.getLevel() > 1) {
        String filePath = Prefs.gameloc;
        for (int i=1; i < currentSelection.getPathCount(); i++)
          filePath += "/"+ currentSelection.getPathComponent(i).toString();

        frame.getJumboFrame().updateJumboImage(filePath);
        result = true;
      }
      else if (currentNode.getLevel() == 1) {
        String cardName = (String)cardListModel.elementAt(cardList.getSelectedIndex());
        String setName = currentSelection.getPathComponent(currentSelection.getPathCount()-1).toString();
        String imageFileName = null;
        List<String> availableImages = Prefs.textDat.availableImages(setName, cardName);
        if (availableImages.size() > 0) imageFileName = availableImages.get(0);
        if (imageFileName != null) {
          String filePath = Prefs.gameloc +"/"+ setName +"/"+ imageFileName;
          frame.getJumboFrame().updateJumboImage(filePath);
          result = true;
        }
      }
    }
    return result;
  }


  /**
   * Reads a deck from a file.
   * Any existing deck will be cleared.
   */
  private void loadDeck(File file) {
    deckTableModel.setRowCount(0);

    BufferedReader inFile = null;
    try {
      InputStreamReader fr = new InputStreamReader(new FileInputStream(file), "UTF-8");
      inFile = new BufferedReader(fr);

      String tmpLine = inFile.readLine();
      for (int i=0; tmpLine != null; i++) {
        deckTableModel.addRow(new String[]{"","","","","",""});
        String[] chunks = tmpLine.split("\t");

        if (chunks.length > 0) {
          if (chunks[0].matches("\\d{"+ chunks[0].length() +"}+")) {
            deckTable.setValueAt(chunks[0], i, 0);
          }
          else if (chunks[0].startsWith("//") || chunks[0].startsWith(";") || chunks[0].startsWith(">") || chunks[0].startsWith("<")) {
            deckTable.setValueAt(chunks[0], i, 0);
          }
          else {
            deckTable.setValueAt("1", i, 0);
          }
        }
        if (chunks.length > 1) deckTable.setValueAt(chunks[1], i, 1);
        if (chunks.length > 2) deckTable.setValueAt(chunks[2], i, 2);
        if (chunks.length > 3) deckTable.setValueAt(chunks[3], i, 3);
        if (chunks.length > 4) deckTable.setValueAt(chunks[4], i, 4);
        if (chunks.length > 5) deckTable.setValueAt(chunks[5], i, 5);

        tmpLine = inFile.readLine();
      }
    }
    catch (FileNotFoundException exception) {
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Selected file does not exist.", "Error", JOptionPane.PLAIN_MESSAGE);
    }
    catch (IOException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't load deck.");
    }
    finally {
      try {if (inFile != null) inFile.close();}
      catch (Exception e) {}
    }

    updateDeckCount();
  }

  /**
   * Writes the current deck to a file.
   *
   * @param file the destination
   */
  private void saveDeck(File file) {
    StringBuffer buf = new StringBuffer();

    int rowCount = deckTable.getRowCount();
    for (int i=0; i < rowCount; i++) {
      String tmpLine = (String)deckTable.getValueAt(i, 0);
        buf.append(tmpLine);

        if (tmpLine.startsWith("//") || tmpLine.startsWith(";") || tmpLine.startsWith(">") || tmpLine.startsWith("<")) {
          tmpLine = (String)deckTable.getValueAt(i, 1);
          if (tmpLine.length() > 0) {
            buf.append("\t"); buf.append(tmpLine);
          }
        }
        else {
          tmpLine = (String)deckTable.getValueAt(i, 1);
          buf.append("\t"); buf.append(tmpLine);
          tmpLine = (String)deckTable.getValueAt(i, 2);
          buf.append("\t"); buf.append(tmpLine);
          tmpLine = (String)deckTable.getValueAt(i, 3);
          buf.append("\t"); buf.append(tmpLine);
          tmpLine = (String)deckTable.getValueAt(i, 4);
          if (tmpLine.length() > 0) {
            buf.append("\t"); buf.append(tmpLine);
            tmpLine = (String)deckTable.getValueAt(i, 5);
            if (tmpLine.length() > 0) {
              buf.append("\t"); buf.append(tmpLine);
            }
          }
        }
        buf.append("\r\n");
      }

    BufferedWriter outFile = null;
    try {
      OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
      outFile = new BufferedWriter(fw);

      outFile.write(buf.toString());

      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Successfully saved "+ file.getName(), "Deck Saved", JOptionPane.PLAIN_MESSAGE);
    }
    catch (FileNotFoundException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't save deck.");
    }
    catch (IOException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't save deck.");
    }
    finally {
      try {if (outFile != null) outFile.close();}
      catch (Exception e) {}
    }
  }

  /**
   * Reads a deck from a LackeyCCG format file.
   * Any existing deck will be cleared.
   */
  private void importLackeyDeck(File file) {
    deckTableModel.setRowCount(0);

    BufferedReader inFile = null;
    try {
      InputStreamReader fr = new InputStreamReader(new FileInputStream(file), "UTF-8");
      inFile = new BufferedReader(fr);

      String tmpLine = inFile.readLine();
      for (int i=0; tmpLine != null; i++) {
        if (tmpLine.endsWith(":")) {
          tmpLine = tmpLine.substring(0, tmpLine.length()-1);
          deckTableModel.addRow(new String[]{"//","","","","",""});
          deckTableModel.addRow(new String[]{";",tmpLine,"","","",""});
        }
        else if (tmpLine.matches("^[0-9]+\t[^\t]+$")) {
          String count = tmpLine.substring(0, tmpLine.indexOf("\t"));
          tmpLine = tmpLine.substring(tmpLine.indexOf("\t")+1);
          tmpLine = Prefs.textDat.sanitizeName(tmpLine);
          if (!Prefs.textDat.cardExists(tmpLine)) count = "//"+ count;
          deckTableModel.addRow(new String[]{count,tmpLine,"","","",""});
        }
        tmpLine = inFile.readLine();
      }
    }
    catch (IndexOutOfBoundsException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't import LackeyCCG deck.");
    }
    catch (FileNotFoundException exception) {
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Selected file does not exist.", "Error", JOptionPane.PLAIN_MESSAGE);
    }
    catch (IOException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't import LackeyCCG deck.");
    }
    finally {
      try {if (inFile != null) inFile.close();}
      catch (Exception e) {}
    }

    updateDeckCount();
  }

  /**
   * Reads a deck from an Apprentice format file.
   * Any existing deck will be cleared.
   */
  private void importApprenticeDeck(File file) {
    deckTableModel.setRowCount(0);

    List<String> tempSB = new ArrayList<String>();
    BufferedReader inFile = null;
    try {
      InputStreamReader fr = new InputStreamReader(new FileInputStream(file), "UTF-8");
      inFile = new BufferedReader(fr);

      String tmpLine = inFile.readLine();
      for (int i=0; tmpLine != null; i++) {
        if (tmpLine.startsWith("//")) {
          tmpLine = tmpLine.substring(8, tmpLine.length());
          deckTableModel.addRow(new String[]{"//",tmpLine,"","","",""});
        }
        else if (tmpLine.startsWith(" ")) {
          tmpLine = tmpLine.substring(8, tmpLine.length());
          String count = tmpLine.substring(0, tmpLine.indexOf(" "));
          tmpLine = tmpLine.substring(tmpLine.indexOf(" ")+1);
          tmpLine = Prefs.textDat.sanitizeName(tmpLine);
          deckTableModel.addRow(new String[]{count,tmpLine,"","","",""});
        }
        else if (tmpLine.startsWith("SB:")) {
          tmpLine = tmpLine.substring(5);
          tmpLine = Prefs.textDat.sanitizeName(tmpLine);
          tempSB.add(tmpLine);
        }
        tmpLine = inFile.readLine();
      }
      if (tempSB.size() > 0)
        deckTableModel.addRow(new String[]{"//","","","","",""});
        deckTableModel.addRow(new String[]{";","Sideboard","","","",""});
      for (int i=0; i < tempSB.size(); i++) {
        tmpLine = tempSB.get(i);
        deckTableModel.addRow(new String[]{tmpLine.substring(0, tmpLine.indexOf(" ")),tmpLine.substring(tmpLine.indexOf(" ")+1, tmpLine.length()),"","","",""});
      }
    }
    catch (IndexOutOfBoundsException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't import Apprentice deck.");
    }
    catch (FileNotFoundException exception) {
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Selected file does not exist.", "Error", JOptionPane.PLAIN_MESSAGE);
    }
    catch (IOException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't import Apprentice deck.");
    }
    finally {
      try {if (inFile != null) inFile.close();}
      catch (Exception e) {}
    }

    updateDeckCount();
  }

  /**
   * Writes the current deck to an Apprentice format file.
   * All nested decks will be treated as a combined sideboard.
   *
   * @param file the destination
   */
  @SuppressWarnings("unchecked")
  private void exportApprenticeDeck(File file) {
    StringBuffer buf = new StringBuffer();
    boolean inSideboard = false;

    int rowCount = deckTable.getRowCount();
    for (int i=0; i < rowCount; i++) {
      String tmpLine = (String)deckTable.getValueAt(i, 0);

      if (tmpLine.startsWith("//") && i == 0) {
        buf.append("//NAME: ");
        buf.append((String)deckTable.getValueAt(i, 1));
        buf.append("\r\n");
      }
      else if (tmpLine.startsWith(";")) {
        inSideboard = true;
      }
      else if (tmpLine.startsWith("//") || tmpLine.startsWith(">") || tmpLine.startsWith("<")) {
      }
      else {
        if (inSideboard == true) buf.append("SB:  ");
        else buf.append("        ");

        buf.append((String)deckTable.getValueAt(i, 0));
        buf.append(" ");
        buf.append((String)deckTable.getValueAt(i, 1));
        buf.append("\r\n");
      }
    }

    BufferedWriter outFile = null;
    try {
      OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
      outFile = new BufferedWriter(fw);

      outFile.write(buf.toString());

      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Successfully exported "+ file.getName(), "Deck Saved", JOptionPane.PLAIN_MESSAGE);
    }
    catch (FileNotFoundException exception) {
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Selected file does not exist.", "Error", JOptionPane.PLAIN_MESSAGE);
    }
    catch (IOException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't export Apprentice deck.");
    }
    finally {
      try {if (outFile != null) outFile.close();}
      catch (Exception e) {}
    }
  }


  /**
   * Writes a spoiler of the deck to a file.
   * This method is not ready for use.
   */
  @SuppressWarnings("unchecked")
  private void exportSpoiler(File file) {
    BufferedWriter outFile = null;
    try {
      OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
      outFile = new BufferedWriter(fw);

      int totalFields = Prefs.textDat.getFieldCount();

      int rowCount = deckTable.getRowCount();
      for (int i=0; i < rowCount; i++) {
        String prefix = (String) deckTable.getValueAt(i, 0);

        if (prefix.startsWith("//")) {
          outFile.write("//"+ (String)deckTable.getValueAt(i, 1) +"\r\n");
        }
        else if (prefix.startsWith(";")) {
          outFile.write("\r\n\r\n\r\n"+ (String)deckTable.getValueAt(i, 1) +"\r\n\r\n");
        }
        else if (prefix.startsWith("//") || prefix.startsWith(">") || prefix.startsWith("<")) {
        }
        else {
          String card = (String)deckTable.getValueAt(i, 1);
          if (card.length() == 0) continue;

          String expansion =  Prefs.textDat.getSetAbbrevFromName( (String)deckTable.getValueAt(i, 2) );
          int mapIndex = Prefs.textDat.findCard(card, expansion);

          outFile.write("Name:\t"+ card +" {x"+ prefix +"}\r\n");

          if (mapIndex != -1) {
            for (int j=1; j < totalFields && j < Prefs.cardTextArray.size() ; j++) {
              String field = Prefs.textDat.getFieldName(j);
              String value = (String)Prefs.textDat.elementList.get(mapIndex+j);
              value = value.replaceAll("\\\\n", "\r\n");
              value = value.replaceAll("\\r\\n", "\r\n\t");
              outFile.write(field +":\t"+ value +"\r\n");
            }
          }
          else {
            outFile.write("*** NO TEXT AVAILABLE ***\r\n");
          }
          outFile.write("\r\n");
        }
      }

      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Successfully exported "+ file.getName(), "Deck Saved", JOptionPane.PLAIN_MESSAGE);
    }
    catch (FileNotFoundException exception) {
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Selected file does not exist.", "Error", JOptionPane.PLAIN_MESSAGE);
    }
    catch (IOException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't export Apprentice deck.");
    }
    finally {
      try {if (outFile != null) outFile.close();}
      catch (Exception e) {}
    }
  }
}