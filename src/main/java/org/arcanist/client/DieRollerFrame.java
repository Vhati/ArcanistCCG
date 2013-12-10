package org.arcanist.client;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.*;
import java.util.Random;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A DieRoller popup.
 */
public class DieRollerFrame extends JInternalFrame {

  private ArcanistCCGFrame frame = null;

  private JTextField diceField = null;
  private JTextField dieField = null;
  private JTextField resultField = null;


  /**
   * Constructs and shows the window.
   */
  public DieRollerFrame(ArcanistCCGFrame f) {
    super("DieRoller",
      false, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);
    if (Prefs.usePaletteFrames) this.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
    frame = f;

    int width = 121, height = 50; //These are old defaults, values get replaced by pack()

    JPanel pane = new JPanel();
      pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
      diceField = new JTextField(new NumberDocument(0,Integer.MAX_VALUE), "1", 2);
        pane.add(diceField);
      JLabel d = new JLabel("d");
        pane.add(d);
      dieField = new JTextField(new NumberDocument(1,Integer.MAX_VALUE), "20", 2);
        pane.add(dieField);
      JButton rollIt = new JButton("Roll");
        rollIt.setMargin(new Insets(1,2,1,2));
        pane.add(rollIt);
      resultField = new JTextField(new NumberDocument(), "", 2);
        resultField.setEditable(false);
        pane.add(resultField);

    //Set up listeners
    rollIt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          String totalString = "";
          int dieRoll = 0;
          int total = 0;
          int diceNum = (diceField.getText().equals("") ? 1 : Integer.parseInt(diceField.getText()));
          int sides = (dieField.getText().equals("") ? 2 : Integer.parseInt(dieField.getText()));
          for (int i=1; i <= diceNum; i++) {
            dieRoll = (int) (Math.random() * sides) + 1;
            total += dieRoll;
            totalString += (totalString.length()>0?",":"")+ dieRoll;
          }
          totalString = total +"  ("+ totalString +")";
          resultField.setText(Integer.toString(total));

          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

          ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +": Rolled "+ diceField.getText() +"d"+ dieField.getText() +": "+ totalString +"--");
        }
        catch (NumberFormatException exception) {}
      }
    });

    this.setContentPane(pane);
    pane.setPreferredSize(new Dimension(pane.getPreferredSize().width, pane.getPreferredSize().height-7));
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
   * Get the number of dice to be rolled.
   * @return number of dice
   */
  public String getDice() {
    return diceField.getText();
  }

  /**
   * Get the number of sides of the dice to be rolled.
   * @return number of sides
   */
  public String getDie() {
    return dieField.getText();
  }

  /**
   * Set the dice to be rolled.
   * @param count number of dice
   * @param sides number of sides
   */
  public void setDice(String count, String sides) {
    diceField.setText(count);
    dieField.setText(sides);
  }

  /**
   * Get the result of the last die roll.
   * @return the result
   */
  public String getResult() {
    return resultField.getText();
  }
}