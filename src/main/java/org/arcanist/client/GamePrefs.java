package org.arcanist.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JInternalFrame;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * Holds per-game settings.
 * This includes everything chosen in the PathsPrefs dialog.
 */
public class GamePrefs {

  /** Game name. */
  public String name;

  /** Image file path to use if no card back specified. */
  public String defaultBackPath;

  /** Image file path to use for empty decks. */
  public String defaultEmptyDeckPath;

  /** Image file path to use if no card face found or text-only. */
  public String defaultBlankPath;


  /** Path to game folder. */
  public String gameloc;

  /** Path to text data file. */
  public String gamedat;

  /** Path to expansion abbreviation file. */
  public String gameset;


  /** Disable alert on missing images when loading decks. */
  public boolean suppressLostCards;

  /** Disable alert on missing text data when loading decks. */
  public boolean suppressLostDats;

  /** Don't try to load images. */
  public boolean useTextOnly;


  /** Holder for text JumboView positions */
  public List<int[]> cardTextArray = new ArrayList<int[]>(0);
  public int cardTextOverlayStatsField = -1;


  public GamePrefs() {
  }


  /**
   * Applies this game to the global prefs.
   *
   * @param frame an existing GUI, or null.
   */
  public void apply(ArcanistCCGFrame frame) {
    Prefs.defaultBackPath = this.defaultBackPath;
    Prefs.defaultEmptyDeckPath = this.defaultEmptyDeckPath;
    Prefs.defaultBlankPath = this.defaultBlankPath;
    Prefs.gameloc = this.gameloc;
    Prefs.gamedat = this.gamedat;
    Prefs.gameset = this.gameset;
    Prefs.suppressLostCards = this.suppressLostCards;
    Prefs.suppressLostDats = this.suppressLostDats;
    Prefs.useTextOnly = this.useTextOnly;
    Prefs.cardTextOverlayStatsField = this.cardTextOverlayStatsField;
    Prefs.cardTextArray = this.cardTextArray;

    DatParser newDat = new DatParser();
      newDat.setup(frame, Prefs.gameloc, Prefs.gamedat, Prefs.gameset);
    synchronized (Prefs.class) {
      if (Prefs.textDat == null) Prefs.textDat = newDat;
      else {
        synchronized (Prefs.textDat) {
          Prefs.textDat = newDat;
        }
      }
    }
    Prefs.Cache.setup();

    if (new File(Prefs.defaultBackPath).exists() == false)
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't find default card back image.");
    if (new File(Prefs.defaultEmptyDeckPath).exists() == false)
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't find empty deck image.");
    if (new File(Prefs.defaultBlankPath).exists() == false)
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't find blank card image.");

    if (frame != null) {
      JInternalFrame[] frames = frame.getDesktop().getAllFrames();
      for (int i=0; i < frames.length; i++) {
        if (frames[i] instanceof DeckBuilder) ((DeckBuilder)frames[i]).datChanged();
        else if (frames[i] instanceof DeckSearchWindow) ((DeckSearchWindow)frames[i]).datChanged();
      }
    }
  }


  /**
   * Creates prefs for the Demo game.
   */
  public static GamePrefs createDemoGame() {
    GamePrefs newGame = new GamePrefs();
      newGame.name = "Demo";
      newGame.defaultBackPath = "./cards/Demo/Back_Demo.gif";
      newGame.defaultEmptyDeckPath = "./images/Empty_Deck.gif";
      newGame.defaultBlankPath = "./images/Empty_Card.jpg";
      newGame.gameloc = "./cards/Demo";
      newGame.gamedat = "./cards/Demo/cardinfo.dat";
      newGame.gameset = "./cards/Demo/Expan.dat";
      newGame.suppressLostCards = false;
      newGame.suppressLostDats = false;
      newGame.useTextOnly = false;
      newGame.cardTextOverlayStatsField = 5;
      newGame.cardTextArray.add(new int[]{1,11,5,215,0});
      newGame.cardTextArray.add(new int[]{0,288,420,28,0});
      newGame.cardTextArray.add(new int[]{1,11,420,215,0});
      newGame.cardTextArray.add(new int[]{1,21,216,195,0});
      newGame.cardTextArray.add(new int[]{1,238,5,80,0});
      newGame.cardTextArray.add(new int[]{1,228,216,90,0});
      newGame.cardTextArray.add(new int[]{2,24,245,275,171});
      newGame.cardTextArray.add(new int[]{2,24,36,275,171});
    return newGame;
  }
}
