package org.arcanist.client;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.arcanist.client.*;


/**
 * The global preference settings.
 */
public class Prefs {
  public static final String homePath = "./";
    public static final File fileHomePath = new File(homePath);
  public static final String homePathAb = fileHomePath.getAbsolutePath().substring(0,fileHomePath.getAbsolutePath().length()-1);

  public static final String tokensPath = Prefs.homePath +"images/tokens/";

  public static final String prefsFilePath = Prefs.homePath +"prefs.txt";
  public static final String appearanceFilePath = Prefs.homePath +"appearance.xml";
  public static final String keyboardFilePath = Prefs.homePath +"keyboard.xml";

  /** Light Image to use for the master frame's icon. */
  public static String appIconPositive = Prefs.homePath +"images/AppIcon_Positive.png";

  /** Dark Image to use for the master frame's icon. */
  public static String appIconNegative = Prefs.homePath +"images/AppIcon_Negative.png";


  /** Image file path to use if no card back specified. */
  public static volatile String defaultBackPath = "./cards/Demo/Back_Demo.gif";

  /** Image file path to use for empty decks. */
  public static volatile String defaultEmptyDeckPath = "./images/Empty_Deck.gif";

  /** Image file path to use if no card face found or text-only. */
  public static volatile String defaultBlankPath = "./images/Empty_Card.jpg";

  /** Image file path to use if no card face and no text found. */
  public static volatile String defaultErrorPath = "./images/Error_Card.jpg";//This one's hardcoded

  /** Path to game folder. */
  public static volatile String gameloc = "./cards/Demo";

  /** Path to text data file. */
  public static volatile String gamedat = "./cards/Demo/cardinfo.dat";

  /** Path to expansion abbreviation file. */
  public static volatile String gameset = "./cards/Demo/Expan.dat";

  /** Disable alert on missing images when loading decks. */
  public static volatile boolean suppressLostCards = false;

  /** Disable alert on missing text data when loading decks. */
  public static volatile boolean suppressLostDats = false;

  /** Don't try to load images. */
  public static volatile boolean useTextOnly = false;

  /** Overlay text on card images. */
  public static volatile boolean textOnImages = false;

  /** Holder for text JumboView positions. */
  public static List<int[]> cardTextArray = new ArrayList<int[]>(0);
  public static int cardTextOverlayStatsField = -1;


  /**
   * Reference to text data file parser.
   * To replace this variable, synchronize
   * on Prefs.class, test for null, and if
   * not, synchronize on the previous dat,
   * then set the new object.
   */
  public static DatParser textDat = null;

  /** Reference to image cache. */
  public static final Cache Cache = new Cache();


  /** Master setting for card width. */
  public static final int defaultCardWidth = 78;

  /** Master setting for card height. */
  public static final int defaultCardHeight = 111;

  /** Horizontal offset of a new deck's drawn cards. */
  public static final int defaultNewCardOffsetH = 0;

  /** Vertical offset of a new deck's drawn cards. */
  public static final int defaultNewCardOffsetV = 121;

  /** Number of shuffles that take place pre shuffle command. */
  public static final int shuffles = 1;

  /** Show all incoming network traffic. */
  public static final boolean verboseChat = false;


  /** Border highlight color of selected cards. */
  public static volatile Color highlightColor = Color.blue;

  /** Multiselect dashed box color. */
  public static volatile Color dragColor = Color.red;

  /** Path to the table's background image. */
  public static volatile String tableBgImage = "";
  public static volatile boolean tableBgTiled = true;
  public static volatile Color mainBgColor = null;
  public static volatile Color tableBgColor = null;
  // Stash user-desired alias here for appearance saving (w/o server relaying)
  public static volatile String savedAlias = "";

  /** The local player's current name. */
  public static volatile String playerAlias = "";

  /** Whether timestamps should appear in the chat. */
  public static volatile boolean chatTimestamps = false;


  public static final Integer glassLayer = new Integer(50);
  public static final Integer ghostLayer = new Integer(10);
  public static final Integer groupLayer = new Integer(7);
  public static final Integer handLayer = new Integer(6);
  public static final Integer noteLayer = new Integer(5);
  public static final Integer tokenLayer = new Integer(4);
  public static final Integer cardLayer = new Integer(3);
  public static final Integer deckLayer = new Integer(2);


  // Vars no one will ever use
  /** Sets whether to use minimal frame decoration.
   *  This will affect all new Chat, Counter, DieRoller, Timer, and Tokens windows.
   *  The effect is nice but a bit too minimal, and limited to Metal L&F.
   *  This will always be false unless the code is modified.
   *  If set to true, old saved layouts will make windows appear too large.
   */
  public static final boolean usePaletteFrames = false;


  // Hotkeys
  public static final String ACTION_TABLE_FOCUS_CHAT = "table_focus_chat";
  public static final String ACTION_CARD_ROTATE_LEFT = "card_rotate_left";
  public static final String ACTION_CARD_ROTATE_RIGHT = "card_rotate_right";
  public static final String ACTION_CARD_REPO_UP = "card_repo_up";
  public static final String ACTION_CARD_REPO_DOWN = "card_repo_down";
  public static final String ACTION_CARD_FLIP_REMOTE = "card_flip_remote";
  public static final String ACTION_CHAT_COUNTER_QUERY = "chat_counter_query";
  public static final String ACTION_CHAT_MACRO1 = "chat_macro_one";
  public static final String ACTION_CHAT_MACRO2 = "chat_macro_two";
  public static final String ACTION_CHAT_MACRO3 = "chat_macro_three";
  public static final String ACTION_CHAT_MACRO4 = "chat_macro_four";

  /** Synchronize on this before accessing hotkeyStrokeMap or hotkeyArgMap. */
  public static final Object hotkeyLock = new Object();

  /** Maps hotkey action strings to keystrokes. */
  public static final Map<String,KeyStroke> hotkeyStrokeMap = new HashMap<String,KeyStroke>();

  /** Maps hotkey action strings to optional args. */
  public static final Map<String,String> hotkeyArgMap = new HashMap<String,String>();
  static {
    hotkeyStrokeMap.put(ACTION_TABLE_FOCUS_CHAT, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true));
    hotkeyStrokeMap.put(ACTION_CARD_ROTATE_LEFT, KeyStroke.getKeyStroke(KeyEvent.VK_O, 0, true));
    hotkeyStrokeMap.put(ACTION_CARD_ROTATE_RIGHT, KeyStroke.getKeyStroke(KeyEvent.VK_P, 0, true));
    hotkeyStrokeMap.put(ACTION_CARD_REPO_UP, KeyStroke.getKeyStroke(KeyEvent.VK_SEMICOLON, 0, true));
    hotkeyStrokeMap.put(ACTION_CARD_REPO_DOWN, KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));
    hotkeyStrokeMap.put(ACTION_CARD_FLIP_REMOTE, KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, true));
    hotkeyStrokeMap.put(ACTION_CHAT_COUNTER_QUERY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0, true));

    hotkeyStrokeMap.put(ACTION_CHAT_MACRO1, null); hotkeyArgMap.put(ACTION_CHAT_MACRO1, null);
    hotkeyStrokeMap.put(ACTION_CHAT_MACRO2, null); hotkeyArgMap.put(ACTION_CHAT_MACRO2, null);
    hotkeyStrokeMap.put(ACTION_CHAT_MACRO3, null); hotkeyArgMap.put(ACTION_CHAT_MACRO3, null);
    hotkeyStrokeMap.put(ACTION_CHAT_MACRO4, null); hotkeyArgMap.put(ACTION_CHAT_MACRO4, null);
  }


  /**
   * Set the local player's alias.
   * It can be upto 20 characters including "a-zA-z0-9 ,-_'.".
   *
   * @param s proposed alias (null for none)
   * @throws IllegalArgumentException if the alias is invalid
   */
  public static void setPlayerAlias(String s) throws IllegalArgumentException {
    if (s == null) {playerAlias = ""; return;}

    if (s.length() > 20)
      throw new IllegalArgumentException("Alias over 20 chars");
    if (!s.matches("^[a-zA-Z0-9 ,-_'.]+$"))
      throw new IllegalArgumentException("Alias contains illegal chars");
    playerAlias = s;
  }

  /**
   * Text if a string could be a player's alias.
   * This applies the same checks as setPlayerAlias(),
   * but a boolean is returned rather than throwing exceptions.
   *
   * @param s proposed alias
   * @return true if valid, false otherwise
   */
  public static boolean isValidPlayerAlias(String s) {
    if (s.length() > 20) return false;
    if (!s.matches("^[a-zA-Z0-9 ,-_'.]+$")) return false;
    return true;
  }
}