package org.arcanist.server.delayedaction;

import java.util.*;
import java.net.*;

import org.arcanist.server.delayedaction.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


/**
 * Sends the deck to all ACTIVE players.
 * Then waits until all BUSY LOADING_DECK players report an outcome.
 */
public class DeckSendServerAction extends DelayedServerAction implements ServerNetInterpreter {

  char[] deckMsg = null;


  public DeckSendServerAction(char[] deckMsg) {
    super();
    this.deckMsg = deckMsg;
    setTimeout(30000);  // 30 sec timeout
  }


  @Override
  public void init(ServerPlayerInfo[] spsArray, ServerThread st) {
    super.init(spsArray, st);
    st.addNetInterpreter(this);

    // Send the deck
    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.status == ServerPlayerState.STATUS_ACTIVE) {
        tmpSps.status = ServerPlayerState.STATUS_LOADING_DECK;
        tmpSps.outcome = ServerPlayerState.OUTCOME_BUSY;

        ServerProtocolEvent statusSpe = new ServerProtocolEvent(st, ServerProtocolEvent.STATUS_CHANGED, new EnqueuedInt(spsArray[i].socket, tmpSps.status));
        st.processServerProtocolEvent(statusSpe);

        st.getOutQueue().enqueueMessage(spsArray[i].socket, deckMsg);
      }
    }
  }

  /**
   * Cleanup called once after the action's final check.
   * BUSY and FAILED LOADING_DECK players are reset to ACTIVE.
   */
  @Override
  public void cleanup(ServerPlayerInfo[] spsArray, ServerThread st) {
    super.cleanup(spsArray, st);
    st.removeNetInterpreter(this);

    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.status == ServerPlayerState.STATUS_LOADING_DECK) {
        // TODO: For now, revert fails to ACTIVE instead of dropping
        tmpSps.status = ServerPlayerState.STATUS_ACTIVE;
        tmpSps.status = ServerPlayerState.OUTCOME_NA;

        ServerProtocolEvent statusSpe = new ServerProtocolEvent(st, ServerProtocolEvent.STATUS_CHANGED, new EnqueuedInt(spsArray[i].socket, tmpSps.status));
        st.processServerProtocolEvent(statusSpe);

        if (tmpSps.outcome == ServerPlayerState.OUTCOME_BUSY) {
          st.getOutQueue().enqueueToAll(st.chatNotice(ServerThread.SEVERITY_ERROR, "--"+ tmpSps.getAlias() +" timed out loading the deck.--"));
        }
      }
    }
  }


  /**
   * Responds to an incoming message, if relevant.
   * Flags players' OUTCOME based on deck:addResult messages.
   *
   * @return true if no other interpretation need be done, false otherwise
   */
  @Override
  public boolean interpret(ServerThread st, ServerPlayerState sps, String[] tokens, String origin, int originId, String type, int id, String action, int t) {
    //'deck',arrayID(),'addResult',success

    if (type.equals("deck")) {
      if (action.equals("addResult")) {
        if (sps.status != ServerPlayerState.STATUS_LOADING_DECK) return false;
        boolean success = tokens[t++].equals("true");

        //st.logMessage(LogManager.INFO_LEVEL, "DelayedServerAction deck loading result interpreted.");

        if (success == true) {
          sps.outcome = ServerPlayerState.OUTCOME_SUCCEEDED;
          st.getOutQueue().enqueueToAll(st.chatNotice(ServerThread.SEVERITY_NOTICE2, "--"+ sps.getAlias() +" successfully loaded the deck.--"));
        }
        else {
          sps.outcome = ServerPlayerState.OUTCOME_FAILED;
          st.getOutQueue().enqueueToAll(st.chatNotice(ServerThread.SEVERITY_ERROR, "--"+ sps.getAlias() +" failed to load the deck.--"));
        }
        st.scheduleActionCheck();
        return true;
      }
    }
    return false;
  }


  /**
   * Checks if the action is completed.
   *
   * @return EXPIRED   - Timed out (30 sec) waiting for all deck acks.<br />
   *         WAIT      - Someone hasn't finished loading the deck.<br />
   *         DONE      - All players loaded the deck successfully.<br />
   *         FAILED    - Someone failed to load the deck (reverted to ACTIVE+NA, suggest a resync?).
   */
  @Override
  public int check(ServerPlayerInfo[] spsArray, ServerThread st) {
    boolean someoneWaiting = false;
    boolean someoneFailed = false;

    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.status == ServerPlayerState.STATUS_LOADING_DECK) {
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
      st.logMessage(LogManager.ERROR_LEVEL, "DeckSendServerAction timed out awaiting all deck acks.");
      return DelayedServerAction.EXPIRED;
    }
    else if (someoneWaiting == true) return DelayedServerAction.WAIT;
    else if (someoneFailed == true) return DelayedServerAction.FAILED;
    else return DelayedServerAction.DONE;
  }
}
