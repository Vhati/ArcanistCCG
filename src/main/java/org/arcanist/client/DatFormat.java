package org.arcanist.client;

import java.io.File;
import java.util.List;

import org.arcanist.client.*;
import org.arcanist.util.*;


public interface DatFormat {

  /**
   * Returns the name of this format.
   */
  public String getName();


  /**
   * Returns true if this format can be written.
   */
  public boolean isWritable();


  /**
   * Returns true if a file is of this format.
   *
   * @param f possible dat file, or null for none
   */
  public boolean isParsable(File f);


  /**
   * Sets a component to indicate parsing progress.
   * The parse() method will reset this to null after it completes.
   *
   * @param c a component to monitor progress
   * @param n update the field after every nth card
   */
  public void setProgressStatusComponent(DatParseProgressFrame c, int n);

  /**
   * Parses a dat.
   * Names will need to be sanitized separately.
   *
   * @param f a file to read
   * @param fieldList an ArrayList to be filled with Strings corresponding to each field name
   * @param spoilerList an ArrayList to be filled with Strings corresponding to each field per card
   */
  public void parse(File f, List<String> fieldList, List<String> spoilerList) throws Exception;


  /**
   * Returns an array of SearchFieldPanels.
   */
  public SearchFieldPanel[] getSearchFieldPanels(List<String> fieldList);


  /**
   * Write card info back to a file.
   *
   * @param f file to write
   * @param fieldList an ArrayList to be filled with Strings corresponding to each field name
   * @param spoilerList an ArrayList of Strings corresponding to each field per card
   */
  public void save(File f, List<String> fieldList, List<String> spoilerList);
}
