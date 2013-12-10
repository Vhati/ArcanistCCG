package org.arcanist.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A list of all available games.
 */
public class GameList extends JList {

  private List<GamePrefs> prefsArray = new ArrayList<GamePrefs>();
  private DefaultListModel gameListModel = new DefaultListModel();
  private boolean includesDemo;


  public GameList(boolean showDemo) {
    super();
    this.setModel(gameListModel);
    includesDemo = showDemo;

    if (includesDemo) {
      this.addGame(GamePrefs.createDemoGame());
      this.setSelectedIndex(0);
    }

    loadPrefs();

    this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  }

  /**
   * Get prefs for selected game.
   */
  public GamePrefs getSelectedGame() {
    if (this.getSelectedIndex() == -1) {
      return null;
    }
    return (GamePrefs) prefsArray.get(this.getSelectedIndex());
  }

  /**
   * Remove selected game.
   */
  public void removeSelectedGame() {
    if (this.getSelectedIndex() != -1) {
      prefsArray.remove(this.getSelectedIndex());
      gameListModel.removeElementAt(this.getSelectedIndex());
    }
  }

  /**
   * Add a new game.
   */
  public void addGame(GamePrefs newGame) {
    prefsArray.add(newGame);
    gameListModel.addElement(newGame.name);
  }

  /**
   * Make selected game the current game.
   * @return If successful, true. Otherwise, false.
   */
  public boolean useSelectedGame(ArcanistCCGFrame frame) {
    if (this.getSelectedIndex() == -1) return false;

    GamePrefs selectedGame = prefsArray.get(this.getSelectedIndex());
    if (!new File(selectedGame.gameloc).exists()) {
      JTextArea message = new JTextArea("The game location does not exist.\nYou can download game files here:\n\n"+ ArcanistCCG.WEBSITE);
        message.setEditable(false);
        message.setBorder(BorderFactory.createEtchedBorder());
      JOptionPane.showInternalMessageDialog(frame.getDesktop(), message, "Game files missing", JOptionPane.PLAIN_MESSAGE);
      return false;
    }

    selectedGame.apply(frame);
    return true;
  }

  /**
   * Read the prefs from disk.
   */
  private void loadPrefs() {
    String line = "";

    BufferedReader inFile = null;
    try {
      InputStreamReader fr = new InputStreamReader(new FileInputStream(Prefs.prefsFilePath), "UTF-8");
      inFile = new BufferedReader(fr);

      for (int j=0; true; j++) {
        line = inFile.readLine();
        if (line.length() > 1) {
          GamePrefs newGame = new GamePrefs();
            newGame.name = line;
            newGame.defaultBackPath = inFile.readLine();
            newGame.defaultEmptyDeckPath = inFile.readLine();
            newGame.defaultBlankPath = inFile.readLine();
            newGame.gameloc = inFile.readLine();
            newGame.gamedat = inFile.readLine();
            newGame.gameset = inFile.readLine();

            String temp = inFile.readLine();
            if (temp.toLowerCase().trim().equals("true"))
              newGame.suppressLostCards = true;
            else
              newGame.suppressLostCards = false;

            temp = inFile.readLine();
            if (temp.toLowerCase().trim().equals("true"))
              newGame.suppressLostDats = true;
            else
              newGame.suppressLostDats = false;

            temp = inFile.readLine();
            if (temp.toLowerCase().trim().equals("true"))
              newGame.useTextOnly = true;
            else
              newGame.useTextOnly = false;

            newGame.cardTextOverlayStatsField = -1;
            temp = inFile.readLine();
            if (temp.length() > 0) newGame.cardTextOverlayStatsField = Integer.parseInt(temp);

            int first=0, second=0, third=0, fourth=0, fifth=0;
            temp = inFile.readLine();
            while (temp.length() > 1) {
              String[] tokens = temp.split(",");
              int t = 0;
              first=0; second=0; third=0; fourth=0; fifth=0;
              first = Integer.parseInt(tokens[t++]);
              second = Integer.parseInt(tokens[t++]);
              third = Integer.parseInt(tokens[t++]);
              fourth = Integer.parseInt(tokens[t++]);
              fifth = Integer.parseInt(tokens[t++]);
              newGame.cardTextArray.add(new int[]{first,second,third,fourth,fifth});
              temp = inFile.readLine();
            }
          line = temp;
          this.addGame(newGame);
        }
        else
          break;
      }
    }
    catch (IndexOutOfBoundsException e) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Malformed game settings ("+ Prefs.prefsFilePath +"): "+ line +".");
    }
    catch (FileNotFoundException exception) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't find game settings ("+ Prefs.prefsFilePath +").");
    }
    catch (IOException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't read game settings ("+ Prefs.prefsFilePath +").");
    }
    finally {
      try {if (inFile != null) inFile.close();}
      catch (IOException e) {}
    }
  }

  /**
   * Write prefs to disk.
   */
  public void savePrefs() {
    BufferedWriter outFile = null;
    try {
      OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(Prefs.prefsFilePath), "UTF-8");
      outFile = new BufferedWriter(fw);

      for (int i=(includesDemo?1:0); i <= prefsArray.size()-1; i++) {
        GamePrefs tmpGame = (GamePrefs)prefsArray.get(i);
        outFile.write(tmpGame.name +"\r\n");
        outFile.write(tmpGame.defaultBackPath +"\r\n");
        outFile.write(tmpGame.defaultEmptyDeckPath +"\r\n");
        outFile.write(tmpGame.defaultBlankPath +"\r\n");
        outFile.write(tmpGame.gameloc +"\r\n");
        outFile.write(tmpGame.gamedat +"\r\n");
        outFile.write(tmpGame.gameset +"\r\n");
        outFile.write(Boolean.toString(tmpGame.suppressLostCards) +"\r\n");
        outFile.write(Boolean.toString(tmpGame.suppressLostDats) +"\r\n");
        outFile.write(Boolean.toString(tmpGame.useTextOnly) +"\r\n");

        outFile.write(tmpGame.cardTextOverlayStatsField +"\r\n");
        for (int j=0; j <=tmpGame.cardTextArray.size()-1; j++) {
          outFile.write( Integer.toString(((int[])tmpGame.cardTextArray.get(j))[0]) +",");
          outFile.write( Integer.toString(((int[])tmpGame.cardTextArray.get(j))[1]) +",");
          outFile.write( Integer.toString(((int[])tmpGame.cardTextArray.get(j))[2]) +",");
          outFile.write( Integer.toString(((int[])tmpGame.cardTextArray.get(j))[3]) +",");
          outFile.write( Integer.toString(((int[])tmpGame.cardTextArray.get(j))[4]) );
          outFile.write("\r\n");
        }
        outFile.write("\r\n");
      }
      outFile.write("\r\n");
    }
    catch (FileNotFoundException exception) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't find game settings ("+ Prefs.prefsFilePath +").");
    }
    catch (IOException exception) {
      ArcanistCCG.LogManager.write(exception, "Couldn't save game settings ("+ Prefs.prefsFilePath +").");
    }
    finally {
      try {if (outFile != null) outFile.close();}
      catch (IOException e) {}
    }
  }
}