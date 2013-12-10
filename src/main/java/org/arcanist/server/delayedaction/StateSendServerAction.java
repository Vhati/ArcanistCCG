package org.arcanist.server.delayedaction;

import org.arcanist.server.delayedaction.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


/**
 * Sends the representative state to all AWAITING_STATE players.
 * Then waits until all BUSY LOADING_STATE players report an outcome.
 */
public class StateSendServerAction extends DelayedServerAction implements ServerNetInterpreter {

  private StateQueryServerAction queryAction = null;
  private char[] stateMsg = null;


  public StateSendServerAction(StateQueryServerAction queryAction) {
    super();
    this.queryAction = queryAction;
    setTimeout(60000);  // 60 sec timeout
  }

  public StateSendServerAction(char[] stateMsg) {
    super();
    this.stateMsg = stateMsg;
    setTimeout(60000);  // 60 sec timeout
  }


  @Override
  public void init(ServerPlayerInfo[] spsArray, ServerThread st) {
    super.init(spsArray, st);
    st.addNetInterpreter(this);

    // Send the state
    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.status == ServerPlayerState.STATUS_AWAITING_STATE) {
        tmpSps.status = ServerPlayerState.STATUS_LOADING_STATE;
        tmpSps.outcome = ServerPlayerState.OUTCOME_BUSY;

        ServerProtocolEvent statusSpe = new ServerProtocolEvent(st, ServerProtocolEvent.STATUS_CHANGED, new EnqueuedInt(spsArray[i].socket, tmpSps.status));
        st.processServerProtocolEvent(statusSpe);

        if (this.queryAction != null) {
          st.getOutQueue().enqueueMessage(spsArray[i].socket, queryAction.getStateMsg());
        } else {
          st.getOutQueue().enqueueMessage(spsArray[i].socket, stateMsg);
        }
      }
    }
  }

  /**
   * Cleanup called once after the action's final check.
   * BUSY and FAILED LOADING_STATE players are kicked.
   */
  @Override
  public void cleanup(ServerPlayerInfo[] spsArray, ServerThread st) {
    super.cleanup(spsArray, st);
    st.removeNetInterpreter(this);

    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.status == ServerPlayerState.STATUS_LOADING_STATE) {
        if (tmpSps.outcome == ServerPlayerState.OUTCOME_BUSY) {
          st.getOutQueue().enqueueToAll(st.chatNotice(ServerThread.SEVERITY_ERROR, "--Kicking "+ tmpSps.getAlias() +" for failing to send state ack.--"));
          st.logMessage(LogManager.ERROR_LEVEL, "DelayedServerAction kicking "+ tmpSps.getAlias() +" for failing to send state ack.");
        } else if (tmpSps.outcome == ServerPlayerState.OUTCOME_FAILED){
          st.getOutQueue().enqueueToAll(st.chatNotice(ServerThread.SEVERITY_ERROR, "--Kicking "+ tmpSps.getAlias() +" for failing to load the state.--"));
          st.logMessage(LogManager.ERROR_LEVEL, "DelayedServerAction kicking "+ tmpSps.getAlias() +" for failing to load the state.");
        }
        st.getOutQueue().enqueueKick(spsArray[i].socket);
      }
    }
  }


  /**
   * Responds to an incoming message, if relevant.
   * Flags players' OUTCOME based on stateResult messages.
   *
   * @return true if no other interpretation need be done, false otherwise
   */
  @Override
  public boolean interpret(ServerThread st, ServerPlayerState sps, String[] tokens, String origin, int originId, String type, int id, String action, int t) {
    //'table',arrayID(),'stateResult',success

    if (type.equals("table")) {
      if (action.equals("stateResult")) {
        if (sps.status != ServerPlayerState.STATUS_LOADING_STATE) return false;
        boolean success = tokens[t++].equals("true");

        //st.logMessage(LogManager.INFO_LEVEL, "DelayedServerAction state loading result interpreted.");

        if (success == true) sps.outcome = ServerPlayerState.OUTCOME_SUCCEEDED;
        else sps.outcome = ServerPlayerState.OUTCOME_FAILED;
        st.scheduleActionCheck();
        return true;
      }
    }
    return false;
  }


  /**
   * Checks if the action is completed.
   *
   * @return EXPIRED   - Timed out (60 sec) waiting for all state acks.<br />
   *         WAIT      - Someone hasn't finished loading the state.<br />
   *         DONE      - All players loaded the state successfully.<br />
   *         FAILED    - Someone failed to load the state.
   */
  @Override
  public int check(ServerPlayerInfo[] spsArray, ServerThread st) {
    boolean someoneWaiting = false;
    boolean someoneFailed = false;

    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.status == ServerPlayerState.STATUS_LOADING_STATE) {
        if (tmpSps.outcome == ServerPlayerState.OUTCOME_BUSY) {
          someoneWaiting = true;
        }
        else if (tmpSps.outcome == ServerPlayerState.OUTCOME_SUCCEEDED) {
          tmpSps.status = ServerPlayerState.STATUS_ACTIVE;
          tmpSps.outcome = ServerPlayerState.OUTCOME_NA;

          ServerProtocolEvent statusSpe = new ServerProtocolEvent(st, ServerProtocolEvent.STATUS_CHANGED, new EnqueuedInt(spsArray[i].socket, tmpSps.status));
          st.processServerProtocolEvent(statusSpe);
        }
        else if (tmpSps.outcome == ServerPlayerState.OUTCOME_FAILED) {
          someoneFailed = true;
        }
      }
    }

    if (isTimedOut(System.currentTimeMillis())) {
      // Not fully acked yet, and timeout expired.
      st.logMessage(LogManager.ERROR_LEVEL, "DelayedServerAction state send timed out awaiting all acks.");
      return DelayedServerAction.EXPIRED;
    }
    else if (someoneWaiting == true) return DelayedServerAction.WAIT;
    else if (someoneFailed == true) return DelayedServerAction.FAILED;
    else return DelayedServerAction.DONE;
  }
}
