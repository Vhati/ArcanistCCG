package org.arcanist.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.arcanist.util.*;


/**
 * Chat panel.
 * A shared instance of a textfield and text area and listeners.
 * Any Internal frame can grab the panel with no further setup.
 */
public class ChatPanel extends JPanel implements DockableChild {

  public static final String STYLE_CHAT = "Chat";
  public static final String STYLE_NOTICE1 = "NoticeOne";
  public static final String STYLE_NOTICE2 = "NoticeTwo";
  public static final String STYLE_ERROR = "Error";
  public static final String STYLE_TIMESTAMP = "TimeStamp";

  private StyleContext areaStyleContext = new StyleContext();
  private DefaultStyledDocument areaDoc = new DefaultStyledDocument();  // Can take context as arg
  private JTextPane chatArea = null;
  //private JTextArea chatArea = null;
  private JScrollPane chatScrollPane = null;
  private JTextField chatField = null;

  private DateFormat dater = DateFormat.getTimeInstance(DateFormat.SHORT);


  public ChatPanel() {
    Style defaultStyle = areaStyleContext.getStyle(StyleContext.DEFAULT_STYLE);
      AttributeSet defaultAttr = defaultStyle.copyAttributes();
    Style chatStyle = areaStyleContext.addStyle(STYLE_CHAT, defaultStyle);
    Style notice1Style = areaStyleContext.addStyle(STYLE_NOTICE1, defaultStyle);
      StyleConstants.setForeground(notice1Style, Color.GRAY);
    Style notice2Style = areaStyleContext.addStyle(STYLE_NOTICE2, defaultStyle);
      StyleConstants.setForeground(notice2Style, Color.LIGHT_GRAY);
    Style errorStyle = areaStyleContext.addStyle(STYLE_ERROR, defaultStyle);
      StyleConstants.setForeground(errorStyle, Color.RED.darker());
    Style timestampStyle = areaStyleContext.addStyle(STYLE_TIMESTAMP, defaultStyle);
      StyleConstants.setForeground(timestampStyle, Color.LIGHT_GRAY);
      StyleConstants.setFontSize(timestampStyle, StyleConstants.getFontSize(defaultAttr)*3/4);

    GridBagLayout chatGrid = new GridBagLayout();
    GridBagConstraints chatGridConstraints = new GridBagConstraints();
      chatGridConstraints.fill = GridBagConstraints.BOTH;
    this.setLayout(chatGrid);

    //chatArea = new JTextArea("", 0, 30);
    chatArea = new JTextPane(areaDoc);
      chatArea.setEditable(false);
    chatScrollPane = new JScrollPane(chatArea);
      chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      //chatArea.setLineWrap(true);
      //chatArea.setWrapStyleWord(true);
      chatGridConstraints.weightx = 1.0;
      chatGridConstraints.weighty = 1.0;
      chatGridConstraints.gridy = 0;
    this.add(chatScrollPane, chatGridConstraints);


    chatGridConstraints.weightx = 1.0;
    chatGridConstraints.weighty = 0;
    chatGridConstraints.gridy = 1;

    chatField = new JTextField(30);

    KeyListener sendIt = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          if (chatField.getText().length() == 0) return;

          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

          ArcanistCCG.NetManager.chatText(chatAlias +": "+ chatField.getText());
          chatField.setText("");
          chatField.requestFocusInWindow();
        }
      }
    };
    chatField.addKeyListener(sendIt);
    this.add(chatField, chatGridConstraints);

    Action keyCounterQueryAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ArcanistCCG.NetManager.chatCounterQuery();
      }
    };
    chatField.getActionMap().put(Prefs.ACTION_CHAT_COUNTER_QUERY, keyCounterQueryAction);

    Action keyMacro1Action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized (Prefs.hotkeyLock) {
          Object tmpArg = Prefs.hotkeyArgMap.get(Prefs.ACTION_CHAT_MACRO1);
          if (tmpArg == null || ((String)tmpArg).length() == 0) return;
          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

          ArcanistCCG.NetManager.chatText(chatAlias +": "+ tmpArg);
        }
      }
    };
    chatField.getActionMap().put(Prefs.ACTION_CHAT_MACRO1, keyMacro1Action);

    Action keyMacro2Action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized (Prefs.hotkeyLock) {
          Object tmpArg = Prefs.hotkeyArgMap.get(Prefs.ACTION_CHAT_MACRO2);
          if (tmpArg == null || ((String)tmpArg).length() == 0) return;
          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

          ArcanistCCG.NetManager.chatText(chatAlias +": "+ tmpArg);
        }
      }
    };
    chatField.getActionMap().put(Prefs.ACTION_CHAT_MACRO2, keyMacro2Action);

    Action keyMacro3Action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized (Prefs.hotkeyLock) {
          Object tmpArg = Prefs.hotkeyArgMap.get(Prefs.ACTION_CHAT_MACRO3);
          if (tmpArg == null || ((String)tmpArg).length() == 0) return;
          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

          ArcanistCCG.NetManager.chatText(chatAlias +": "+ tmpArg);
        }
      }
    };
    chatField.getActionMap().put(Prefs.ACTION_CHAT_MACRO3, keyMacro3Action);

    Action keyMacro4Action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized (Prefs.hotkeyLock) {
          Object tmpArg = Prefs.hotkeyArgMap.get(Prefs.ACTION_CHAT_MACRO4);
          if (tmpArg == null || ((String)tmpArg).length() == 0) return;
          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

          ArcanistCCG.NetManager.chatText(chatAlias +": "+ tmpArg);
        }
      }
    };
    chatField.getActionMap().put(Prefs.ACTION_CHAT_MACRO4, keyMacro4Action);

    updateHotkeys();
  }


  /**
   * Append to ChatLog.
   *
   * @param styleName any of the defined style fields
   * @param input text
   */
  public void append(String styleName, String input) {
    //chatArea.append(input +"\n\n");
    //AttributeSet attr = areaDoc.getDefaultRootElement().getAttributes().copyAttributes();
    AttributeSet attr = areaStyleContext.getStyle(styleName).copyAttributes();

    try {
      if (styleName.equals(STYLE_CHAT)) {
        areaDoc.insertString(areaDoc.getLength(), "\n", attr);
      }
      if (Prefs.chatTimestamps == true) {
        AttributeSet timestampAttr = areaStyleContext.getStyle(STYLE_TIMESTAMP).copyAttributes();
        areaDoc.insertString(areaDoc.getLength(), dater.format(new java.util.Date()) +": ", timestampAttr);
      }
      areaDoc.insertString(areaDoc.getLength(), input +"\n", attr);
      chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum());
      //chatArea.setCaretPosition(chatArea.getText().length()-1);
    }
    catch (BadLocationException e) {
      //This shouldn't happen
      ArcanistCCG.LogManager.write(e, "Couldn't append to chat.");
    }
  }

  /**
   * Append to ChatLog.
   *
   * @param input text
   */
  public void append(String input) {
    append(STYLE_CHAT, input);
  }


  /**
   * Clear ChatLog.
   */
  public void clear() {
    //chatArea.setText("");
    try {areaDoc.remove(0, areaDoc.getLength());}
    catch (BadLocationException e) {
      //This shouldn't happen
      ArcanistCCG.LogManager.write(e, "Couldn't clear chat.");
    }
  }


  /**
   * Get the JTextField.
   * This is for requesting focus.
   *
   * @return the JTextField
   */
  public JTextField getInputField() {return chatField;}


  /**
   * Saves the chat contents as html to a file.
   */
  public void save(File outFile) {
    FileWriter writer = null;
    try {
      writer = new FileWriter(outFile);
      javax.swing.text.html.MinimalHTMLWriter htmlWriter = new javax.swing.text.html.MinimalHTMLWriter(writer, areaDoc);
      htmlWriter.write();
    }
    catch (IOException e) {
      ArcanistCCG.LogManager.write(e, "Couldn't save chat.");
    }
    catch (BadLocationException e) {
      ArcanistCCG.LogManager.write(e, "Couldn't save chat.");
    }
    finally {
      try {if (writer != null) writer.close();}
      catch (Exception e) {}
    }
  }


  /**
   * Clears this object's hotkeys and reapplies current global ones.
   */
  public void updateHotkeys() {
    InputMap map = chatField.getInputMap();
    KeyStroke[] keys = map.keys();
    if (keys != null) {
      for (int i=0; i < keys.length; i++) {
        Object o = map.get(keys[i]);
        if (o == Prefs.ACTION_CHAT_COUNTER_QUERY) map.remove(keys[i]);
        else if (o == Prefs.ACTION_CHAT_MACRO1) map.remove(keys[i]);
        else if (o == Prefs.ACTION_CHAT_MACRO2) map.remove(keys[i]);
        else if (o == Prefs.ACTION_CHAT_MACRO3) map.remove(keys[i]);
        else if (o == Prefs.ACTION_CHAT_MACRO4) map.remove(keys[i]);
      }
    }

    synchronized (Prefs.hotkeyLock) {
      Object tmpStroke = null;
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CHAT_COUNTER_QUERY);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CHAT_COUNTER_QUERY);
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CHAT_MACRO1);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CHAT_MACRO1);
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CHAT_MACRO2);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CHAT_MACRO2);
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CHAT_MACRO3);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CHAT_MACRO3);
      tmpStroke = Prefs.hotkeyStrokeMap.get(Prefs.ACTION_CHAT_MACRO4);
      if (tmpStroke != null) map.put((KeyStroke)tmpStroke, Prefs.ACTION_CHAT_MACRO4);
    }
  }
}
