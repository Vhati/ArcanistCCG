package org.arcanist.client;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A simple popup that monitors parser progress.
 */
public class DatParseProgressFrame extends JInternalFrame {

  private ArcanistCCGFrame frame = null;
  private JLabel currentCard = null;


  public DatParseProgressFrame(ArcanistCCGFrame f) {
    super("Loading CardText...",
      false, //resizable
      false, //closable
      false, //maximizable
      false); //iconifiable
    this.frame = f;
    this.reshape(frame.getDesktop().getSize().width/2-100, frame.getDesktop().getSize().height/2-30, 200, 60);

    JPanel pane = new JPanel();
      currentCard = new JLabel("");
        pane.add(currentCard);
      this.setContentPane(pane);
  }


  /**
   * Sets the displayed card name.
   */
  public void setCurrentCard(String s) {currentCard.setText(s);}


  /**
   * Shows this window.
   */
  public void parsingStarted() {
    frame.getDesktop().add(this);
    this.show();
    this.moveToFront();
  }


  /**
   * Closes and disposes of this window.
   */
  public void parsingCompleted() {
    try {this.setClosed(true);}                              //If closed while dragging
    catch (java.beans.PropertyVetoException e) {}            //  Bad things happen
    this.setVisible(false);                                  //  Unless setClosed is called
                                                             //setVisible prevents leaks
    this.dispose();
    frame.getDesktop().repaint();
  }
}
