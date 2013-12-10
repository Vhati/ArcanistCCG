package org.arcanist.client;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.arcanist.client.*;
import org.arcanist.util.*;
import org.arcanist.server.*;


/**
 * The main ArcanistCCG class.
 */
public class ArcanistCCG {
  public static final String VERSION = "4.5";
  public static final String WEBSITE = "http://vhati.zxq.net/ArcanistCCG/";

  private static ArcanistCCGFrame frame = null;

  private static ServerThread ServerThread = null;
  public static final Object serverThreadLock = new Object();

  /** Forwards network traffic and appends to chat. */
  public static volatile NetMsgMgr NetManager = null;

  /** Fills log file and appends to console. */
  public static final LogManager LogManager = new LogManager();

  /** Tracks used id numbers. */
  private static Object idLock = new Object();
  private static TreeSet<Integer> usedIdList = new TreeSet<Integer>();


  /** Tracks whether adjustGui has been called */
  private static boolean adjustedGui = false;
  private static final Object adjustGuiLock = new Object();

  /** Reacts to the main window maximizing and calls adjustGUI(true) */
  private static ComponentListener maxListener = new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
      frame.removeComponentListener(this);
      synchronized(adjustGuiLock) {
        if (!adjustedGui) {
          adjustedGui = true;
          adjustGui(true);
        }
      }
    }
  };

  /** Waits 3 seconds and, if necessary, calls adjustGui(false) */
  private static Thread delayedGuiAdjuster = new Thread() {
    public void run() {
      try {Thread.sleep(3000);}
      catch (InterruptedException e) {LogManager.write(e, "The delayed GUI adjuster thread was interrupted while sleeping.");}

      synchronized(adjustGuiLock) {
        if (!adjustedGui) {
          adjustedGui = true;

          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              adjustGui(false);
            }
          });
        }
      }
    }
  };


  public static void main(String[] args) {
    //Test for terminal users...
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("This application is not ascii friendly. You need a GUI.");
      System.exit(1);
    }

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        guiInit();
      }
    });
  }

  private static void guiInit() {
    /* A snippet to remind me about reflection...
    //This method is private until Java 1.5
    System.out.println(javax.swing.plaf.metal.MetalLookAndFeel.getCurrentTheme())
    java.lang.reflect.Method[] metalStuff = javax.swing.plaf.metal.MetalLookAndFeel.class.getMethods();
    for (int i=0; i < metalStuff.length; i++) {
      if (metalStuff[i].getName().equals("setCurrentTheme")) {
        //This line is superfluous since it's gotta be public to show up
        System.out.println(java.lang.reflect.Modifier.isPublic(metalStuff[i].getModifiers()));
      }
    }
    */

    //Removes icons from InternalFrames
    //UIManager.getDefaults().put("InternalFrame.icon", "");

    //I hate 1.5's Ocean theme...
    try {
      //Change Theme to Metal
      javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new javax.swing.plaf.metal.DefaultMetalTheme());
      UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
      //Update each existing component
      //SwingUtilities.updateComponentTreeUI(...);
    }
    catch (Exception e) {
      LogManager.write(LogManager.ERROR_LEVEL, e.toString());
    }


    GamePrefs demoGame = GamePrefs.createDemoGame();
      demoGame.apply(null);

    if (new File(Prefs.appIconPositive).exists() == false) Prefs.appIconPositive = null;
    if (new File(Prefs.appIconNegative).exists() == false) Prefs.appIconNegative = null;

    frame = new ArcanistCCGFrame("ArcanistCCG  "+ VERSION);
    NetManager = new NetMsgMgr(frame);


    // Jump through hoops to maximize
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        frame.pack();
        frame.setSize(400, 400);
        frame.setVisible(true);

        if (Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
          frame.addComponentListener(maxListener);

          // Gotta 'realize' via setvisible, show, or pack before maximizing
          frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
        }
        else {
          synchronized(adjustGuiLock) {
            if (!adjustedGui) {
              adjustedGui = true;
              adjustGui(false);
            }
          }
        }
      }
    });

    delayedGuiAdjuster.setPriority(Thread.NORM_PRIORITY);
    delayedGuiAdjuster.setDaemon(true);
    delayedGuiAdjuster.start();
  }


  private static void adjustGui(boolean maximized) {
    //System.out.println(frame.getWidth());

    if (!frame.loadAppearance()) {
      // No user-defined table window layout, use some defaults
      if (maximized) {
        // Shrink te table and create a JumboFrame
        frame.getTableFrame().reshape(0, 0, frame.getDesktop().getSize().width-325, frame.getDesktop().getSize().height);
        if (frame.getJumboFrame() == null) {frame.createJumboFrame();}
      }
      else {
        // No room for a JumboFrame, enlarge the table
        frame.getTableFrame().reshape(0, 0, frame.getDesktop().getSize().width, frame.getDesktop().getSize().height);
      }
    }
    frame.centerTableView();
    new GamePromptWindow(frame);
  }


  /**
   * Sets the ServerThread.
   * Any previous instance will be killed.
   *
   * @param st the new thread, or null to remove
   */
  public static void setServerThread(ServerThread st) {
    synchronized (serverThreadLock) {
      ServerThread oldThread = ServerThread;
      ServerThread = st;
      if (oldThread != null) {
        oldThread.killThread();
      }
    }
  }

  /**
   * Gets the ServerThread.
   */
  public static ServerThread getServerThread() {
    synchronized (serverThreadLock) {
      return ServerThread;
    }
  }


  /**
   * Replaces the global list of used ids.
   * This method must not be called if any ids are in use.
   * So clear the table first!
   *
   * @param newIds a list of Integers, or null for none
   * @see ArcanistCCGFrame#clearTable()
   */
  public static void setUsedIds(Collection<Integer> newIds) {
    synchronized (idLock) {
      usedIdList.clear();
      if (newIds != null) usedIdList.addAll(newIds);
    }
  }

  /**
   * Marks an id as used.
   *
   * @return true if successful, false if already used
   */
  public static boolean reserveId(int id) {
    synchronized (idLock) {
      return usedIdList.add(new Integer(id));
    }
  }

  /**
   * Returns the next available id.
   */
  public static int getNextUnusedId() {
    int result = 0;
    synchronized (idLock) {
      int idListSize = usedIdList.size();
      if (idListSize > 0) {
        if (usedIdList.last().intValue() == idListSize-1) {
          result = idListSize;
        } else {
          Iterator<Integer> it = usedIdList.iterator();
          int prevId = -1;
          while (it.hasNext()) {
            int curId = it.next().intValue();
            if (curId - prevId > 1) break;
            prevId = curId;
          }
          result = prevId + 1;
        }
      }
      usedIdList.add(new Integer(result));
    }
    return result;
  }
}
