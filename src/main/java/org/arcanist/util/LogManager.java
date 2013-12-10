package org.arcanist.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;


/**
 * This forwards messages from objects to the console window and log file.
 */
public class LogManager {

  public static int INFO_LEVEL = 0;
  public static int ERROR_LEVEL = 1;

  String logfile = "log.txt";


  public LogManager() {
    try {
      new File(logfile).delete();
    }
    catch (Exception e) {}
  }


  /**
   * Forward a message.
   * @param severity 0-info, 1-error
   * @param message explanation of error
   */
  public void write(int severity, String message) {
    if (message == null) message = "";
    String[] chunks = message.split("\n");
    String tmp = "";

    if (severity == INFO_LEVEL) tmp = "Info:  "+ chunks[0];
    else if (severity == ERROR_LEVEL) tmp = "Error: "+ chunks[0];
    else tmp = severity +"?:    "+ chunks[0];
    System.out.println(tmp);
    appendToFile(tmp);

    for (int i=1; i < chunks.length; i++) {
      tmp = "       "+ chunks[i];
      System.out.println(tmp);
      appendToFile(tmp);
    }
  }

  /**
   * Writes a stack trace for an error.
   * @param e error to log
   * @param message explanation of error
   */
  public void write(Throwable e, String message) {
    StackTraceElement[] tmpStack = e.getStackTrace();
    if (message == null) message = "";
    String result = (message.length()>0?"Error: ":"") + message;
    result += (result.length()>0?"\n":"") +"Problem: "+ e.toString();

    for (int i=0; i < tmpStack.length; i++) {
      result += (result.length()>0?"\n":"")+ tmpStack[i].toString();
    }

    Throwable cause = e.getCause();
    while (cause != null) {
      result += "\n\nCause: "+ cause.toString();

      tmpStack = cause.getStackTrace();
      for (int i=0; i < tmpStack.length; i++) {
        result += (result.length()>0?"\n":"")+ tmpStack[i].toString();
      }

      cause = cause.getCause();
    }

    write(ERROR_LEVEL, result);
  }


  private void appendToFile(String tmp) {
    try {
      FileWriter fw = new FileWriter(logfile, true);
      BufferedWriter outFile = new BufferedWriter(fw);
      outFile.write(tmp +"\r\n");
      outFile.close();
    }
    catch (FileNotFoundException exception) {}
    catch (IOException exception) {}
  }
}