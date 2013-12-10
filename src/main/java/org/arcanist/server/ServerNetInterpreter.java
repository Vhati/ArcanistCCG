package org.arcanist.server;

import org.arcanist.server.*;


public interface ServerNetInterpreter {


  /**
   * Responds to an incoming message, if relevant.
   * Flags players' OUTCOME based on stateResult messages.
   *
   * @return true if no other interpretation need be done, false otherwise
   */
  public boolean interpret(ServerThread st, ServerPlayerState sps, String[] tokens, String origin, int originId, String type, int id, String action, int t);
}
