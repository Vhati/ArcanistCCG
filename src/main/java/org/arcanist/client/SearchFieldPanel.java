package org.arcanist.client;

import javax.swing.JPanel;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import org.arcanist.client.*;
import org.arcanist.util.*;


public abstract class SearchFieldPanel extends JPanel {

  public static final Spring PadX_Spring = Spring.constant(2);
  public static final Spring PadY_Spring = Spring.constant(0);


  /**
   * Returns the minimum number of columns for nested components.
   */
  abstract int getMinColumnCount();

  /**
   * Gets a nested component's constraints.
   *
   * @param n the column of the component
   * @return a Constraints object, or null if absent or no SpringLayout
   */
  abstract SpringLayout.Constraints getSpringConstraintsForColumn(int n);


  /**
   * Compiles regexes and such, before each search.
   */
  abstract void prepareForSearch();

  /**
   * Returns true if a given String matches.
   */
  abstract boolean matches(String s);


  /**
   * Resets this panel.
   */
  abstract void reset();


  /**
   * Requests that this panel get focus.
   *
   * @return true if focus was requested, false if not focusable
   */
  abstract boolean focus();


  /**
   * Returns true if this panel is blank.
   */
  abstract boolean isBlank();
}
