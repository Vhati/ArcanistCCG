package org.arcanist.client;


/**
 * Search query.
 * This is a really basic grammar for searching.
 * Keywords are separated by spaces.
 * Phrases are quoted.
 * Exclusions have minus before them.
 * Searching for quotes doesn't work.
 */
public class SearchQuery {

  String lowerField;
  String[] keywords;
  boolean[] keytypes;
  boolean allfalse = true;

  public SearchQuery(String originalField) {
    if (originalField.length() == 0) return;

    lowerField = originalField.toLowerCase();
    keywords = lowerField.split(" (?=([^\"]*\"[^\"]*\")*(?![^\"]*\"))");        //It's really:  (?=([^"]*"[^"]*")*(?![^"]*"))
    if (keywords.length == 0) return;

    keytypes = new boolean[keywords.length];
    for (int i=0; i < keywords.length; i++) {
      if (keywords[i].startsWith("-\"") && keywords[i].length() > 3) {
        keywords[i] = keywords[i].substring(2,keywords[i].length()-1);
        keytypes[i] = false;
      }
      else if (keywords[i].startsWith("-") && keywords[i].length() > 1) {
        keywords[i] = keywords[i].substring(1,keywords[i].length());
        keytypes[i] = false;
      }

      else if (keywords[i].startsWith("\"") && keywords[i].length() > 2) {
        keywords[i] = keywords[i].substring(1,keywords[i].length()-1);
        keytypes[i] = true;
        allfalse = false;
      }
      else {
        keytypes[i] = true;
        allfalse = false;
      }
    }
  }

  public boolean matches(String line) {
    if (keywords == null) return false;
    if (line.length() == 0) {
      if (allfalse == true) return true;
      else return false;
    }

    line = line.toLowerCase();
    for (int i=0; i < keywords.length; i++) {
      if (keywords[i].length()==0)
        continue;
      if ( keytypes[i] != (line.indexOf(keywords[i]) > -1) )
        return false;
    }
    return true;
  }

}
