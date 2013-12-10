package org.arcanist.client;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.ComboBoxEditor;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.Spring;
import java.util.regex.Pattern;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A keyword/regex text search field.
 */
public class ExpanSearchFieldPanel extends SearchFieldPanel {

  public static final int MIN_COLUMNS = 3;

  private String name = "";
  private JLabel nameLbl = null;
  private WideComboBox expanCombo = null;
  private RegexToggleButton typeBtn = null;

  private Pattern preppedPattern = null;
  private SearchQuery preppedQuery = null;

  private Component[] componentsByColumn = null;


  public ExpanSearchFieldPanel(String name, int columns) {
    super();
    this.name = name;
    if (columns < MIN_COLUMNS) throw new IllegalArgumentException(this.getClass().getName() +" requires at least "+ MIN_COLUMNS +" columns");

    //this.setBorder(BorderFactory.createEtchedBorder(1));

    SpringLayout layout = new SpringLayout();
    this.setLayout(layout);

    JTextField dummyField = new JTextField(10);
    String[] fullSetNames = Prefs.textDat.getTextSetNames();
    String[] choices = new String[fullSetNames.length+1];
      choices[0] = "";
      for (int i=0; i < fullSetNames.length; i++) {
        String tmpFullName = fullSetNames[fullSetNames.length-1-i];
        choices[i+1] = Prefs.textDat.getSetAbbrevFromName(tmpFullName) +"-"+ tmpFullName;
      }

    componentsByColumn = new Component[columns];
    int currentCol = 0;
    nameLbl = new JLabel(this.name);
      componentsByColumn[currentCol++] = nameLbl;
      this.add(nameLbl);
    JPanel comboHolder = new JPanel(new GridBagLayout());
      GridBagConstraints gridC = new GridBagConstraints();
        gridC.fill = GridBagConstraints.HORIZONTAL;
        gridC.weightx = 1.0;
      expanCombo = new WideComboBox(choices);
        expanCombo.setEditable(true);
        expanCombo.setEditor(new ExpanSearchFieldEditor());
        expanCombo.setFont(dummyField.getFont());
        expanCombo.recachePopupWidth();
        expanCombo.setPreferredSize(dummyField.getPreferredSize());
        comboHolder.add(expanCombo, gridC);
      componentsByColumn[currentCol++] = comboHolder;
      this.add(comboHolder);
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

    String pendingString = expanCombo.getEditor().getItem().toString();

    if (typeBtn.isSelected()) {
      try {
        preppedPattern = Pattern.compile(pendingString, Pattern.DOTALL+Pattern.MULTILINE);
      }
      catch (java.util.regex.PatternSyntaxException f) {
        ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Bad regex syntax in DeckBuilder.");
      }
    } else {
      preppedQuery = new SearchQuery(pendingString);
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
    expanCombo.setSelectedItem("");
  }


  /**
   * Requests that this panel get focus.
   *
   * @return true if focus was requested, false if not focusable
   */
  public boolean focus() {
    expanCombo.requestFocusInWindow();
    return true;
  }


  /**
   * Returns true if this panel is blank.
   */
  public boolean isBlank() {
    String pendingString = expanCombo.getEditor().getItem().toString();
    return (pendingString.length() == 0);
  }



  private static class ExpanSearchFieldEditor implements ComboBoxEditor {
    private JTextField textField = new JTextField();

    public ExpanSearchFieldEditor() {}

    public void setItem(Object anObject) {
      if (anObject != null) {
        String pendingString = anObject.toString();
        if (!textField.getText().equals(pendingString)) {
          if (pendingString.length() > 0) {
            if (pendingString.indexOf("-") != -1)
              textField.setText(pendingString.substring(0, pendingString.indexOf("-")));
            else
              textField.setText(pendingString +" :)");
          } else {
            textField.setText("");
          }
        }
      }
    }

    public Component getEditorComponent() {
      return textField;
    }

    public Object getItem() {
      return textField.getText();
    }

    public void selectAll() {
      textField.selectAll();
    }

    public void addActionListener(ActionListener l) {
      textField.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
      textField.removeActionListener(l);
    }
  }
}
