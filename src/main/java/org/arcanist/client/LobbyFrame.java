package org.arcanist.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.arcanist.client.*;
import org.arcanist.server.*;


/**
 * A Lobby popup.
 */
public class LobbyFrame extends JInternalFrame implements ActionListener {

  private ArcanistCCGFrame frame = null;

  private ServerThread serverThread = null;
  private ServerListenPanel listenPanel = null;


  public LobbyFrame(ArcanistCCGFrame f, ServerThread st) {
    super("Lobby",
      true, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);
    frame = f;
    serverThread = st;

    int width = 440, height = 200;

    listenPanel = new ServerListenPanel(serverThread);
      listenPanel.addActionListener(this);
      serverThread.addServerProtocolListener(listenPanel);

    ServerPlayerInfo[] spsArray = st.getPlayerStates();
    listenPanel.setPlayers(spsArray);

    this.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosing(InternalFrameEvent e) {
        if (serverThread.getConnectionManager().isRunning()) {
          serverThread.getConnectionManager().enqueueAction(ServerConnectionManager.ACTION_STOP_ACCEPTING, null);
        }
      }
    });


    this.setContentPane(listenPanel);
    frame.getDesktop().add(this);
    this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    this.show();
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == listenPanel) {
      this.doDefaultCloseAction();
    }
  }
}