package org.arcanist.client;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A keyword/regex text search field.
 */
public class TextSearchFieldPanel extends SearchFieldPanel {

  public static final int MIN_COLUMNS = 3;

  private String name = "";
  private JLabel nameLbl = null;
  private JTextField textField = null;
  private RegexToggleButton typeBtn = null;

  private Pattern preppedPattern = null;
  private SearchQuery preppedQuery = null;

  private Component[] componentsByColumn = null;


  public TextSearchFieldPanel(String name, int columns) {
    super();
    this.name = name;
    if (columns < MIN_COLUMNS) throw new IllegalArgumentException(this.getClass().getName() +" requires at least "+ MIN_COLUMNS +" columns");

    //this.setBorder(BorderFactory.createEtchedBorder(1));

    SpringLayout layout = new SpringLayout();
    this.setLayout(layout);

    componentsByColumn = new Component[columns];
    int currentCol = 0;
    nameLbl = new JLabel(this.name);
      componentsByColumn[currentCol++] = nameLbl;
      this.add(nameLbl);
    JPanel textHolder = new JPanel(new GridBagLayout());
      GridBagConstraints gridC = new GridBagConstraints();
        gridC.fill = GridBagConstraints.HORIZONTAL;
        gridC.weightx = 1.0;
      textField = new JTextField(10);
        textHolder.add(textField, gridC);
      componentsByColumn[currentCol++] = textHolder;
      this.add(textHolder);
    JPanel typeHolder = new JPanel(new GridBagLayout());
      typeBtn = new RegexToggleButton();
        typeHolder.add(typeBtn);
      componentsByColumn[currentCol++] = typeHolder;
      this.add(typeHolder);
    for (int i=currentCol; i < columns; i++) {
      Component tmpComp = Box.createHorizontalStrut(0);
      componentsByColumn[i] = tmpComp;
      this.add(tmpComp);
    }

    // Find the tallest component and use that as a fixed value
    Spring height = Spring.constant(0);
    for (int i=0; i < componentsByColumn.length; i++) {
      height = Spring.max(height, layout.getConstraints(componentsByColumn[i]).getHeight());
    }
    height = Spring.constant(height.getValue());

    // Standardize the heights, base X pos on prev's East edge
    for (int i=0; i < componentsByColumn.length; i++) {
      SpringLayout.Constraints cons = layout.getConstraints(componentsByColumn[i]);
      cons.setHeight(height);
      cons.setY(SearchFieldPanel.PadY_Spring);
      if (i == 0) {
        cons.setX(SearchFieldPanel.PadX_Spring);
      } else {
        cons.setX(Spring.sum(Spring.constant(5), layout.getConstraint("East", componentsByColumn[i-1])));
      }
    }

    // Make this panel's preferred size depend on its contents
    SpringLayout.Constraints thisCons = layout.getConstraints(this);
      thisCons.setConstraint("East", Spring.sum(layout.getConstraint("East", componentsByColumn[componentsByColumn.length-1]), SearchFieldPanel.PadX_Spring));
      thisCons.setConstraint("South", Spring.sum(height, Spring.sum(SearchFieldPanel.PadY_Spring, SearchFieldPanel.PadY_Spring)));
  }


  /**
   * Returns the minimum number of columns for nested components.
   */
  public int getMinColumnCount() {
    return MIN_COLUMNS;
  }

  /**
   * Gets a nested component's constraints.
   *
   * @param n the column of the component
   * @return a Constraints object, or null if absent or no SpringLayout
   */
  public SpringLayout.Constraints getSpringConstraintsForColumn(int n) {
    if (n < 0 || n >= componentsByColumn.length) return null;

    return ((SpringLayout)this.getLayout()).getConstraints(componentsByColumn[n]);
  }


  /**
   * Compiles regexes and such, before each search.
   */
  public void prepareForSearch() {
    preppedPattern = null;
    preppedQuery = null;

    if (typeBtn.isSelected()) {
      try {
        preppedPattern = Pattern.compile(textField.getText(), Pattern.DOTALL+Pattern.MULTILINE);
      }
      catch (java.util.regex.PatternSyntaxException f) {
        ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Bad regex syntax in DeckBuilder");
      }
    } else {
      preppedQuery = new SearchQuery(textField.getText());
    }
  }

  /**
   * Returns true if a given String matches.
   */
  public boolean matches(String s) {
    boolean result = false;
    if (typeBtn.isSelected()) {
      if (preppedPattern != null) result = preppedPattern.matcher(s).matches();
    } else {
      if (preppedQuery != null) result = preppedQuery.matches(s);
    }
    return result;
  }


  /**
   * Resets this panel.
   */
  public void reset() {
    typeBtn.reset();
    textField.setText("");
  }


  /**
   * Requests that this panel get focus.
   *
   * @return true if focus was requested, false if not focusable
   */
  public boolean focus() {
    textField.requestFocusInWindow();
    return true;
  }


  /**
   * Returns true if this panel is blank.
   */
  public boolean isBlank() {
    return (textField.getText().length() == 0);
  }
}
