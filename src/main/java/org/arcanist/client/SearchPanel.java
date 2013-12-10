package org.arcanist.client;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Spring;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A scrollable group of text fields for searching.
 */
public class SearchPanel extends JPanel {

  private SearchFieldPanel[] fieldPanels = null;
  private JPanel filterLFields = null;
  private JPanel filterRFields = null;


  public SearchPanel() {
    super(new GridLayout(0,1));

    JPanel filterPanel = new JPanel(new FlowLayout());
      filterLFields = new JPanel();
        filterLFields.setLayout(new GridLayout(0,1));
        filterPanel.add(filterLFields);
      filterPanel.add(Box.createHorizontalStrut(1));
      filterRFields = new JPanel();
        filterRFields.setLayout(new GridLayout(0,1));
        filterPanel.add(filterRFields);

    JScrollPane filterScrollPane = new JScrollPane(filterPanel);
      filterScrollPane.setPreferredSize(new Dimension(480,95));
      filterScrollPane.getVerticalScrollBar().setUnitIncrement(10);
      filterScrollPane.setBorder(BorderFactory.createEtchedBorder());
    this.add(filterScrollPane);

    populateFieldComponents();
  }


  private void populateFieldComponents() {
    filterLFields.removeAll();
    filterRFields.removeAll();

    DatParser textDat = Prefs.textDat;
    synchronized (textDat) {
      fieldPanels = Prefs.textDat.getSearchFieldPanels();
    }

    // Calc the left half's count
    int leftCount = (fieldPanels.length+1)/2;

    // Find the max column count among nested components
    int cols = 0;
    for (int i=0; i < fieldPanels.length; i++) {
      int n = fieldPanels[i].getMinColumnCount();
      if (n > cols) cols = n;
    }

    // For each half, standardize widths for each column
    int[] minRanges = new int[] {0, leftCount};
    int[] maxRanges = new int[] {leftCount, fieldPanels.length};
    for (int r=0; r < minRanges.length; r++) {
      for (int c=0; c < cols; c++) {
        Spring width = Spring.constant(0);
        for (int i=minRanges[r]; i < maxRanges[r]; i++) {
          width = Spring.max(width, fieldPanels[i].getSpringConstraintsForColumn(c).getWidth());
        }
        width = Spring.constant(width.getValue());
        for (int i=minRanges[r]; i < maxRanges[r]; i++) {
          fieldPanels[i].getSpringConstraintsForColumn(c).setWidth(width);
        }
      }
    }

    for (int i=0; i < fieldPanels.length; i++) {
      if (i < leftCount) {
        filterLFields.add(fieldPanels[i]);
      } else {
        filterRFields.add(fieldPanels[i]);
      }
    }

    if (leftCount > 0 && fieldPanels.length % 2 != 0) {
      filterRFields.add(Box.createVerticalStrut(fieldPanels[leftCount-1].getPreferredSize().height));
    }

    revalidate();
  }


  /**
   * Resyncs this SearchPanel with the dat.
   * Call this after a new dat has been parsed.
   */
  public void datChanged() {
    populateFieldComponents();
  }


  /**
   * Resets the panel.
   */
  public void reset() {
    for (int i=0; i < fieldPanels.length; i++) {
      fieldPanels[i].reset();
    }
  }


  /**
   * Returns whether this panel is blank.
   *
   * @return true if all fields are blank, false otherwise
   */
  public boolean isBlank() {
    boolean blank = true;

    for (int i=0; i < fieldPanels.length; i++) {
      if (fieldPanels[i].isBlank() == false) {
        blank = false;
        break;
      }
    }
    return blank;
  }


  /**
   * Performs a search, against all cards in the game.
   *
   * @return a list of card names that match
   */
  public List<String> search() {
    boolean proceed;
    List<String> resultList = new ArrayList<String>();

    // Parse field regexes in advance
    for (int i=0; i < fieldPanels.length; i++) {
      fieldPanels[i].prepareForSearch();
    }

    DatParser textDat = Prefs.textDat;
    synchronized (textDat) {
      if (fieldPanels.length != textDat.getFieldCount()) return resultList;

      int lineCount = textDat.elementList.size();
      for (int i=0; i < lineCount; i+=fieldPanels.length) {
        proceed = checkCard(textDat, i);
                                                             //Add the card to results
      if (proceed == true) {
          String tmpName = textDat.elementList.get(i);
          if (resultList.contains(tmpName) == false) {
            resultList.add(tmpName);
          }
        }
      }
      Collections.sort(resultList);
    }

    return resultList;
  }

  /**
   * Performs a search, against the contents of a CardListContainer.
   *
   * @return a list of Integer indexes that match
   */
  public List<Integer> search(CardListContainer searchContainer) {
    boolean proceed;
    List<Integer> resultList = new ArrayList<Integer>();

    // Parse field regexes in advance
    for (int i=0; i < fieldPanels.length; i++) {
      fieldPanels[i].prepareForSearch();
    }

    DatParser textDat = Prefs.textDat;
    synchronized (textDat) {
      if (fieldPanels.length != textDat.getFieldCount()) return resultList;

      for (int i=searchContainer.getCardCount()-1; i >= 0; i--) {
        Card tmpCard = searchContainer.getCard(i);
        int mapIndex = textDat.findCard(tmpCard.getFrontName(), tmpCard.getSetAbbrev());
        proceed = checkCard(textDat, mapIndex);

        if (proceed == true) {                               //Add the card to results
          resultList.add(new Integer(i));
        }
      }
    }

    return resultList;
  }


  /**
   * Checks whether the non-blank search fields match a card.
   * Synchronize on the DatParser first!
   *
   * @param i index of a card name within the DatParser's elementList
   * @return true if the card matches, false otherwise
   */
  private boolean checkCard(DatParser textDat, int i) {
    if (i < 0) return false;

    for (int j=0; j < fieldPanels.length && i < textDat.elementList.size(); j++) {
      if (fieldPanels[j].isBlank() == false) {
        if ( !fieldPanels[j].matches( ((String)textDat.elementList.get(i+j)) )  ) {
          return false;
        }
      }
    }
    return true;
  }
}
