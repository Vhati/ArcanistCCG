package org.arcanist.server;

import org.arcanist.server.*;


public class ServerConnectionEvent {
  public static final int ACCEPTING_STARTED = 0;
  public static final int ACCEPTING_STOPPED = 1;
  public static final int SOCKET_ADDED = 2;
  public static final int SOCKET_REMOVED = 3;
  public static final int MAX_CLIENTS_CHANGED = 4;
  public static final int PORT_CHANGED = 5;
  public static final int SERVER_STOPPED = 6;


  private Object src = null;
  private int id = -1;
  private Object content = null;


  public ServerConnectionEvent(Object src, int id, Object content) {
    this.src = src;
    this.id = id;
    this.content = content;
  }


  /**
   * The object on which the Event initially occurred.
   */
  public Object getSource() {return src;}

  /**
   * Returns the event type.
   */
  public int getID() {return id;}

  /**
   * Returns the object associated with this event.
   *
   * @return varies with id...
   *          ACCEPTING_STARTED: null,
   *          ACCEPTING_STOPPED: null,
   *          SOCKET_ADDED: Socket,
   *          SOCKET_REMOVED: EnqueuedChars (message = remote address),
   *          MAX_CLIENTS_CHANGED: Integer,
   *          PORT_CHANGED: Integer,
   *          SERVER_STOPPED: null
   */
  public Object getContent() {return content;}
}
