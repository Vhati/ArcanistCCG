package org.arcanist.util;

import java.io.File;
import java.io.FileFilter;


/**
 * This filter is used to show only directories in dialogs.
 */
public class DirFileFilter implements FileFilter {

  public DirFileFilter() {
  }


  @Override
  public boolean accept(File f) {
    return f.isDirectory();
  }
}