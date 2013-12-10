package org.arcanist.client;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A thread to load multiple decks and set their attributes.
 * Instantiate it with an expected count.
 * Then call addDeck() that many times.
 * Then call start().
 */
public class MultiDeckLoader implements Runnable {

  private ArcanistCCGFrame frame = null;

  private MultiDeckLoader pronoun = this;

  private volatile boolean keepRunning = false;
  private volatile boolean reportResult = false;
  private int readyCount = 0;
  private List<String> deckStrings = null;
  private List<DeckAttribs> attribs = null;

  private List<List<Card>> decklists = null;


  public MultiDeckLoader(ArcanistCCGFrame f) {
    frame = f;
    deckStrings = new ArrayList<String>();
    attribs = new ArrayList<DeckAttribs>();
    decklists = new ArrayList<List<Card>>();
  }


  public Thread startThread() {
    if (keepRunning == true) return null;

    keepRunning = true;
    Thread t = new Thread(this);
    t.setPriority(Thread.NORM_PRIORITY);
    t.setDaemon(true);
    t.setName("MultiDeckLoader ("+ t.getName() +")");
    t.start();

    return t;
  }

  /**
   * Kills the thread.
   */
  public void killThread() {
    keepRunning = false;
  }


  /**
   * Sets whether to notify the server on completion.
   */
  public void setReportResult(boolean state) {reportResult = state;}


  /**
   * Set each deck's attributes.
   * Call this as part of setup before starting the thread.
   * Nothing happens if already started.
   *
   * id The desired id, or -1 for next available.
   *    Subdecks beyond the first will be -1.
   */
  public synchronized void addDeck(int id, String contents, int x, int y, boolean locked, boolean autoface, boolean facing, boolean toBottom, char drawOffset) {
    if (keepRunning) return;
    if (id != -1) {
      if (ArcanistCCG.reserveId(id) == false) {
        ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't reserve id "+ id +". Using next available instead.");
        id = -1;
      }
    }

    List<String> subDecks = DeckParser.parseNestedDecks(contents);
    for (int i=0; i < subDecks.size(); i++) {
      if (i > 0) id = -1;
      if (id == -1) id = ArcanistCCG.getNextUnusedId();

      deckStrings.add(subDecks.get(i));
      attribs.add(new DeckAttribs(id, x+20*i, y+20*i, locked, autoface, facing, toBottom, drawOffset));
    }
  }


  @Override
  public void run() {
    synchronized(pronoun) {
      for (int i=0; i < deckStrings.size(); i++) {
        List<Card> newDeckList = DeckParser.parseDeckList(frame, deckStrings.get(i));
        decklists.add(newDeckList);
      }
    }

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        synchronized(pronoun) {
          for (int i=0; i < decklists.size(); i++) {
            DeckAttribs newAttribs = attribs.get(i);
            Deck newDeck;
            newDeck = new Deck(frame, decklists.get(i), newAttribs.x, newAttribs.y, newAttribs.facing);
            if (newAttribs.id == -1) newDeck.setId(ArcanistCCG.getNextUnusedId());
            else newDeck.setId(newAttribs.id);
            newDeck.reverse();
            newDeck.setLocked(newAttribs.locked);
            newDeck.setAutoFace(newAttribs.autoface);
            newDeck.setToBottom(newAttribs.toBottom);
            newDeck.setOffset(newAttribs.drawOffset);
            newDeck.refresh();
            newDeck.addToTable();
          }
          frame.getTablePane().revalidate();
          frame.getTablePane().repaint();
        }
        if (reportResult == true) {ArcanistCCG.NetManager.deckAddResult(true);}
      }
    });

    keepRunning = false;
  }


  /**
   * A helper to consolidate deck attributes.
   */
  private static class DeckAttribs {
    public int id;
    public int x;
    public int y;
    public boolean locked;
    public boolean autoface;
    public boolean facing;
    public boolean toBottom;
    public char drawOffset;

    public DeckAttribs(int id, int x, int y, boolean locked, boolean autoface, boolean facing, boolean toBottom, char drawOffset) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.locked = locked;
      this.autoface = autoface;
      this.facing = facing;
      this.toBottom = toBottom;
      this.drawOffset = drawOffset;
    }
  }
}
