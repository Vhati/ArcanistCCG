package org.arcanist.client;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.net.SocketException;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * This handles all incoming network traffic.
 * A loop waits for messages, and sends them
 * to the interpret function.
 */
public class ClientThread implements Runnable {
  private ArcanistCCGFrame frame = null;
  private Socket socket = null;

  private volatile boolean keepRunning = false;

  /** Connection status. */
  public volatile boolean connected = false;

  /** Network output stream. This will be overhauled. */
  public PrintWriter out;
  private Object outLock = new Object();

  //Global so interpret() can read this through an invoked runnable
  private String inputLine = null;

  private boolean clearOnConnect = false;


  public ClientThread(ArcanistCCGFrame f, Socket s, boolean clear) {frame = f; socket = s; clearOnConnect = clear;}
  public ClientThread(ArcanistCCGFrame f, Socket s) {frame = f; socket = s;}


  public void startThread() {
    if (keepRunning == true) return;

    keepRunning = true;
    Thread t = new Thread(this);
    t.setPriority(Thread.NORM_PRIORITY);
    t.setDaemon(true);
    t.setName("ClientThread ("+ t.getName() +")");
    t.start();
  }

  /**
   * Kills the thread.
   */
  public void killThread() {
    keepRunning = false;
  }


  public void send(String s) {
    synchronized (outLock) {
      out.println(s);
    }
  }


  @Override
  public void run() {
    try {
      out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      connected = true;
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          frame.setConnectedState(true);
          ArcanistCCG.NetManager.notify(ChatPanel.STYLE_NOTICE1, "--Connected successfully.--");

          //Automatic table clearing signalled by NetConnect (as server)
          if (clearOnConnect == true) {
            frame.clearTable();
            ArcanistCCG.NetManager.tableClear();
          }
        }
      });

      //The delayed invoking is okay because the code
      //  above is queued in the event dispatch thread before the interpret loop.
      //  So they still run in order.
      // And there's nothing interesting in this thread to accidently splice between them on the sockets.

      //Socket exceptions will still be caught here, since the reading is in this thread.

      try {
        while (keepRunning && !socket.isClosed() && (inputLine = in.readLine()) != null) {
          if (inputLine != "") {
            delayedInterpret(inputLine);
          }
        }
      }
      catch (SocketException e) {
        if (connected == true) {
          //This disconnect was a surprise, report it
          ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Connection lost: Socket Issue.");
        }
      }

      socket.close();
    }
    catch (IOException e) {
      ArcanistCCG.LogManager.write(e, "Connection lost: I/O Issue.");
    }
    connected = false;
    //If disconnected from a local server, kill the server
    ArcanistCCG.setServerThread(null);
    ArcanistCCG.NetManager.setClientThread(null);


    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        frame.unuseEverything();
        frame.setNerfed(false);
        frame.setConnectedState(false);

        ArcanistCCG.LogManager.write(LogManager.INFO_LEVEL, "Connection lost: Network Thread Terminated.");
        ArcanistCCG.NetManager.notify(ChatPanel.STYLE_ERROR, "--Connection lost.--");
        JOptionPane.showInternalMessageDialog(frame.getDesktop(), "Connection lost", "Error", JOptionPane.PLAIN_MESSAGE);
      }
    });
  }

  /**
   * Run interpret() in the event thread.
   * This function is a hack to have a
   * final var passed to the Runnable, while
   * reusing that variable in a loop.
   */
  public void delayedInterpret(final String s) {
    Runnable r = new Runnable() {
      private String inputLine = s;

      @Override
      public void run() {
        interpret(inputLine);
      }
    };
    SwingUtilities.invokeLater(r);
  }


  /**
   * Interpret incoming message.
   * Tokenize a "|" separated list: "type|id|action|arg|arg|arg..."
   * @param input network message
   */
  public void interpret(String input) {
    if (Prefs.verboseChat == true)
      ArcanistCCG.NetManager.notify(ChatPanel.STYLE_NOTICE2, "--"+ input +"--");

    String origin = null;
    int originId = -1;
    String type = null;
    String[] tokens = input.replaceAll("[\\r\\n]", "").split("[|]");
    int t = 0;
    try {
      origin = tokens[t++];
      originId = Integer.parseInt(tokens[t++]);
      type = tokens[t++];
      int id = Integer.parseInt(tokens[t++]);
      String action = tokens[t++];
      if (type.equals("card")) {
        //'card',arrayID(),'used'
        if (action.equals("used")) {
          frame.getCard(id).setInUse(true);
          frame.getTablePane().repaint();
          return;
        }
        //'card',arrayID(),'unused'
        if (action.equals("unused")) {
          frame.getCard(id).setInUse(false);
          frame.getTablePane().repaint();
          return;
        }
        //'card',arrayID(),'moved',x,y
        if (action.equals("moved")) {
          frame.getCard(id).setLocation(Integer.parseInt(tokens[t++]), Integer.parseInt(tokens[t++]));
          return;
        }
        //'card',arrayID(),'roted',rotation
        if (action.equals("roted")) {
          frame.getCard(id).setRotation(Integer.parseInt(tokens[t++]));
          return;
        }
        //'card',arrayID(),'handed',handNum,dstRow
        if (action.equals("handed")) {
          int handNum = Integer.parseInt(tokens[t++]);
          int dstRow = Integer.parseInt(tokens[t++]);
          Card tmpCard = frame.getCard(id);
          Hand tmpHand = frame.getHand(handNum);
          tmpCard.removeFromTable();
          if (tmpCard != null) {
            if (dstRow == -1) {tmpHand.addCard(tmpCard);}
            else {tmpHand.addCard(tmpCard, dstRow);}
          }
          tmpHand.repaint();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
        //'card',arrayID(),'decked',arrayID()
        if (action.equals("decked")) {
          int num = Integer.parseInt(tokens[t++]);
          Card tmpCard = frame.getCard(id);
          Deck tmpDeck = frame.getDeck(num);
          tmpCard.removeFromTable();
          tmpDeck.addCard(tmpCard);
          tmpDeck.refresh();
          return;
        }
        //'card',arrayID(),'flipped'
        if (action.equals("flipped")) {
          frame.getCard(id).flip();
          return;
        }
        //'card',arrayID(),'remoteFlipped'
        if (action.equals("remoteFlipped")) {
          if (ArcanistCCG.NetManager.getPlayerId() != originId) {
            frame.getCard(id).flip();
          }
          return;
        }
        //'card',arrayID(),'localFlipped',peeked
        if (action.equals("localFlipped")) {
          boolean peeked = tokens[t++].equals("true");
          Card c = frame.getCard(id);

          if (ArcanistCCG.NetManager.getPlayerId() == originId) {
            c.flip();
            frame.getTablePane().repaint();
          }
          else if (peeked == true && c.getFlipState()==true) {
            ArcanistCCG.NetManager.notify(ChatPanel.STYLE_NOTICE1, "--Opponent locally flipped to see "+ c.getFrontName() +"--");
          }
          return;
        }
        //'card',arrayID(),'added',x,y,frontName,backName,expansion,path,pathBack,flipState,rotation
        if (action.equals("added")) {
          int x = Integer.parseInt(tokens[t++]);
          int y = Integer.parseInt(tokens[t++]);
          String frontName = tokens[t++];
            if (frontName.equals("?"))  frontName = "";
          String backName = tokens[t++];
            if (backName.equals("?"))  backName = "";
          String expansion = tokens[t++];
            if (expansion.equals("?"))  expansion = "";
          String frontFile = tokens[t++];
            if (frontFile.equals("?"))  frontFile = "";
          String backFile = tokens[t++];
            if (backFile.equals("?"))  backFile = "";
          boolean flipState = tokens[t++].equals("true");
          int rotation = Integer.parseInt(tokens[t++]);
          String path = "", pathBack = "";

          String[] actualPaths = CardImagePathParser.getPaths(expansion, frontName, frontFile, backName, backFile);
            expansion = actualPaths[0];
            path = actualPaths[1];
            pathBack = actualPaths[2];

          expansion = Prefs.textDat.getSetAbbrevFromName(expansion);

          if (path == null) path = Prefs.defaultBlankPath;
          if (pathBack == null) pathBack = Prefs.defaultBackPath;

          Card newCard = new Card(frame, frontName, backName, expansion, path, pathBack, flipState);
          newCard.setId(ArcanistCCG.getNextUnusedId());
          newCard.setRotation(rotation);
          newCard.addToTable(x, y);
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
        //'card',arrayID(),'repoed',position
        if (action.equals("repoed")) {
          frame.getTablePane().setPosition(frame.getCard(id).getComponent(), Integer.parseInt(tokens[t++]));
          if (ArcanistCCG.NetManager.getPlayerId() == originId) {
            frame.getCard(id).focus();
          }
          return;
        }
        //'card',arrayID(),'fronted'
        if (action.equals("fronted")) {
          frame.getCard(id).moveToFront();
          return;
        }
        //'card',arrayID(),'backed'
        if (action.equals("backed")) {
          frame.getCard(id).moveToBack();
          return;
        }
        //'card',arrayID(),'removed'
        if (action.equals("removed")) {
          frame.getCard(id).removeFromTable();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
      }
      else if (type.equals("deck")) {
        //'deck',arrayID(),'used'
        if (action.equals("used")) {
          frame.getDeck(id).setInUse(true);
          frame.getTablePane().repaint();
          return;
        }
        //'deck',arrayID(),'unused'
        if (action.equals("unused")) {
          frame.getDeck(id).setInUse(false);
          frame.getTablePane().repaint();
          return;
        }
        //'deck',arrayID(),'moved',x,y
        if (action.equals("moved")) {
          frame.getDeck(id).setLocation(Integer.parseInt(tokens[t++]), Integer.parseInt(tokens[t++]));
          return;
        }
        //'deck',arrayID(),'tabledCard',cardNum,honorAutoFace
        if (action.equals("tabledCard")) {
          boolean youDrew = (ArcanistCCG.NetManager.getPlayerId() == originId);
          int cardNum = Integer.parseInt(tokens[t++]);
          boolean honorAutoFace = tokens[t++].equals("true");
          Deck tmpDeck = frame.getDeck(id);
          Card tmpCard = tmpDeck.takeCard(cardNum);
          if (tmpCard != null) {
            if (youDrew && honorAutoFace) {
              tmpDeck.applyAutoFaceToCard(tmpCard);
            } else {
              tmpCard.setFlipState(tmpDeck.getFacing());
            }

            Point rallyPoint = tmpDeck.getRallyPoint();
            tmpCard.addToTable(rallyPoint.x, rallyPoint.y);
            tmpCard.moveToFront();
          }
          return;
        }
        //'deck',arrayID(),'removedCard',cardNum
        if (action.equals("removedCard")) {
          Deck tmpDeck = frame.getDeck(id);
          tmpDeck.removeCard(Integer.parseInt(tokens[t++]));
          tmpDeck.refresh();
          return;
        }
        //'deck',arrayID(),'tabledXCards',cardCount
        if (action.equals("tabledXCards")) {
          boolean youDrew = (ArcanistCCG.NetManager.getPlayerId() == originId);
          int cardCount = Integer.parseInt(tokens[t++]);
          Deck tmpDeck = frame.getDeck(id);
          Point rallyPoint = tmpDeck.getRallyPoint();

          for (int i=0; i < cardCount; i++) {
            Card tmpCard = tmpDeck.takeCard(tmpDeck.getCardCount()-1);
            if (youDrew) {
              tmpDeck.applyAutoFaceToCard(tmpCard);
            } else {
              tmpCard.setFlipState(tmpDeck.getFacing());
            }
            if (tmpCard != null) {
              tmpCard.addToTable(rallyPoint.x, rallyPoint.y);
              tmpCard.moveToFront();
            }
          }
          return;
        }
        //'deck',arrayID(),'shuffled',randCard,randCard...
        if (action.equals("shuffled")) {
          Deck tmpDeck = frame.getDeck(id);
          List<Integer> newOrder = new ArrayList<Integer>();

          while (t+1 <= tokens.length) {
            newOrder.add(new Integer(tokens[t++]));
          }
          tmpDeck.reorder(newOrder);
          return;
        }
        //'deck',arrayID(),'added',x,y,locked,autoface,facing,toBottom,drawOffset,deckString
        if (action.equals("added")) {
          int x = Integer.parseInt(tokens[t++]);
          int y = Integer.parseInt(tokens[t++]);

          boolean locked = tokens[t++].equals("true");
          boolean autoface = tokens[t++].equals("true");
          boolean facing = tokens[t++].equals("true");
          boolean toBottom = tokens[t++].equals("true");
          char drawOffset = tokens[t++].charAt(0);

          StringBuffer deckString = new StringBuffer();
          while (t+1 <= tokens.length) {
            deckString.append(tokens[t++]);
            deckString.append("|");
          }
          if (deckString.length() > 1) {
            ArcanistCCG.NetManager.notify(ChatPanel.STYLE_NOTICE2, "--MD5:"+ DeckParser.genChecksum(deckString.toString()) +"--");
            MultiDeckLoader mdl = new MultiDeckLoader(frame);
            mdl.setReportResult(true);
            mdl.addDeck(-1, deckString.toString(), x, y, locked, autoface, facing, toBottom, drawOffset);
            mdl.startThread();
            //DeckParser newDeckParser = new DeckParser(frame, deckString.toString(), x, y);
            //newDeckParser.startThread();
          }
          else {
            Deck newDeck = new Deck(frame, new ArrayList<Card>(0), x, y, facing);
            newDeck.setId(ArcanistCCG.getNextUnusedId());
            newDeck.setLocked(locked);
            newDeck.setAutoFace(autoface);
            newDeck.setToBottom(toBottom);
            newDeck.setOffset(drawOffset);
            newDeck.addToTable();
            ArcanistCCG.NetManager.deckAddResult(true);
          }
          return;
        }
        //'deck',arrayID(),'setFacing',state
        if (action.equals("setFacing")) {
          boolean state = tokens[t++].equals("true");
          frame.getDeck(id).setFacing(state);
          return;
        }
        //'deck',arrayID(),'setToBottom',state
        if (action.equals("setToBottom")) {
          boolean state = tokens[t++].equals("true");
          frame.getDeck(id).setToBottom(state);
          return;
        }
        //'deck',arrayID(),'decked',arrayID()
        if (action.equals("decked")) {
          int num = Integer.parseInt(tokens[t++]);
          Deck srcDeck = frame.getDeck(id);
          Deck dstDeck = frame.getDeck(num);
          srcDeck.removeFromTable();
          dstDeck.addDeck(srcDeck);
          dstDeck.refresh();
          return;
        }
        //'deck',arrayID(),'handed',handNum
        if (action.equals("handed")) {
          int handNum = Integer.parseInt(tokens[t++]);
          Deck tmpDeck = frame.getDeck(id);
          Hand tmpHand = frame.getHand(handNum);
          tmpDeck.removeFromTable();
          if (tmpDeck != null && tmpDeck.getCardCount() > 0) {
            List<Card> deckList = tmpDeck.removeCards(0, tmpDeck.getCardCount());
            Collections.reverse(deckList);
            tmpHand.addCards(deckList);
          }
          tmpHand.repaint();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
        //'deck',arrayID(),'removed'
        if (action.equals("removed")) {
          frame.getDeck(id).removeFromTable();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
        //'deck',arrayID(),'setLocked',state
        if (action.equals("setLocked")) {
          boolean state = tokens[t++].equals("true");
          frame.getDeck(id).setLocked(state);
          return;
        }
        //'deck',arrayID(),'setAutoFace',state
        if (action.equals("setAutoFace")) {
          boolean state = tokens[t++].equals("true");
          frame.getDeck(id).setAutoFace(state);
          return;
        }
        //'deck',arrayID(),'searched'
        if (action.equals("searched")) {
          if (ArcanistCCG.NetManager.getPlayerId() == originId) {
            Deck tmpDeck = frame.getDeck(id);
            new DeckSearchWindow(frame, tmpDeck);
          }
          return;
        }
        //'deck',arrayID(),'listed'
        if (action.equals("listed")) {
          if (ArcanistCCG.NetManager.getPlayerId() == originId) {
            Deck tmpDeck = frame.getDeck(id);
            new DeckListWindow(frame, tmpDeck);
          }
          return;
        }
        //'deck',arrayID(),'reversed'
        if (action.equals("reversed")) {
          frame.getDeck(id).reverse();
          return;
        }
        //'deck',arrayID(),'setDir',(U/D/L/R)
        if (action.equals("setDir")) {
          String direction = tokens[t++];
          if (direction.equals("U")) {
            frame.getDeck(id).setOffset('U');
          }
          else if (direction.equals("D")) {
            frame.getDeck(id).setOffset('D');
          }
          else if (direction.equals("L")) {
            frame.getDeck(id).setOffset('L');
          }
          else if (direction.equals("R")) {
            frame.getDeck(id).setOffset('R');
          }
          return;
        }
      }
      else if (type.equals("token")) {
        //'token',arrayID(),'used'
        if (action.equals("used")) {
          frame.getToken(id).setInUse(true);
          frame.getTablePane().repaint();
          return;
        }
        //'token',arrayID(),'unused'
        if (action.equals("unused")) {
          frame.getToken(id).setInUse(false);
          frame.getTablePane().repaint();
          return;
        }
        //'token',arrayID(),'moved',x,y
        if (action.equals("moved")) {
          frame.getToken(id).setLocation(Integer.parseInt(tokens[t++]), Integer.parseInt(tokens[t++]));
          return;
        }
        if (action.equals("added")) {
          //'token',arrayID(),'added',x,y,fileName
          int x = Integer.parseInt(tokens[t++]);
          int y = Integer.parseInt(tokens[t++]);
          String path = Prefs.tokensPath + tokens[t++];
          Token newToken = new Token(frame, path);
          newToken.setId(ArcanistCCG.getNextUnusedId());
          newToken.addToTable(x, y);
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
        //'token',arrayID(),'removed'
        if (action.equals("removed")) {
          frame.getToken(id).removeFromTable();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
      }
      else if (type.equals("note")) {
        //'note',arrayID(),'used'
        if (action.equals("used")) {
          frame.getNote(id).setInUse(true);
          frame.getTablePane().repaint();
          return;
        }
        //'note',arrayID(),'unused'
        if (action.equals("unused")) {
          frame.getNote(id).setInUse(false);
          frame.getTablePane().repaint();
          return;
        }
        //'note',arrayID(),'moved',x,y
        if (action.equals("moved")) {
          frame.getNote(id).setLocation(Integer.parseInt(tokens[t++]), Integer.parseInt(tokens[t++]));
          return;
        }
        //'note',arrayID(),'altered',noteText
        if (action.equals("altered")) {
          frame.getNote(id).setText(tokens[t++]);
          return;
        }
        //'note',arrayID(),'added',x,y,noteText
        if (action.equals("added")) {
          int x = Integer.parseInt(tokens[t++]);
          int y = Integer.parseInt(tokens[t++]);
          String noteText = tokens[t++];
          FloatingNote newNote = new FloatingNote(frame, noteText);
          newNote.setId(ArcanistCCG.getNextUnusedId());
          newNote.addToTable(x, y);
          return;
        }
        //'note',arrayID(),'removed'
        if (action.equals("removed")) {
          frame.getNote(id).removeFromTable();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
      }
      else if (type.equals("hand")) {
        //'hand',arrayID(),'used'
        if (action.equals("used")) {
          frame.getHand(id).setInUse(true);
          frame.getTablePane().repaint();
          return;
        }
        //'hand',arrayID(),'unused'
        if (action.equals("unused")) {
          frame.getHand(id).setInUse(false);
          frame.getTablePane().repaint();
          return;
        }
        //'hand',arrayID(),'gotCard',cardId,x,y,flipState
        if (action.equals("gotCard")) {
          int cardId = Integer.parseInt(tokens[t++]);
          int x = Integer.parseInt(tokens[t++]);
          int y = Integer.parseInt(tokens[t++]);
          boolean flipState = tokens[t++].equals("true");
          Card tmpCard = frame.getHand(id).takeCardById(cardId);
          if (tmpCard != null) {
            tmpCard.setFlipState(flipState);
            tmpCard.addToTable(x, y);
            frame.getTablePane().revalidate();
            frame.getTablePane().repaint();
          }
          return;
        }
        //'hand',arrayID(),'reorderedCard',cardId,dstRow
        if (action.equals("reorderedCard")) {
          int cardId = Integer.parseInt(tokens[t++]);
          int dstRow = Integer.parseInt(tokens[t++]);
          Hand tmpHand = frame.getHand(id);
          Card tmpCard = frame.getHand(id).takeCardById(cardId);
          if (tmpCard != null) {
            if (dstRow == -1) {tmpHand.addCard(tmpCard);}
            else {tmpHand.addCard(tmpCard, dstRow);}
          }
          return;
        }
        //'hand',arrayID(),'moved',x,y
        if (action.equals("moved")) {
          frame.getHand(id).setLocation(Integer.parseInt(tokens[t++]), Integer.parseInt(tokens[t++]));
          return;
        }
        //'hand',arrayID(),'handed',dstNum,cardId,dstRow
        if (action.equals("handed")) {
          int dstNum = Integer.parseInt(tokens[t++]);
          int cardId = Integer.parseInt(tokens[t++]);
          int dstRow = Integer.parseInt(tokens[t++]);
          Hand dstHand = frame.getHand(dstNum);
          Card tmpCard = frame.getHand(id).takeCardById(cardId);
          if (tmpCard != null) {
            if (dstRow == -1) {dstHand.addCard(tmpCard);}
            else {dstHand.addCard(tmpCard, dstRow);}
          }
          return;
        }
        //'hand',arrayID(),'resized',w,h
        if (action.equals("resized")) {
          frame.getHand(id).setSize(Integer.parseInt(tokens[t++]), Integer.parseInt(tokens[t++]));
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
        //'hand',arrayID(),'drew',deckNum,amount
        if (action.equals("drew")) {
          int deckNum = Integer.parseInt(tokens[t++]);
          int amount = Integer.parseInt(tokens[t++]);
          Deck tmpDeck = frame.getDeck(deckNum);
          frame.getHand(id).draw(tmpDeck, amount);
          return;
        }
        //'hand',arrayID(),'discarded',deckNum,cardId
        if (action.equals("discarded")) {
          int deckNum = Integer.parseInt(tokens[t++]);
          int cardId = Integer.parseInt(tokens[t++]);
          Deck tmpDeck = frame.getDeck(deckNum);
          Card tmpCard = frame.getHand(id).takeCardById(cardId);
          if (tmpCard != null) {
            tmpDeck.addCard(tmpCard);
            tmpDeck.refresh();
          }
          return;
        }
        //'hand',arrayID(),'cardSelected',cardNum
        if (action.equals("cardSelected")) {
          frame.getHand(id).selectCard(Integer.parseInt(tokens[t++]));
          return;
        }
        //'hand',arrayID(),'reordered',cardIndex,cardIndex...
        if (action.equals("reordered")) {
          Hand tmpHand = frame.getHand(id);
          List<Integer> newOrder = new ArrayList<Integer>();

          while (t+1 <= tokens.length) {
            newOrder.add(new Integer(tokens[t++]));
          }
          tmpHand.reorder(newOrder);
          return;
        }
        //'hand',arrayID(),'setRemoteRevealed',state
        if (action.equals("setRemoteRevealed")) {
          if (ArcanistCCG.NetManager.getPlayerId() != originId) {
            boolean state = tokens[t++].equals("true");
            frame.getHand(id).setRevealed(state);
          }
          return;
        }
        //'hand',arrayID(),'setLocalRevealed',state
        if (action.equals("setLocalRevealed")) {
          if (ArcanistCCG.NetManager.getPlayerId() == originId) {
            boolean state = tokens[t++].equals("true");
            frame.getHand(id).setRevealed(state);
          }
          return;
        }
        //'hand',arrayID(),'setDrawDeck',deckNum
        if (action.equals("setDrawDeck")) {
          int deckNum = Integer.parseInt(tokens[t++]);
          if (deckNum == -1) frame.getHand(id).setDrawDeck(null);
          else frame.getHand(id).setDrawDeck(frame.getDeck(deckNum));
          return;
        }
        //'hand',arrayID(),'setDiscardDeck',deckNum
        if (action.equals("setDiscardDeck")) {
          int deckNum = Integer.parseInt(tokens[t++]);
          if (deckNum == -1) frame.getHand(id).setDiscardDeck(null);
          else frame.getHand(id).setDiscardDeck(frame.getDeck(deckNum));
          return;
        }
        if (action.equals("added")) {
          //'hand',arrayID(),'added',x,y,title,w,h
          int x = Integer.parseInt(tokens[t++]);
          int y = Integer.parseInt(tokens[t++]);
          String title = tokens[t++];
          int w = Integer.parseInt(tokens[t++]);
          int h = Integer.parseInt(tokens[t++]);
          Hand newHand = new Hand(frame, title, w, h);
          newHand.setId(ArcanistCCG.getNextUnusedId());
          if (ArcanistCCG.NetManager.getPlayerId() != originId) {
            newHand.setRevealed(false);
          }
          newHand.addToTable(x, y);
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
        //'hand',arrayID(),'removed'
        if (action.equals("removed")) {
          frame.getHand(id).removeFromTable();
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
          return;
        }
      }
      else if (type.equals("table")) {
        //'table',arrayID(),'resized',x,y
        if (action.equals("resized")) {
          int tableWidth = Integer.parseInt(tokens[t++]);
          int tableHeight = Integer.parseInt(tokens[t++]);
          frame.resizeTable(tableWidth, tableHeight);
          ArcanistCCG.NetManager.notify(ChatPanel.STYLE_NOTICE1, "--Resized Table: "+ tableWidth +"x"+ tableHeight +"--");
          return;
        }
        //'table',arrayID(),'cleared'
        if (action.equals("cleared")) {
          frame.clearTable();
          ArcanistCCG.NetManager.notify(ChatPanel.STYLE_NOTICE1, "--Cleared table--");
          return;
        }
        //'table',arrayID(),'stateLoaded',neutralView,xml lines...
        if (action.equals("stateLoaded")) {
          boolean neutralView = tokens[t++].equals("true");
          StringBuffer buf = new StringBuffer();
          while (t+1 <= tokens.length) {
            buf.append(tokens[t++]);
            buf.append("\n");
          }

          org.jdom2.Document doc = XmlUtils.readXML(buf.toString());
          if (doc == null) {
            ArcanistCCG.NetManager.notify(ChatPanel.STYLE_ERROR, "--Error loading state. Check the log for details.--");
          } else {
            XmlUtils.launchStateLoader(frame, doc, neutralView);
          }
          return;
        }
        //Set the table's nerfed state (only sent by server)
        //  The client will respond
        //'table',arrayID(),'nerfed',state
        if (action.equals("nerfed")) {
          final boolean state = tokens[t++].equals("true");
          frame.setNerfed(state);
          // In case things were invokeLater'd while nerfing
          //   Enqueue the reply afterward to ensure it's after them
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              ArcanistCCG.NetManager.tableNerfResult(state);
            }
          });
        }
        //Request a table state (only sent by server, to specific clients)
        //  No multiplexing!
        //  The client will respond
        //'table',arrayID(),'stateQuery',queryId
        if (action.equals("stateQuery")) {
          int queryId = Integer.parseInt(tokens[t++]);
          String state = XmlUtils.saveState(frame);
          ArcanistCCG.NetManager.tableStateReply(queryId, state);
        }
      }
      else if (type.equals("chat")) {
        //'chat',arrayID(),'talked',text
        if (action.equals("talked")) {
          ArcanistCCG.NetManager.notify(ChatPanel.STYLE_CHAT, tokens[t++]);
          return;
        }
        //'chat',arrayID(),'notice',severity,toOthers,[toSelf]
        if (action.equals("notice")) {
          String severity = tokens[t++];
          if (severity.equals("1")) severity = ChatPanel.STYLE_NOTICE1;
          else if (severity.equals("2")) severity = ChatPanel.STYLE_NOTICE2;
          else if (severity.equals("3")) severity = ChatPanel.STYLE_ERROR;
          else severity = ChatPanel.STYLE_NOTICE1;
          String msg = tokens[t++];

          //If this player was the source, get the optional toSelf message instead
          if (ArcanistCCG.NetManager.getPlayerId() == originId) {
            if (t+1 <= tokens.length) msg = tokens[t++];
          }
          ArcanistCCG.NetManager.notify(severity, msg);
          return;
        }
        //'chat',arrayID(),'counterQuery'
        //  Another curious client sends this
        //  The client will respond, describing all its counter frames
        if (action.equals("counterQuery")) {
          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

          JInternalFrame[] frames = frame.getDesktop().getAllFrames();
          for (int i=0; i < frames.length; i++) {
            if (frames[i] instanceof CounterFrame) {
              CounterFrame tmpCounter = (CounterFrame)frames[i];
              ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +": "+ tmpCounter.getName() +" counter: "+ tmpCounter.getValue() +"--");
            }
          }
          return;
        }
      }
      else if (type.equals("main")) {
        //'main',arrayID(),'playerid',id
        //  Issue a new player id (only sent by server, to specific clients)
        //No multiplexing!
        if (action.equals("playerid")) {
          int newId = Integer.parseInt(tokens[t++]);
          ArcanistCCG.NetManager.setPlayerId(newId);
          return;
        }
        //'main',arrayID(),'disconnected',id
        if (action.equals("disconnected")) {
          int deadId = Integer.parseInt(tokens[t++]);
          //Handle disconnected player id
          return;
        }
        //'main',arrayID(),'versionQuery'
        //  The client will respond
        if (action.equals("versionQuery")) {
          ArcanistCCG.NetManager.mainVersion();
          return;
        }
        //'main',arrayID(),'aliasQuery'
        //  The client will respond, if a custom name is desired
        if (action.equals("aliasQuery")) {
          if (Prefs.playerAlias.length() > 0) {
            ArcanistCCG.NetManager.serverRequestAlias(Prefs.playerAlias);
          }
          return;
        }
        //'main',arrayID(),'aliasSet',newAlias
        if (action.equals("aliasSet")) {
          String newAlias = tokens[t++];
          if (ArcanistCCG.NetManager.getPlayerId() == originId) {
            try {Prefs.setPlayerAlias(newAlias);}
            catch (IllegalArgumentException e) {ArcanistCCG.LogManager.write(e, "Server sent a bogus alias.");}
          }
          return;
        }
      }
    }
    catch (NumberFormatException e) {
      ArcanistCCG.LogManager.write(e, "Number parsing error: "+ input);
    }
    catch (IndexOutOfBoundsException e) {
      ArcanistCCG.LogManager.write(e, "Out of sync: "+ input);
      ArcanistCCG.NetManager.serverRequestSync(originId);
    }
  }


  /**
   * Close the socket and disconnect.
   */
  public void disconnect() {
    keepRunning = false;
    if (connected == false) return;

    connected = false;
    try {socket.close();}
    catch(IOException e) {
      ArcanistCCG.LogManager.write(e, "An error occurred while disconnecting: I/O Issue.");
    }
  }
}
