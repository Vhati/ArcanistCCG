package org.arcanist.server;

import org.arcanist.server.*;


public class ServerPlayerState {
  public static final int STATUS_ACTIVE = 0;
  public static final int STATUS_AWAITING_STATE = 1;
  public static final int STATUS_LOADING_STATE = 2;
  public static final int STATUS_LOADING_DECK = 3;

  public static final int OUTCOME_NA = 0;
  public static final int OUTCOME_BUSY = 1;
  public static final int OUTCOME_SUCCEEDED = 2;
  public static final int OUTCOME_FAILED = 3;

  public volatile int id = -1;
  public volatile String alias = "";
  public volatile int status = STATUS_ACTIVE;
  public volatile int outcome = OUTCOME_NA;
  public volatile boolean nerfed = false;


  /**
   * Gets the string representation of a status int.
   */
  public static String statusString(int n) {
    if (n == STATUS_ACTIVE) return "Active";
    if (n == STATUS_AWAITING_STATE) return "Awaiting State";
    if (n == STATUS_LOADING_STATE) return "Loading State";
    if (n == STATUS_LOADING_DECK) return "Loading Deck";
    return "???";
  }


  /**
   * Returns the player's alias.
   * If a custom alias is not set, [P#] is returned (# = player id).
   */
  public String getAlias() {
    if (alias.length() > 0) return alias;
    else if (id != -1) return "[P"+ id +"]";
    else return "[???]";
  }
}
