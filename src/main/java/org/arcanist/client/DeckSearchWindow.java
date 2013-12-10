package org.arcanist.client;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * This searches the contents of a deck in a popup window.
 */
public class DeckSearchWindow extends NerfableInternalFrame implements ActionListener, Dependent, ListDataListener {

  private ArcanistCCGFrame frame = null;

  private DeckSearchWindow pronoun = this;
  private Deck deck = null;

  private List<Object[]> resultList = new ArrayList<Object[]>(0);
  private DeckSearchResultComparator deckComparator = new DeckSearchResultComparator();
  private int currentSort = DeckSearchResultComparator.STACK;

  private boolean cardListFiltered = false;
  private int lastDeckSize = 0;
  private int awaitingDeckChangeCount = 0;

  private DefaultTableModel deckTableModel = new DefaultTableModel(new String[]{"Nth", "Name"}, 0) {
    @Override
    public boolean isCellEditable(int row, int column) {
      return false;
    }
  };
  private JTable deckTable = null;

  private SearchPanel searchPanel = new SearchPanel();

  private JLabel resultsLabel = new JLabel("");
  private JButton sortButton = null;
  private JButton removeButton = null;
  private JButton newDeckButton = null;
  private JButton drawButton = null;
  private JButton resetButton = null;
  private JButton filterButton = null;


  public DeckSearchWindow(ArcanistCCGFrame f, Deck inDeck) {
    super("DeckSearch",
      true, //resizable
      true, //closable
      true, //maximizable
      false); //iconifiable
    JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    deck = inDeck;
    frame = f;
    lastDeckSize = deck.getCardCount();

    deck.addDependent((Dependent)pronoun);
    deck.addListDataListener((ListDataListener)pronoun);
    // //deck.setInUse(true);
    ArcanistCCG.NetManager.deckUse(deck.arrayID());
    frame.getTablePane().repaint();

    this.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosing(InternalFrameEvent e) {
        if (deck.existsOnTable()) {
          deck.removeListDataListener((ListDataListener)pronoun);
          deck.removeDependent((Dependent)pronoun);
          // //deck.setInUse(false);
          ArcanistCCG.NetManager.deckUnuse(deck.arrayID());
          frame.getTablePane().repaint();
        }
      }

      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        //Break references so they can be garbage collected
        //If a model lingers, it keeps its GUI on life-support
        deckTable.setModel(new DefaultTableModel());
      }
    });

    JPanel deckPanel = new JPanel();
      GridBagLayout searchGridbag = new GridBagLayout();
      GridBagConstraints searchC = new GridBagConstraints();
        searchC.fill = GridBagConstraints.BOTH;
        searchC.weightx = 1;
        searchC.weighty = 0;
        searchC.gridwidth = GridBagConstraints.REMAINDER;   //Whole Row
      deckPanel.setLayout(searchGridbag);

      JPanel searchOptionsPanel = new JPanel();
        sortButton = new JButton("Stack");
          sortButton.setMargin(new Insets(0,2,0,2));
          sortButton.setFocusable(false);
          sortButton.setToolTipText("Sort the list");
          searchOptionsPanel.add(sortButton);
        searchOptionsPanel.add(Box.createHorizontalStrut(5));
        // See top: resultsLabel (updateResultsLabel accesses it)
          searchOptionsPanel.add(resultsLabel);
        searchOptionsPanel.add(Box.createHorizontalStrut(5));
        removeButton = new JButton("Remove");
          removeButton.setMargin(new Insets(0,2,0,2));
          removeButton.setFocusable(false);
          removeButton.setToolTipText("Remove selected card(s)");
          searchOptionsPanel.add(removeButton);
        newDeckButton = new JButton("NewDeck");
          newDeckButton.setMargin(new Insets(0,2,0,2));
          newDeckButton.setFocusable(false);
          newDeckButton.setToolTipText("Add a deck with selected card(s)");
          searchOptionsPanel.add(newDeckButton);
        drawButton = new JButton("Draw");
          drawButton.setMargin(new Insets(0,2,0,2));
          drawButton.setFocusable(false);
          drawButton.setToolTipText("Draw selected card(s)");
          searchOptionsPanel.add(drawButton);
        searchOptionsPanel.add(Box.createHorizontalStrut(8));
        resetButton = new JButton("Reset");
          resetButton.setMargin(new Insets(0,2,0,2));
          resetButton.setFocusable(false);
          resetButton.setToolTipText("Show all cards");
          searchOptionsPanel.add(resetButton);
        filterButton = new JButton("Filter");
          filterButton.setMargin(new Insets(0,2,0,2));
          filterButton.setFocusable(false);
          filterButton.setToolTipText("Filter search criteria");
          searchOptionsPanel.add(filterButton);
        deckPanel.add(searchOptionsPanel, searchC);

      searchC.gridy = 1;
      searchC.weighty = 1;

      deckTable = new JTable(deckTableModel);
        deckTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        deckTable.getTableHeader().setReorderingAllowed(false);
        deckTable.getColumn("Nth").setPreferredWidth(30);
        deckTable.getColumn("Nth").setMaxWidth(30);
        deckTable.getColumn("Name").setPreferredWidth(162);
        deckTable.addNotify();
      JScrollPane deckListScrollPane = new JScrollPane(deckTable);
        deckPanel.add(deckListScrollPane, searchC);

    pane.setTopComponent(searchPanel);
    pane.setBottomComponent(deckPanel);
    pane.setDividerLocation(searchPanel.getPreferredSize().height);

    reset();

    deckTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == true) return;
        updateResultsLabel();
        if (deckTable.getRowCount() == 0 || deckTable.getSelectedRowCount() != 1) return;
        if ((frame.getJumboFrame() != null && frame.getJumboFrame().isJumboActive()) == false) return;

        int tmpIndex = deck.getCardCount()-((Integer)deckTableModel.getValueAt(deckTable.getSelectedRow(), 0)).intValue();
        try {
          Card tmpCard = deck.getCard(tmpIndex);
          if (tmpCard.isText() == true || frame.getJumboFrame().getPreferText())
            frame.getJumboFrame().updateJumboText(new CardTextPanel(tmpCard.getFrontName(), tmpCard.getSetAbbrev(), false));
          else
            frame.getJumboFrame().updateJumboImage(tmpCard.getFrontPath());
        } catch (IndexOutOfBoundsException exception) {
          exception.printStackTrace();
          pronoun.doDefaultCloseAction();
          pronoun.setVisible(false);
          pronoun.dispose();
        }
      }
    });

    sortButton.addActionListener(this);
    removeButton.addActionListener(this);
    newDeckButton.addActionListener(this);
    drawButton.addActionListener(this);
    resetButton.addActionListener(this);
    filterButton.addActionListener(this);


    Action searchAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        performSearch();
      }
    };
    searchPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("released ENTER"), "startSearch");
    searchPanel.getActionMap().put("startSearch", searchAction);


    this.setContentPane(pane);
    frame.getDesktop().add(this);

    if (frame.getTableFrame().getSize().width/2-250 > 0 && frame.getTableFrame().getSize().height/2-175 > 0)
      this.reshape(frame.getTableFrame().getSize().width/2-250, frame.getTableFrame().getSize().height/2-175, 500, 350);
    else this.reshape(0, 0, 500, 350);

    this.show();
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == sortButton) {
      if ( sortButton.getText().equals("Alpha") ) {
        sortButton.setText("Stack");
        currentSort = DeckSearchResultComparator.STACK;
        deckComparator.setMethod(currentSort);
      }
      else {
        sortButton.setText("Alpha");
        currentSort = DeckSearchResultComparator.ALPHA;
        deckComparator.setMethod(currentSort);
      }
      Collections.sort(resultList, deckComparator);

      deckTableModel.setRowCount(0);
      for(int i=0; i < resultList.size(); i++) {
        deckTableModel.addRow(resultList.get(i));
      }

      //The label shouldn't change, but this'll let me know if something broke
      updateResultsLabel();
    }
    else if (source == removeButton) {
      if (deckTable.getSelectedRowCount() == 0) return;
      if (awaitingDeckChangeCount > 0) return;
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Are you SURE you want to remove the cards?", "Remove?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        return;

      int[] selection = deckTable.getSelectedRows();
      int[] deckIndeces = new int[selection.length];
      for(int i=0; i < selection.length; i++) {
        int fromTopOneBasedIndex = ((Integer)resultList.get(selection[i])[0]).intValue();
        deckIndeces[i] = deck.getCardCount()-1 - (fromTopOneBasedIndex-1);
      }

      awaitingDeckChangeCount = selection.length;
      if (!isNerfed()) setNerfed(true);

      if (selection.length > 1) deckTable.clearSelection();

      Arrays.sort(deckIndeces);
      for(int i=deckIndeces.length-1; i >= 0; i--) {
        ArcanistCCG.NetManager.deckRemoveCard(deck.arrayID(), deckIndeces[i]);
      }
    }
    else if (source == newDeckButton) {
      if (deckTable.getSelectedRowCount() == 0) return;
      if (awaitingDeckChangeCount > 0) return;

      StringBuffer newDeck = new StringBuffer();
      String tmp;

      int[] selection = deckTable.getSelectedRows();
      int[] deckIndeces = new int[selection.length];
      for(int i=0; i < selection.length; i++) {
        int fromTopOneBasedIndex = ((Integer)resultList.get(selection[i])[0]).intValue();
        deckIndeces[i] = deck.getCardCount()-1 - (fromTopOneBasedIndex-1);
      }

      // Collect the cards in visible order,
      //   remove them in reverse stack order,
      //   then add the new deck
      for(int i=0; i < deckIndeces.length; i++) {
        Card tmpCard = deck.getCard(deckIndeces[i]);
        tmp = tmpCard.getFrontName();
        if (tmp != null) {
          newDeck.append("1");
          newDeck.append("\t").append(tmp);
          newDeck.append("\t").append(Prefs.textDat.getSetNameFromAbbrev(tmpCard.getSetAbbrev()));
          newDeck.append("\t").append(tmpCard.getFrontFile());
          newDeck.append("\t").append(tmpCard.getBackName());
          newDeck.append("\t").append(tmpCard.getBackFile());
          newDeck.append("\n");
        }
      }

      awaitingDeckChangeCount = selection.length;
      if (!isNerfed()) setNerfed(true);

      if (selection.length > 1) deckTable.clearSelection();

      Arrays.sort(deckIndeces);
      for(int i=deckIndeces.length-1; i >= 0; i--) {
        ArcanistCCG.NetManager.deckRemoveCard(deck.arrayID(), deckIndeces[i]);
      }

      tmp = newDeck.toString().replaceAll("\\n","|") +"|";

      Rectangle tableView = frame.getTableView();

      ArcanistCCG.NetManager.deckAdd(tableView.x, tableView.y, tmp);
      //DeckParser newDeckParser = new DeckParser(frame, true, tmp, tableView.x, tableView.y);
      //newDeckParser.startThread();
    }
    else if (source == drawButton) {
      if (deckTable.getSelectedRowCount() == 0) return;
      if (awaitingDeckChangeCount > 0) return;

      int[] selection = deckTable.getSelectedRows();
      int[] deckIndeces = new int[selection.length];
      for(int i=0; i < selection.length; i++) {
        int fromTopOneBasedIndex = ((Integer)resultList.get(selection[i])[0]).intValue();
        deckIndeces[i] = deck.getCardCount()-1 - (fromTopOneBasedIndex-1);
      }

      awaitingDeckChangeCount = selection.length;
      if (!isNerfed()) setNerfed(true);

      Arrays.sort(deckIndeces);
      for(int i=deckIndeces.length-1; i >= 0; i--) {
        ArcanistCCG.NetManager.deckTableCard(deck.arrayID(), deckIndeces[i], true);
      }
    }
    else if (source == resetButton) {
      reset();
      cardListFiltered = false;
      searchPanel.reset();
    }
    else if (source == filterButton) {
      performSearch();
    }
  }


  /**
   * Notifies the deck was removed.
   * Closes the window.
   */
  @Override
  public void sourceRemoved(Object source) {
    deck.removeListDataListener((ListDataListener)pronoun);
    pronoun.setVisible(false);
    pronoun.dispose();
    deck = null;
  }


  /**
   * Resets the table to deck order.
   */
  private void reset() {
    resultList.clear();
    int tmpIndex;
    for(int i=deck.getCardCount()-1; i >= 0; i--) {
      tmpIndex = i;
      resultList.add(new Object[]{new Integer(deck.getCardCount()-tmpIndex), deck.getCard(tmpIndex).getFrontName()});
    }
    Collections.sort(resultList, deckComparator);

    deckTableModel.setRowCount(0);
    for(int i=0; i < resultList.size(); i++) {
      deckTableModel.addRow(resultList.get(i));
    }
    updateResultsLabel();
  }


  /**
   * Searches the deck.
   */
  private void performSearch() {
    if (searchPanel.isBlank()) return;

    List<Integer> tmpList = searchPanel.search(deck);

    resultList.clear();
    int tmpIndex;
    for(int i=0; i < tmpList.size(); i++) {
      tmpIndex = tmpList.get(i).intValue();
      resultList.add(new Object[]{new Integer(deck.getCardCount()-tmpIndex), deck.getCard(tmpIndex).getFrontName()});
    }
    Collections.sort(resultList, deckComparator);

    deckTableModel.setRowCount(0);
    for(int i=0; i < resultList.size(); i++) {
      deckTableModel.addRow(resultList.get(i));
    }
    cardListFiltered = true;
    updateResultsLabel();
  }


  /**
   * Syncs the count label with the table of cards.
   */
  private void updateResultsLabel() {
    resultsLabel.setText(deckTable.getSelectedRowCount() +"/"+ deckTableModel.getRowCount() +"/"+ deck.getCardCount());
  }


  @Override
  public void contentsChanged(ListDataEvent e) {
    lastDeckSize = deck.getCardCount();

    int deckIndex0 = e.getIndex0();
    int deckIndex1 = e.getIndex1();
    int changedFromTopIndex0 = lastDeckSize-1 - deckIndex1;
    int changedFromTopIndex1 = lastDeckSize-1 - deckIndex0;

    // Visible table indicies are human-friendly 1-based
    int changedFromTopOneBasedIndex0 = changedFromTopIndex0 + 1;
    int changedFromTopOneBasedIndex1 = changedFromTopIndex1 + 1;

    for (int i=resultList.size()-1; i >= 0; i--) {
      Object[] resultRow = resultList.get(i);
      int fromTopOneBasedIndex = ((Integer)resultRow[0]).intValue();

      if (fromTopOneBasedIndex >= changedFromTopOneBasedIndex0 && fromTopOneBasedIndex <= changedFromTopOneBasedIndex1) {
        resultRow[1] = deck.getCard(lastDeckSize-1 - (fromTopOneBasedIndex-1)).getFrontName();
        deckTableModel.setValueAt(resultRow[1], i, 1);
      }
    }
    updateResultsLabel();

    if (--awaitingDeckChangeCount <= 0 && isNerfed()) setNerfed(false);
  }

  @Override
  public void intervalAdded(ListDataEvent e) {
    lastDeckSize = deck.getCardCount();

    int deckIndex0 = e.getIndex0();
    int deckIndex1 = e.getIndex1();
    int addedFromTopIndex0 = lastDeckSize-1 - deckIndex1;
    int addedFromTopIndex1 = lastDeckSize-1 - deckIndex0;

    // Visible table indicies are human-friendly 1-based
    int addedFromTopOneBasedIndex0 = addedFromTopIndex0 + 1;
    int addedFromTopOneBasedIndex1 = addedFromTopIndex1 + 1;
    int addedCount = addedFromTopOneBasedIndex1+1 - addedFromTopOneBasedIndex0;

    // Increment indeces greater than the added block
    for (int i=resultList.size()-1; i >= 0; i--) {
      Object[] resultRow = resultList.get(i);
      int fromTopOneBasedIndex = ((Integer)resultRow[0]).intValue();

      if (fromTopOneBasedIndex >= addedFromTopOneBasedIndex0) {
        Integer incFromTopOneBasedIndex = new Integer(fromTopOneBasedIndex+addedCount);
        for (int j=0; j < deckTableModel.getRowCount(); j++) {
          if (deckTableModel.getValueAt(j, 0).equals(resultRow[0])) {
            deckTableModel.setValueAt(incFromTopOneBasedIndex, j, 0);
            break;
          }
        }
        resultRow[0] = incFromTopOneBasedIndex;
      }
    }

    // Insert each added card after a lesser row, or the beginning
    for (int i=addedFromTopOneBasedIndex1; i >= addedFromTopOneBasedIndex0; i--) {
      Object[] addedObject = new Object[]{new Integer(i), deck.getCard(lastDeckSize-1 - (i-1)).getFrontName()};
      boolean added = false;

      for (int j=resultList.size()-1; j >= 0; j--) {
        if (deckComparator.compare(addedObject, resultList.get(j)) >= 0) {
          resultList.add(j+1, addedObject);
          deckTableModel.insertRow(j+1, addedObject);
          added = true;
          break;
        }
      }
      if (!added) {
        resultList.add(0, addedObject);
        deckTableModel.insertRow(0, addedObject);
        // If 0th was selected, JTable grows selection to include the inserted row!?
        deckTable.removeRowSelectionInterval(0, 0);
      }
    }
    updateResultsLabel();

    if (--awaitingDeckChangeCount <= 0 && isNerfed()) setNerfed(false);
  }

  @Override
  public void intervalRemoved(ListDataEvent e) {
    int prevDeckSize = lastDeckSize;  // Indeces refer to prev size
    lastDeckSize = deck.getCardCount();

    int deckIndex0 = e.getIndex0();
    int deckIndex1 = e.getIndex1();
    int doomedFromTopIndex0 = prevDeckSize-1 - deckIndex1;
    int doomedFromTopIndex1 = prevDeckSize-1 - deckIndex0;

    // Visible table indicies are human-friendly 1-based
    int doomedFromTopOneBasedIndex0 = doomedFromTopIndex0 + 1;
    int doomedFromTopOneBasedIndex1 = doomedFromTopIndex1 + 1;
    int doomedCount = doomedFromTopOneBasedIndex1+1 - doomedFromTopOneBasedIndex0;

    int[] prevSelection = deckTable.getSelectedRows();

    for (int i=resultList.size()-1; i >= 0; i--) {
      Object[] resultRow = resultList.get(i);
      int fromTopOneBasedIndex = ((Integer)resultRow[0]).intValue();

      if (fromTopOneBasedIndex > doomedFromTopOneBasedIndex1) {
        // Decrement indeces greater than the removed block
        Integer decFromTopOneBasedIndex = new Integer(fromTopOneBasedIndex-doomedCount);
        for (int j=0; j < deckTableModel.getRowCount(); j++) {
          if (deckTableModel.getValueAt(j, 0).equals(resultRow[0])) {
            deckTableModel.setValueAt(decFromTopOneBasedIndex, j, 0);
            break;
          }
        }
        resultRow[0] = decFromTopOneBasedIndex;
      }
      else if (fromTopOneBasedIndex >= doomedFromTopOneBasedIndex0 && fromTopOneBasedIndex <= doomedFromTopOneBasedIndex1) {
        // Remove the specific index
        for (int j=0; j < deckTableModel.getRowCount(); j++) {
          if (deckTableModel.getValueAt(j, 0).equals(resultRow[0])) {
            deckTableModel.removeRow(j);
            break;
          }
        }
        resultList.remove(i);
      }
    }

    // Reselect the last row or the same row
    if (prevSelection.length == 1 && deckTableModel.getRowCount() > 0) {
      prevSelection[0] = prevSelection[0] - (doomedCount-1);
      if (prevSelection[0] >= deckTableModel.getRowCount())
        deckTable.changeSelection(deckTableModel.getRowCount()-1, 0, false, false);
      else
        deckTable.changeSelection(prevSelection[0], 0, false, false);
    }

    updateResultsLabel();

    if (--awaitingDeckChangeCount <= 0 && isNerfed()) setNerfed(false);
  }


  /**
   * Resyncs this DeckSearchWindow with the dat.
   * Call this after a new dat has been parsed.
   */
  public void datChanged() {
    searchPanel.datChanged();
  }



  /**
   * Compares an Object[Integer, String] array by either element.
   * DeckSearchWindow uses it to sort its JTable.
   */
  private static class DeckSearchResultComparator implements Comparator<Object[]> {

    public static final int STACK = 0;
    public static final int ALPHA = 1;
    private int sortByIndex = STACK;


    public DeckSearchResultComparator() {}


    public void setMethod(int input) {
      if (input < 0 || input > 1) return;
      sortByIndex = input;
    }

    @Override
    public int compare(Object[] a, Object[] b) throws ClassCastException {
      try {
        if ( a.length != 2 || b.length != 2) throw new ClassCastException();
        if (sortByIndex == STACK) return ( (Integer)a[sortByIndex] ).compareTo( (Integer)b[sortByIndex] );
        else if (sortByIndex == ALPHA) return ( (String)a[sortByIndex] ).compareTo( (String)b[sortByIndex] );
        else throw new ClassCastException();
      }
      catch(ClassCastException e) {
        throw new ClassCastException("Can only compare Object[Integer, String] arrays.");
      }
    }
  }
}
