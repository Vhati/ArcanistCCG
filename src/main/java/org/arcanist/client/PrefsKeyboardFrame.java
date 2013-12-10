package org.arcanist.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * The Keyboard shortcuts settings window.
 */
public class PrefsKeyboardFrame extends JInternalFrame implements ActionListener {
  private ArcanistCCGFrame frame = null;

  private PrefsKeyboardFrame pronoun = this;

  // Maps fields to hotkey action strings
  private Map<JTextField,String> strokeFieldMap = new HashMap<JTextField,String>();
  private Map<JTextField,String> argFieldMap = new HashMap<JTextField,String>();

  private Border unfocusedBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
  private Border focusedBorder = BorderFactory.createLineBorder(new Color(30, 30, 220), 1);
  private Border conflictUnfocusedBorder = BorderFactory.createLineBorder(new Color(220, 30, 30), 1);
  private Border conflictFocusedBorder = BorderFactory.createLineBorder(Color.MAGENTA, 1);

  private FocusListener highlightListener = new FocusListener() {
    @Override
    public void focusGained(FocusEvent e) {
      JComponent source = (JComponent)e.getSource();
      JComponent parent = (JComponent)source.getParent();
      if (parent.getBorder() == conflictUnfocusedBorder) parent.setBorder(conflictFocusedBorder);
      else if (parent.getBorder() == unfocusedBorder) parent.setBorder(focusedBorder);
    }

    @Override
    public void focusLost(FocusEvent e) {
      JComponent source = (JComponent)e.getSource();
      JComponent parent = (JComponent)source.getParent();
      if (parent.getBorder() == conflictFocusedBorder) parent.setBorder(conflictUnfocusedBorder);
      else if (parent.getBorder() == focusedBorder) parent.setBorder(unfocusedBorder);
    }
  };

  private KeyListener hotkeyFieldListener = new KeyAdapter() {
    @Override
    public void keyReleased(KeyEvent e) {
      Object source = e.getSource();
      KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
      JTextField hotkeyField = (JTextField)source;

      if (stroke.getKeyEventType() == KeyEvent.KEY_TYPED) {
        if (stroke.getKeyChar() == Character.MAX_VALUE) return;  // Unsupported key, nothing should be \uFFFF
      }
      else if (stroke.getKeyEventType() == KeyEvent.KEY_RELEASED) {
        switch (stroke.getKeyCode()) {
          case KeyEvent.VK_TAB: return;
          case KeyEvent.VK_SHIFT: return;
          case KeyEvent.VK_CONTROL: return;
          case KeyEvent.VK_ALT: return;
          case KeyEvent.VK_META: return;
          case KeyEvent.VK_SPACE:
            hotkeyField.setText("");
            highlightConflicts();
            return;
          case KeyEvent.VK_ESCAPE:
            String hotkeyAction = strokeFieldMap.get(hotkeyField);
            synchronized (Prefs.hotkeyLock) {
              if (Prefs.hotkeyStrokeMap.containsKey(hotkeyAction)) {
                KeyStroke tmpStroke = Prefs.hotkeyStrokeMap.get(hotkeyAction);
                String strokeString = "";
                if (tmpStroke != null) strokeString = XmlUtils.keyStroke2String(tmpStroke);
                hotkeyField.setText(strokeString);
              }
            }
            highlightConflicts();
            return;
        }
      }
      hotkeyField.setText(XmlUtils.keyStroke2String(stroke));
      highlightConflicts();
    }
  };

  private JButton saveBtn = null;
  private JButton applyBtn = null;


  public PrefsKeyboardFrame(ArcanistCCGFrame f) {
    super("Settings - Keyboard",
      true, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);

    frame = f;

    int maxHeight = 300;  // Cap pack() height of this window

    JPanel pane = new JPanel(new BorderLayout());
      pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JPanel prefsPanel = new JPanel(new GridBagLayout());
      prefsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      GridBagConstraints prefsC = new GridBagConstraints();

      prefsC.gridy = 0;
      prefsC.fill = GridBagConstraints.HORIZONTAL;
      prefsC.weightx = 1.0;
      prefsC.weighty = 0;
      prefsC.gridx = 0;
      prefsC.gridwidth = GridBagConstraints.REMAINDER;
      JLabel hintLbl = new JLabel("(Click and press a key. Escape resets. Space clears.)", SwingConstants.CENTER);
        hintLbl.setFont(hintLbl.getFont().deriveFont(Font.PLAIN));
        prefsPanel.add(hintLbl, prefsC);

      prefsC.gridy++;
      addSection("Table", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_TABLE_FOCUS_CHAT, "Focus the Chat Window", prefsPanel, prefsC);
      addSection("Card", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CARD_ROTATE_LEFT, "Rotate CCW", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CARD_ROTATE_RIGHT, "Rotate CW", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CARD_REPO_UP, "Move Above", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CARD_REPO_DOWN, "Move Below", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CARD_FLIP_REMOTE, "Remote Flip", prefsPanel, prefsC);
      addSection("Chat", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CHAT_COUNTER_QUERY, "List Counters", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CHAT_MACRO1, "Macro 1", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CHAT_MACRO2, "Macro 2", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CHAT_MACRO3, "Macro 3", prefsPanel, prefsC);
      addHotkey(Prefs.ACTION_CHAT_MACRO4, "Macro 4", prefsPanel, prefsC);

      prefsC.fill = GridBagConstraints.VERTICAL;
      prefsC.weightx = 0;
      prefsC.weighty = 1.0;
      prefsC.gridx = 0;
      prefsPanel.add(Box.createVerticalGlue(), prefsC);

    JScrollPane prefsScrollPane = new JScrollPane(prefsPanel);
      prefsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      prefsScrollPane.getHorizontalScrollBar().setUnitIncrement(25);
      prefsScrollPane.getVerticalScrollBar().setUnitIncrement(25);

    JPanel saveApplyPanel = new JPanel(new BorderLayout());
      JSeparator saveApplySep = new JSeparator(SwingConstants.HORIZONTAL);
        saveApplySep.setPreferredSize(new Dimension(1,7));
        saveApplyPanel.add(saveApplySep, BorderLayout.NORTH);
      saveBtn = new JButton("Save");
        saveBtn.setToolTipText("Save keyboard settings to disk");
        saveApplyPanel.add(saveBtn, BorderLayout.WEST);
      applyBtn = new JButton("Apply");
        applyBtn.setToolTipText("Apply without saving");
        saveApplyPanel.add(applyBtn, BorderLayout.EAST);

    // Set up listeners
    saveBtn.addActionListener(this);
    applyBtn.addActionListener(this);

    highlightConflicts();

    pane.add(prefsScrollPane, BorderLayout.CENTER);
    pane.add(saveApplyPanel, BorderLayout.SOUTH);

    this.setContentPane(pane);
    this.pack();
    frame.getDesktop().add(this);

    int width = this.getWidth()+5;
    int height = Math.min(this.getHeight()+5, maxHeight);
    if (frame.getDesktop().getSize().width/2-width/2 > 0 && frame.getDesktop().getSize().height/2-height/2 > 0)
      this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();
  }


  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == saveBtn) {
      applySettings();
      frame.saveKeyboard();
      pronoun.setVisible(false);
      pronoun.dispose();
    }
    else if (source == applyBtn) {
      applySettings();
      pronoun.setVisible(false);
      pronoun.dispose();
    }
  }


  private void addSection(String title, JPanel parent, GridBagConstraints prefsC) {
    prefsC.fill = GridBagConstraints.HORIZONTAL;
    prefsC.weightx = 1.0;
    prefsC.weighty = 0;

    if (prefsC.gridy > 0) {
      prefsC.gridx = 0;
      prefsC.gridwidth = GridBagConstraints.REMAINDER;

      parent.add(Box.createVerticalStrut(5), prefsC);

      prefsC.gridy++;
    }

    prefsC.gridx = 0;
    prefsC.gridwidth = GridBagConstraints.REMAINDER;

    JLabel tmpLbl = new JLabel(title);
      parent.add(tmpLbl, prefsC);

    prefsC.gridy++;

    JSeparator tmpSep = new JSeparator(SwingConstants.HORIZONTAL);
      tmpSep.setPreferredSize(new Dimension(1,5));
      parent.add(tmpSep, prefsC);

    prefsC.gridy++;
  }

  private void addHotkey(String actionString, String description, JPanel parent, GridBagConstraints prefsC) {
    String strokeString = "";
    String argString = null;
    synchronized (Prefs.hotkeyLock) {
      KeyStroke tmpStroke = Prefs.hotkeyStrokeMap.get(actionString);
      if (tmpStroke != null) strokeString = XmlUtils.keyStroke2String(tmpStroke);
      if (Prefs.hotkeyArgMap.containsKey(actionString)) {
        String tmpArg = Prefs.hotkeyArgMap.get(actionString);
        if (tmpArg != null) argString = tmpArg;
        else argString = "";
      }
    }

    prefsC.fill = GridBagConstraints.NONE;
    prefsC.weightx = 0;
    prefsC.weighty = 0;

    prefsC.gridx = 0;
    prefsC.gridwidth = 1;

    JLabel tmpLbl = new JLabel(description);
      tmpLbl.setFont(tmpLbl.getFont().deriveFont(Font.PLAIN));
      tmpLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
      parent.add(tmpLbl, prefsC);

    prefsC.fill = GridBagConstraints.HORIZONTAL;
    prefsC.weightx = 1.0;
    prefsC.gridx = 1;
    prefsC.gridwidth = 1;

    JPanel strokeFieldPanel = new JPanel(new BorderLayout());
      strokeFieldPanel.setBorder(unfocusedBorder);
      JTextField strokeField = new JTextField(18);
        strokeField.setText(strokeString);
        strokeField.setEditable(false);
        strokeField.addFocusListener(highlightListener);
        strokeField.addKeyListener(hotkeyFieldListener);
        strokeFieldMap.put(strokeField, actionString);
        strokeFieldPanel.add(strokeField);
      parent.add(strokeFieldPanel, prefsC);

    if (argString != null) {
      prefsC.fill = GridBagConstraints.HORIZONTAL;
      prefsC.weightx = 1.0;
      prefsC.gridx = 2;
      prefsC.gridwidth = GridBagConstraints.REMAINDER;  //End Row

      JTextField argField = new JTextField(12);
        argField.setText(argString);
        argFieldMap.put(argField, actionString);
        parent.add(argField, prefsC);
    }

    prefsC.gridy++;
  }


  private void highlightConflicts() {
    List<JTextField> uniqueFields = new ArrayList<JTextField>(strokeFieldMap.size());
    List<JTextField> conflictFields = new ArrayList<JTextField>(strokeFieldMap.size());

    Set<JTextField> fields = strokeFieldMap.keySet();
    uniqueFields.addAll(fields);

    Iterator<JTextField> aIt = uniqueFields.iterator();
    while (aIt.hasNext()) {
      JTextField aField = aIt.next();
      if (aField.getText().length() == 0) continue;
      if (conflictFields.contains(aField)) {
        aIt.remove();
        continue;
      }

      boolean found = false;
      Iterator<JTextField> bIt = uniqueFields.iterator();
      while (bIt.hasNext()) {
        JTextField bField = bIt.next();
        if (aField == bField) continue;

        if (aField.getText().equals(bField.getText())) {
          found = true;
          conflictFields.add(bField);
          // Can't remove from within this concurrent iterator
        }
      }
      if (found == true) {
        conflictFields.add(aField);
        aIt.remove();
      }
    }

    Iterator<JTextField> cIt = conflictFields.iterator();
    while (cIt.hasNext()) {
      JTextField cField = cIt.next();
      JComponent parent = (JComponent)cField.getParent();
      if (cField.isFocusOwner()) {
        if (parent.getBorder() != conflictFocusedBorder) parent.setBorder(conflictFocusedBorder);
      } else {
        if (parent.getBorder() != conflictUnfocusedBorder) parent.setBorder(conflictUnfocusedBorder);
      }
    }

    Iterator<JTextField> dIt = uniqueFields.iterator();
    while (dIt.hasNext()) {
      JTextField dField = dIt.next();
      JComponent parent = (JComponent)dField.getParent();
      if (dField.isFocusOwner()) {
        if (parent.getBorder() != focusedBorder) parent.setBorder(focusedBorder);
      } else {
        if (parent.getBorder() != unfocusedBorder) parent.setBorder(unfocusedBorder);
      }
    }
  }


  private void applySettings() {
    synchronized (Prefs.hotkeyLock) {
      Iterator<JTextField> it = null;

      Set<JTextField> strokeFields = strokeFieldMap.keySet();
      it = strokeFields.iterator();
      while (it.hasNext()) {
        JTextField strokeField = it.next();
        String hotkeyAction = strokeFieldMap.get(strokeField);
        KeyStroke hotkeyStroke = KeyStroke.getKeyStroke(strokeField.getText());

        if (Prefs.hotkeyStrokeMap.containsKey(hotkeyAction)) {
          Prefs.hotkeyStrokeMap.put(hotkeyAction, hotkeyStroke);
        }
      }

      Set<JTextField> argFields = argFieldMap.keySet();
      it = argFields.iterator();
      while (it.hasNext()) {
        JTextField argField = it.next();
        String hotkeyAction = argFieldMap.get(argField);
        String hotkeyArg = argField.getText();
        if (hotkeyArg.length() == 0) hotkeyArg = null;

        if (Prefs.hotkeyArgMap.containsKey(hotkeyAction)) {
          Prefs.hotkeyArgMap.put(hotkeyAction, hotkeyArg);
        }
      }
    }
    frame.updateHotkeys();
  }
}
