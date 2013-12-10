package org.arcanist.client;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * An Apprentice-bitmask-based row of color buttons.
 */
public class ColorBarSearchFieldPanel extends SearchFieldPanel {

  public static final int MIN_COLUMNS = 2;

  private String name = "";
  private JLabel nameLbl = null;
  private ColorBar colorBar = null;

  private Component[] componentsByColumn = null;


  public ColorBarSearchFieldPanel(String name, int columns) {
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
    JPanel colorHolder = new JPanel(new GridBagLayout());
      colorBar = new ColorBar();
        colorHolder.add(colorBar);
      componentsByColumn[currentCol++] = colorHolder;
      this.add(colorHolder);
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
   * Returns the number of columns of nested components.
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
  }

  /**
   * Returns true if a given String matches.
   */
  public boolean matches(String s) {
    return colorBar.matches(s);
  }


  /**
   * Resets this panel.
   */
  public void reset() {
    colorBar.reset();
  }

  /**
   * Returns true if this panel is blank.
   */
  public boolean isBlank() {
    return colorBar.isSelectionEmpty();
  }



  /**
   * Color filter panel.
   * It's a row of sticky buttons representing an Apprentice color bitmask.
   */
  private static class ColorBar extends JPanel {
    private JToggleButton XBtn = new JToggleButton("X");
    private JToggleButton LBtn = new JToggleButton("L");
    private JToggleButton ABtn = new JToggleButton("A");
    private JToggleButton GBtn = new JToggleButton("G");
    private JToggleButton RBtn = new JToggleButton("R");
    private JToggleButton BBtn = new JToggleButton("B");
    private JToggleButton UBtn = new JToggleButton("U");
    private JToggleButton WBtn = new JToggleButton("W");

    public ColorBar() {
      super(new FlowLayout(FlowLayout.CENTER, 0, 0));
      XBtn.setMargin(new Insets(-1,0,-1,0)); this.add(XBtn);
      LBtn.setMargin(new Insets(-1,0,-1,0)); this.add(LBtn);
      ABtn.setMargin(new Insets(-1,0,-1,0)); this.add(ABtn);
      GBtn.setMargin(new Insets(-1,0,-1,0)); this.add(GBtn);
      RBtn.setMargin(new Insets(-1,0,-1,0)); this.add(RBtn);
      BBtn.setMargin(new Insets(-1,0,-1,0)); this.add(BBtn);
      UBtn.setMargin(new Insets(-1,0,-1,0)); this.add(UBtn);
      WBtn.setMargin(new Insets(-1,0,-1,0)); this.add(WBtn);
    }

    /**
     * Returns the total value of the bitmask.
     */
    public int getValue() {
      int value = 0;
      if (XBtn.isSelected()) value += 32;
      if (LBtn.isSelected()) value += 128;
      if (ABtn.isSelected()) value += 64;
      if (GBtn.isSelected()) value += 16;
      if (RBtn.isSelected()) value += 8;
      if (BBtn.isSelected()) value += 4;
      if (UBtn.isSelected()) value += 2;
      if (WBtn.isSelected()) value += 1;

      return value;
    }

    /**
     * Returns whether any colors are selected.
     *
     * @return true if any colors are selected, false otherwise
     */
    public boolean isSelectionEmpty() {
      return (getValue() == 0);
    }

    /**
     * Matches the selected colors against a number.
     *
     * @param line the number to match against, as a String
     * @return true if the color matches, false otherwise
     */
    public boolean matches(String line) {
      int testColor;

      try {testColor = Integer.parseInt(line);}
      catch (NumberFormatException e) {return false;}

      if (getValue() == testColor || (getValue() == 32 && testColor >= 32 && testColor < 64) ) {
        return true;
      }
      return false;
    }

    /**
     * Deselects all colors.
     */
    public void reset() {
      XBtn.setSelected(false);
      LBtn.setSelected(false);
      ABtn.setSelected(false);
      GBtn.setSelected(false);
      RBtn.setSelected(false);
      BBtn.setSelected(false);
      UBtn.setSelected(false);
      WBtn.setSelected(false);
    }
  }


  /**
   * Requests that this panel get focus.
   *
   * @return true if focus was requested, false if not focusable
   */
  public boolean focus() {
    return false;
  }
}
