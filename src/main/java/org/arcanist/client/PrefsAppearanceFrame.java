package org.arcanist.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * The Appearance settings window.
 */
public class PrefsAppearanceFrame extends JInternalFrame {

  private ArcanistCCGFrame frame = null;

  private PrefsAppearanceFrame pronoun = this;
  private JPanel pane = new JPanel();

  private JComboBox mainBgList = null;
  private JComboBox tableBgList = null;
  private JCheckBox tableImageTiledCheck = null;
  private JTextField tableImagePathField = null;
  private JTextField gridSnapField = null;
  private JTextField playerAliasField = null;
  private JCheckBox chatTimestampCheck = null;
  private JCheckBox saveLayoutCheck = null;


  public PrefsAppearanceFrame(ArcanistCCGFrame f) {
    super("Settings - Appearance",
      true, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);

    frame = f;

    int width = 250; int height = 230; //These are old defaults, values get replaced by pack()

    JPanel prefsPanel = new JPanel(new GridBagLayout());
      GridBagConstraints prefsC = new GridBagConstraints();
        prefsC.fill = GridBagConstraints.BOTH;
        prefsC.weightx = 1.0;
        prefsC.weighty = 0;
        prefsC.gridwidth = GridBagConstraints.REMAINDER;  //End Row

      prefsC.gridy = 0;
      JPanel mainBgPanel = new JPanel();
        String[] mainBgcolors = {"Default", "Blue", "Gray", "Black", "White"};
        JLabel mainBgLabel = new JLabel("Main Background Color");
          mainBgPanel.add(mainBgLabel);
        mainBgList = new JComboBox(mainBgcolors);
          if (frame.getContentPane().getBackground().equals(new Color(153,153,204))) {
            mainBgList.setSelectedIndex(1);
          } else if (frame.getContentPane().getBackground().equals(Color.lightGray)) {
            mainBgList.setSelectedIndex(2);
          } else if (frame.getContentPane().getBackground().equals(Color.black)) {
            mainBgList.setSelectedIndex(3);
          } else if (frame.getContentPane().getBackground().equals(Color.white)) {
            mainBgList.setSelectedIndex(4);
          }
          if (frame.getContentPane().getBackground() != null && UIManager.get("Desktop.background") != null) {
            if (frame.getContentPane().getBackground().getRGB() == ((Color)UIManager.get("Desktop.background")).getRGB() ) {
              mainBgList.setSelectedIndex(0);
            }
          }
          mainBgPanel.add(mainBgList);
        prefsPanel.add(mainBgPanel, prefsC);

      prefsC.gridy++;
      JPanel tableBgPanel = new JPanel();
        String[] tableBgcolors = {"Default", "Gray", "Black", "White"};
        JLabel tableBgLabel = new JLabel("Table Background Color");
          tableBgPanel.add(tableBgLabel);
        tableBgList = new JComboBox(tableBgcolors);
          if (frame.getTablePane().getParent().getBackground().equals(new Color(204,204,204))) {
            tableBgList.setSelectedIndex(1);
          } else if (frame.getTablePane().getParent().getBackground().equals(Color.black)) {
            tableBgList.setSelectedIndex(2);
          } else if (frame.getTablePane().getParent().getBackground().equals(Color.white)) {
            tableBgList.setSelectedIndex(3);
          }
          if (frame.getTablePane().getParent().getBackground() != null && UIManager.get("Viewport.background") != null) {
            if (frame.getTablePane().getParent().getBackground().getRGB() == ((Color)UIManager.get("Viewport.background")).getRGB() ) {
              tableBgList.setSelectedIndex(0);
            }
          }
          tableBgPanel.add(tableBgList);
        prefsPanel.add(tableBgPanel, prefsC);

      prefsC.gridy++;
      JPanel tableImagePanel = new JPanel();
        tableImagePanel.setLayout(new BoxLayout(tableImagePanel, BoxLayout.Y_AXIS));
        JPanel tableImagePanelOne = new JPanel();
          tableImagePanelOne.setLayout(new BoxLayout(tableImagePanelOne, BoxLayout.X_AXIS));
          JLabel tableImageLabel = new JLabel("Table Background Image");
            tableImagePanelOne.add(tableImageLabel);
          tableImageTiledCheck = new JCheckBox("Tiled");
            tableImageTiledCheck.setSelected(Prefs.tableBgTiled);
            tableImagePanelOne.add(tableImageTiledCheck);
          tableImagePanel.add(tableImagePanelOne);

        JPanel tableImagePanelTwo = new JPanel();
          tableImagePanelTwo.setLayout(new BoxLayout(tableImagePanelTwo, BoxLayout.X_AXIS));
          tableImagePathField = new JTextField(Prefs.tableBgImage,15);
            tableImagePathField.setMaximumSize(new Dimension(175, 25));
            tableImagePanelTwo.add(tableImagePathField);
          JButton tableImageBtn = new JButton("Choose");
            tableImageBtn.setMargin(new Insets(0,1,0,1));
            tableImagePanelTwo.add(tableImageBtn);
          tableImagePanel.add(tableImagePanelTwo);
        prefsPanel.add(tableImagePanel, prefsC);

    prefsC.gridy++;
    JPanel tableSnapHolderPanel = new JPanel();
      tableSnapHolderPanel.setLayout(new BoxLayout(tableSnapHolderPanel, BoxLayout.Y_AXIS));
      JPanel tableSnapPanel = new JPanel();
        tableSnapPanel.setLayout(new BoxLayout(tableSnapPanel, BoxLayout.X_AXIS));
        JLabel gridSnapLabel = new JLabel("Table Grid Snap");
          tableSnapPanel.add(gridSnapLabel);
        gridSnapField = new JTextField(new NumberDocument(1, 50), Integer.toString(frame.getTablePane().getGridSnap()),2);
          gridSnapField.setMaximumSize(gridSnapField.getPreferredSize());
          tableSnapPanel.add(gridSnapField);
        tableSnapHolderPanel.add(tableSnapPanel);
      prefsPanel.add(tableSnapHolderPanel, prefsC);

    prefsC.gridy++;
    JPanel playerSepHolderPanel = new JPanel();
      playerSepHolderPanel.setLayout(new BoxLayout(playerSepHolderPanel, BoxLayout.Y_AXIS));
        playerSepHolderPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        JSeparator playerSep = new JSeparator();
        playerSepHolderPanel.add(playerSep);
      prefsPanel.add(playerSepHolderPanel, prefsC);

    prefsC.gridy++;
    JPanel playerAliasHolderPanel = new JPanel();
      playerAliasHolderPanel.setLayout(new BoxLayout(playerAliasHolderPanel, BoxLayout.Y_AXIS));
      JPanel playerAliasPanel = new JPanel();
        playerAliasPanel.setLayout(new BoxLayout(playerAliasPanel, BoxLayout.X_AXIS));
        JLabel playerAliasLabel = new JLabel("Player Alias");
          playerAliasPanel.add(playerAliasLabel);
        playerAliasField = new JTextField(new RegexDocument("^[a-zA-Z0-9 ,-_'.]{0,20}$"), Prefs.playerAlias, 13);
          playerAliasField.setMaximumSize(playerAliasField.getPreferredSize());
          playerAliasPanel.add(playerAliasField);
        playerAliasHolderPanel.add(playerAliasPanel);
      JPanel chatTimestampPanel = new JPanel(new BorderLayout());
        chatTimestampCheck = new JCheckBox("Show Timestamps in Chat");
          chatTimestampCheck.setSelected(Prefs.chatTimestamps);
          chatTimestampPanel.add(chatTimestampCheck);
        playerAliasHolderPanel.add(chatTimestampPanel);
      prefsPanel.add(playerAliasHolderPanel, prefsC);

    prefsC.gridy++;
    JPanel saveSepHolderPanel = new JPanel();
      saveSepHolderPanel.setLayout(new BoxLayout(saveSepHolderPanel, BoxLayout.Y_AXIS));
        saveSepHolderPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 1, 0));
        JSeparator saveSep = new JSeparator();
        saveSepHolderPanel.add(saveSep);
      prefsPanel.add(saveSepHolderPanel, prefsC);

    prefsC.gridy++;
    JPanel saveApplyPanel = new JPanel();
      saveApplyPanel.setLayout(new BoxLayout(saveApplyPanel, BoxLayout.Y_AXIS));
      JPanel saveApplyPanelOne = new JPanel(new BorderLayout());
      saveLayoutCheck = new JCheckBox("Save Window Layout");
        saveApplyPanelOne.add(saveLayoutCheck);
      saveApplyPanel.add(saveApplyPanelOne);
      JPanel saveApplyPanelTwo = new JPanel(new BorderLayout());
      JButton saveBtn = new JButton("Save");
        saveBtn.setToolTipText("Save appearance settings to disk");
        saveApplyPanelTwo.add(saveBtn, BorderLayout.WEST);
      JButton applyBtn = new JButton("Apply");
        applyBtn.setToolTipText("Apply without saving");
        saveApplyPanelTwo.add(applyBtn, BorderLayout.EAST);
      saveApplyPanel.add(saveApplyPanelTwo);
      prefsPanel.add(saveApplyPanel, prefsC);


    // Set up listeners
    tableImageBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        File tableImageFile = frame.fileChooser(JFileChooser.OPEN_DIALOG, Prefs.homePath +"images/backgrounds", null);
        if (tableImageFile != null) {
          String fileName = "";
          if (tableImageFile.getAbsolutePath().startsWith(Prefs.homePathAb) == true && tableImageFile.getAbsolutePath().length() > Prefs.homePathAb.length())
            fileName = Prefs.homePath + tableImageFile.getAbsolutePath().substring(Prefs.homePathAb.length(), tableImageFile.getAbsolutePath().length()).replace('\\','/');
          else
            fileName = tableImageFile.getAbsolutePath().replace('\\','/');
          tableImagePathField.setText(fileName);
        }
      }
    });

    saveBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        pronoun.applySettings();
        frame.saveAppearance(saveLayoutCheck.isSelected());
        pronoun.setVisible(false);
        pronoun.dispose();
      }
    });

    applyBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        pronoun.applySettings();
        pronoun.setVisible(false);
        pronoun.dispose();
      }
    });

    pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
    pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    pane.add(prefsPanel);

    this.setContentPane(pane);
    this.pack();
    frame.getDesktop().add(this);

    width = this.getWidth();
    height = this.getHeight();
    if (frame.getDesktop().getSize().width/2-width/2 > 0 && frame.getDesktop().getSize().height/2-height/2 > 0)
      this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();
  }


  /**
   * Apply settings without saving.
   */
  private void applySettings() {
    switch (mainBgList.getSelectedIndex()) {
      case 0:
        if (UIManager.get("Desktop.background") != null) {
          Prefs.mainBgColor = (Color)UIManager.get("Desktop.background");
        } else {
          Prefs.mainBgColor = null;
        }
        break;
      case 1: Prefs.mainBgColor = new Color(153,153,204); break;
      case 2: Prefs.mainBgColor = Color.lightGray; break;
      case 3: Prefs.mainBgColor = Color.black; break;
      case 4: Prefs.mainBgColor = Color.white; break;
    }
    frame.getContentPane().setBackground(Prefs.mainBgColor);

    switch (tableBgList.getSelectedIndex()) {
      case 0:
        if (UIManager.get("Viewport.background") != null) {
          Prefs.tableBgColor = (Color)UIManager.get("Viewport.background");
        } else {
          Prefs.tableBgColor = null;
        }
        break;
      case 1: Prefs.tableBgColor = new Color(204,204,204); break;
      case 2: Prefs.tableBgColor = Color.black; break;
      case 3: Prefs.tableBgColor = Color.white; break;
    }
    frame.getTablePane().getParent().setBackground(Prefs.tableBgColor);

    try {
      Prefs.tableBgImage = tableImagePathField.getText();
      Prefs.tableBgTiled = tableImageTiledCheck.isSelected();
      if (Prefs.tableBgImage.length() > 0) {
        frame.getTablePane().setBackgroundImage(Prefs.tableBgImage, Prefs.tableBgTiled);
      } else {
        frame.getTablePane().setBackgroundImage(null, Prefs.tableBgTiled);
      }
    }
    catch (IOException f) {
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Couldn't load background image.", "Error", JOptionPane.PLAIN_MESSAGE);
      Prefs.tableBgImage = "";
      Prefs.tableBgTiled = false;
    }

    if (gridSnapField.getText().length() > 0) {
      try {
        int gridSnap = Integer.parseInt(gridSnapField.getText());
        frame.getTablePane().setGridSnap(gridSnap);
      }
      catch (NumberFormatException f) {
        gridSnapField.setText("");
      }
    }

    String prevAlias = Prefs.playerAlias;
    String reqAlias = "";
    String userinput = playerAliasField.getText();
    if (playerAliasField.getText().length() > 0) {
      if (Prefs.isValidPlayerAlias(userinput)) reqAlias = userinput;
      else JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Your new alias can be up to 20 characters including \"a-zA-Z0-9 ,-_'.\"\nIf blank, [P#] will be used.", "Error", JOptionPane.PLAIN_MESSAGE);
    }
    if (prevAlias.equals(reqAlias) == false) {
      Prefs.savedAlias = reqAlias;
      ArcanistCCG.NetManager.serverRequestAlias(reqAlias);
    }
    Prefs.chatTimestamps = chatTimestampCheck.isSelected();
  }


  /**
   * Append a rectangle's x,y,w,h to a buffer.
   * Tabs separate each value.
   * There are no leading or trailing tabs.
   *
   * @param tab append a trailing tab
   * @param buf the buffer
   * @param bounds the bounds
   */
  private void appendValue(boolean tab, StringBuffer buf, Rectangle bounds) {
    appendValue(true, buf, Integer.toString(bounds.x));
    appendValue(true, buf, Integer.toString(bounds.y));
    appendValue(true, buf, Integer.toString(bounds.width));
    appendValue(tab, buf, Integer.toString(bounds.height));
  }

  /**
   * Append a value to a buffer.
   *
   * @param tab append a trailing tab
   * @param buf the buffer
   * @param value the value
   */
  private void appendValue(boolean tab, StringBuffer buf, String value) {
    buf.append(value);
    if (tab) buf.append("\t");
  }
}
