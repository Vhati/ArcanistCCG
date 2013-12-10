package org.arcanist.client;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;


/**
 * This is the text-only jumbo view.
 * Depending on the contents of the game settings file,
 * elements are fields, areas, or invisible.
 */
public class CardTextPanel extends JPanel {

  private static final String BLANK_STANDIN = " ";

  private int panelWidth = 325;
  private int panelHeight = 485;

  private CardTextPanel pronoun = this;

  private boolean editable = false;
  private int datIndex = -1;                                 //Location within elementMap
  private JComponent[] fields;

  private String cardName = null;
  private String cardSetAbbrev = null;


  /**
   * Constructs a new CardTextPanel.
   * If used for editing, the JumboView adds 
   * a "Save Card" tab to confirm changes.
   *
   * @param name card name, or "" if a creating new card
   * @param setAbbrev abbreviated set name, if any
   * @param isEditable whether the card shall be edited
   */
  public CardTextPanel(String name, String setAbbrev, boolean isEditable) {
    super();
    cardName = name;
    cardSetAbbrev = setAbbrev;
    editable = isEditable;

    this.setLayout(null);

    DatParser textDat = Prefs.textDat;
    synchronized (textDat) {
      String[] cardData = null;
      if (cardName.length() > 0) {
        // Exsting card
        datIndex = textDat.findCard(cardName, cardSetAbbrev);
        cardData = textDat.getCardText(datIndex);
      } else {
        // Blank name: new card, cardData will only matter for length
        cardData = new String[textDat.getFieldCount()];
      }

      if (cardName.length() > 0 && cardData == null) {
        editable = false;                                    //Remove "save card" tab
        JTextField failField = new JTextField("No text: "+ cardName +(setAbbrev.length()==0 ? "" : " ("+ setAbbrev +")"));
          failField.setBounds(new Rectangle(10, 0, failField.getPreferredSize().width, failField.getPreferredSize().height));
          this.add(failField);

        this.setPreferredSize(new Dimension(panelWidth, panelHeight));
        return;
      }

      fields = new JComponent[cardData.length];
      String tempLine = "";
      int fieldWidth = 0;
      JTextField tmpField = null;
      JTextArea tmpArea = null;
      JScrollPane tmpScroll = null;

      for (int j=0; j < cardData.length; j++) {
        String fieldName = textDat.getFieldName(j);

        if (cardName.length()==0)
          tempLine = fieldName;
        else
          tempLine = cardData[j];
        if (isEditable && tempLine.length()==0)
          tempLine = BLANK_STANDIN;

        int fieldLayoutType = Prefs.cardTextArray.get(j)[0];
        int fieldLayoutX = Prefs.cardTextArray.get(j)[1];
        int fieldLayoutY = Prefs.cardTextArray.get(j)[2];
        int fieldLayoutWidth = Prefs.cardTextArray.get(j)[3];
        int fieldLayoutHeight = Prefs.cardTextArray.get(j)[4];

        switch (fieldLayoutType) {
          case 0:
            // Hidden TextField
            tmpField = new JTextField(tempLine);
              tmpField.setVisible(isEditable);
              tmpField.setMargin(new Insets(2,2,2,2));
              tmpField.setEditable(isEditable);
              fieldWidth = tmpField.getPreferredSize().width;
              if (fieldLayoutWidth > 0 && (fieldLayoutWidth < fieldWidth || isEditable==true)) {
                fieldWidth = fieldLayoutWidth;
              } else {
                tmpField.setCaretPosition(0);
              }
              tmpField.setBounds(new Rectangle(fieldLayoutX, fieldLayoutY, fieldWidth, tmpField.getPreferredSize().height));
              tmpField.setToolTipText(fieldName);
              this.add(tmpField);
              fields[j] = tmpField;
            break;
          case 1:
            // TextField
            tmpField = new JTextField(tempLine);
              tmpField.setVisible((tempLine.length()!=0));
              tmpField.setMargin(new Insets(2,2,2,2));
              tmpField.setEditable(isEditable);
              fieldWidth = tmpField.getPreferredSize().width;
              if (fieldLayoutWidth > 0 && (fieldLayoutWidth < fieldWidth || isEditable==true)) {
                fieldWidth = fieldLayoutWidth;
              } else {
                tmpField.setCaretPosition(0);
              }
              tmpField.setBounds(new Rectangle(fieldLayoutX, fieldLayoutY, fieldWidth, tmpField.getPreferredSize().height));
              tmpField.setToolTipText(fieldName);
              this.add(tmpField);
              fields[j] = tmpField;
            break;
          case 2:
            // TextArea
            tempLine = tempLine.replaceAll("\\\\n", "\n");     //Some dats may have escaped newlines
            tempLine = tempLine.replaceAll("] ; ", "]\n\n");   //For Magic flavortext
            tmpArea = new JTextArea(tempLine);
              tmpArea.setMargin(new Insets(2,2,2,2));
              tmpArea.setEditable(isEditable);
              tmpArea.setLineWrap(true);
              tmpArea.setWrapStyleWord(true);
              tmpArea.setToolTipText(fieldName);
              tmpScroll = new JScrollPane(tmpArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
              tmpScroll.setBounds(new Rectangle(fieldLayoutX, fieldLayoutY, fieldLayoutWidth, fieldLayoutHeight));
              this.add(tmpScroll);
              fields[j] = tmpArea;
            break;
        }
        if (panelWidth < fieldLayoutX + fieldLayoutWidth) {
          panelWidth = fieldLayoutX + fieldLayoutWidth;
        }
        if (panelHeight < fieldLayoutY + fieldLayoutHeight) {
          panelHeight = fieldLayoutY + fieldLayoutHeight;
        }
      }
    }
    this.setPreferredSize(new Dimension(panelWidth, panelHeight));
  }


  /**
   * Gets the name used to create this panel.
   */
  public String getName() {return cardName;}

  /**
   * Gets the abbreviated set name used to create this panel.
   */
  public String getSetAbbrev() {return cardSetAbbrev;}

  /**
   * Checks if this is editable.
   *
   * @return true if this is editable
   */
  public boolean isEditable() {return editable;}


  /**
   * Gets the location within the dat's elementList.
   *
   * @return the index
   */
  public int getDatIndex() {return datIndex;}


  /**
   * Saves changes to this card.
   */
  public void save(DatParser textDat) {
    if (!editable) return;

    synchronized (textDat) {
      if (fields.length != textDat.getFieldCount()) return;

      String[] newData = new String[fields.length];
      String tempLine = null;

      for (int i=0; i < fields.length; i++) {
        if (fields[i] != null && fields[i] instanceof JTextComponent) {
          tempLine = ((JTextComponent)fields[i]).getText();
          if (tempLine.equals(" ")) tempLine = "";
        } else {
          tempLine = "";
        }
        tempLine = tempLine.replaceAll("\\r", "");
        tempLine = tempLine.replaceAll("\\n", "\\\\n");
        newData[i] = tempLine;
      }

      if (datIndex == -1) {
        //this is a new card
        textDat.addCard(newData);
      } else {
        //this is an edited card
        textDat.editCard(newData, datIndex);
      }
    }
  }
}
