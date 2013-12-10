package org.arcanist.server;

import org.arcanist.server.*;


public class ServerProtocolEvent {
  public static final int WAKE_UP = 0;
  public static final int LINE_ADDED = 1;
  public static final int ID_CHANGED = 2;
  public static final int ALIAS_CHANGED = 3;
  public static final int STATUS_CHANGED = 4;


  private Object src = null;
  private int id = -1;
  private Object content = null;


  public ServerProtocolEvent(Object src, int id, Object content) {
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
   *          WAKE_UP: null,
   *          LINE_ADDED: EnqueuedChars
   *          ID_CHANGED: EnqueuedInt
   *          ALIAS_CHANGED: EnqueuedChars
   *          STATUS_CHANGED: EnqueuedInt (ServerPlayerState status)
   */
  public Object getContent() {return content;}
}
