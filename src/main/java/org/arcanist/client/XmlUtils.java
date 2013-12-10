package org.arcanist.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filter;
import org.jdom2.input.SAXBuilder;

import org.arcanist.client.*;
import org.arcanist.util.*;


public class XmlUtils {

  private static int MAX_STATE_VERSION = 1;


  /**
   * Loads a saved state.
   * Calls loadState() from a new thread.
   *
   * @param frame the main window
   * @param doc XML Document of a saved state
   * @param neutralView true to hide things that might've been local-only
   */
  public static void launchStateLoader(final ArcanistCCGFrame frame, final Document doc, final boolean neutralView) {
    Thread stateLoader = new Thread() {
      @Override
      public void run() {
        loadState(frame, doc, neutralView);
      }
    };
    stateLoader.setPriority(Thread.NORM_PRIORITY);
    stateLoader.setDaemon(true);
    stateLoader.start();
  }

  /**
   * Loads a saved state.
   * The current thread should be expected to block.
   * If anything goes wrong, the table is cleared.
   *
   * @param frame the main window
   * @param doc XML Document of a saved state
   * @param neutralView true to hide things that might've been local-only
   */
  public static void loadState(final ArcanistCCGFrame frame, Document doc, boolean neutralView) {
    parseWorker("starting", null, null, neutralView);
    boolean failed = false;
    try {
      Element rootNode = doc.getRootElement();
      int stateVersion = parseInt(rootNode.getAttributeValue("version"), 0);
      if (stateVersion > MAX_STATE_VERSION || stateVersion == 0) throw new IllegalArgumentException("Bad game state version.");

      parseWorker("tableClear", frame, null, neutralView);

      TreeSet<Integer> usedIdList = new TreeSet<Integer>();
      Iterator idIterator = rootNode.getDescendants(new org.jdom2.filter.AbstractFilter<Element>() {
        @Override
        public Element filter(Object content) {
          if ( content instanceof Element == false ) return null;
          Element tmpElement = (Element)content;
          if (tmpElement.getName().equals("component-id") == false) return null;
          return tmpElement;
        }
      });
      while (idIterator.hasNext()) {
        Element tmpElement = (Element)idIterator.next();
        int usedId = parseInt(tmpElement.getText(), -1);
        if (usedId != -1) {
          usedIdList.add(new Integer(usedId));
        }
      }
      ArcanistCCG.setUsedIds(usedIdList);

      Element tableElement = rootNode.getChild("table");
        parseWorker("parseTable", frame, tableElement, neutralView);

      List<Element> cardList = rootNode.getChildren("card");
        parseWorker("parseCards", frame, cardList, neutralView);

      List<Element> deckList = rootNode.getChildren("deck");
        MultiDeckLoader mdl = parseDeckElements(frame, deckList, neutralView);
        if (mdl != null) {
          try {Thread t = mdl.startThread(); if (t != null) t.join();}
          catch (InterruptedException e) {ArcanistCCG.LogManager.write(e, "State loading thread was interrupted waiting for decks to load.");}
        }

      List<Element> noteList = rootNode.getChildren("floating-note");
        parseWorker("parseNotes", frame, noteList, neutralView);

      List<Element> tokenList = rootNode.getChildren("token");
        parseWorker("parseTokens", frame, tokenList, neutralView);

      List<Element> handList = rootNode.getChildren("hand");
        parseWorker("parseHands", frame, handList, neutralView);
    }
    catch (JDOMException e) {
      failed = true;
      ArcanistCCG.LogManager.write(e, "Couldn't load state.");
    }
    catch (IllegalArgumentException e) {
      failed = true;
      if (e.getMessage() == null || !e.getMessage().equals("Bad game state version.")) {
        ArcanistCCG.LogManager.write(e, "Couldn't load state.");
      }
      else {
        ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Bad game state version.");
      }
    }
    if (failed) {
      parseWorker("failed", frame, null, neutralView);
    } else {
      parseWorker("done", null, null, neutralView);
    }
    parseWorker("tableRepaint", frame, null, neutralView);
  }


  /**
   * Reads a string to get an XML Document.
   *
   * @param s an xml string
   * @return the Document, or null if something failed
   */
  public static Document readXML(String s) {
    StringReader sr = null;
    Document doc = null;
    try {
      sr = new StringReader(s);
      doc = new SAXBuilder().build(sr);
    }
    catch (JDOMException e) {ArcanistCCG.LogManager.write(e, "Couldn't read xml.");}
    catch (IOException e) {ArcanistCCG.LogManager.write(e, "Couldn't read xml.");}
    finally {
      sr.close();
    }
    return doc;
  }

  /**
   * Reads a file to get an XML Document.
   *
   * @param file an xml file
   * @return the Document, or null if something failed
   */
  public static Document readXML(File file) {
    Document doc = null;
    BufferedReader inFile = null;
    try {
      InputStreamReader fr = new InputStreamReader(new FileInputStream(file), "UTF-8");
      inFile = new BufferedReader(fr);
      doc = new SAXBuilder().build(inFile);
    }
    catch (JDOMException e) {ArcanistCCG.LogManager.write(e, "Couldn't read xml from \""+ file.getName() +"\".");}
    catch (IOException e) {ArcanistCCG.LogManager.write(e, "Couldn't read xml from \""+ file.getName() +"\".");}
    finally {
      try {if (inFile != null) inFile.close();}
      catch (IOException e) {}
    }
    return doc;
  }


  /**
   * Ensures things run in the event thread while this thread waits.
   * This calls one of several functions, wrapped in a Runnable.
   * If this is the event thread, the runnable's run() is called directly (no threading or waiting).
   * Otherwise the runnable is fed to invokeAndWait().
   *
   * @param action tableClear, tableRepaint, parseTable, parseCards, parseNotes, parseTokens, starting, done
   * @param frame the main window
   * @param arg an argument for the parse methods, null otherwise
   * @param neutralView true to hide things that might've been local-only
   */
  @SuppressWarnings("unchecked")
  private static void parseWorker(final String action, final ArcanistCCGFrame frame, final Object arg, final boolean neutralView) {
    Runnable worker = new Runnable() {
      @Override
      public void run() {
        try {
          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

          if (action.equals("tableClear")) frame.clearTable();
          else if (action.equals("tableRepaint")) {
            frame.getTableFrame().revalidate();
            frame.getTableFrame().repaint();
          }
          else if (action.equals("parseTable")) parseTableElement(frame, (Element)arg, neutralView);
          else if (action.equals("parseCards")) parseCardElements(frame, (List<Element>)arg, neutralView);
          else if (action.equals("parseNotes")) parseNoteElements(frame, (List<Element>)arg, neutralView);
          else if (action.equals("parseTokens")) parseTokenElements(frame, (List<Element>)arg, neutralView);
          else if (action.equals("parseHands")) parseHandElements(frame, (List<Element>)arg, neutralView);
          else if (action.equals("starting")) {}
          else if (action.equals("done")) {
            ArcanistCCG.NetManager.chatNotice("--"+ chatAlias +" finished loading the game state.--");
            ArcanistCCG.NetManager.tableStateResult(true);
          }
          else if (action.equals("failed")) {
            ArcanistCCG.NetManager.chatNotice("--"+ chatAlias +" failed to load the game state.--");
            ArcanistCCG.NetManager.tableStateResult(false);
          }
        }
        catch (JDOMException e) {ArcanistCCG.LogManager.write(e, "Couldn't load state.");}
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      worker.run();
    } else {
      try {
        SwingUtilities.invokeAndWait(worker);
      }
      catch (java.lang.reflect.InvocationTargetException e) {ArcanistCCG.LogManager.write(e, "Couldn't load state.");}
      catch (InterruptedException e) {ArcanistCCG.LogManager.write(e, "Couldn't load state.");}
    }
  }


  private static void parseTableElement(ArcanistCCGFrame frame, Element tableElement, boolean neutralView) {
    Element tmpElement = tableElement.getChild("size");
    if (tmpElement != null) {
      int[] wh = parseXYAttribs(tmpElement, 1000, 1000);
      frame.resizeTable(wh[0], wh[1]);
    }

    tmpElement = tableElement.getChild("location");
    if (tmpElement != null) {
      int[] xy = parseXYAttribs(tmpElement, 0, 0);
      frame.getTablePane().scrollRectToVisible(new Rectangle(xy[0], xy[1], 1, 1));
    }
  }

  private static void parseCardElements(ArcanistCCGFrame frame, List<Element> cardList, boolean neutralView) throws JDOMException {
    Card[] cardsByIndex = new Card[cardList.size()];
    Card[] cardsByDepth = new Card[cardList.size()];
    int[][] coordsByDepth = new int[cardList.size()][];
    Iterator<Element> it = cardList.iterator();
    while(it.hasNext()) {
      Element cardElement = it.next();
      int arrayIndex = parseInt(cardElement.getChildText("array-index"), -1);
      int componentId = parseInt(cardElement.getChildText("component-id"), -1);
      int tableDepth = parseInt(cardElement.getChildText("table-depth"), 0);
      int[] xy = parseXYAttribs(cardElement.getChild("location"), 0, 0);
      String expansion = parseString(cardElement.getChildText("expansion"), "");
      String frontName = parseString(cardElement.getChildText("front-name"), "");
      String backName = parseString(cardElement.getChildText("back-name"), "");
      String frontFile = parseString(cardElement.getChildText("front-file"), "");
      String backFile = parseString(cardElement.getChildText("back-file"), "");
      boolean flipState = parseString(cardElement.getChildText("flipstate"), "").equals("true");
      int rotation = parseInt(cardElement.getChildText("rotation"), 0);

      if (neutralView) flipState = false;

      Card newCard = Card.createValidCard("Loading state: ", frame, frontName, backName, expansion, frontFile, backFile, flipState);

      if (newCard != null) {
        newCard.setId(componentId);
        newCard.setRotation(rotation);
        if (arrayIndex < cardsByIndex.length && tableDepth < cardsByDepth.length) {
          cardsByIndex[arrayIndex] = newCard;
          cardsByDepth[tableDepth] = newCard;
          coordsByDepth[tableDepth] = xy;
        }
      }
    }
    for (int i=0; i < cardsByIndex.length; i++) {
      if (cardsByIndex[i] != null) frame.addCard(cardsByIndex[i]);
    }
    for (int i=0; i < cardsByDepth.length; i++) {
      if (cardsByDepth[i] != null) {
        frame.getTablePane().add(cardsByDepth[i].getComponent(), Prefs.cardLayer);
        cardsByDepth[i].setLocation(coordsByDepth[i][0], coordsByDepth[i][1]);
      }
    }
  }

  private static MultiDeckLoader parseDeckElements(ArcanistCCGFrame frame, List<Element> deckList, boolean neutralView) throws JDOMException {
    MultiDeckLoader mdl = null;
    if (deckList.size() > 0) {
      mdl = new MultiDeckLoader(frame);
      mdl.setReportResult(false);  // Report how the whole state went, not each deck
    }
    Iterator<Element> it = deckList.iterator();
    while(it.hasNext()) {
      Element deckElement = it.next();
      int arrayIndex = parseInt(deckElement.getChildText("array-index"), -1);
      int componentId = parseInt(deckElement.getChildText("component-id"), -1);
      int[] xy = parseXYAttribs(deckElement.getChild("location"), 0, 0);
      boolean locked = parseString(deckElement.getChildText("locked"), "").equals("true");
      boolean autoface = parseString(deckElement.getChildText("autoface"), "").equals("true");
      boolean facing = parseString(deckElement.getChildText("facing"), "").equals("true");
      boolean toBottom = parseString(deckElement.getChildText("tobottom"), "").equals("true");
      String tmp = parseString(deckElement.getChildText("draw-offset"), "D");
      char drawOffset = 'D';
      if (tmp.equals("U")) drawOffset = 'U';
      else if (tmp.equals("D")) drawOffset = 'D';
      else if (tmp.equals("L")) drawOffset = 'L';
      else if (tmp.equals("R")) drawOffset = 'R';
      String contents = parseString(deckElement.getChildText("deck-contents"), "");
        contents = contents.replaceAll("\\n","|") +"|";

      mdl.addDeck(componentId, contents, xy[0], xy[1], locked, autoface, facing, toBottom, drawOffset);
    }
    return mdl;
  }

  private static void parseNoteElements(ArcanistCCGFrame frame, List<Element> noteList, boolean neutralView) throws JDOMException {
    Iterator<Element> it = noteList.iterator();
    while(it.hasNext()) {
      Element noteElement = it.next();
      int arrayIndex = parseInt(noteElement.getChildText("array-index"), -1);
      int componentId = parseInt(noteElement.getChildText("component-id"), -1);
      int[] xy = parseXYAttribs(noteElement.getChild("location"), 0, 0);
      String noteText = parseString(noteElement.getChildText("note-text"), "");

      FloatingNote newNote = new FloatingNote(frame, noteText);
      newNote.setId(componentId);
      newNote.addToTable(xy[0], xy[1]);
    }
  }

  private static void parseTokenElements(ArcanistCCGFrame frame, List<Element> tokenList, boolean neutralView) throws JDOMException {
    Iterator<Element> it = tokenList.iterator();
    while(it.hasNext()) {
      Element tokenElement = it.next();
      int arrayIndex = parseInt(tokenElement.getChildText("array-index"), -1);
      int componentId = parseInt(tokenElement.getChildText("component-id"), -1);
      int[] xy = parseXYAttribs(tokenElement.getChild("location"), 0, 0);
      String tokenPath = parseString(tokenElement.getChildText("image-path"), "");

      Token newToken = new Token(frame, Prefs.homePath +"images/Tokens/"+ tokenPath);
      newToken.setId(componentId);
      newToken.addToTable(xy[0], xy[1]);
    }
  }

  private static void parseHandElements(ArcanistCCGFrame frame, List<Element> noteList, boolean neutralView) throws JDOMException {
    Iterator<Element> it = noteList.iterator();
    while(it.hasNext()) {
      Element handElement = it.next();
      int arrayIndex = parseInt(handElement.getChildText("array-index"), -1);
      int componentId = parseInt(handElement.getChildText("component-id"), -1);
      int[] wh = parseXYAttribs(handElement.getChild("size"), 1, 1);
      int[] xy = parseXYAttribs(handElement.getChild("location"), 0, 0);
      String title = parseString(handElement.getChildText("hand-title"), "");
      boolean hovering = parseString(handElement.getChildText("hand-hovering"), "").equals("true");
      boolean revealed = parseString(handElement.getChildText("hand-revealed"), "").equals("true");
      int drawDeckId = parseInt(handElement.getChildText("hand-draw-deck-id"), -1);
      int discardDeckId = parseInt(handElement.getChildText("hand-discard-deck-id"), -1);
      String contents = parseString(handElement.getChildText("hand-contents"), "");

      if (neutralView) revealed = false;

      Hand newHand = new Hand(frame, title, wh[0], wh[1]);
      newHand.setId(componentId);
      newHand.setInUse(true);
      newHand.setHovering(hovering);
      newHand.setRevealed(revealed);
      Deck drawDeck = (drawDeckId==-1 ? null : frame.getDeck(drawDeckId));
      if (drawDeck != null) newHand.setDrawDeck(drawDeck);
      Deck discardDeck = (discardDeckId==-1 ? null : frame.getDeck(discardDeckId));
      if (discardDeck != null) newHand.setDrawDeck(discardDeck);
      newHand.addToTable(xy[0], xy[1]);

      String[] lines = contents.split("[\r\n]");
      List<Card> newCardList = new ArrayList<Card>(lines.length);
      for (int i=0; i < lines.length; i++) {
        String[] chunks = lines[i].split("\t");
        if (chunks.length > 0) {
          Card newCard = null;
          int copies = Integer.parseInt(chunks[0]);
          if (copies > 0) {
            String frontName = (chunks.length>1 ? chunks[1] : "");
            String backName = (chunks.length>4 ? chunks[4] : "");
            String expansion = (chunks.length>2 ? chunks[2] : "");
            String frontFile = (chunks.length>3 ? chunks[3] : "");
            String backFile = (chunks.length>5 ? chunks[5] : "");
            newCard = Card.createValidCard("Loading state: ", frame, frontName, backName, expansion, frontFile, backFile, true);
          }
          if (newCard != null) {
            newCard.setId(ArcanistCCG.getNextUnusedId());
            newCardList.add(newCard);
            for (int j=0; j < copies-1; j++) {
              Card tmpCard = new Card(frame, newCard);
              tmpCard.setId(ArcanistCCG.getNextUnusedId());
              newCardList.add(tmpCard);
            }
          }
        }
      }
      newHand.addCards(newCardList);

      newHand.setInUse(false);
    }
  }


  /**
   * Saves the table's state to a file.
   *
   * @param frame the main window
   */
  public static void saveState(ArcanistCCGFrame frame, String statePath) {
    BufferedWriter outFile = null;
    try {
      OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(statePath), "UTF-8");
      outFile = new BufferedWriter(fw);
      outFile.write(saveState(frame));
      outFile.close();
    }
    catch (IOException e) {ArcanistCCG.LogManager.write(e, "Couldn't save state.");}
    finally {
      try {if (outFile != null) outFile.close();}
      catch (IOException e) {}
    }
  }

  /**
   * Generates XML to save the table's state.
   *
   * @return the saved state
   */
  public static String saveState(ArcanistCCGFrame frame) {
    StringBuffer buf = new StringBuffer();
    buf.append("<?xml version=\"1.0\"?>\n");
    buf.append("<state version=\"1\">\n");

    Dimension tableSize = frame.getTablePane().getPreferredSize();
    Rectangle tableView = frame.getTableView();
    buf.append("  <table>\n");
    buf.append("    <size x=\""); buf.append(tableSize.width); buf.append("\" y=\""); buf.append(tableSize.height); buf.append("\" />\n");
    buf.append("    <location x=\""); buf.append(tableView.x); buf.append("\" y=\""); buf.append(tableView.y); buf.append("\" />\n");
    buf.append("  </table>\n");

    int cardsCount = frame.getCardCount();
    for (int i=0; i < cardsCount; i++) {
      Card tmpCard = frame.getCard(i);
      int componentId = tmpCard.getId();
      int tableDepth = frame.getTablePane().getPosition(tmpCard.getComponent());
      Point location = tmpCard.getLocation();
      String expansion = Prefs.textDat.getSetNameFromAbbrev(tmpCard.getSetAbbrev());
      String frontName = tmpCard.getFrontName();
      String backName = tmpCard.getBackName();
      String frontFile = tmpCard.getFrontFile();
      String backFile = tmpCard.getBackFile();
      boolean flipState = tmpCard.getFlipState();
      int rotation = tmpCard.getRotation();

      buf.append("  <card>\n");
      buf.append("    <array-index>"); buf.append(i); buf.append("</array-index>\n");
      buf.append("    <component-id>"); buf.append(componentId); buf.append("</component-id>\n");
      buf.append("    <table-depth>"); buf.append(tableDepth); buf.append("</table-depth>\n");
      buf.append("    <location x=\""); buf.append(location.x); buf.append("\" y=\""); buf.append(location.y); buf.append("\" />\n");
      buf.append("    <expansion><![CDATA["); buf.append(expansion); buf.append("]]></expansion>\n");
      buf.append("    <front-name><![CDATA["); buf.append(frontName); buf.append("]]></front-name>\n");
      buf.append("    <back-name><![CDATA["); buf.append(backName); buf.append("]]></back-name>\n");
      buf.append("    <front-file><![CDATA["); buf.append(frontFile); buf.append("]]></front-file>\n");
      buf.append("    <back-file><![CDATA["); buf.append(backFile); buf.append("]]></back-file>\n");
      buf.append("    <flipstate>"); buf.append(flipState); buf.append("</flipstate>\n");
      buf.append("    <rotation>"); buf.append(rotation); buf.append("</rotation>\n");
      buf.append("  </card>\n");
    }

    int decksCount = frame.getDeckCount();
    for (int i=0; i < decksCount; i++) {
      Deck tmpDeck = frame.getDeck(i);
      Point location = tmpDeck.getLocation();
      boolean locked = tmpDeck.getLocked();
      boolean autoface = tmpDeck.getAutoFace();
      boolean facing = tmpDeck.getFacing();
      boolean toBottom = tmpDeck.getToBottom();
      char drawOffset = tmpDeck.getOffset();
      String contents = tmpDeck.save();

      buf.append("  <deck>\n");
      buf.append("    <array-index>"); buf.append(i); buf.append("</array-index>\n");
      buf.append("    <location x=\""); buf.append(location.x); buf.append("\" y=\""); buf.append(location.y); buf.append("\" />\n");
      buf.append("    <locked>"); buf.append(locked); buf.append("</locked>\n");
      buf.append("    <autoface>"); buf.append(autoface); buf.append("</autoface>\n");
      buf.append("    <facing>"); buf.append(facing); buf.append("</facing>\n");
      buf.append("    <tobottom>"); buf.append(toBottom); buf.append("</tobottom>\n");
      buf.append("    <draw-offset>"); buf.append(drawOffset); buf.append("</draw-offset>\n");
      buf.append("    <deck-contents><![CDATA["); buf.append(contents); buf.append("]]></deck-contents>\n");
      buf.append("  </deck>\n");
    }

    int tokensCount = frame.getTokenCount();
    for (int i=0; i < tokensCount; i++) {
      Token tmpToken = frame.getToken(i);
      int componentId = tmpToken.getId();
      Point location = tmpToken.getLocation();
      String tokenPath = tmpToken.getImagePath();
        String tokenPathPrefix = Prefs.homePath +"images/Tokens/";
        if (tokenPath.indexOf(tokenPathPrefix) != -1) {
          tokenPath = tokenPath.substring(tokenPath.indexOf(tokenPathPrefix)+ tokenPathPrefix.length());
        } else {
          continue;  //Skip problematic tokens
        }

      buf.append("  <token>\n");
      buf.append("    <array-index>"); buf.append(i); buf.append("</array-index>\n");
      buf.append("    <component-id>"); buf.append(componentId); buf.append("</component-id>\n");
      buf.append("    <location x=\""); buf.append(location.x); buf.append("\" y=\""); buf.append(location.y); buf.append("\" />\n");
      buf.append("    <image-path>"); buf.append(tokenPath); buf.append("</image-path>\n");
      buf.append("  </token>\n");
    }

    int notesCount = frame.getNoteCount();
    for (int i=0; i < notesCount; i++) {
      FloatingNote tmpNote = frame.getNote(i);
      int componentId = tmpNote.getId();
      Point location = tmpNote.getLocation();
      String noteText = tmpNote.getText();

      buf.append("  <floating-note>\n");
      buf.append("    <array-index>"); buf.append(i); buf.append("</array-index>\n");
      buf.append("    <component-id>"); buf.append(componentId); buf.append("</component-id>\n");
      buf.append("    <location x=\""); buf.append(location.x); buf.append("\" y=\""); buf.append(location.y); buf.append("\" />\n");
      buf.append("    <note-text>"); buf.append(noteText); buf.append("</note-text>\n");
      buf.append("  </floating-note>\n");
    }

    int handsCount = frame.getHandCount();
    for (int i=0; i < handsCount; i++) {
      Hand tmpHand = frame.getHand(i);
      int componentId = tmpHand.getId();
      Dimension size = tmpHand.getSize();
      Point location = tmpHand.getLocation();
      String handTitle = tmpHand.getTitle();
      boolean handHovering = tmpHand.getHovering();
      boolean handRevealed = tmpHand.getRevealed();
      int drawDeckId = -1;
      if (tmpHand.getDrawDeck() != null) drawDeckId = tmpHand.getDrawDeck().getId();
      int discardDeckId = -1;
      if (tmpHand.getDiscardDeck() != null) discardDeckId = tmpHand.getDiscardDeck().getId();
      String contents = tmpHand.save();

      buf.append("  <hand>\n");
      buf.append("    <array-index>"); buf.append(i); buf.append("</array-index>\n");
      buf.append("    <component-id>"); buf.append(componentId); buf.append("</component-id>\n");
      buf.append("    <size x=\""); buf.append(size.width); buf.append("\" y=\""); buf.append(size.height); buf.append("\" />\n");
      buf.append("    <location x=\""); buf.append(location.x); buf.append("\" y=\""); buf.append(location.y); buf.append("\" />\n");
      buf.append("    <hand-title>"); buf.append(handTitle); buf.append("</hand-title>\n");
      buf.append("    <hand-hovering>"); buf.append(handHovering); buf.append("</hand-hovering>\n");
      buf.append("    <hand-revealed>"); buf.append(handRevealed); buf.append("</hand-revealed>\n");
      buf.append("    <hand-draw-deck-id>"); buf.append(drawDeckId); buf.append("</hand-draw-deck-id>\n");
      buf.append("    <hand-discard-deck-id>"); buf.append(drawDeckId); buf.append("</hand-discard-deck-id>\n");
      buf.append("    <hand-contents><![CDATA["); buf.append(contents); buf.append("]]></hand-contents>\n");
      buf.append("  </hand>\n");
    }

    buf.append("</state>");
    return buf.toString();
  }


  /**
   * Parses an xml Element's x and y attributes.
   * If either is absent or not an integer, its default is used.
   *
   * @param e an xml Element
   * @param x the default x
   * @param y the default y
   * @return the parsed result [x,y]
   */
  public static int[] parseXYAttribs(Element e, int x, int y) {
    int[] result = new int[] {x, y};
    if (e == null) return result;
    result[0] = parseInt(e.getAttributeValue("x"), x);
    result[1] = parseInt(e.getAttributeValue("y"), y);
    return result;
  }

  /**
   * Parses an xml Element's r,g,b attributes.
   * If any are absent or not integers, the default is returned.
   *
   * @param e an xml Element
   * @param defaultColor the default, or null (to return null)
   * @return the parsed result
   */
  public static Color parseRGBAttribs(Element e, Color defaultColor) {
    Color result = defaultColor;
    if (e == null) return result;
    int r = parseInt(e.getAttributeValue("r"), -1);
    int g = parseInt(e.getAttributeValue("g"), -1);
    int b = parseInt(e.getAttributeValue("b"), -1);
    if (r < 0 || g < 0 || b < 0) return result;
    result = new Color(r, g, b);
    return result;
  }


  /**
   * Parses an integer.
   * If null or not an integer, the default is returned.
   *
   * @param s a string
   * @param defaultValue the default
   * @return the parsed result
   */
  public static int parseInt(String s, int defaultValue) {
    if (s == null) return defaultValue;
    try {
      return Integer.parseInt(s);
    }
    catch(NumberFormatException e) {
      return defaultValue;
    }
  }


  /**
   * Parses a float.
   * If null or not a float, the default is returned.
   *
   * @param s a string
   * @param defaultValue the default
   * @return the parsed result
   */
  public static float parseFloat(String s, float defaultValue) {
    if (s == null) return defaultValue;
    try {
      return Float.parseFloat(s);
    }
    catch(NumberFormatException e) {
      return defaultValue;
    }
  }


  /**
   * Parses a string.
   * If null, the default is returned.
   *
   * @param s a string
   * @param defaultValue the default
   * @return the parsed result
   */
  public static String parseString(String s, String defaultValue) {
    if (s == null) return defaultValue;
    return s;
  }


  /**
   * Serializes a Keystrike to a String.
   * It can then be fed into KeyStroke.getKeyStroke(String).
   *
   * See: http://www.exampledepot.com/egs/javax.swing/Key2Str.html
   */
  public static String keyStroke2String(KeyStroke key) {
    StringBuffer s = new StringBuffer(50);
    int m = key.getModifiers();

    if ((m & (InputEvent.SHIFT_DOWN_MASK|InputEvent.SHIFT_MASK)) != 0) {
      s.append("shift ");
    }
    if ((m & (InputEvent.CTRL_DOWN_MASK|InputEvent.CTRL_MASK)) != 0) {
      s.append("ctrl ");
    }
    if ((m & (InputEvent.META_DOWN_MASK|InputEvent.META_MASK)) != 0) {
      s.append("meta ");
    }
    if ((m & (InputEvent.ALT_DOWN_MASK|InputEvent.ALT_MASK)) != 0) {
      s.append("alt ");
    }
    if ((m & (InputEvent.BUTTON1_DOWN_MASK|InputEvent.BUTTON1_MASK)) != 0) {
      s.append("button1 ");
    }
    if ((m & (InputEvent.BUTTON2_DOWN_MASK|InputEvent.BUTTON2_MASK)) != 0) {
      s.append("button2 ");
    }
    if ((m & (InputEvent.BUTTON3_DOWN_MASK|InputEvent.BUTTON3_MASK)) != 0) {
      s.append("button3 ");
    }

    switch (key.getKeyEventType()) {
      case KeyEvent.KEY_TYPED:
        if (key.getKeyChar() == Character.MAX_VALUE) {
          s.append("unknown-typed-key ");
          break;
        }
        s.append("typed ").append(key.getKeyChar()).append(" ");
        break;
      case KeyEvent.KEY_PRESSED:
        s.append("pressed ").append(getKeyText(key.getKeyCode())).append(" ");
        break;
      case KeyEvent.KEY_RELEASED:
        s.append("released ").append(getKeyText(key.getKeyCode())).append(" ");
        break;
      default:
        s.append("unknown-event-type ");
        break;
    }

    return s.toString();
  }

  public static String getKeyText(int keyCode) {
    if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9 ||
      keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
      return String.valueOf((char)keyCode);
    }

    switch(keyCode) {
      case KeyEvent.VK_COMMA: return "COMMA";
      case KeyEvent.VK_PERIOD: return "PERIOD";
      case KeyEvent.VK_SLASH: return "SLASH";
      case KeyEvent.VK_SEMICOLON: return "SEMICOLON";
      case KeyEvent.VK_EQUALS: return "EQUALS";
      case KeyEvent.VK_OPEN_BRACKET: return "OPEN_BRACKET";
      case KeyEvent.VK_BACK_SLASH: return "BACK_SLASH";
      case KeyEvent.VK_CLOSE_BRACKET: return "CLOSE_BRACKET";

      case KeyEvent.VK_ENTER: return "ENTER";
      case KeyEvent.VK_BACK_SPACE: return "BACK_SPACE";
      case KeyEvent.VK_TAB: return "TAB";
      case KeyEvent.VK_CANCEL: return "CANCEL";
      case KeyEvent.VK_CLEAR: return "CLEAR";
      case KeyEvent.VK_SHIFT: return "SHIFT";
      case KeyEvent.VK_CONTROL: return "CONTROL";
      case KeyEvent.VK_ALT: return "ALT";
      case KeyEvent.VK_PAUSE: return "PAUSE";
      case KeyEvent.VK_CAPS_LOCK: return "CAPS_LOCK";
      case KeyEvent.VK_ESCAPE: return "ESCAPE";
      case KeyEvent.VK_SPACE: return "SPACE";
      case KeyEvent.VK_PAGE_UP: return "PAGE_UP";
      case KeyEvent.VK_PAGE_DOWN: return "PAGE_DOWN";
      case KeyEvent.VK_END: return "END";
      case KeyEvent.VK_HOME: return "HOME";
      case KeyEvent.VK_LEFT: return "LEFT";
      case KeyEvent.VK_UP: return "UP";
      case KeyEvent.VK_RIGHT: return "RIGHT";
      case KeyEvent.VK_DOWN: return "DOWN";

      // numpad numeric keys handled below
      case KeyEvent.VK_MULTIPLY: return "MULTIPLY";
      case KeyEvent.VK_ADD: return "ADD";
      case KeyEvent.VK_SEPARATOR: return "SEPARATOR";
      case KeyEvent.VK_SUBTRACT: return "SUBTRACT";
      case KeyEvent.VK_DECIMAL: return "DECIMAL";
      case KeyEvent.VK_DIVIDE: return "DIVIDE";
      case KeyEvent.VK_DELETE: return "DELETE";
      case KeyEvent.VK_NUM_LOCK: return "NUM_LOCK";
      case KeyEvent.VK_SCROLL_LOCK: return "SCROLL_LOCK";

      case KeyEvent.VK_F1: return "F1";
      case KeyEvent.VK_F2: return "F2";
      case KeyEvent.VK_F3: return "F3";
      case KeyEvent.VK_F4: return "F4";
      case KeyEvent.VK_F5: return "F5";
      case KeyEvent.VK_F6: return "F6";
      case KeyEvent.VK_F7: return "F7";
      case KeyEvent.VK_F8: return "F8";
      case KeyEvent.VK_F9: return "F9";
      case KeyEvent.VK_F10: return "F10";
      case KeyEvent.VK_F11: return "F11";
      case KeyEvent.VK_F12: return "F12";
      case KeyEvent.VK_F13: return "F13";
      case KeyEvent.VK_F14: return "F14";
      case KeyEvent.VK_F15: return "F15";
      case KeyEvent.VK_F16: return "F16";
      case KeyEvent.VK_F17: return "F17";
      case KeyEvent.VK_F18: return "F18";
      case KeyEvent.VK_F19: return "F19";
      case KeyEvent.VK_F20: return "F20";
      case KeyEvent.VK_F21: return "F21";
      case KeyEvent.VK_F22: return "F22";
      case KeyEvent.VK_F23: return "F23";
      case KeyEvent.VK_F24: return "F24";

      case KeyEvent.VK_PRINTSCREEN: return "PRINTSCREEN";
      case KeyEvent.VK_INSERT: return "INSERT";
      case KeyEvent.VK_HELP: return "HELP";
      case KeyEvent.VK_META: return "META";
      case KeyEvent.VK_BACK_QUOTE: return "BACK_QUOTE";
      case KeyEvent.VK_QUOTE: return "QUOTE";

      case KeyEvent.VK_KP_UP: return "KP_UP";
      case KeyEvent.VK_KP_DOWN: return "KP_DOWN";
      case KeyEvent.VK_KP_LEFT: return "KP_LEFT";
      case KeyEvent.VK_KP_RIGHT: return "KP_RIGHT";

      case KeyEvent.VK_DEAD_GRAVE: return "DEAD_GRAVE";
      case KeyEvent.VK_DEAD_ACUTE: return "DEAD_ACUTE";
      case KeyEvent.VK_DEAD_CIRCUMFLEX: return "DEAD_CIRCUMFLEX";
      case KeyEvent.VK_DEAD_TILDE: return "DEAD_TILDE";
      case KeyEvent.VK_DEAD_MACRON: return "DEAD_MACRON";
      case KeyEvent.VK_DEAD_BREVE: return "DEAD_BREVE";
      case KeyEvent.VK_DEAD_ABOVEDOT: return "DEAD_ABOVEDOT";
      case KeyEvent.VK_DEAD_DIAERESIS: return "DEAD_DIAERESIS";
      case KeyEvent.VK_DEAD_ABOVERING: return "DEAD_ABOVERING";
      case KeyEvent.VK_DEAD_DOUBLEACUTE: return "DEAD_DOUBLEACUTE";
      case KeyEvent.VK_DEAD_CARON: return "DEAD_CARON";
      case KeyEvent.VK_DEAD_CEDILLA: return "DEAD_CEDILLA";
      case KeyEvent.VK_DEAD_OGONEK: return "DEAD_OGONEK";
      case KeyEvent.VK_DEAD_IOTA: return "DEAD_IOTA";
      case KeyEvent.VK_DEAD_VOICED_SOUND: return "DEAD_VOICED_SOUND";
      case KeyEvent.VK_DEAD_SEMIVOICED_SOUND: return "DEAD_SEMIVOICED_SOUND";

      case KeyEvent.VK_AMPERSAND: return "AMPERSAND";
      case KeyEvent.VK_ASTERISK: return "ASTERISK";
      case KeyEvent.VK_QUOTEDBL: return "QUOTEDBL";
      case KeyEvent.VK_LESS: return "LESS";
      case KeyEvent.VK_GREATER: return "GREATER";
      case KeyEvent.VK_BRACELEFT: return "BRACELEFT";
      case KeyEvent.VK_BRACERIGHT: return "BRACERIGHT";
      case KeyEvent.VK_AT: return "AT";
      case KeyEvent.VK_COLON: return "COLON";
      case KeyEvent.VK_CIRCUMFLEX: return "CIRCUMFLEX";
      case KeyEvent.VK_DOLLAR: return "DOLLAR";
      case KeyEvent.VK_EURO_SIGN: return "EURO_SIGN";
      case KeyEvent.VK_EXCLAMATION_MARK: return "EXCLAMATION_MARK";
      case KeyEvent.VK_INVERTED_EXCLAMATION_MARK:
               return "INVERTED_EXCLAMATION_MARK";
      case KeyEvent.VK_LEFT_PARENTHESIS: return "LEFT_PARENTHESIS";
      case KeyEvent.VK_NUMBER_SIGN: return "NUMBER_SIGN";
      case KeyEvent.VK_MINUS: return "MINUS";
      case KeyEvent.VK_PLUS: return "PLUS";
      case KeyEvent.VK_RIGHT_PARENTHESIS: return "RIGHT_PARENTHESIS";
      case KeyEvent.VK_UNDERSCORE: return "UNDERSCORE";

      case KeyEvent.VK_FINAL: return "FINAL";
      case KeyEvent.VK_CONVERT: return "CONVERT";
      case KeyEvent.VK_NONCONVERT: return "NONCONVERT";
      case KeyEvent.VK_ACCEPT: return "ACCEPT";
      case KeyEvent.VK_MODECHANGE: return "MODECHANGE";
      case KeyEvent.VK_KANA: return "KANA";
      case KeyEvent.VK_KANJI: return "KANJI";
      case KeyEvent.VK_ALPHANUMERIC: return "ALPHANUMERIC";
      case KeyEvent.VK_KATAKANA: return "KATAKANA";
      case KeyEvent.VK_HIRAGANA: return "HIRAGANA";
      case KeyEvent.VK_FULL_WIDTH: return "FULL_WIDTH";
      case KeyEvent.VK_HALF_WIDTH: return "HALF_WIDTH";
      case KeyEvent.VK_ROMAN_CHARACTERS: return "ROMAN_CHARACTERS";
      case KeyEvent.VK_ALL_CANDIDATES: return "ALL_CANDIDATES";
      case KeyEvent.VK_PREVIOUS_CANDIDATE: return "PREVIOUS_CANDIDATE";
      case KeyEvent.VK_CODE_INPUT: return "CODE_INPUT";
      case KeyEvent.VK_JAPANESE_KATAKANA: return "JAPANESE_KATAKANA";
      case KeyEvent.VK_JAPANESE_HIRAGANA: return "JAPANESE_HIRAGANA";
      case KeyEvent.VK_JAPANESE_ROMAN: return "JAPANESE_ROMAN";
      case KeyEvent.VK_KANA_LOCK: return "KANA_LOCK";
      case KeyEvent.VK_INPUT_METHOD_ON_OFF: return "INPUT_METHOD_ON_OFF";

      case KeyEvent.VK_AGAIN: return "AGAIN";
      case KeyEvent.VK_UNDO: return "UNDO";
      case KeyEvent.VK_COPY: return "COPY";
      case KeyEvent.VK_PASTE: return "PASTE";
      case KeyEvent.VK_CUT: return "CUT";
      case KeyEvent.VK_FIND: return "FIND";
      case KeyEvent.VK_PROPS: return "PROPS";
      case KeyEvent.VK_STOP: return "STOP";

      case KeyEvent.VK_COMPOSE: return "COMPOSE";
      case KeyEvent.VK_ALT_GRAPH: return "ALT_GRAPH";
    }

    if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9) {
      char c = (char)(keyCode - KeyEvent.VK_NUMPAD0 + '0');
      return "NUMPAD"+c;
    }

    return "unknown(0x" + Integer.toString(keyCode, 16) + ")";
  }
}
