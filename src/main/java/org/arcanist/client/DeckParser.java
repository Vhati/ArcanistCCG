package org.arcanist.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * This loads a text file and parses cards from it.
 * The complication arises from supporting the old
 * deck format and the optional fields of the new format.
 */
public class DeckParser implements Runnable {
  private ArcanistCCGFrame frame = null;

  private volatile boolean keepRunning = false;

  private int x, y;
  private String contents = "";


  /**
   * Returns a pipe-separated list of lines from a deck file.
   * The result can be fed into DeckParser constructors.
   */
  public static String readDeckStringFromFile(ArcanistCCGFrame f, File file) {
    String result = null;

    //Read the file's lines...
    StringBuffer buf = new StringBuffer();
    BufferedReader inFile = null;
    try {
      InputStreamReader fr = new InputStreamReader(new FileInputStream(file), "UTF-8");
      inFile = new BufferedReader(fr);

      while (inFile.ready()) {
        buf.append(inFile.readLine());
        buf.append("|");
      }
      result = buf.toString();
    }
    catch (FileNotFoundException e) {
      JOptionPane.showInternalMessageDialog(f.getDesktop(), "The file, "+ file +" was not found");
      result = null;
    }
    catch (IOException e) {
      ArcanistCCG.LogManager.write(e, "Couldn't load deck.");
      result = null;
    }
    finally {
      try {if (inFile != null) inFile.close();}
      catch (IOException e) {}
    }

    return result;
  }


  public DeckParser(ArcanistCCGFrame f, String deckString, int ix, int iy) {
    frame = f;
    contents = deckString; x = ix; y=iy;
  }


  public void startThread() {
    if (keepRunning == true) return;

    keepRunning = true;
    Thread t = new Thread(this);
    t.setPriority(Thread.NORM_PRIORITY);
    t.setDaemon(true);
    t.setName("DeckParser ("+ t.getName() +")");
    t.start();
  }

  /**
   * Kills the thread.
   */
  public void killThread() {
    keepRunning = false;
  }


  public void run() {
    final List<List<Card>> newDeckList = new ArrayList<List<Card>>();
    List<String> subDecks = DeckParser.parseNestedDecks(contents);
    for (int i=0; i < subDecks.size(); i++) {
      newDeckList.add(DeckParser.parseDeckList(frame, subDecks.get(i)));
    }

    if (newDeckList.isEmpty() == false) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          Deck newDeck;
          for (int j=0; j < newDeckList.size(); j++) {
            newDeck = new Deck(frame, newDeckList.get(j), x+20*j, y+20*j, false);
            newDeck.setId(ArcanistCCG.getNextUnusedId());
            newDeck.reverse();
            newDeck.refresh();
            newDeck.addToTable();
          }
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();

          String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();
          ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +" successfully loaded deck.--");
        }
      });
    }
    System.gc();
  }


  /**
   * Generate an MD5 checksum of a deck's contents.
   *
   * @return the checksum, or null if something failed
   */
  public static String genChecksum(String deckString) {
    String checksumString = null;
    try {
      java.security.MessageDigest checksum = java.security.MessageDigest.getInstance("MD5");
      checksum.update(deckString.getBytes());
      byte[] checksumBytes = checksum.digest();
      StringBuffer checksumBuf = new StringBuffer();
      for (int i=0; i < checksumBytes.length; i++) {
        checksumBuf.append(Integer.toString( ( checksumBytes[i] & 0xff ) + 0x100, 16).substring(1));
      }
      checksumString = checksumBuf.toString();
    }
    catch (java.security.NoSuchAlgorithmException e) {ArcanistCCG.LogManager.write(e, "Failed to generate a deck's MD5.");}

    return checksumString;
  }


  /**
   * Breaks up a composite deck string into nested decks.
   *
   * @return an array of deck strings
   */
  public static List<String> parseNestedDecks(String s) {
    List<String> deckStrings = new ArrayList<String>();

    int deckStart = 0;
    int deckEnd = 0;
    while (s.length() > 0) {
      deckEnd = s.indexOf("|;\t", deckStart);
      if (deckEnd == -1) {
        deckStrings.add(s);
        break;
      } else {
        deckStrings.add(s.substring(deckStart, deckEnd+1));
        s = s.substring(deckEnd+1);
      }
    }

    return deckStrings;
  }


  /**
   * Parse a deck's contents as an array of cards.
   * Side decks should be parsed out first.
   */
  public static List<Card> parseDeckList(final ArcanistCCGFrame frame, String deckString) {
    List<Card> newDeckList = new ArrayList<Card>();

    final JInternalFrame iframe = new JInternalFrame("Loading a Deck",
      false, //resizable
      false, //closable
      false, //maximizable
      false); //iconifiable

    String[] deckLines = deckString.split("[|]");
    int totalLines = deckLines.length;
    //Done counting
    JProgressBar progressBar = new JProgressBar(0, totalLines);
      progressBar.setStringPainted(true);
      progressBar.setValue(0);
      iframe.getContentPane().add(progressBar);
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          frame.getDesktop().add(iframe);
          iframe.reshape(frame.getDesktop().getSize().width/2-100, frame.getDesktop().getSize().height/2-25, 200, 50);
          iframe.show();
        }
      });


    String frontName = "", expansion = "", frontFile ="", backName = "", backFile = "";
    String frontPath = "", backPath = "";
    String defaultBackPath = Prefs.defaultBackPath;
    String temp = ""; int copies = 0;
    Card newCard = null;
    int lineNum = 1;
    for (int n=0; n < totalLines; n++) {
      if (deckLines[n].length() == 0) break;

      String[] chunks = deckLines[n].split("\t");
      int chunkIndex = 0;

      temp = chunks[chunkIndex];
      if (temp.startsWith("//") || temp.startsWith(";") || temp.startsWith(">") || temp.startsWith("<")) {
        if (temp.startsWith(">") && chunks.length > chunkIndex+1) {
          defaultBackPath = Prefs.gameloc +"/"+ chunks[++chunkIndex];
        }
        else if (temp.startsWith("<"))
          defaultBackPath = null;
        else if (temp.startsWith(";")) {
          //Arg is an optional title
          //parseNestedDecks() handles this
        }
      }
      else {                                             //If not deck markup
        copies = 1;
        if (temp.matches("\\d{"+ temp.length() +"}+") && chunks.length > chunkIndex+1) {
          copies = Integer.parseInt(temp);
          temp = chunks[++chunkIndex];
        }
        if (temp.indexOf("/") != -1 || temp.indexOf("\\") != -1) {
          //#?  front.jpg  back.jpg?

          frontPath = temp;
          if (defaultBackPath != null)
            backPath = defaultBackPath;
          else
            backPath = Prefs.defaultBackPath;

          if (chunks.length > chunkIndex+1) {
            backPath = chunks[++chunkIndex];
          }
          newCard = Card.createValidCard("Loading deck ("+ lineNum +"): ", frame, frontPath, backPath, false);
        }
        else {
          //#?  frontName  expansion?  backName?  backFile?
          //These vars default to ""

          frontName = temp;
          if (chunks.length > chunkIndex+1) expansion = chunks[++chunkIndex];
          if (chunks.length > chunkIndex+1) frontFile = chunks[++chunkIndex];
          if (chunks.length > chunkIndex+1) backName = chunks[++chunkIndex];
          if (chunks.length > chunkIndex+1) backFile = chunks[++chunkIndex];

          newCard = Card.createValidCard("Loading deck ("+ lineNum +"): ", frame, frontName, backName, expansion, frontFile, backFile, false);
          if (newCard != null && defaultBackPath != null && newCard.getBackPath().equals(Prefs.defaultBackPath)) {
            newCard.setBackImage(defaultBackPath);
          }
        }

        if (newCard != null) {
          newCard.setId(ArcanistCCG.getNextUnusedId());
          newDeckList.add(newCard);
          for (int k=0; k < copies-1; k++) {
            Card tmpCard = new Card(frame, newCard);
            tmpCard.setId(ArcanistCCG.getNextUnusedId());
            newDeckList.add(tmpCard);
          }
        }
      }
      newCard = null; frontName = ""; expansion = ""; frontFile = ""; backName = ""; backFile = ""; frontPath = ""; backPath = "";
      lineNum++;
      updateProgress(progressBar, lineNum);
    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {iframe.setClosed(true);}                        //If closed while dragging
        catch (java.beans.PropertyVetoException e) {}        //  Bad things happen
                                                             //  Unless setClosed is called
        iframe.setVisible(false);                            //setVisible prevents leaks
        iframe.dispose();
        frame.getDesktop().repaint();                        //Then remove the drag outline
      }
    });

    return newDeckList;
  }


  /**
   * Set the progress bar's value from the event thread.
   *
   * @param the value
   */
  private static void updateProgress(final JProgressBar progressBar, final int n) {
    if (progressBar == null) return;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        progressBar.setValue(n);
      }
    });
  }
}
