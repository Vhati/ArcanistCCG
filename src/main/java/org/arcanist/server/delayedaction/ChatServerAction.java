package org.arcanist.server.delayedaction;

import java.util.*;
import java.net.*;

import org.arcanist.server.delayedaction.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


/**
 * Sends chat message to all local player ids.
 * See ServerThread for the severity arg.
 */
public class ChatServerAction extends DelayedServerAction {

  private int chatSeverity = ServerThread.SEVERITY_NOTICE1;
  private String chatString = null;


  public ChatServerAction(int chatSeverity, String chatString) {
    super();
    this.chatSeverity = chatSeverity;
    this.chatString = chatString;
  }


  /**
   * Checks if the action is completed.
   *
   * @return DONE      - All local players were notified.
   */
  @Override
  public int check(ServerPlayerInfo[] spsArray, ServerThread st) {
    char[] chatMsg = st.chatNotice(chatSeverity, chatString);

    for (int i=0; i < spsArray.length; i++) {
      ServerPlayerState tmpSps = spsArray[i].state;
      if (!isLocalPlayerId(tmpSps.id)) continue;
      if (!st.isPlayerIdConnected(tmpSps.id)) continue;

      st.getOutQueue().enqueueMessage(spsArray[i].socket, chatMsg);
    }

    st.logMessage(LogManager.INFO_LEVEL, "DelayedServerAction chat: "+ chatString);

    return DelayedServerAction.DONE;
  }
}
