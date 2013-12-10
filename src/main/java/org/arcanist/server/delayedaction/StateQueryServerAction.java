package org.arcanist.server.delayedaction;

import java.util.ArrayList;
import java.util.List;

import org.arcanist.server.delayedaction.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


/**
 * Waits for and collects an incoming state from a player.
 */
public class StateQueryServerAction extends DelayedServerAction implements ServerNetInterpreter {
  private List<Integer> blacklistedIds = new ArrayList<Integer>();
  private boolean candidatesExhausted = false;
  private int sentQueryId = 0;
  private char[] stateMsg = null;


  public StateQueryServerAction() {
    super();
    setTimeout(15000);  // 15 sec timeout
  }


  @Override
  public void init(ServerPlayerInfo[] spsArray, ServerThread st) {
    super.init(spsArray, st);
    stateMsg = null;
    sentQueryId = st.getNextQueryId();
    st.addNetInterpreter(this);

    // Send the query to a non-blacklisted player
    boolean querySent = false;
    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (isBlacklistedPlayerId(tmpSps.id)) continue;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.status == ServerPlayerState.STATUS_ACTIVE) {
        st.getOutQueue().enqueueMessage(spsArray[i].socket, st.tableStateQuery(sentQueryId));
        addBlacklistedPlayerId(tmpSps.id);
        querySent = true;
        break;
      }
    }
    if (querySent == false) candidatesExhausted = true;
  }

  @Override
  public void cleanup(ServerPlayerInfo[] spsArray, ServerThread st) {
    super.cleanup(spsArray, st);
    st.removeNetInterpreter(this);

    if (candidatesExhausted == true) {
      st.getOutQueue().enqueueToAll(st.chatNotice(ServerThread.SEVERITY_ERROR, "--No player could provide a representative state.--"));
      st.logMessage(LogManager.ERROR_LEVEL, "DelayedServerAction state query found no viable players with a representative state.");
    }
  }


  /**
   * Responds to an incoming message, if relevant.
   *
   * @return true if no other interpretation need be done, false otherwise
   */
  @Override
  public boolean interpret(ServerThread st, ServerPlayerState sps, String[] tokens, String origin, int originId, String type, int id, String action, int t) {
    //'table',arrayID(),'stateReply',queryId,xml lines...

    if (type.equals("table")) {
      if (action.equals("stateReply")) {
        int queryId = Integer.parseInt(tokens[t++]);
        if (queryId == sentQueryId) {
          //st.logMessage(LogManager.INFO_LEVEL, "DelayedServerAction state query reply interpreted.");

          StringBuffer stateBuf = new StringBuffer();
          while (t+1 <= tokens.length) {
            if (stateBuf.length() > 0) stateBuf.append("|");
            stateBuf.append(tokens[t++]);
          }
          stateMsg = st.tableStateLoaded(true, stateBuf.toString());
          st.scheduleActionCheck();
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Checks if the action is completed.
   *
   * @return FAILED    - No candidate players were left to query.<br />
   *         MOOT      - Nobody is awaiting a state.<br />
   *         WAIT      - Continue waiting for a representative state.<br />
   *         DONE      - All players loaded state successfully.<br />
   *         EXPIRED   - Timed out waiting for a representative state.
   */
  @Override
  public int check(ServerPlayerInfo[] spsArray, ServerThread st) {
    if (candidatesExhausted == true) return DelayedServerAction.FAILED;

    boolean awaitingState = false;
    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.status == ServerPlayerState.STATUS_AWAITING_STATE) {
        awaitingState = true;
        break;
      }
    }
    if (!awaitingState) {
      st.logMessage(LogManager.INFO_LEVEL, "DelayedServerAction state query moot, nobody needs a state now.");
      return DelayedServerAction.MOOT;
    }

    if (stateMsg != null) return DelayedServerAction.DONE;
    else if (isTimedOut(System.currentTimeMillis())) {
      // Someone's waiting, the state msg isn't set, and timeout expired.
      st.logMessage(LogManager.ERROR_LEVEL, "DelayedServerAction state query timed out awaiting a representative state, retry is possible.");
      return DelayedServerAction.EXPIRED;
    }
    else return DelayedServerAction.WAIT;
  }


  public void addBlacklistedPlayerId(int id) {
    Integer tmpId = new Integer(id);
    if (!blacklistedIds.contains(tmpId)) blacklistedIds.add(tmpId);
  }
  public boolean isBlacklistedPlayerId(int id) {
    return blacklistedIds.contains(new Integer(id));
  }


  public char[] getStateMsg() {
    return stateMsg;
  }
}
