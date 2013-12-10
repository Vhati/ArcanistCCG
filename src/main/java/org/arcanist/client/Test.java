package org.arcanist.client;

import java.awt.GridBagLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;
import org.arcanist.util.*;


public class Test {

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        guiInit();
      }
    });
  }


  private static void guiInit() {
    JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel pane = new JPanel(new GridBagLayout());
      frame.setContentPane(pane);


    frame.setSize(400, 400);
    frame.setLocationRelativeTo(null);
    //frame.pack();
    frame.setVisible(true);
  }
}
