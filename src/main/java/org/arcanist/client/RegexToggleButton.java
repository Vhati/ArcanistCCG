package org.arcanist.client;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JToggleButton;


/**
 * Search type button.
 * This is a little button that changes from b to r
 * to represent basic/regex searching in SearchPanels.
 */
public class RegexToggleButton extends JToggleButton implements ActionListener {

  private static final String BASIC_TEXT = " ";
  private static final String REGEX_TEXT = "r";

  private boolean isRegex = false;


  public RegexToggleButton() {
    super();
    this.setMargin(new Insets(-1,0,-1,0));
    this.setToolTipText("Basic/Regex");
    this.addActionListener(this);

    this.setText(REGEX_TEXT);
    Dimension rSize = this.getPreferredSize();
    this.setText(BASIC_TEXT);
    Dimension bSize = this.getPreferredSize();

    int pW = rSize.width; int pH = rSize.height;
    if (bSize.width > pW) pW = bSize.width;
    if (bSize.height > pH) pH = bSize.height;
    this.setPreferredSize(new Dimension(pW, pH));
  }

  public void actionPerformed(ActionEvent e) {
    if (isRegex == true) {
      isRegex = false;
      this.setText(BASIC_TEXT);
    } else {
      isRegex = true;
      this.setText(REGEX_TEXT);
    }
  }

  /**
   * Equivalent to isSelected.
   */
  public boolean isRegex() {
    return isRegex;
  }

  /**
   * Reset the button to its off state.
   */
  public void reset() {
    isRegex = false;
    this.setText(BASIC_TEXT);
    this.setSelected(false);
  }
}
