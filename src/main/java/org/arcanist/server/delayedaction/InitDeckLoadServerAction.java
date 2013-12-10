package org.arcanist.server.delayedaction;

import java.util.ArrayList;
import java.util.List;

import org.arcanist.server.delayedaction.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


/**
 * Populates an empty DelayedActionGroup to sync table states.
 */
public class InitDeckLoadServerAction extends DelayedServerAction {

  private char[] deckMsg = null;
  private String startString = "--Deck Loading Started--";


  public InitDeckLoadServerAction(char[] deckMsg, String startString) {
    super();
    this.deckMsg = deckMsg;
    if (startString != null) this.startString = startString;
  }


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

    DelayedServerAction chatStartAction = new ChatServerAction(ServerThread.SEVERITY_ERROR, startString);
      // Inherit all local player ids from the group
      actionGroup.addAction(chatStartAction);
    DelayedServerAction nerfAction = new NerfServerAction(true);
      nerfAction.addLocalPlayerId(activePlayerIds);
      actionGroup.addAction(nerfAction);
    // NerfServerAction timeout: kick

    DelayedServerAction deckSendAction = new DeckSendServerAction(deckMsg);
      deckSendAction.addLocalPlayerId(activePlayerIds);
      actionGroup.addAction(deckSendAction);
    // DeckSendServerAction failure/timeout: ignore (let humans sort it out)

    DelayedServerAction unnerfAction = new NerfServerAction(false);
      unnerfAction.addLocalPlayerId(activePlayerIds);
      actionGroup.addAction(unnerfAction);
    // NerfServerAction timeout: kick

    DelayedServerAction chatDoneAction = new ChatServerAction(ServerThread.SEVERITY_NOTICE1, "--Deck Loading Done--");
      // Inherit all local player ids from the group
      actionGroup.addAction(chatDoneAction);

    return DelayedServerAction.DONE;
  }
}
