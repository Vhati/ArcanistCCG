package org.arcanist.client;

import java.awt.EventQueue;
import java.util.List;
import java.io.File;

import org.arcanist.client.*;
import org.arcanist.util.*;


public class ImageDatFormat implements DatFormat {

  private DatParseProgressFrame parseStatusComponent = null;


  public ImageDatFormat() {}


  /**
   * Returns the name of this format.
   */
  @Override
  public String getName() {return "Images";}


  /**
   * Returns true if this format can be written.
   */
  @Override
  public boolean isWritable() {return false;}


  /**
   * Returns true if a file is of this format.
   *
   * @param f possible dat file, or null for none
   */
  @Override
  public boolean isParsable(File f) {
    if (f == null || !f.isDirectory()) return false;
    return true;
  }


  /**
   * Sets a component to indicate parsing progress.
   * The parse() method will reset this to null after it completes.
   *
   * @param c a component to monitor progress
   * @param n update the field after every nth card
   */
  @Override
  public void setProgressStatusComponent(DatParseProgressFrame c, int n) {
    parseStatusComponent = c;
  }


  /**
   * Parses a dat.
   * Names will need to be sanitized separately.
   *
   * @param f a file to read
   * @param fieldList an ArrayList to be filled with Strings corresponding to each field name
   * @param spoilerList an ArrayList to be filled with Strings corresponding to each field per card
   */
  @Override
  public void parse(File f, List<String> fieldList, List<String> spoilerList) throws Exception {
    File gameDir = f;
    Exception exception = null;

    if (parseStatusComponent != null) {
      final DatParseProgressFrame tmpComp = parseStatusComponent;
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          tmpComp.parsingStarted();
        }
      });
    }

    fieldList.add("Name");
    fieldList.add("Set");

    File[] setDirs = gameDir.listFiles(new DirFileFilter());
    for (int i=0; i < setDirs.length; i++) {
      String setName = setDirs[i].getName();
      File[] setCards = setDirs[i].listFiles();
      for (int j=0; j < setCards.length; j++) {
        String cardName = setCards[j].getName();
        cardName = CardFilenameFilter.trimExtension(cardName);

        spoilerList.add(cardName);
        spoilerList.add(setName);
      }
    }

    if (exception != null) throw exception;


    if (parseStatusComponent != null) {
      final DatParseProgressFrame tmpComp = parseStatusComponent;
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          tmpComp.parsingCompleted();
        }
      });
    }
    setProgressStatusComponent(null, -1);
  }


  /**
   * Returns an array of SearchFieldPanels.
   */
  @Override
  public SearchFieldPanel[] getSearchFieldPanels(List<String> fieldList) {
    SearchFieldPanel[] result = new SearchFieldPanel[fieldList.size()];

    int cols = 0;
    for (int i=0; i < fieldList.size(); i++) {
      String name = (String)fieldList.get(i);
      int n = 0;
      if (name.equals("Set")) {
        n = ExpanSearchFieldPanel.MIN_COLUMNS;
      } else {
        n = TextSearchFieldPanel.MIN_COLUMNS;
      }
      if (n > cols) cols = n;
    }

    for (int i=0; i < fieldList.size(); i++) {
      String name = (String)fieldList.get(i);
      if (name.equals("Set")) {
        result[i] = new ExpanSearchFieldPanel(name, cols);
      } else {
        result[i] = new TextSearchFieldPanel(name, cols);
      }
    }

    return result;
  }


  /**
   * Write card info back to a file.
   *
   * @param f file to write
   * @param fieldList an ArrayList to be filled with Strings corresponding to each field name
   * @param spoilerList an ArrayList of Strings corresponding to each field per card
   */
  @Override
  public void save(File f, List<String> fieldList, List<String> spoilerList) {
  }
}
