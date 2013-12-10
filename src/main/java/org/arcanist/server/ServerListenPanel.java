package org.arcanist.server;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import org.arcanist.server.*;


public class ServerListenPanel extends JPanel implements ActionListener, ServerConnectionListener, ServerProtocolListener {

  private static final int COLUMN_ID = 0;
  private static final int COLUMN_ADDRESS = 1;
  private static final int COLUMN_ALIAS = 2;
  private static final int COLUMN_STATUS = 3;

  private ServerThread serverThread = null;

  private final String portPrefix = "Port: ";
  private final String clientsPrefix = "Players: ";
  private String clientsSuffix = "";

  private String listenStopString = "Stop Listening";
  private String listenStartString = "Start Listening";

  private JLabel portLbl = null;
  private JLabel maxClientsLbl = null;
  private JButton listenBtn = null;
  private JButton kickBtn = null;
  private JButton kickAllBtn = null;

  private List<Socket> socketsByRowList = new ArrayList<Socket>();

  private DefaultTableModel clientTableModel = null;
  private JTable clientTable = null;


  public ServerListenPanel(ServerThread st) {
    super(new BorderLayout());
    serverThread = st;

    JPanel northPanel = new JPanel();
      northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.X_AXIS));

    northPanel.add(Box.createHorizontalStrut(5));

    portLbl = new JLabel();
      portLbl.setText(portPrefix + serverThread.getConnectionManager().getPort());
      northPanel.add(portLbl);

    northPanel.add(Box.createHorizontalStrut(10));

    maxClientsLbl = new JLabel();
      northPanel.add(maxClientsLbl);

    northPanel.add(Box.createHorizontalGlue());

    listenBtn = new JButton(listenStopString);
      listenBtn.setMargin(new Insets(0, 2, 0, 2));
      if (serverThread.getConnectionManager().isAccepting()) {
        listenBtn.setText(listenStopString);
      } else {
        listenBtn.setText(listenStartString);
      }
      northPanel.add(listenBtn);
      listenBtn.addActionListener(this);

    this.add(northPanel, BorderLayout.NORTH);


    JPanel centerPanel = new JPanel(new BorderLayout());
      centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

    clientTableModel = new DefaultTableModel(null, new String[] {"ID", "Address", "Alias", "Status"});
    clientTable = new JTable(clientTableModel) {
      public boolean isCellEditable(int row, int column) {return false;}
    };
      clientTable.setModel(clientTableModel);
      clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      clientTable.getTableHeader().setReorderingAllowed(false);
      clientTable.getColumnModel().getColumn(COLUMN_ID).setMaxWidth(20);
      clientTable.getColumnModel().getColumn(COLUMN_ID).setPreferredWidth(20);
      clientTable.getColumnModel().getColumn(COLUMN_ADDRESS).setMaxWidth(100);
      clientTable.getColumnModel().getColumn(COLUMN_ADDRESS).setPreferredWidth(100);
      clientTable.getColumnModel().getColumn(COLUMN_STATUS).setMaxWidth(95);
      clientTable.getColumnModel().getColumn(COLUMN_STATUS).setPreferredWidth(95);
    JScrollPane clientTableScrollPane = new JScrollPane(clientTable);
      clientTable.addNotify();
      centerPanel.add(clientTableScrollPane, BorderLayout.CENTER);

    JPanel clientActionPanel = new JPanel();
      clientActionPanel.setLayout(new BoxLayout(clientActionPanel, BoxLayout.Y_AXIS));
      clientActionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      centerPanel.add(clientActionPanel, BorderLayout.EAST);

    kickBtn = new JButton("Kick");
      kickBtn.setMargin(new Insets(0, 2, 0, 2));
      kickBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
      kickBtn.setToolTipText("Kick Selected Player");
      clientActionPanel.add(kickBtn);
      kickBtn.addActionListener(this);

    clientActionPanel.add(Box.createVerticalStrut(5));

    kickAllBtn = new JButton("KickAll");
      kickAllBtn.setMargin(new Insets(0, 2, 0, 2));
      kickAllBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
      kickAllBtn.setToolTipText("Kick All Players");
      clientActionPanel.add(kickAllBtn);
      kickAllBtn.addActionListener(this);

    this.add(centerPanel, BorderLayout.CENTER);


    JPanel southPanel = new JPanel(new BorderLayout());
      southPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
      JSeparator southSep = new JSeparator(SwingConstants.HORIZONTAL);
        southSep.setPreferredSize(new Dimension(1,3));
        southPanel.add(southSep, BorderLayout.NORTH);
      JLabel southLbl = new JLabel("(Closing this window will not disconnect anyone)");
        southLbl.setHorizontalAlignment(SwingConstants.CENTER);
        southPanel.add(southLbl, BorderLayout.CENTER);
    this.add(southPanel, BorderLayout.SOUTH);

    serverThread.addServerConnectionListener(this);


    int maxClients = serverThread.getConnectionManager().getMaxClients();
    clientsSuffix = (maxClients==0 ? "" : " / "+maxClients);
    updateClientsLabel();
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == listenBtn) {
      if (listenBtn.getText().equals(listenStopString)) {
        serverThread.getConnectionManager().enqueueAction(ServerConnectionManager.ACTION_STOP_ACCEPTING, null);
      } else {
        serverThread.getConnectionManager().enqueueAction(ServerConnectionManager.ACTION_START_ACCEPTING, null);
      }
    }
    else if (e.getSource() == kickBtn) {
      if (clientTableModel.getRowCount() > 0 && clientTable.getSelectedRowCount() == 1) {
        int row = clientTable.getSelectedRow();
        Object s = null;
        synchronized (socketsByRowList) {s = socketsByRowList.get(row);}
        serverThread.getConnectionManager().enqueueAction(ServerConnectionManager.ACTION_KICK_SOCKET, s);
      }
    }
    else if (e.getSource() == kickAllBtn) {
      Object[] allSockets = null;
      synchronized (socketsByRowList) {allSockets = socketsByRowList.toArray();}
      if (allSockets != null) {
        for (int i=allSockets.length-1; i >= 0; i--) {
          serverThread.getConnectionManager().enqueueAction(ServerConnectionManager.ACTION_KICK_SOCKET, allSockets[i]);
        }
      }
    }
  }


  @Override
  public void fireServerConnectionEvent(ServerConnectionEvent e) {
    if (e.getSource() == this) return;
    if (e.getID() == ServerConnectionEvent.SOCKET_ADDED) {
      Socket s = (Socket)e.getContent();
      String address = s.getInetAddress().toString();

      synchronized (socketsByRowList) {socketsByRowList.add(s);}
      clientTableModel.addRow(new String[] {"", address, "", ""});
      updateClientsLabel();
    }
    else if (e.getID() == ServerConnectionEvent.SOCKET_REMOVED) {
      Socket s = ((EnqueuedChars)e.getContent()).socket;
      int row = -1;
      synchronized (socketsByRowList) {
        row = socketsByRowList.indexOf(s);
        if (row != -1) socketsByRowList.remove(s);
      }
      if (row != -1) {
        clientTableModel.removeRow(row);
        updateClientsLabel();
      }
    }
    else if (e.getID() == ServerConnectionEvent.ACCEPTING_STOPPED) {
      listenBtn.setText(listenStartString);
    }
    else if (e.getID() == ServerConnectionEvent.ACCEPTING_STARTED) {
      listenBtn.setText(listenStopString);
    }
    else if (e.getID() == ServerConnectionEvent.PORT_CHANGED) {
      portLbl.setText(portPrefix + ((Integer)e.getContent()).toString());
    }
    else if (e.getID() == ServerConnectionEvent.MAX_CLIENTS_CHANGED) {
      int maxClients = ((Integer)e.getContent()).intValue();
      clientsSuffix = (maxClients==0 ? "" : " / "+maxClients);
      updateClientsLabel();
    }
    else if (e.getID() == ServerConnectionEvent.SERVER_STOPPED) {
      ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Server Stopped");
      fireActionPerformed(ae);
    }
  }

  @Override
  public void fireServerProtocolEvent(ServerProtocolEvent e) {
    if (e.getSource() == this) return;
    if (e.getID() == ServerProtocolEvent.ID_CHANGED) {
      EnqueuedInt ei = (EnqueuedInt)e.getContent();
      int row = -1;
      synchronized (socketsByRowList) {
        row = socketsByRowList.indexOf(ei.socket);
      }
      if (row != -1) {
        clientTableModel.setValueAt(Integer.toString(ei.message), row, COLUMN_ID);
      }
    }
    else if (e.getID() == ServerProtocolEvent.ALIAS_CHANGED) {
      EnqueuedChars es = (EnqueuedChars)e.getContent();
      int row = -1;
      synchronized (socketsByRowList) {
        row = socketsByRowList.indexOf(es.socket);
      }
      if (row != -1) {
        clientTableModel.setValueAt(new String(es.message), row, COLUMN_ALIAS);
      }
    }
    else if (e.getID() == ServerProtocolEvent.STATUS_CHANGED) {
      EnqueuedInt ei = (EnqueuedInt)e.getContent();
      int row = -1;
      synchronized (socketsByRowList) {
        row = socketsByRowList.indexOf(ei.socket);
      }
      if (row != -1) {
        clientTableModel.setValueAt(ServerPlayerState.statusString(ei.message), row, COLUMN_STATUS);
      }
    }
  }


  private void updateClientsLabel() {
    int clientCount = clientTableModel.getRowCount();
    maxClientsLbl.setText(clientsPrefix + clientCount + clientsSuffix);
  }


  /**
   * Adds an ActionListener.
   *
   * @param l the ActionListener to be added
   */
  public void addActionListener(ActionListener l) {
    super.listenerList.add(ActionListener.class, l);
  }

  /**
   * Removes an ActionListener.
   *
   * @param l the listener to be removed
   */
  public void removeActionListener(ActionListener l) {
    super.listenerList.remove(ActionListener.class, l);
  }

  /**
   * Notifies all listeners that have registered interest for
   * notification on this event type. The event instance
   * is lazily created using the event parameter.
   *
   * @param event the ActionEvent object
   */
  protected void fireActionPerformed(ActionEvent event) {
    // Guaranteed to return a non-null array
    Object[] listeners = super.listenerList.getListenerList();
    ActionEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i >= 0; i-=2) {
      if (listeners[i] == ActionListener.class) {
        // Lazily create the event:
        if (e == null) {
          String actionCommand = event.getActionCommand();
          e = new ActionEvent(this,
                              ActionEvent.ACTION_PERFORMED,
                              actionCommand,
                              event.getWhen(),
                              event.getModifiers());
        }
        ((ActionListener)listeners[i+1]).actionPerformed(e);
      }
    }
  }


  public void setPlayers(ServerPlayerInfo[] spsArray) {
    if (spsArray == null) return;

    synchronized (socketsByRowList) {
      socketsByRowList.clear();
      clientTableModel.getDataVector().clear();
      for (int i=0; i < spsArray.length; i++) {
        Socket s = spsArray[i].socket;
        ServerPlayerState sps = spsArray[i].state;
        String address = s.getInetAddress().toString();

        socketsByRowList.add(s);
        clientTableModel.addRow(new String[] {""+ sps.id, address, sps.alias});
      }
    }
    updateClientsLabel();
  }
}
