package org.arcanist.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


/**
 * When applied to a JTextField via setDocument(), you can only enter numbers.
 */
public class NumberDocument extends PlainDocument {

  private boolean nomin = false;
  private boolean nomax = false;
  private int min = 0;
  private int max = 0;


  public NumberDocument(int minValue, int maxValue) {
    super();
    if (minValue == Integer.MIN_VALUE) nomin = true;
    else min = minValue;
    if (maxValue == Integer.MAX_VALUE) nomax = true;
    else max = maxValue;
  }

  public NumberDocument() {
    super();
    nomin = true;
    nomax = true;
  }


  @Override
  public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
    if (str == null) {return;}

    try {
      String tmp = super.getText(0, offs) + str;
      if (offs < super.getLength()) tmp += getText(offs, super.getLength());
      tmp = tmp.trim();
      int tmpNum = Integer.parseInt(tmp);
      if ((nomin || tmpNum >= min) && (nomax || tmpNum <= max)) {
        super.insertString(offs, str, a);
      }
    }
    catch (NumberFormatException exception) {}
  }
}
