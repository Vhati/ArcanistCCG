package org.arcanist.server;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.arcanist.server.*;


public class TestServer {

  public static void main(String[] args) {
    final ServerThread st = new ServerThread();

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        ServerListenPanel lp = new ServerListenPanel(st);
          st.addServerProtocolListener(lp);
        JFrame frame = new JFrame();
          frame.setContentPane(lp);
          frame.setSize(400, 200);
          frame.setVisible(true);
        st.startThread();
      }
    });

  }
}
