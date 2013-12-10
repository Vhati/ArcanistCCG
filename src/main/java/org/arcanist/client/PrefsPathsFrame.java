package org.arcanist.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * The game Paths settings window.
 */
public class PrefsPathsFrame extends JInternalFrame implements ActionListener {

  private ArcanistCCGFrame frame = null;

  private PrefsPathsFrame pronoun = this;
  private JPanel pane = new JPanel();
  private List<int[]> tempTextArray = new ArrayList<int[]>();
  private int tempTextOverlayStatsField = -1;

  private GameList gameList = null;
  private JTextField backField = null;
  private JTextField emptyDeckField = null;
  private JTextField blankCardField = null;
  private JTextField gamelocField = null;
  private JTextField gamedatField = null;
  private JTextField gamesetField = null;
  private JCheckBox lostCardsCheck = null;
  private JCheckBox lostDatsCheck = null;
  private JCheckBox textOnlyCheck = null;
  private JPanel prefsPanel = null;
  private JPanel layoutPanel = null;

  private JButton gameAddBtn = null;
  private JButton gameRemBtn = null;
  private JButton gameSaveBtn = null;
  private JButton gameLoadBtn = null;
  private JButton backBtn = null;
  private JButton emptyDeckBtn = null;
  private JButton blankCardBtn = null;
  private JButton gamelocBtn = null;
  private JButton gamedatBtn = null;
  private JButton gamesetBtn = null;
  private JButton editLayoutBtn = null;
  private JButton saveBtn = null;
  private JButton applyBtn = null;


  public PrefsPathsFrame(ArcanistCCGFrame f) {
    super("Settings - Paths",
      true, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);

    frame = f;

    tempTextArray.addAll(Prefs.cardTextArray);
    tempTextOverlayStatsField = Prefs.cardTextOverlayStatsField;

    pane.setLayout(new BorderLayout());

    JPanel gamePanel = new JPanel();
      gamePanel.setMaximumSize(new Dimension(380, 1000));
      GridBagLayout gameGridbag = new GridBagLayout();
      GridBagConstraints gameC = new GridBagConstraints();
        gameC.fill = GridBagConstraints.BOTH;
        gameC.weightx = 1.0;
        gameC.weighty = 0;
        gameC.gridwidth = GridBagConstraints.REMAINDER;  //End Row
      gamePanel.setLayout(gameGridbag);
      gamePanel.setBorder(BorderFactory.createEtchedBorder());
      JPanel addremPanel = new JPanel();
        gameAddBtn = new JButton("Add");
          gameAddBtn.setToolTipText("Add a game with currently applied paths");
          addremPanel.add(gameAddBtn);
        gameRemBtn = new JButton("Rem");
          gameRemBtn.setToolTipText("Remove selected game");
          addremPanel.add(gameRemBtn);
        gamePanel.add(addremPanel, gameC);

      gameC.gridy = 1;
      gameC.weighty = 1.0;
      gameC.insets = new Insets(0,5,0,5);
      gameList = new GameList(false);
        gameList.setFixedCellWidth(120);
        gameList.setVisibleRowCount(14);
      JScrollPane gameListScrollPane = new JScrollPane(gameList);
        gamePanel.add(gameListScrollPane, gameC);

      gameC.gridy = 2;
      gameC.weighty = 0;
      JPanel loadsavePanel = new JPanel();
        gameSaveBtn = new JButton("Set");
          gameSaveBtn.setToolTipText("Set game paths");
          loadsavePanel.add(gameSaveBtn);
        gameLoadBtn = new JButton("Get");
          gameLoadBtn.setToolTipText("Get game paths");
          loadsavePanel.add(gameLoadBtn);
        gamePanel.add(loadsavePanel, gameC);

      gameC.gridy = 3;
      saveBtn = new JButton("Save");
        saveBtn.setToolTipText("Save all game paths to disk");
        gamePanel.add(saveBtn, gameC);

    int leftMargin = 80;
    int rightMargin = 110;
    int fieldMargin = 9;
    JPanel pathsPanel = new JPanel();
      pathsPanel.setLayout(new BoxLayout(pathsPanel, BoxLayout.Y_AXIS));
      pathsPanel.setBorder(BorderFactory.createEtchedBorder());

      JPanel backLabelPanel = new JPanel(new BorderLayout());
        JLabel backLbl = new JLabel("Default Card Back Image");
          backLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, rightMargin));
          backLabelPanel.add(backLbl, BorderLayout.EAST);
        pathsPanel.add(backLabelPanel);
      JPanel backPanel = new JPanel();
        backPanel.setLayout(new BoxLayout(backPanel, BoxLayout.X_AXIS));
        backPanel.setBorder(BorderFactory.createEmptyBorder(0, fieldMargin, 0, fieldMargin));
        backField = new JTextField(Prefs.defaultBackPath, 20);
          backField.setMaximumSize(new Dimension(600, 25));
          backPanel.add(backField);
        backBtn = new JButton("Choose");
          backBtn.setMargin(new Insets(0,1,0,1));
          backPanel.add(backBtn);
        pathsPanel.add(backPanel);

      JPanel emptyDeckLabelPanel = new JPanel(new BorderLayout());
        JLabel emptyDeckLbl = new JLabel("Empty Deck Image");
          emptyDeckLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, rightMargin));
          emptyDeckLabelPanel.add(emptyDeckLbl, BorderLayout.EAST);
        pathsPanel.add(emptyDeckLabelPanel);
      JPanel emptyDeckPanel = new JPanel();
        emptyDeckPanel.setLayout(new BoxLayout(emptyDeckPanel, BoxLayout.X_AXIS));
        emptyDeckPanel.setBorder(BorderFactory.createEmptyBorder(0, fieldMargin, 0, fieldMargin));
        emptyDeckField = new JTextField(Prefs.defaultEmptyDeckPath, 20);
          emptyDeckField.setMaximumSize(new Dimension(600, 25));
          emptyDeckPanel.add(emptyDeckField);
        emptyDeckBtn = new JButton("Choose");
          emptyDeckBtn.setMargin(new Insets(0,1,0,1));
          emptyDeckPanel.add(emptyDeckBtn);
        pathsPanel.add(emptyDeckPanel);

      JPanel blankCardLabelPanel = new JPanel(new BorderLayout());
        JLabel blankCardLbl = new JLabel("Blank Card Image");
          blankCardLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, rightMargin));
          blankCardLabelPanel.add(blankCardLbl, BorderLayout.EAST);
        pathsPanel.add(blankCardLabelPanel);
      JPanel blankCardPanel = new JPanel();
        blankCardPanel.setLayout(new BoxLayout(blankCardPanel, BoxLayout.X_AXIS));
        blankCardPanel.setBorder(BorderFactory.createEmptyBorder(0, fieldMargin, 0, fieldMargin));
        blankCardField = new JTextField(Prefs.defaultBlankPath, 20);
          blankCardField.setMaximumSize(new Dimension(600, 25));
          blankCardPanel.add(blankCardField);
        blankCardBtn = new JButton("Choose");
          blankCardBtn.setMargin(new Insets(0,1,0,1));
          blankCardPanel.add(blankCardBtn);
        pathsPanel.add(blankCardPanel);

      JPanel gamelocLabelPanel = new JPanel(new BorderLayout());
        JLabel gamelocLbl = new JLabel("Game Location");
          gamelocLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, rightMargin));
          gamelocLabelPanel.add(gamelocLbl, BorderLayout.EAST);
        pathsPanel.add(gamelocLabelPanel);
      JPanel gamelocPanel = new JPanel();
        gamelocPanel.setLayout(new BoxLayout(gamelocPanel, BoxLayout.X_AXIS));
        gamelocPanel.setBorder(BorderFactory.createEmptyBorder(0, fieldMargin, 0, fieldMargin));
        gamelocField = new JTextField(Prefs.gameloc, 20);
          gamelocField.setMaximumSize(new Dimension(600, 25));
          gamelocPanel.add(gamelocField);
        gamelocBtn = new JButton("Choose");
          gamelocBtn.setMargin(new Insets(0,1,0,1));
          gamelocPanel.add(gamelocBtn);
        pathsPanel.add(gamelocPanel);

      JPanel gamedatLabelPanel = new JPanel(new BorderLayout());
        JLabel gamedatLbl = new JLabel("CardText File");
          gamedatLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, rightMargin));
          gamedatLabelPanel.add(gamedatLbl, BorderLayout.EAST);
        pathsPanel.add(gamedatLabelPanel);
      JPanel gamedatPanel = new JPanel();
        gamedatPanel.setLayout(new BoxLayout(gamedatPanel, BoxLayout.X_AXIS));
        gamedatPanel.setBorder(BorderFactory.createEmptyBorder(0, fieldMargin, 0, fieldMargin));
        gamedatField = new JTextField(Prefs.gamedat, 20);
          gamedatField.setMaximumSize(new Dimension(600, 25));
          gamedatPanel.add(gamedatField);
        gamedatBtn = new JButton("Choose");
          gamedatBtn.setMargin(new Insets(0,1,0,1));
          gamedatPanel.add(gamedatBtn);
        pathsPanel.add(gamedatPanel);

      JPanel gamesetLabelPanel = new JPanel(new BorderLayout());
        JLabel gamesetLbl = new JLabel("CardText Expan.dat File");
          gamesetLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, rightMargin));
          gamesetLabelPanel.add(gamesetLbl, BorderLayout.EAST);
        pathsPanel.add(gamesetLabelPanel);
      JPanel gamesetPanel = new JPanel();
        gamesetPanel.setLayout(new BoxLayout(gamesetPanel, BoxLayout.X_AXIS));
        gamesetPanel.setBorder(BorderFactory.createEmptyBorder(0, fieldMargin, 0, fieldMargin));
        gamesetField = new JTextField(Prefs.gameset, 20);
          gamesetField.setMaximumSize(new Dimension(600, 25));
          gamesetPanel.add(gamesetField);
        gamesetBtn = new JButton("Choose");
          gamesetBtn.setMargin(new Insets(0,1,0,1));
          gamesetPanel.add(gamesetBtn);
        pathsPanel.add(gamesetPanel);

      JPanel lostCardsPanel = new JPanel(new BorderLayout());
        lostCardsCheck = new JCheckBox("Suppress Image Errors");
          lostCardsCheck.setBorder(BorderFactory.createEmptyBorder(0, leftMargin, 0, 0));
          lostCardsCheck.setSelected(Prefs.suppressLostCards);
          lostCardsPanel.add(lostCardsCheck, BorderLayout.WEST);
        pathsPanel.add(lostCardsPanel);
      JPanel lostDatsPanel = new JPanel(new BorderLayout());
        lostDatsCheck = new JCheckBox("Suppress CardText Errors");
          lostDatsCheck.setBorder(BorderFactory.createEmptyBorder(0, leftMargin, 0, 0));
          lostDatsCheck.setSelected(Prefs.suppressLostDats);
          lostDatsPanel.add(lostDatsCheck, BorderLayout.WEST);
        pathsPanel.add(lostDatsPanel);
      JPanel textOnlyPanel = new JPanel(new BorderLayout());
        textOnlyCheck = new JCheckBox("Use Text Only");
          textOnlyCheck.setBorder(BorderFactory.createEmptyBorder(0, leftMargin, 0, 0));
          textOnlyCheck.setSelected(Prefs.useTextOnly);
          textOnlyCheck.setToolTipText("Treat new cards as text");
          textOnlyPanel.add(textOnlyCheck, BorderLayout.WEST);
        pathsPanel.add(textOnlyPanel);
      JPanel editLayoutPanel = new JPanel();
        editLayoutPanel.setLayout(new BoxLayout(editLayoutPanel, BoxLayout.X_AXIS));
        editLayoutPanel.add(Box.createHorizontalGlue());
        editLayoutBtn = new JButton("Edit Text Layout");
          editLayoutPanel.add(editLayoutBtn);
        editLayoutPanel.add(Box.createHorizontalGlue());
        pathsPanel.add(editLayoutPanel);
      pathsPanel.add(Box.createVerticalStrut(5));
      JPanel applyPanel = new JPanel();
        applyPanel.setLayout(new BoxLayout(applyPanel, BoxLayout.X_AXIS));
        applyPanel.add(Box.createHorizontalGlue());
        applyBtn = new JButton("Apply");
          applyBtn.setToolTipText("Use these paths without saving");
          applyPanel.add(applyBtn);
        applyPanel.add(Box.createHorizontalGlue());
        pathsPanel.add(applyPanel);

    prefsPanel = new JPanel();
      prefsPanel.setLayout(new BoxLayout(prefsPanel, BoxLayout.X_AXIS));
      prefsPanel.add(gamePanel);
      prefsPanel.add(Box.createHorizontalStrut(9));
      prefsPanel.add(pathsPanel);

    layoutPanel = new JPanel();
      layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.X_AXIS));


    // Set up listeners
    gameAddBtn.addActionListener(this);
    gameRemBtn.addActionListener(this);
    gameLoadBtn.addActionListener(this);
    gameSaveBtn.addActionListener(this);
    backBtn.addActionListener(this);
    emptyDeckBtn.addActionListener(this);
    blankCardBtn.addActionListener(this);
    gamelocBtn.addActionListener(this);
    gamedatBtn.addActionListener(this);
    gamesetBtn.addActionListener(this);
    editLayoutBtn.addActionListener(this);
    saveBtn.addActionListener(this);
    applyBtn.addActionListener(this);

    pane.add(prefsPanel);
    this.setContentPane(pane);
    this.pack();
    frame.getDesktop().add(this);

    int width = this.getWidth();
    int height = this.getHeight();
    if (frame.getDesktop().getSize().width/2-width/2 > 0 && frame.getDesktop().getSize().height/2-height/2 > 0)
      this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == gameAddBtn) {
      String userinput = JOptionPane.showInternalInputDialog(frame.getDesktop(), "What should the new game be called?", "Add a Game", JOptionPane.QUESTION_MESSAGE);
      if (userinput == null || userinput.length() == 0) {return;}

      GamePrefs newGame = new GamePrefs();
        newGame.name = userinput;
        newGame.defaultBackPath = Prefs.defaultBackPath;
        newGame.defaultEmptyDeckPath = Prefs.defaultEmptyDeckPath;
        newGame.defaultBlankPath = Prefs.defaultBlankPath;
        newGame.gameloc = Prefs.gameloc;
        newGame.gamedat = Prefs.gamedat;
        newGame.gameset = Prefs.gameset;
        newGame.suppressLostCards = Prefs.suppressLostCards;
        newGame.suppressLostDats = Prefs.suppressLostDats;
        newGame.useTextOnly = Prefs.useTextOnly;
        for (int i=0; i < Prefs.cardTextArray.size(); i++) {
          newGame.cardTextArray.add(Prefs.cardTextArray.get(i));
        }
        newGame.cardTextOverlayStatsField = Prefs.cardTextOverlayStatsField;
      gameList.addGame(newGame);
    }
    else if (source == gameRemBtn) {
      gameList.removeSelectedGame();
    }
    else if (source == gameLoadBtn) {
      GamePrefs selectedGame = gameList.getSelectedGame();
      backField.setText(selectedGame.defaultBackPath);
      emptyDeckField.setText(selectedGame.defaultEmptyDeckPath);
      blankCardField.setText(selectedGame.defaultBlankPath);
      gamelocField.setText(selectedGame.gameloc);
      gamedatField.setText(selectedGame.gamedat);
      gamesetField.setText(selectedGame.gameset);
      lostCardsCheck.setSelected(selectedGame.suppressLostCards);
      lostDatsCheck.setSelected(selectedGame.suppressLostDats);
      textOnlyCheck.setSelected(selectedGame.useTextOnly);
      tempTextOverlayStatsField = selectedGame.cardTextOverlayStatsField;
      tempTextArray = selectedGame.cardTextArray;
    }
    else if (source == gameSaveBtn) {
      GamePrefs selectedGame = gameList.getSelectedGame();
      selectedGame.defaultBackPath = backField.getText();
      selectedGame.defaultEmptyDeckPath = emptyDeckField.getText();
      selectedGame.defaultBlankPath = blankCardField.getText();
      selectedGame.gameloc = gamelocField.getText();
      selectedGame.gamedat = gamedatField.getText();
      selectedGame.gameset = gamesetField.getText();
      selectedGame.suppressLostCards = lostCardsCheck.isSelected();
      selectedGame.suppressLostDats = lostDatsCheck.isSelected();
      selectedGame.useTextOnly = textOnlyCheck.isSelected();
      selectedGame.cardTextOverlayStatsField = tempTextOverlayStatsField;
      selectedGame.cardTextArray = tempTextArray;
    }
    else if (source == backBtn) {
      String result = choosePath(Prefs.homePath +"cards", false);
      if (result != null) backField.setText(result);
    }
    else if (source == emptyDeckBtn) {
      String result = choosePath(Prefs.homePath +"images", false);
      if (result != null) emptyDeckField.setText(result);
    }
    else if (source == blankCardBtn) {
      String result = choosePath(Prefs.homePath +"images", false);
      if (result != null) blankCardField.setText(result);
    }
    else if (source == gamelocBtn) {
      String result = choosePath(Prefs.homePath +"cards", true);
      if (result != null) gamelocField.setText(result);
    }
    else if (source == gamedatBtn) {
      String result = choosePath(Prefs.homePath +"cards", false);
      if (result != null) gamedatField.setText(result);
    }
    else if (source == gamesetBtn) {
      String result = choosePath(Prefs.homePath +"cards", false);
      if (result != null) gamesetField.setText(result);
    }
    else if (source == saveBtn) {
      boolean result = applyPaths();
      if (result == true) {
        gameList.savePrefs();
        pronoun.setVisible(false);
        pronoun.dispose();
        Prefs.Cache.setup();
      }
    }
    else if (source == applyBtn) {
      boolean result = applyPaths();
      if (result == true) {
        pronoun.setVisible(false);
        pronoun.dispose();
        Prefs.Cache.setup();
      }
    }
    else if (source == editLayoutBtn) {
      layoutPanel.removeAll();
      layoutPanel.add(new PrefsLayoutPanel(frame, tempTextArray));
      showPanel("layout");
    }
  }


  /**
   * Switches the content of the frame and calls pack().
   * CardLayout would've used the largest preferred size
   * among all its panels, not reshaping on switches.
   *
   * @param name either "games" or "layout"
   */
  public void showPanel(String name) {
    if (!name.equals("games") && !name.equals("layout")) return;
    pane.removeAll();
    if (name.equals("games")) pane.add(prefsPanel);
    else if (name.equals("layout")) pane.add(layoutPanel);
    pronoun.pack();
  }


  private boolean applyPaths() {
    if (!new File(gamelocField.getText()).exists()) {
      JTextArea message = new JTextArea("The game location does not exist.\nYou can download game files here:\n\n"+ ArcanistCCG.WEBSITE);
        message.setEditable(false);
        message.setBorder(BorderFactory.createEtchedBorder());
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), message, "Game files missing", JOptionPane.PLAIN_MESSAGE);
      return false;
    }

    GamePrefs tmpGame = new GamePrefs();
      tmpGame.defaultBackPath = backField.getText();
      tmpGame.defaultEmptyDeckPath = emptyDeckField.getText();
      tmpGame.defaultBlankPath = blankCardField.getText();
      tmpGame.gameloc = gamelocField.getText();
      tmpGame.gamedat = gamedatField.getText();
      tmpGame.gameset = gamesetField.getText();
      tmpGame.suppressLostCards = lostCardsCheck.isSelected();
      tmpGame.suppressLostDats = lostDatsCheck.isSelected();
      tmpGame.useTextOnly = textOnlyCheck.isSelected();
      tmpGame.cardTextOverlayStatsField = tempTextOverlayStatsField;
      tmpGame.cardTextArray = tempTextArray;
      tmpGame.apply(frame);

    return true;
  }


  private String choosePath(String startPath, boolean dirOnly) {
    JFileChooser chooser = new JFileChooser(startPath);
      if (dirOnly) chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    int status = chooser.showOpenDialog(frame.getDesktop());
    if (status == JFileChooser.APPROVE_OPTION) {
      File filegrab = chooser.getSelectedFile();
      String fileName = "";
      if (filegrab.getAbsolutePath().startsWith(Prefs.homePathAb) == true && filegrab.getAbsolutePath().length() > Prefs.homePathAb.length())
        fileName = Prefs.homePath + filegrab.getAbsolutePath().substring(Prefs.homePathAb.length(), filegrab.getAbsolutePath().length()).replace('\\','/');
      else
        fileName = filegrab.getAbsolutePath().replace('\\','/');
      return fileName;
    }
    return null;
  }
}
