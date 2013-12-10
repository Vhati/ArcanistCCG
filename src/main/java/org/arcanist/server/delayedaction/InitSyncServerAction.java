package org.arcanist.server.delayedaction;

import java.util.ArrayList;
import java.util.List;

import org.arcanist.server.delayedaction.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


/**
 * Populates an empty DelayedActionGroup to sync table states.
 */
public class InitSyncServerAction extends DelayedServerAction {

  private DelayedServerAction chatStartAction = null;
  private DelayedServerAction nerfAction = null;
  private StateQueryServerAction stateQueryAction = null;
  private DelayedServerAction stateQueryRetryAction = null;
  private DelayedServerAction stateSendAction = null;
  private DelayedServerAction stateSendMoreAction = null;
  private DelayedServerAction unnerfAction = null;
  private DelayedServerAction chatDoneAction = null;


  public InitSyncServerAction() {super();}


  @Override
  public int check(ServerPlayerInfo[] spsArray, ServerThread st) {
    DelayedActionGroup actionGroup = getActionGroup();
    if (actionGroup == null) return DelayedServerAction.MOOT;

    List<Integer> activePlayerIds = new ArrayList<Integer>();
    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      if (tmpSps.status == ServerPlayerState.STATUS_ACTIVE) {
        activePlayerIds.add(new Integer(tmpSps.id));
      }
      actionGroup.addLocalPlayerId(tmpSps.id);
    }

    chatStartAction = new ChatServerAction(ServerThread.SEVERITY_ERROR, "--Sync Started--");
      // Inherit all local player ids from the group
      actionGroup.addAction(chatStartAction);
    nerfAction = new NerfServerAction(true);
      nerfAction.addLocalPlayerId(activePlayerIds);
      actionGroup.addAction(nerfAction);
    // NerfServerAction timeout: kick

    stateQueryAction = new StateQueryServerAction();
      // Inherit all local player ids from the group
      actionGroup.addAction(stateQueryAction);

    stateQueryRetryAction = new DelayedServerAction() {
      public int check(ServerPlayerInfo[] spsArray, ServerThread st) {
        DelayedActionGroup actionGroup = getActionGroup();
        int lastResult = actionGroup.getLastResult();
        if (lastResult == DelayedServerAction.EXPIRED) {      // Retry
          actionGroup.setNextAction(stateQueryAction);
        }
        else if (lastResult == DelayedServerAction.MOOT) {    // Skip to the end
          actionGroup.setNextAction(unnerfAction);
        }
        else if (lastResult == DelayedServerAction.FAILED) {  // Let humans sort it out
          actionGroup.setNextAction(unnerfAction);
        }
        return DelayedServerAction.DONE;
      }
    };
      actionGroup.addAction(stateQueryRetryAction);

    stateSendAction = new StateSendServerAction(stateQueryAction);
      // Inherit all local player ids from the group
      actionGroup.addAction(stateSendAction);
    // StateSendServerAction failure/timeout: kick

    // If any new global players connected during this action group and are AWAITING_STATE,
    //   add them to the group's local player ids and roll back to stateSendAction
    stateSendMoreAction = new DelayedServerAction() {
      public int check(ServerPlayerInfo[] spsArray, ServerThread st) {
        DelayedActionGroup actionGroup = getActionGroup();
        boolean addedPlayers = false;

        for (int i=0; i < spsArray.length; i++) {
          ServerPlayerState tmpSps = spsArray[i].state;
          if (isLocalPlayerId(tmpSps.id) == true) continue;
          if (!st.isPlayerIdConnected(tmpSps.id)) continue;
          if (tmpSps.status == ServerPlayerState.STATUS_AWAITING_STATE) {
            addedPlayers = true;
            actionGroup.addLocalPlayerId(tmpSps.id);
          }
        }
        if (addedPlayers == true) {
          actionGroup.setNextAction(stateSendAction);
          st.logMessage(LogManager.INFO_LEVEL, "DelayedServerAction resending the state, to new players.");
        }
        return DelayedServerAction.DONE;
      }
    };
      actionGroup.addAction(stateSendMoreAction);

    unnerfAction = new NerfServerAction(false);
      // Inherit all local player ids from the group
      actionGroup.addAction(unnerfAction);
    // NerfServerAction timeout: kick

    chatDoneAction = new ChatServerAction(ServerThread.SEVERITY_NOTICE1, "--Sync Done--");
      // Inherit all local player ids from the group
      actionGroup.addAction(chatDoneAction);

    return DelayedServerAction.DONE;
  }
}
