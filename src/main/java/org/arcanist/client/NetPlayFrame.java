package org.arcanist.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.arcanist.client.*;
import org.arcanist.util.*;
import org.arcanist.server.*;


/**
 * A dialog to connect to or serve a network game.
 */
public class NetPlayFrame extends JInternalFrame implements ActionListener, ServerConnectionListener {

  private ArcanistCCGFrame frame = null;

  private NetPlayFrame pronoun = this;
  private Properties sysProp = System.getProperties();

  private JTabbedPane connectOrServePanel = null;
  private JTextField clientAddrField = null;
  private JTextField clientPortField = null;
  private JCheckBox clientProxyCheck = null;
  private JTextField clientProxyAddrField = null;
  private JTextField clientProxyPortField = null;
  private JTextField serverPortField = null;
  private JCheckBox serverClearCheck = null;
  private JButton okBtn = null;
  private JButton cancelBtn = null;

  public NetPlayFrame(ArcanistCCGFrame f) {
    super("NetPlay...",
      false, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);

    frame = f;

    JPanel pane = new JPanel(new BorderLayout());
      pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    GridBagConstraints gridC = new GridBagConstraints();

    JPanel clientPanel = new JPanel(new GridBagLayout());
      clientPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      gridC.fill = GridBagConstraints.NONE;
      gridC.weightx = 0;
      gridC.weighty = 0;

      gridC.gridx = 0;
      gridC.gridy = 0;
      gridC.gridheight = 1;
      gridC.gridwidth = 1;
      clientPanel.add(new JLabel("Server Address"), gridC);
      gridC.gridx++;
      clientPanel.add(new JLabel("Port"), gridC);

      gridC.gridx++;
      gridC.gridheight = 4;
      clientProxyCheck = new JCheckBox("Proxy");
        clientProxyCheck.setSelected( sysProp.get("socksProxyHost") != null );
        clientPanel.add(clientProxyCheck, gridC);

      gridC.gridx = 0;
      gridC.gridy++;
      gridC.gridheight = 1;
      gridC.gridwidth = 1;
      clientAddrField = new JTextField(10);
        clientPanel.add(clientAddrField, gridC);
      gridC.gridx++;
      clientPortField = new JTextField(new NumberDocument(), "6774", 4);
        clientPanel.add(clientPortField, gridC);

      gridC.gridx = 0;
      gridC.gridy++;
      clientPanel.add(new JLabel("Socks Address"), gridC);
      gridC.gridx++;
      clientPanel.add(new JLabel("Port"), gridC);

      gridC.gridx = 0;
      gridC.gridy++;
      clientProxyAddrField = new JTextField(10);
        clientProxyAddrField.setEnabled( sysProp.get("socksProxyHost") != null );
        clientPanel.add(clientProxyAddrField, gridC);
      gridC.gridx++;
      clientProxyPortField = new JTextField(new NumberDocument(), "1080", 4);
        clientProxyPortField.setEnabled( sysProp.get("socksProxyHost") != null );
        clientPanel.add(clientProxyPortField, gridC);

      gridC.fill = GridBagConstraints.VERTICAL;
      gridC.weighty = 1.0;
      gridC.gridx = 0;
      gridC.gridy++;
      gridC.gridheight = GridBagConstraints.REMAINDER;
      gridC.gridwidth = GridBagConstraints.REMAINDER;
      clientPanel.add(Box.createVerticalGlue(), gridC);


    JPanel serverPanel = new JPanel(new GridBagLayout());
      serverPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      gridC.fill = GridBagConstraints.NONE;
      gridC.weightx = 0;
      gridC.weighty = 0;

      gridC.gridx = 0;
      gridC.gridy = 0;
      gridC.gridheight = 1;
      gridC.gridwidth = 1;
      serverPanel.add(new JLabel("Port"), gridC);

      gridC.gridx = 0;
      gridC.gridy++;
      serverPortField = new JTextField(new NumberDocument(), "6774", 4);
        serverPanel.add(serverPortField, gridC);
      gridC.gridx++;
      serverClearCheck = new JCheckBox("Clear");
        serverClearCheck.setSelected(true);
        serverClearCheck.setToolTipText("Clear Table");
        serverPanel.add(serverClearCheck, gridC);

      gridC.fill = GridBagConstraints.VERTICAL;
      gridC.weighty = 1.0;
      gridC.gridx = 0;
      gridC.gridy++;
      gridC.gridheight = GridBagConstraints.REMAINDER;
      gridC.gridwidth = GridBagConstraints.REMAINDER;
      serverPanel.add(Box.createVerticalGlue(), gridC);

    connectOrServePanel = new JTabbedPane();
      connectOrServePanel.add(clientPanel, "Client");
      connectOrServePanel.add(serverPanel, "Server");

    JPanel southPanel = new JPanel();
      southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.X_AXIS));
      southPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
      southPanel.add(Box.createHorizontalGlue());
      okBtn = new JButton("OK");
        okBtn.setMargin(new Insets(2, 8, 2, 8));
        southPanel.add(okBtn);
      southPanel.add(Box.createHorizontalStrut(5));
      cancelBtn = new JButton("Cancel");
        cancelBtn.setMargin(new Insets(2, 8, 2, 8));
        southPanel.add(cancelBtn);
      southPanel.add(Box.createHorizontalGlue());

    Dimension okSize = okBtn.getPreferredSize();
    Dimension cancelSize = cancelBtn.getPreferredSize();
    Dimension southButtonSize = new Dimension(Math.max(okSize.width, cancelSize.width), Math.max(okSize.height, cancelSize.height));
    okBtn.setPreferredSize(southButtonSize);
    cancelBtn.setPreferredSize(southButtonSize);

    clientProxyCheck.addActionListener(this);
    okBtn.addActionListener(this);
    cancelBtn.addActionListener(this);

    pane.add(connectOrServePanel, BorderLayout.CENTER);
    pane.add(southPanel, BorderLayout.SOUTH);

    this.setContentPane(pane);
    this.pack();
    frame.getDesktop().add(this);

    int width = this.getWidth();
    int height = this.getHeight();
    if (frame.getDesktop().getSize().width/2-width/2 > 0 && frame.getDesktop().getSize().height/2-height/2 > 0)
      this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();
    pronoun.getRootPane().setDefaultButton(okBtn);
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == clientProxyCheck) {
      clientProxyAddrField.setEnabled(clientProxyCheck.isSelected());
      clientProxyPortField.setEnabled(clientProxyCheck.isSelected());
    }
    else if (source == okBtn) {
      int intPort = 0;
      try {
        if (connectOrServePanel.getSelectedIndex() == 0) {
          intPort = Integer.parseInt(clientPortField.getText());
        } else if (connectOrServePanel.getSelectedIndex() == 1) {
          intPort = Integer.parseInt(serverPortField.getText());
        }
      }
      catch (NumberFormatException f) {}
      if (intPort <= 0 || intPort > 65535) {
        JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Port must be from 1 to 65535.", "Invalid Port", JOptionPane.PLAIN_MESSAGE);
        return;
      }

      if (connectOrServePanel.getSelectedIndex() == 0) {
        if (clientAddrField.getText().length() == 0) {
          JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Fill in a server address to connect to.", "No Server Specified", JOptionPane.PLAIN_MESSAGE);
          return;
        }

        String proxyHost = "", proxyPort = "";
        if (clientProxyCheck.isSelected()) {
          proxyHost = clientProxyAddrField.getText();
          proxyPort = clientProxyPortField.getText();
          if (proxyHost.equals("") || proxyPort.equals("")) {
            proxyHost = ""; proxyPort = "";
          }
        }
        pronoun.setVisible(false);
        pronoun.dispose();

        connect(clientAddrField.getText(), intPort, proxyHost, proxyPort);
      }
      else if (connectOrServePanel.getSelectedIndex() == 1) {
        pronoun.setVisible(false);
        pronoun.dispose();

        if (serverClearCheck.isSelected()) frame.clearTable();
        ArcanistCCG.setServerThread(null);

        final ServerThread st = new ServerThread();
          st.setLogManager(ArcanistCCG.LogManager);
          st.getConnectionManager().addServerConnectionListener(this);
          st.getConnectionManager().setPort(intPort);

        new LobbyFrame(frame, st);

        st.startThread();

        ArcanistCCG.setServerThread(st);
      }
    }
    else if (source == cancelBtn) {
      pronoun.setVisible(false);
      pronoun.dispose();
    }
  }


  @Override
  public void fireServerConnectionEvent(ServerConnectionEvent e) {
    // When the server's ready, unregister this listener and connect()
    if (e.getID() == ServerConnectionEvent.ACCEPTING_STARTED) {
      ServerConnectionManager scm = (ServerConnectionManager)e.getSource();
      scm.removeServerConnectionListener(this);
      int port = scm.getPort();
      connect("127.0.0.1", port, null, null);
    }
  }


  private void connect(String dstHost, int dstPort, String socksHost, String socksPort) {
    if (socksHost != null && socksHost.length() > 0) {
      sysProp.put("socksProxyHost", socksHost);
      sysProp.put("socksProxyPort", socksPort);
    }

    try {
      Socket mySocket = new Socket();
      mySocket.connect(new InetSocketAddress(dstHost, dstPort), 7000);
      ClientThread ct = new ClientThread(frame, mySocket);
      ArcanistCCG.NetManager.setClientThread(ct);
      ct.startThread();
    }
    catch (UnknownHostException e) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Host not found.");
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Host not found.", "Error", JOptionPane.PLAIN_MESSAGE);
      return;
    }
    catch (IllegalArgumentException e) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Bad host or port.");
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Bad host or port.", "Error", JOptionPane.PLAIN_MESSAGE);
      return;
    }
    catch (SocketTimeoutException e) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Connection attempt timed out.");
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Connection attempt timed out.", "Error", JOptionPane.PLAIN_MESSAGE);
      return;
    }
    catch (IOException e) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't get I/O for the connection.");
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Couldn't get I/O for the connection.", "Error", JOptionPane.PLAIN_MESSAGE);
      return;
    }

    if (socksHost != null && socksHost.length() > 0) {
      sysProp.remove("socksProxyHost");
      sysProp.remove("socksProxyPort");
      //sysProp.put("socksProxyHost", null);
      sysProp.put("socksProxyPort", "1080");
    }
  }
}
