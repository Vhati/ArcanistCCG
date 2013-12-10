package org.arcanist.client;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.basic.BasicArrowButton;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A counter popup.
 */
public class CounterFrame extends JInternalFrame {

  private ArcanistCCGFrame frame = null;

  private CounterFrame pronoun = this;

  private JPanel pane = new JPanel();
  private String prevTitle = "";                             //Determines when a counter's title changed
  private String prevValue = "";                             //Determines when a counter's value changed
  private JTextField subTitle = new JTextField("Amount", 6);
  private JTextField value = new JTextField(new NumberDocument(), "0", 3);


  /**
   * Constructs and shows the window
   */
  public CounterFrame(ArcanistCCGFrame f) {
    super("Counter",
      true, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);
    if (Prefs.usePaletteFrames) this.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
    frame = f;

    int width = 170, height = 50; //These are old defaults, values get replaced by pack()

    subTitle.setHorizontalAlignment(JTextField.CENTER);
    value.setHorizontalAlignment(JTextField.CENTER);
    BasicArrowButton arrowLeft = new BasicArrowButton(SwingConstants.WEST);
    BasicArrowButton arrowRight = new BasicArrowButton(SwingConstants.EAST);

    pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
    pane.add(subTitle);
    pane.add(arrowLeft);
    pane.add(value);
    pane.add(arrowRight);

    // Set up listeners (-/+ the amount)
    arrowLeft.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try{value.setText(Integer.toString(value.getText().equals("") ? 0 : Integer.parseInt(value.getText())-1));}
        catch (NumberFormatException exception) {}
      }
    });

    arrowRight.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try{value.setText(Integer.toString(value.getText().equals("") ? 0 : Integer.parseInt(value.getText())+1));}
        catch (NumberFormatException exception) {}
      }
    });

    this.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameDeactivated(InternalFrameEvent e) {
        if (subTitle.getText().equals(prevTitle) == false || value.getText().equals(prevValue) == false) {
          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

          ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +": Set "+ subTitle.getText() +" counter: "+ value.getText() +"--");
          prevTitle = subTitle.getText();
          prevValue = value.getText();
        }
      }
    });

    this.setContentPane(pane);
    pane.setPreferredSize(new Dimension(pane.getPreferredSize().width, pane.getPreferredSize().height-3));
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
   * Sets the Counter's description.
   */
  public void setName(String input) {
    subTitle.setText(input);
  }

  /**
   * Gets the Counter's description.
   */
  public String getName() {
    return subTitle.getText();
  }


  /**
   * Sets the Counter's value.
   */
  public void setValue(String input) {
    value.setText(input);
  }

  /**
   * Gets the Counter's value.
   */
  public String getValue() {
    return value.getText();
  }
}
