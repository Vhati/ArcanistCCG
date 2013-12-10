package org.arcanist.client;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.Timer;

import org.arcanist.client.*;


/**
 * This forwards network traffic from objects to the network thread.
 */
public class NetMsgMgr {

  private ArcanistCCGFrame frame = null;

  private ClientThread ct = null;
  private int playerId = 0;


  /**
   * Cycles the frame's icon to blink in the taskbar.
   */
  private ActionListener appIconListener = new ActionListener() {
    Image positive = new ImageIcon(Prefs.appIconPositive).getImage();
    Image negative = new ImageIcon(Prefs.appIconNegative).getImage();
    boolean dim = false;

    @Override
    public void actionPerformed(ActionEvent e) {
      if (frame == null) return;

      if (frame.isActive()) {
        frame.setIconImage(positive);
        dim = false;
        ((javax.swing.Timer)e.getSource()).stop();
        return;
      }
      if (dim) {
        frame.setIconImage(positive);
        dim = false;
      } else {
        frame.setIconImage(negative);
        dim = true;
      }
    }
  };
  private Timer iconBlinker = new Timer(1000, appIconListener);



  public NetMsgMgr(ArcanistCCGFrame f) {
    frame = f;
    ct = new ClientThread(frame, null);
  }


  /**
   * Returns the playerId.
   */
  public int getPlayerId() {return playerId;}

  /**
   * Sets the playerId.
   */
  public void setPlayerId(int n) {playerId = n;}


  /**
   * Check online status.
   *
   * @return true if connected, false otherwise
   */
  public synchronized boolean isOnline() {
    return ct.connected;
  }


  /**
   * Returns the local player's alias.
   * If a custom alias is not set, [P#] is returned (# = player id).
   */
  public String getPlayerAlias() {
    if (Prefs.playerAlias.length() > 0) {
      return Prefs.playerAlias;
    } else {
      return "[P"+ ArcanistCCG.NetManager.getPlayerId() +"]";
    }
  }


  /**
   * Send data to opponents. Also appends to ChatLog.
   * Aside from deck adding, messages' empty args ("||") get replaced ("|?|").
   *
   * @param toNet message to opponent
   * @param toChat message to local chat
   */
/*
  public synchronized void send(String toNet, String toChat) {
    if (!toNet.startsWith("deck|0|added"))
      toNet = toNet.replaceAll("\\|\\|", "\\|\\?\\|").replaceAll("\\|\\|", "\\|\\?\\|");
    toNet = "player|"+ getPlayerId() +"|"+ toNet;

    if (isOnline()) ct.send(toNet);
    else ct.delayedInterpret(toNet);

    // //if (Prefs.verboseChat == true)
    // //  chatAppend("--"+ toNet +"--");
    if (toChat.length()>0)
      chatAppend(toChat);
  }
*/

  /**
   * Send data to opponents.
   * Aside from deck adding, messages' empty args ("||") get replaced ("|?|").
   *
   * @param toNet message to opponent
   */
  public synchronized void send(String toNet) {
    if (!toNet.startsWith("deck|0|added"))
      toNet = toNet.replaceAll("\\|\\|", "\\|\\?\\|").replaceAll("\\|\\|", "\\|\\?\\|");
    toNet = "player|"+ getPlayerId() +"|"+ toNet;

    if (isOnline()) ct.send(toNet);
    else ct.delayedInterpret(toNet);

    // //if (Prefs.verboseChat == true) {
    // //  chatAppend("--"+ toNet +"--");
    // //}
  }


  /**
   * Appends to ChatLog.
   * If the JumboView is available, the Chat tab turns yellow.
   * If the ArcanistCCG window is not focused, its taskbar icon blinks.
   *
   * @param styleName any of the defined style fields in ChatPanel
   * @param toChat message to local chat
   */
  public synchronized void notify(String styleName, String toChat) {
    if (toChat.length()>0 && frame.getJumboFrame() != null) {
      chatAppend(styleName, toChat);
      if (!styleName.equals(ChatPanel.STYLE_NOTICE2)) {
        frame.getJumboFrame().chatAlert();
      }
    }
    if (!styleName.equals(ChatPanel.STYLE_NOTICE2) && frame.isFocused() == false) {
      if (Prefs.appIconPositive != null && Prefs.appIconNegative != null) {
        iconBlinker.start();
      }
    }
  }

  /**
   * Backend method to append to ChatLog.
   *
   * @param styleName any of the defined style fields in ChatPanel
   * @param input message
   */
  private synchronized void chatAppend(String styleName, String input) {
    if (frame.getChatPanel() != null) {
      frame.getChatPanel().append(styleName, input);
    }
  }

  /**
   * Backend method to append to ChatLog.
   *
   * @param input message
   */
  private synchronized void chatAppend(String input) {
    chatAppend(ChatPanel.STYLE_CHAT, input);
  }


  /**
   * Switch to a new connection thread.
   * If one already exists and is online, it will be disconnected and replaced with this one.
   *
   * @param newThread the new ClientThread, or null for a local echo
   */
  public synchronized void setClientThread(ClientThread newThread) {
    if (isOnline()) ct.disconnect();
    if (newThread == null) newThread = new ClientThread(frame, null);
    ct = newThread;
  }


  public void mainVersion() {
    send("main|0|version|"+ ArcanistCCG.VERSION);
  }

  public void tableResize(int width, int height) {
    send("table|0|resized|"+ width +"|"+ height);
  }

  public void tableClear() {
    send("table|0|cleared");
  }

  public void tableStateLoad(String s, boolean neutralView) {
    s = s.replaceAll("[|]", "");
    s = s.replaceAll("\\r\\n", "\n");
    s = s.replaceAll("[\\r\\n]", "|");
    send("table|0|stateLoaded|"+ neutralView +"|"+ s);
  }

  public void tableStateResult(boolean success) {
    send("table|0|stateResult|"+ success);
  }

  public void tableStateReply(int queryId, String s) {
    s = s.replaceAll("[|]", "");
    s = s.replaceAll("\\r\\n", "\n");
    s = s.replaceAll("[\\r\\n]", "|");
    send("table|0|stateReply|"+ queryId +"|"+ s);
  }

  public void tableNerfResult(boolean nerfed) {
    send("table|0|nerfResult|"+ nerfed);
  }

  public void cardAdd(int x, int y, String cardName, String backName, String expansion, String pathFront, String pathBack, boolean flipState, int rotation) {
    send("card|"+ 0 +"|added|"+ x +"|"+ y +"|"+ cardName +"|"+ backName +"|"+ expansion +"|"+ pathFront +"|"+ pathBack +"|"+ flipState +"|"+ rotation);
  }

  public void cardRemove(int index) {
    send("card|"+ index +"|removed");
  }

  public void cardUse(int index) {
    send("card|"+ index +"|used");
  }

  public void cardUnuse(int index) {
    send("card|"+ index +"|unused");
  }

  public void cardMove(int index, int x, int y) {
    send("card|"+ index +"|moved|"+ x +"|"+ y);
  }

  public void cardRotate(int index, int angle) {
    send("card|"+ index +"|roted|"+ angle);
  }

  public void cardFlip(int index) {
    send("card|"+ index +"|flipped");
  }

  public void cardRemoteFlip(int index) {
    send("card|"+ index +"|remoteFlipped");
  }

  public void cardLocalFlip(int index, boolean peeked) {
    send("card|"+ index +"|localFlipped|"+ peeked);
  }

  public void cardToFront(int index) {
    send("card|"+ index +"|fronted");
  }

  public void cardToBack(int index) {
    send("card|"+ index +"|backed");
  }

  public void cardReposition(int index, int position) {
    send("card|"+ index +"|repoed|"+ position);
  }

  public void cardHand(int cardIndex, int handIndex, int dstRow) {
    send("card|"+ cardIndex +"|handed|"+ handIndex +"|"+ dstRow);
  }

  public void cardDeck(int cardIndex, int deckIndex) {
    send("card|"+ cardIndex +"|decked|"+ deckIndex);
  }

  /* Adds a deck with stock settings.
   * Locked=F,AutoFace=T,Facing=F,ToBottom=F,DrawOffset=D
   */
  public void deckAdd(int x, int y, String contents) {
    deckAdd(x, y, false, true, false, false, 'D', contents);
  }

  public void deckAdd(int x, int y, boolean locked, boolean autoface, boolean facing, boolean toBottom, char drawOffset, String contents) {
    send("deck|"+ 0 +"|added|"+ x +"|"+ y +"|"+ locked +"|"+ autoface +"|"+ facing +"|"+ toBottom +"|"+ drawOffset +"|"+ contents);
  }

  public void deckAddResult(boolean success) {
    send("deck|0|addResult|"+ success);
  }

  public void deckRemove(int index) {
    send("deck|"+ index +"|removed");
  }

  public void deckUse(int index) {
    send("deck|"+ index +"|used");
  }

  public void deckUnuse(int index) {
    send("deck|"+ index +"|unused");
  }

  public void deckMove(int index, int x, int y) {
    send("deck|"+ index +"|moved|"+ x +"|"+ y);
  }

  public void deckShuffle(int index, List<Integer> newOrder) {
    StringBuffer message = new StringBuffer("deck|"+ index +"|shuffled");
    for (int i=0; i < newOrder.size(); i++) {
      message.append("|"+ newOrder.get(i));
    }
    ArcanistCCG.NetManager.send(message.toString());
  }

  public void deckTableCard(int index, int n, boolean honorAutoFace) {
    send("deck|"+ index +"|tabledCard|"+ n +"|"+ honorAutoFace);
  }

  public void deckRemoveCard(int index, int n) {
    send("deck|"+ index +"|removedCard|"+ n);
  }

  public void deckTableXCards(int index, int cardCount) {
    send("deck|"+ index +"|tabledXCards|"+ cardCount);
  }

  public void deckSearch(int index) {
    send("deck|"+ index +"|searched");
  }

  public void deckList(int index) {
    send("deck|"+ index +"|listed");
  }

  public void deckReverse(int index) {
    send("deck|"+ index +"|reversed");
  }

  public void deckSetLocked(int index, boolean state) {
    send("deck|"+ index +"|setLocked|"+ state);
  }

  public void deckSetFacing(int index, boolean state) {
    send("deck|"+ index +"|setFacing|"+ state);
  }

  public void deckSetAutoFace(int index, boolean state) {
    send("deck|"+ index +"|setAutoFace|"+ state);
  }

  public void deckSetToBottom(int index, boolean state) {
    send("deck|"+ index +"|setToBottom|"+ state);
  }

  public void deckSetOffset(int index, char direction) {
    send("deck|"+ index +"|setDir|"+ direction);
  }

  public void deckDeck(int srcIndex, int destIndex) {
    send("deck|"+ srcIndex +"|decked|"+ destIndex);
  }

  public void deckHand(int deckIndex, int handIndex) {
    send("deck|"+ deckIndex +"|handed|"+ handIndex);
  }

  public void noteAdd(int x, int y, String noteText) {
    send("note|"+ 0 +"|added|"+ x +"|"+ y +"|"+ noteText);
  }

  public void noteRemove(int index) {
    send("note|"+ index +"|removed");
  }

  public void noteUse(int index) {
    send("note|"+ index +"|used");
  }

  public void noteUnuse(int index) {
    send("note|"+ index +"|unused");
  }

  public void noteMove(int index, int x, int y) {
    send("note|"+ index +"|moved|"+ x +"|"+ y);
  }

  public void noteAlter(int index, String s) {
    send("note|"+ index +"|altered|"+ s);
  }

  public void tokenAdd(int x, int y, String fileName) {
    send("token|"+ 0 +"|added|"+ x +"|"+ y +"|"+ fileName);
  }

  public void tokenRemove(int index) {
    send("token|"+ index +"|removed");
  }

  public void tokenUse(int index) {
    send("token|"+ index +"|used");
  }

  public void tokenUnuse(int index) {
    send("token|"+ index +"|unused");
  }

  public void tokenMove(int index, int x, int y) {
    send("token|"+ index +"|moved|"+ x +"|"+ y);
  }

  public void handAdd(int x, int y, String title, int w, int h) {
    send("hand|"+ 0 +"|added|"+ x +"|"+ y +"|"+ title +"|"+ w +"|"+ h);
  }

  public void handRemove(int index) {
    send("hand|"+ index +"|removed");
  }

  public void handUse(int index) {
    send("hand|"+ index +"|used");
  }

  public void handUnuse(int index) {
    send("hand|"+ index +"|unused");
  }

  public void handGetCard(int index, int cardId, int x, int y, boolean flipState) {
    send("hand|"+ index +"|gotCard|"+ cardId +"|"+ x +"|"+ y +"|"+ flipState);
  }

  public void handReorderCard(int index, int cardId, int dstRow) {
    send("hand|"+ index +"|reorderedCard|"+ cardId +"|"+ dstRow);
  }

  public void handMove(int index, int x, int y) {
    send("hand|"+ index +"|moved|"+ x +"|"+ y);
  }

  public void handHand(int srcIndex, int dstIndex, int cardId, int dstRow) {
    send("hand|"+ srcIndex +"|handed|"+ dstIndex +"|"+ cardId +"|"+ dstRow);
  }

  public void handResize(int index, int w, int h) {
    send("hand|"+ index +"|resized|"+ w +"|"+ h);
  }

  public void handCardSelect(int index, int n) {
    send("hand|"+ index +"|cardSelected|"+ n);
  }

  public void handDraw(int handIndex, int deckIndex, int amount) {
    send("hand|"+ handIndex +"|drew|"+ deckIndex +"|"+ amount);
  }

  public void handDiscard(int handIndex, int deckIndex, int cardId) {
    send("hand|"+ handIndex +"|discarded|"+ deckIndex +"|"+ cardId);
  }

  public void handReorder(int index, List<Integer> newOrder) {
    StringBuffer message = new StringBuffer("hand|"+ index +"|reordered");
    for (int i=0; i < newOrder.size(); i++) {
      message.append("|"+ newOrder.get(i));
    }
    ArcanistCCG.NetManager.send(message.toString());
  }

  public void handSetRemoteRevealed(int index, boolean state) {
    send("hand|"+ index +"|setRemoteRevealed|"+ state);
  }

  public void handSetLocalRevealed(int index, boolean state) {
    send("hand|"+ index +"|setLocalRevealed|"+ state);
  }

  public void handSetDrawDeck(int index, int n) {
    send("hand|"+ index +"|setDrawDeck|"+ n);
  }

  public void handSetDiscardDeck(int index, int n) {
    send("hand|"+ index +"|setDiscardDeck|"+ n);
  }

  public void chatText(String toAll) {
    toAll = toAll.replaceAll("[|]", "");
    send("chat|0|talked|"+ toAll);
  }

  public void chatNotice(String styleName, String toOthers, String toSelf) {
    toOthers = toOthers.replaceAll("[|]", "");
    toSelf = toSelf.replaceAll("[|]", "");
    int severity = 1;
    if (styleName.equals(ChatPanel.STYLE_NOTICE1)) severity = 1;
    else if (styleName.equals(ChatPanel.STYLE_NOTICE2)) severity = 2;
    else if (styleName.equals(ChatPanel.STYLE_ERROR)) severity = 3;
    send("chat|0|notice|"+ severity +"|"+ toOthers +(toSelf.length()==0 ? "" : "|"+ toSelf));
  }

  public void chatNotice(String styleName, String toAll) {
    chatNotice(styleName, toAll, "");
  }

  public void chatNotice(String toAll) {
    chatNotice(ChatPanel.STYLE_NOTICE1, toAll, "");
  }

  public void chatCounterQuery() {
    send("chat|0|counterQuery");
  }

  public void serverRequestAlias(String newAlias) {
    if (isOnline()) send("server|0|aliasRequested|"+ newAlias);
    else send("main|0|aliasSet|"+ newAlias);
  }

  public void serverRequestSync(int offendingPlayerId) {
    send("server|0|syncRequested|"+ offendingPlayerId);
  }
}
