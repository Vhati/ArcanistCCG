package org.arcanist.client;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.arcanist.client.*;
import org.arcanist.util.*;


public class ArcanistDatFormat implements DatFormat {

  private DatParseProgressFrame parseStatusComponent = null;
  private int parseStatusUpdateInterval = -1;


  public ArcanistDatFormat() {}


  /**
   * Returns the name of this format.
   */
  @Override
  public String getName() {return "ArcanistCCG";}


  /**
   * Returns true if this format can be written.
   */
  @Override
  public boolean isWritable() {return true;}


  /**
   * Returns true if a file is of this format.
   *
   * @param f possible dat file, or null for none
   */
  @Override
  public boolean isParsable(File f) {
    if (f == null || !f.isFile()) return false;
    boolean result = false;

    //Look for "Name"...
    BufferedReader inFile = null;
    try {
      InputStreamReader fr = new InputStreamReader(new FileInputStream(f), "UTF-8");
      inFile = new BufferedReader(fr);
      if (inFile.read() == 'N' && inFile.read() == 'a' && inFile.read() == 'm' && inFile.read() == 'e') {
        result = true;
      }
    }
    catch (FileNotFoundException e) {/* Ignore */}
    catch (IOException e) {/* Ignore */}
    finally {
      try {if (inFile != null) inFile.close();}
      catch (IOException e) {}
    }

    return result;
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
    parseStatusUpdateInterval = n;
    if (parseStatusComponent == null) parseStatusUpdateInterval = -1;
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
    Exception exception = null;

    if (parseStatusComponent != null) {
      final DatParseProgressFrame tmpComp = parseStatusComponent;
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          tmpComp.parsingStarted();
        }
      });
    }

    String tmpLine = "";
    BufferedReader inFile = null;
    try {
      InputStreamReader fr = new InputStreamReader(new FileInputStream(f), "UTF-8");
      inFile = new BufferedReader(fr);
      tmpLine = inFile.readLine();
      while (tmpLine != null && tmpLine.equals("") == false) {
        fieldList.add(tmpLine);
        tmpLine = inFile.readLine();
      }
      int fieldCount = fieldList.size();


      tmpLine = inFile.readLine();
      for (int i=fieldCount+2; tmpLine != null; i++) {
        // Skip blanks between cards
        if (i % (fieldCount+1) == 0) {
          tmpLine = inFile.readLine();
          continue;
        }

        spoilerList.add(tmpLine);

        if (parseStatusComponent != null && i % (fieldCount+1) == 1) {
          final String tmpName = tmpLine;
          final DatParseProgressFrame tmpComp = parseStatusComponent;
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              tmpComp.setCurrentCard(tmpName);
            }
          });
        }

        tmpLine = inFile.readLine();
      }
    }
    catch (FileNotFoundException e) {
      exception = e;
    }
    catch (IOException e) {
      exception = e;
    }
    finally {
      try {if (inFile != null) inFile.close();}
      catch (IOException e) {}
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
    int fieldCount = fieldList.size();
    BufferedWriter outFile = null;
    String tmpLine = "";

    try {
      OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
      outFile = new BufferedWriter(fw);

      for (int i=0; i < fieldCount; i++) {
        tmpLine = ((String)fieldList.get(i));
        outFile.write(tmpLine);
        outFile.write("\r\n");
      }
      outFile.write("\r\n");

      for (int i=0; i < spoilerList.size(); i++) {
        if ((i==0 || (i+1)%fieldCount == 1) && ((String)spoilerList.get(i)).length()==0) {
          i += fieldCount-1;                                 //Skip empty cards, counter the loop's inc
          continue;
        }
        tmpLine = (String)spoilerList.get(i);
        tmpLine = tmpLine.replaceAll("\\r", "");             //Moved from loader
        tmpLine = tmpLine.replaceAll("\\n", "\\\\n");        //for faster loading
        outFile.write(tmpLine);
        outFile.write("\r\n");

        if ((i+1)%fieldCount == 0 && i != spoilerList.size()-1) {
          outFile.write("\r\n");
        }
      }
    }
    catch (IOException e) {
      ArcanistCCG.LogManager.write(e, "Couldn't save CardText file.");
    }
    finally {
      try {if (outFile != null) outFile.close();}
      catch (IOException e) {}
    }
  }
}
