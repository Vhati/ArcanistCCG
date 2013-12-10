package org.arcanist.client;

import java.io.File;
import java.io.FilenameFilter;


/**
 * This filter is used when searching for image files related to a card.
 * It chops off the extension (and .full if it's there).
 * Then it compares trailing numbers.
 * Then it chops off trailing numbers, and fails if it still won't match.
 */
public class CardFilenameFilter implements FilenameFilter {

  private static String[] exts = new String[] {".jpg",".JPG",".gif",".GIF",".png",".PNG",".bmp",".BMP"};

  private String filterName = "";


  public CardFilenameFilter(String string) {setFilterName(string);}


  public void setFilterName(String string) {filterName = string;}


  @Override
  public boolean accept(File f, String fileName) {
    if (fileName.length() < 4) return false;

    boolean trimmedExtension = false;
    for (int i=0; i < exts.length; i++) {
      if (fileName.endsWith(exts[i])) {
        fileName = fileName.substring(0, fileName.length()-exts[i].length());
        trimmedExtension = true;
        break;
      }
    }
    if (trimmedExtension == false) return false;

    if (fileName.endsWith(".full"))                           //Deal with suitcase pics
      fileName = fileName.substring(0, fileName.length()-5);

    if (filterName.equalsIgnoreCase(fileName) == true)
      return true;

    // Trim off a numeric suffix
    if (Character.isDigit((fileName.charAt(fileName.length()-1))))
      while (fileName.length() > 0 && Character.isDigit((fileName.charAt(fileName.length()-1))))
        fileName = fileName.substring(0, fileName.length()-1);
    if (fileName.endsWith(" "))
      fileName = fileName.substring(0, fileName.length()-1);

    if (filterName.equalsIgnoreCase(fileName) == true)
      return true;
    else
      return false;
  }


  public static String trimExtension(String s) {
    boolean trimmedExtension = false;
    for (int i=0; i < exts.length; i++) {
      if (s.endsWith(exts[i])) {
        s = s.substring(0, s.length()-exts[i].length());
        trimmedExtension = true;
        break;
      }
    }
    if (trimmedExtension == false) return null;

    if (s.endsWith(".full"))                           //Deal with suitcase pics
      s = s.substring(0, s.length()-5);

    return s;
  }


  public static String trimNumericSuffix(String s) {
    if (!Character.isDigit(s.charAt(s.length()-1))) return null;

    StringBuffer buf = new StringBuffer(s);
    while (Character.isDigit(buf.charAt(buf.length()-1))) {
      buf.deleteCharAt(buf.length()-1);
    }
    if (buf.length() > 0 && buf.charAt(buf.length()-1) == ' ') {
      buf.deleteCharAt(buf.length()-1);
    }

    return buf.toString();
  }
}
