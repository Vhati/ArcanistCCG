package org.arcanist.util;

import javax.swing.filechooser.FileFilter;
import java.io.*;


/**
 * A JFileChooser filter for arbitrary extensions.
 * If any of the suffixes appear at the end of a file, it is shown.
 * Existing directories are always shown.
 */
public class ExtensionFileFilter extends FileFilter {

  private String desc = null;
  private String[] exts = null;


  /**
   * Creates an ExtensionFileFilter.
   *
   * @param description the description of this filter
   * @param suffixes an array of extensions to check, or null for all files
   */
  public ExtensionFileFilter(String description, String[] suffixes) {
    super();
    desc = description;
    exts = suffixes;
  }


  @Override
  public boolean accept(File file) {
    if (file.exists()) {
      if (file.isDirectory()) return true;
      if (!file.isFile()) return false;
    }
    if (exts == null) return true;

    String filename = file.getName();
    for (int i=0; i < exts.length; i++) {
      if (filename.endsWith(exts[i])) return true;
    }
    return false;
  }


  /**
   * Returns the description of this filter.
   */
  @Override
  public String getDescription() {
    return desc;
  }

  /**
   * Returns the first suffix this filter checks.
   */
  public String getPrimarySuffix() {
    if (exts != null && exts.length > 0) return exts[0];
    else return null;
  }
}