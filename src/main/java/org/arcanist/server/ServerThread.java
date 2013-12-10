package org.arcanist.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;
import javax.swing.SwingUtilities;

import org.arcanist.server.*;
import org.arcanist.server.delayedaction.*;
import org.arcanist.util.*;


public class ServerThread implements Runnable, ServerConnectionListener, ServerProtocolListener {

  //These match arcanist.client.NetMsgMgr's chatNotice()
  public static final int SEVERITY_NOTICE1 = 1;
  public static final int SEVERITY_NOTICE2 = 2;
  public static final int SEVERITY_ERROR = 3;

  private ServerConnectionManager connectionManager = new ServerConnectionManager();
  private volatile boolean keepRunning = false;
  private Vector<Object> lineQueue = new Vector<Object>();
  private Object waitLock = new Object();
  private volatile LogManager logManager = null;
  private int nextQueryId = 0;

  /**
   * A Map to look up ServerPlayerStates from Sockets.
   * As sockets are added/removed, states are added/removed here too.
   */
  private Map<Socket,ServerPlayerState> stateMap = new WeakHashMap<Socket,ServerPlayerState>();
  private int maxPlayerId = 0;

  private LinkedList<ServerConnectionListener> connectionListeners = new LinkedList<ServerConnectionListener>();
  private LinkedList<ServerProtocolListener> protocolListeners = new LinkedList<ServerProtocolListener>();
  private LinkedList<ServerNetInterpreter> interpreterListeners = new LinkedList<ServerNetInterpreter>();

  // Synchronize on stateMap to use these too
  private boolean isActionCheckPending = false;
  private DelayedActionGroup delayedActionGroup = null;

  private SocketOutQueue outQueue = null;


  public ServerThread() {
    outQueue = new SocketOutQueue(connectionManager);

    connectionManager.addServerConnectionListener(this);
    connectionManager.addServerProtocolListener(this);
  }


  /**
   * Sets the log manager.
   * Use null to remove.
   * The connection manager and out queue threads will set it as well.
   */
  public void setLogManager(LogManager lm) {
    logManager = lm;
    connectionManager.setLogManager(logManager);
    outQueue.setLogManager(logManager);
  }

  /**
   * Sends an alert to the log manager.
   * Or if one isn't set, stdout. In either case,
   * use the static severity values from LogManager.
   *
   * @see LogManager#write(int severity, String message)
   */
  public void logMessage(int severity, String message) {
    if (logManager != null) {
      logManager.write(severity, message);
    } else {
      if (severity == LogManager.INFO_LEVEL) message = "Info:  "+ message;
      else if (severity == LogManager.ERROR_LEVEL) message = "Error: "+ message;
      else message = severity +"?:    "+ message;
      if (message != null) System.out.println(message);
    }
  }

  /**
   * Sends an alert to the log manager.
   * Or if one isn't set, stdout.
   *
   * @see LogManager#write(Throwable e, String message)
   */
  public void logException(Throwable e, String message) {
    if (logManager != null) {
      logManager.write(e, message);
    } else {
      if (message != null) System.out.println(message);
      e.printStackTrace();
    }
  }


  public ServerConnectionManager getConnectionManager() {return connectionManager;}


  public void startThread() {
    if (keepRunning == true) return;

    keepRunning = true;
    Thread t = new Thread(this);
    t.setPriority(Thread.NORM_PRIORITY);
    t.setDaemon(true);
    t.setName("ServerThread ("+ t.getName() +")");
    t.start();

    outQueue.startThread();
    connectionManager.startThread();
    connectionManager.enqueueAction(ServerConnectionManager.ACTION_START_ACCEPTING, null);
  }

  /**
   * Kills the thread.
   */
  public void killThread() {
    keepRunning = false;
    outQueue.killThread();
    connectionManager.killThread();
  }


  public void wakeup() {
    synchronized(waitLock) {
      waitLock.notify();
    }
  }


  @Override
  public void run() {
    while(keepRunning) {
      if (lineQueue.size() > 0) {
        EnqueuedChars es = (EnqueuedChars)lineQueue.remove(0);
        interpret(new String(es.message));
        actionCheck();
      } else {
        scheduleActionCheck();
        actionCheck();
        synchronized(waitLock) {
          try {waitLock.wait(1000);}
          catch (InterruptedException e) {}
        }
      }
    }
  }


  @Override
  public void fireServerConnectionEvent(ServerConnectionEvent e) {
    if (e.getSource() == this) return;

    processServerConnectionEvent(e);

    if (e.getID() == ServerConnectionEvent.SOCKET_ADDED) {
      Socket s = (Socket)e.getContent();

      synchronized (stateMap) {
        ServerPlayerState newState = new ServerPlayerState();
        newState.id = maxPlayerId++;
        //There are other players, sync
        if (stateMap.size() > 0) {
          newState.status = ServerPlayerState.STATUS_AWAITING_STATE;
          newState.outcome = ServerPlayerState.OUTCOME_BUSY;
        }
        stateMap.put(s, newState);

        String address = s.getInetAddress().toString();
        System.out.println("Client connected: "+ address);

        outQueue.includeSocket(s);
        outQueue.enqueueMessage(s, new String("server|0|main|0|playerid|"+ newState.id +"\n").toCharArray());
        ServerProtocolEvent idSpe = new ServerProtocolEvent(this, ServerProtocolEvent.ID_CHANGED, new EnqueuedInt(s, newState.id));
        processServerProtocolEvent(idSpe);
        ServerProtocolEvent statusSpe = new ServerProtocolEvent(this, ServerProtocolEvent.STATUS_CHANGED, new EnqueuedInt(s, newState.status));
        processServerProtocolEvent(statusSpe);

        outQueue.enqueueMessage(s, new String("server|0|main|0|aliasQuery\n").toCharArray());
        outQueue.enqueueMessage(s, new String("server|0|main|0|versionQuery\n").toCharArray());

        if (newState.status == ServerPlayerState.STATUS_AWAITING_STATE) {
          outQueue.enqueueMessage(s, tableNerf(true));
        }
        checkPlayerStates();
      }
    }
    else if (e.getID() == ServerConnectionEvent.SOCKET_REMOVED) {
      EnqueuedChars es = (EnqueuedChars)e.getContent();

      synchronized (stateMap) {
        Object sps = null;
        sps = stateMap.remove(es.socket);

        String address = new String(es.message);
        System.out.println("Client disconnected: "+ address);

        if (sps != null) announcePlayerDisconnect((ServerPlayerState)sps);
        checkPlayerStates();
      }
    }
    else if (e.getID() == ServerConnectionEvent.SERVER_STOPPED) {
      killThread();
    }
  }

  @Override
  public void fireServerProtocolEvent(ServerProtocolEvent e) {
    if (e.getSource() == this) return;
    if (e.getID() == ServerProtocolEvent.LINE_ADDED) {
      lineQueue.add(e.getContent());
    }
    else if (e.getID() == ServerProtocolEvent.WAKE_UP) {
      wakeup();
    }
  }


  public void addServerConnectionListener(ServerConnectionListener l) {
    synchronized (connectionListeners) {connectionListeners.add(l);}
  }

  public void removeServerConnectionListener(ServerConnectionListener l) {
    synchronized (connectionListeners) {connectionListeners.remove(l);}
  }

  public void processServerConnectionEvent(final ServerConnectionEvent e) {
    synchronized (connectionListeners) {
      final Object[] listeners = connectionListeners.toArray();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          for (int i=0; i < listeners.length; i++) {
            ((ServerConnectionListener)listeners[i]).fireServerConnectionEvent(e);
          }
        }
      });
    }
  }

  public void addServerProtocolListener(ServerProtocolListener l) {
    synchronized (protocolListeners) {protocolListeners.add(l);}
  }

  public void removeServerProtocolListener(ServerProtocolListener l) {
    synchronized (protocolListeners) {protocolListeners.remove(l);}
  }

  public void processServerProtocolEvent(final ServerProtocolEvent e) {
    synchronized (protocolListeners) {
      final Object[] listeners = protocolListeners.toArray();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          for (int i=0; i < listeners.length; i++) {
            ((ServerProtocolListener)listeners[i]).fireServerProtocolEvent(e);
          }
        }
      });
    }
  }


  public void addNetInterpreter(ServerNetInterpreter ni) {
    synchronized (interpreterListeners) {interpreterListeners.add(ni);}
  }

  public void removeNetInterpreter(ServerNetInterpreter ni) {
    synchronized (interpreterListeners) {interpreterListeners.remove(ni);}
  }

  public void scheduleActionCheck() {
    synchronized (stateMap) {
      isActionCheckPending = true;
    }
  }

  private void actionCheck() {
    synchronized (stateMap) {
      // Check for player states that need attending
      if (!isActionCheckPending) checkPlayerStates();
    }

    // Process delayed actions, looping if another check gets scheduled
    while (isActionCheckPending) {
      // Break synchronization between checks, and rebuild spsArray
      while (true) {
        synchronized (stateMap) {
          if (delayedActionGroup == null) break;
          ServerPlayerInfo[] spsArray = getPlayerStates();

          int checkResult = delayedActionGroup.check(spsArray, this);
          if (checkResult == DelayedServerAction.WAIT) {
            // No more checking for now
            break;
          } else if (checkResult == DelayedServerAction.NONE) {
            // Done with the group
            delayedActionGroup = null;
            break;
          } else {
            // Move on to the next action
            delayedActionGroup.nextAction(spsArray, this);
          }
        }
      }

      synchronized (stateMap) {
        isActionCheckPending = false;
        // Give player states another chance to queue up actions
        checkPlayerStates();
      }
    }
  }


  private void interpret(String input) {
    if (input.length() == 0) return;

    String origin = null;
    int originId = -1;
    String type = null;
    String[] tokens = input.replaceAll("[\\r\\n]", "").split("[|]");
    int t = 0;
    try {
      synchronized (stateMap) {
        origin = tokens[t++];
        originId = Integer.parseInt(tokens[t++]);
        type = tokens[t++];
        int id = Integer.parseInt(tokens[t++]);
        String action = tokens[t++];

        Socket s = getSocketByPlayerId(originId);
        if (s == null) return;

        ServerPlayerState sps = stateMap.get(s);
        if (sps == null) return;

        boolean preempted = false;

        // Handle these actions, regardless of player status
        if (!preempted && type.equals("main")) {
          //'main',arrayID(),'version',version
          if (!preempted && action.equals("version")) {
            String chatAlias = sps.getAlias();
            String versionStr = tokens[t++];

            outQueue.enqueueToAll(chatNotice(SEVERITY_NOTICE2, "--"+ chatAlias +" version: "+ versionStr +"--"));
            preempted = true;
          }
        }
        if (!preempted && type.equals("server")) {
          //'server',arrayID(),'aliasRequested',reqAlias
          if (!preempted && action.equals("aliasRequested")) {
            String reqAlias = (t==tokens.length?"":tokens[t++]);
            String oldAlias = sps.getAlias();
            sps.alias = reqAlias;

            ServerProtocolEvent aliasSpe = new ServerProtocolEvent(this, ServerProtocolEvent.ALIAS_CHANGED, new EnqueuedChars(s, reqAlias));
            processServerProtocolEvent(aliasSpe);

            String tmpResponse = "player|"+ originId +"|main|0|aliasSet|"+ reqAlias +"\n";
            outQueue.enqueueMessage(s, tmpResponse.toCharArray());

            outQueue.enqueueToAll(chatNotice(SEVERITY_NOTICE1, "--"+ oldAlias +" set alias to "+ ((ServerPlayerState)sps).getAlias() +"--"));
            preempted = true;
          }

          //'server',arrayID(),'syncRequested',offendingPlayerId
          if (!preempted && action.equals("syncRequested")) {
            int offendingPlayerId = Integer.parseInt(tokens[t++]);
            String offenderAlias = "[???]";
            String complainerAlias = sps.getAlias();

            ServerPlayerInfo[] spsArray = getPlayerStates();

            for (int i=0; i < spsArray.length; i++) {
              ServerPlayerState tmpSps = spsArray[i].state;
              if (tmpSps.id == offendingPlayerId) {
                offenderAlias = tmpSps.getAlias();
                break;
              }
            }

            outQueue.enqueueToAll(chatNotice(SEVERITY_ERROR, "--Out of sync: "+ complainerAlias +" couldn't do something "+ offenderAlias +" wanted. 'File-Send State' will resync everybody to one's own table.--"));
            preempted = true;
          }
        }


        // Let registered interpreters take a crack at the message
        synchronized (interpreterListeners) {
          for (int i=0; !preempted && i < interpreterListeners.size(); i++) {
            ServerNetInterpreter ni = (ServerNetInterpreter)interpreterListeners.get(i);
            preempted = ni.interpret(this, sps, tokens, origin, originId, type, id, action, t);
          }
        }

        // Whitelist actions for busy players, or drop the message
        if (sps.status != ServerPlayerState.STATUS_ACTIVE) {
          boolean drop = true;
          if (type.equals("chat")) {
            drop = false;
          }

          if (!drop) outQueue.enqueueToAll(new String(input+"\n").toCharArray());
          preempted = true;
        }

        // If active, handle specific actions, or relay
        if (!preempted && type.equals("deck")) {
          //'deck',arrayID(),'added',x,y,locked,autoface,facing,toBottom,drawOffset,deckString
          if (!preempted && action.equals("added")) {
            // If the server has other plans, don't bother
            if (delayedActionGroup == null) {
              String startString = "--"+ sps.getAlias() +" is loading a deck.--";
              delayedActionGroup = new DelayedActionGroup();
              InitDeckLoadServerAction initDeckAction = new InitDeckLoadServerAction(new String(input+"\n").toCharArray(), startString);
                delayedActionGroup.addAction(initDeckAction);
              scheduleActionCheck();
              preempted = true;
            }
          }
        }
        if (!preempted && type.equals("table")) {
          //'table',arrayID(),'stateLoaded',neutralView,xml lines...
          if (!preempted && action.equals("stateLoaded")) {
            if (delayedActionGroup == null) {
              String startString = "--"+ sps.getAlias() +" is loading a game state.--";
              delayedActionGroup = new DelayedActionGroup();
              InitStateLoadServerAction initStateAction = new InitStateLoadServerAction(new String(input+"\n").toCharArray(), startString);
                delayedActionGroup.addAction(initStateAction);
              scheduleActionCheck();
              preempted = true;
            }
          }
        }

        if (!preempted) outQueue.enqueueToAll(new String(input+"\n").toCharArray());
      }
    }
    catch (NumberFormatException e) {
      logException(e, "Number parsing error");
    }
    catch (IndexOutOfBoundsException e) {
      logException(e, "Array index error while interpreting line: "+ input);
    }
  }


  /**
   * Returns the next available query id.
   */
  public int getNextQueryId() {
    return nextQueryId++;
  }


  /**
   * Returns this ServerThread's outbound network message queue.
   */
  public SocketOutQueue getOutQueue() {
    return outQueue;
  }


  private void announcePlayerDisconnect(ServerPlayerState sps) {
    String chatAlias = ((ServerPlayerState)sps).getAlias();
    outQueue.enqueueToAll(chatNotice(SEVERITY_ERROR, "--"+ chatAlias +" disconnected--"));

    String toAll = "server|0|main|0|disconnected|"+ ((ServerPlayerState)sps).id +"\n";
    outQueue.enqueueToAll(toAll.toCharArray());
  }

  public char[] chatNotice(int severity, String msg) {
    String result = "server|0|chat|0|notice|"+ severity +"|"+ msg +"\n";
    return result.toCharArray();
  }

  public char[] tableNerf(boolean state) {
    String result = "server|0|table|0|nerfed|"+ state +"\n";
    return result.toCharArray();
  }

  public char[] tableStateQuery(int queryId) {
    String result = "server|0|table|0|stateQuery|"+ queryId +"\n";
    return result.toCharArray();
  }

  public char[] tableStateLoaded(boolean neutralView, String state) {
    String result = "server|0|table|0|stateLoaded|true|"+ state +"\n";
    return result.toCharArray();
  }


  /**
   * Reacts to player states instead of incoming messages.
   * If someone is AWAITING_STATE, an appropriate delayed
   * server action group will be queued up, if one isn't
   * already in progress.
   */
  private void checkPlayerStates() {
    synchronized (stateMap) {
      if (delayedActionGroup != null) return;
      ServerPlayerInfo[] spsArray = getPlayerStates();

      for (int i=0; i < spsArray.length; i++) {
        ServerPlayerState tmpSps = spsArray[i].state;
        if (tmpSps.status == ServerPlayerState.STATUS_AWAITING_STATE) {
          delayedActionGroup = new DelayedActionGroup();
          InitSyncServerAction initSyncAction = new InitSyncServerAction();
            delayedActionGroup.addAction(initSyncAction);
          scheduleActionCheck();
          return;
        }
      }
    }
  }


  public boolean isPlayerIdConnected(int id) {
    if (getSocketByPlayerId(id) != null) return true;
    else return false;
  }

  private Socket getSocketByPlayerId(int id) {
    if (id == -1) return null;
    synchronized (stateMap) {
      for (Map.Entry<Socket,ServerPlayerState> entry : stateMap.entrySet()) {
        ServerPlayerState tmpSps = entry.getValue();
        if (tmpSps != null && tmpSps.id == id) {
          return entry.getKey();
        }
      }
    }
    return null;
  }


  public ServerPlayerInfo[] getPlayerStates() {
    ServerPlayerInfo[] result = null;
    synchronized (stateMap) {
      result = new ServerPlayerInfo[stateMap.size()];
      int oopsCount = 0;
      int i = 0;
      for (Map.Entry<Socket,ServerPlayerState> entry : stateMap.entrySet()) {
        if (entry.getValue() != null) {
          result[i] = new ServerPlayerInfo();
          result[i].socket = entry.getKey();
          result[i].state = entry.getValue();
        } else {
          result[i] = null;
          oopsCount++;
        }
        i++;
      }
      if (oopsCount > 0) {
        ServerPlayerInfo[] oldResult = result;
        result = new ServerPlayerInfo[stateMap.size() - oopsCount];
        int j = 0;
        for (i=0; i < oldResult.length; i++) {
          if (oldResult[i] != null) result[j++] = oldResult[i];
        }
      }
    }
    return result;
  }
}
