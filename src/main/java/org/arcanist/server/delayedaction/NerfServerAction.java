package org.arcanist.server.delayedaction;

import org.arcanist.server.delayedaction.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


/**
 * Sends nerfed(true/false) message to all local player ids.
 */
public class NerfServerAction extends DelayedServerAction implements ServerNetInterpreter {
  private boolean nerfedState = false;


  public NerfServerAction(boolean b) {
    super();
    nerfedState = b;
    setTimeout(5000);  // 5 sec timeout
  }


  public void init(ServerPlayerInfo[] spsArray, ServerThread st) {
    super.init(spsArray, st);
    st.addNetInterpreter(this);

    // Send the nerf message
    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      st.getOutQueue().enqueueMessage(spsArray[i].socket, st.tableNerf(nerfedState));
    }
  }

  /**
   * Cleanup called once after the action's final check.
   * Kick players aren't in the desired nerfed state.
   */
  @Override
  public void cleanup(ServerPlayerInfo[] spsArray, ServerThread st) {
    super.cleanup(spsArray, st);
    st.removeNetInterpreter(this);

    // Kick players who didn't end up in the desired nerfed state
    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.nerfed != nerfedState) {
        char[] chatMsg = st.chatNotice(ServerThread.SEVERITY_ERROR, "--Kicking "+ tmpSps.getAlias() +" for failing to send nerf ack.--");

        for (int j=0; j < spsArray.length; j++) {
          ServerPlayerState nagSps = spsArray[j].state;
          if (!isLocalPlayerId(nagSps.id)) continue;
          if (!st.isPlayerIdConnected(nagSps.id)) continue;

          st.getOutQueue().enqueueMessage(spsArray[j].socket, chatMsg);
        }
        st.logMessage(LogManager.ERROR_LEVEL, "DelayedServerAction kicking "+ tmpSps.getAlias() +" for failing to send nerf ack ("+ nerfedState +").");


        st.getOutQueue().enqueueKick(spsArray[i].socket);
      }
    }
  }


  /**
   * Responds to an incoming message, if relevant.
   *
   * @return true if no other interpretation need be done, false otherwise
   */
  @Override
  public boolean interpret(ServerThread st, ServerPlayerState sps, String[] tokens, String origin, int originId, String type, int id, String action, int t) {
    //'table',arrayID(),'nerfResult',success

    if (type.equals("table")) {
      if (action.equals("nerfResult")) {
        boolean success = tokens[t++].equals("true");

        //st.logMessage(LogManager.INFO_LEVEL, "DelayedServerAction nerf result interpreted.");

        sps.nerfed = success;
        st.scheduleActionCheck();
        return true;
      }
    }
    return false;
  }


  /**
   * Checks if the action is completed.
   *
   * @return EXPIRED   - Timed out (5 sec) waiting for all nerf acks.<br />
   *         WAIT      - Continue waiting for all nerf acks.<br />
   *         DONE      - All local players were notified.
   */
  @Override
  public int check(ServerPlayerInfo[] spsArray, ServerThread st) {
    boolean notDoneYet = false;

    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.nerfed != nerfedState) {
        notDoneYet = true;
        break;
      }
    }

    if (isTimedOut(System.currentTimeMillis())) {
      // Not fully acked yet, and timeout expired.
      st.logMessage(LogManager.ERROR_LEVEL, "DelayedServerAction nerf timed out awaiting all acks.");
      return DelayedServerAction.EXPIRED;
    }
    else if (notDoneYet == true) return DelayedServerAction.WAIT;
    else return DelayedServerAction.DONE;
  }
}
