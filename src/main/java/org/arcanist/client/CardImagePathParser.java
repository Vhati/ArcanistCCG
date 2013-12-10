package org.arcanist.client;

import java.util.List;
import java.util.regex.Pattern;


public class CardImagePathParser {


  public CardImagePathParser() {
  }


  /**
   * Find actual paths to proposed a card
   *
   * @param setName full set name
   * @param frontFile explicit front filename
   * @param backFile explicit back filename
   */
  public static String[] getPaths(String setName, String frontName, String frontFile, String backName, String backFile) {
    if (setName == null) setName = "";
    if (frontName == null) frontName = "";
    if (frontFile == null) frontFile = "";
    if (backName == null) backName = "";
    if (backFile == null) backFile = "";
    String path = null;
    String pathBack = null;

    if (setName.length() == 0) {
      setName = Prefs.textDat.availableImages(frontName);
    }

    if (setName.length() > 0) {
      if (frontFile.length() > 0) {
        if (Prefs.textDat.imageExists(setName, frontFile) == false) {
          frontFile = "";
        }
      }
      if (frontName.length() > 0 && frontFile.length() == 0) {
        List<String> picList = Prefs.textDat.availableImages(setName, frontName);
        if (picList.size() > 0) {
          frontFile = picList.get(0);
        }
      }
      if (frontFile.length() > 0) {
        path = Prefs.gameloc +"/"+ setName +"/"+ frontFile;
      }
    }

    if (setName.length() > 0) {
      if (backFile.length() > 0) {
        if (Prefs.textDat.imageExists(setName, backFile) == false) {
          backFile = "";
        }
      }
      if (backName.length() > 0 && backFile.length() == 0) {
        List<String> picList = Prefs.textDat.availableImages(setName, backName);
        if (picList.size() > 0) {
          backFile = picList.get(0);
        }
      }
      if (backFile.length() > 0) {
        pathBack = Prefs.gameloc +"/"+ setName +"/"+ backFile;
      }
    }

    return new String[] {setName, path, pathBack};
  }

}
