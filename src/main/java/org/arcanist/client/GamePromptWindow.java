package org.arcanist.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.io.File;

import org.arcanist.client.*;


/**
 * A popup to choose a game.
 */
public class GamePromptWindow extends JInternalFrame implements ActionListener {

  private ArcanistCCGFrame frame = null;

  private GamePromptWindow pronoun = this;

  private JPanel pane = new JPanel(new BorderLayout());
  private GameList gameList = null;
  private JButton okBtn = null;
  private JButton cancelBtn = null;


  public GamePromptWindow(ArcanistCCGFrame f) {
    super("Choose a game...",
      false, //resizable
      false, //closable
      false, //maximizable
      false); //iconifiable

    frame = f;

    gameList = new GameList(true);
      gameList.setVisibleRowCount(10);

    int minWidth = 240;

    JScrollPane gameListScrollPane = new JScrollPane(gameList);
      if (gameListScrollPane.getPreferredSize().width > minWidth) {
        minWidth = gameListScrollPane.getPreferredSize().width;
      }
      gameListScrollPane.setPreferredSize(new Dimension(minWidth, gameListScrollPane.getPreferredSize().height));
      //gameListScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), BorderFactory.createLoweredBevelBorder()));
      gameListScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), gameListScrollPane.getBorder()));
      pane.add(gameListScrollPane, BorderLayout.CENTER);

    JPanel choicePanel = new JPanel(new BorderLayout());
      cancelBtn = new JButton("Cancel");
        cancelBtn.setMargin(new Insets(3, 5, 3, 5));
        choicePanel.add(cancelBtn, BorderLayout.EAST);
      okBtn = new JButton("OK");
        okBtn.setMargin(new Insets(3, 5, 3, 5));
        okBtn.setPreferredSize(cancelBtn.getPreferredSize());
        choicePanel.add(okBtn, BorderLayout.WEST);
      choicePanel.add(Box.createHorizontalStrut(5), BorderLayout.CENTER);
      choicePanel.setBorder(BorderFactory.createEmptyBorder(0, minWidth/2-choicePanel.getPreferredSize().width/2+5, 10, minWidth/2-choicePanel.getPreferredSize().width/2+5));
      pane.add(choicePanel, BorderLayout.SOUTH);


    okBtn.addActionListener(this);
    cancelBtn.addActionListener(this);


    this.setContentPane(pane);
    frame.getDesktop().add(this);

    this.pack();
    int width = this.getWidth();
    int height = this.getHeight();
    if (frame.getDesktop().getSize().width/2-width/2 > 0 && frame.getDesktop().getSize().height/2-height/2 > 0)
      this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    boolean picked = false;
    Object source = e.getSource();

    if (source == okBtn) {
      if (gameList.getSelectedIndex() == 0) {
        Rectangle newView = frame.getTableView();
        if (new File(Prefs.homePath +"cards/Demo").exists()) {
          Card bullies = new Card(frame, "Bullies", "", "Demo", Prefs.gameloc +"/DemoSet/Bullies.jpg", Prefs.defaultBackPath, false);
          bullies.setId(ArcanistCCG.getNextUnusedId());
          bullies.addToTable(newView.x, newView.y);

          Card nerdling = new Card(frame, "Nerdling", "", "Demo", Prefs.gameloc +"/DemoSet/Nerdling.jpg", Prefs.defaultBackPath, true);
          nerdling.setId(ArcanistCCG.getNextUnusedId());
          nerdling.addToTable(newView.x, newView.y+100);
        }
        else {
          Prefs.defaultBackPath = Prefs.homePath +"images/Back_default.gif";
        }
        picked = true;
      }
      else {
        picked = gameList.useSelectedGame(frame);
      }
    }
    else if (source == cancelBtn) {
      picked = true;
    }

    if (picked == true) {
      pronoun.setVisible(false);
      pronoun.dispose();
    }
  }
}
