package org.arcanist.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * Parses various text data files into memory.
 */
public class DatParser {

  public static final int TYPE_ARCANIST = 0;
  public static final int TYPE_APPRENTICE = 1;
  public static final int TYPE_IMAGES = 2;


  private File datFile = null;
  private DatFormat datFormat = null;

  /** Names for each text element on a card. */
  private final List<String> fieldList = new ArrayList<String>(0);

  /** Index of a card's set field. */
  private int setField = -1;

  /** All text data in order, with each field an object. */
  public final List<String> elementList = new ArrayList<String>(0);

  /** Contains card names and their indeces within the elementList. */
  private final Map<String,Integer> elementMap = new HashMap<String,Integer>();

  /**
   * Contains Maps of card names and their indeces within elementList.
   * Maps are used to hold a pair of strings and to support 'contains()'.
   * This is an array of HashMaps, each containing multiple reprints, but only one of any card.
   */
  private final List<Map<String,Integer>> dupList = new ArrayList<Map<String,Integer>>(0);

  private final Map<String,List<String>> imageMap = new HashMap<String,List<String>>();
  public List<String> imageSetList = new ArrayList<String>();

  private final List<String> textSetNameList = new ArrayList<String>();
  private final Map<String,String> textSetToAbbrevMap = new HashMap<String,String>();
  private final Map<String,String> textAbbrevToSetMap = new HashMap<String,String>();


  public DatParser() {}


  /**
   * Gets this dat's type.
   *
   * @return TYPE_ARCANIST, TYPE_APPRENTICE, or TYPE_IMAGES
   */
  public synchronized int getType() {
     if (datFormat == null) return TYPE_IMAGES;
     if (datFormat instanceof ApprenticeDatFormat) return TYPE_APPRENTICE;
     if (datFormat instanceof ArcanistDatFormat) return TYPE_ARCANIST;
     return TYPE_IMAGES;
  }

  /**
   * Returns true if this dat is writable.
   */
  public synchronized boolean isWritable() {
     if (datFormat != null) return datFormat.isWritable();
     return false;
  }

  /**
   * Returns an array of SearchFieldPanels.
   *
   * @return an array of panels, or null if no dat format is set
   */
  public SearchFieldPanel[] getSearchFieldPanels() {
    if (datFormat != null) return datFormat.getSearchFieldPanels(fieldList);
    return null;
  }


  /**
   * Determines a data file's format.
   * The result is stored internally.
   * This is called upon creation.
   *
   * @param frame an existing GUI, or null
   * @param gamePath path to the game dir
   * @param datPath path to the CardText dat
   * @param expanPath path to Expan.dat
   */
  public synchronized void setup(ArcanistCCGFrame frame, String gamePath, String datPath, String expanPath) {
    datFile = null;

    if (new File(gamePath).exists() == false) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Game location not found.");
      return;
    }

    DatFormat format = null;
    String tempLine = "";
    if (datPath.length() > 0) {
      datFile = new File(datPath);
      if (!datFile.exists()) datFile = null;
    }

    if (datFile != null) {
      // Instantiate the appropriate DatFormat for datFile
      Class[] formats = new Class[] {ApprenticeDatFormat.class, ArcanistDatFormat.class};
      for (int i=0; i < formats.length; i++) {
        try {
          DatFormat tmpFormat = (DatFormat)formats[i].newInstance();
          if (tmpFormat.isParsable(datFile) == true) {
            format = tmpFormat;
            break;
          }
        }
        catch (InstantiationException e) {
          ArcanistCCG.LogManager.write(e, "Couldn't instantiate CardText parser.");
        }
        catch (IllegalAccessException e) {
          ArcanistCCG.LogManager.write(e, "Couldn't instantiate CardText parser.");
        }
      }
    }
    if (format == null) {
      // Fall back on scanning the game folder for images
      datPath = gamePath;
      expanPath = gamePath;
      format = new ImageDatFormat();
      datFile = new File(datPath);
    }

    datFormat = format;
    final String finalGamePath = gamePath;
    final String finalExpanPath = expanPath;
    final DatParser pronoun = this;
    final DatFormat detectedFormat = format;

    DatParseProgressFrame progFrame = null;
    if (frame != null) progFrame = new DatParseProgressFrame(frame);
    final DatParseProgressFrame finalProgFrame = progFrame;

    Thread t = new Thread() {
      public void run() {
        synchronized(pronoun) {
          if (detectedFormat != null) {
            parse(finalProgFrame, detectedFormat, datFile);
            if (setField != -1) parseExpanDat(finalExpanPath);
          }
          buildFileList(finalGamePath);
        }
      }
    };
    t.setPriority(Thread.NORM_PRIORITY);
    t.setDaemon(true);
    t.start();
  }


  /**
   * Parses a data file.
   */
  private synchronized void parse(DatParseProgressFrame progFrame, DatFormat format, File f) {
    List<String> tempFieldList = new ArrayList<String>();
    List<String> tempSpoilerList = new ArrayList<String>();
    Map<String,Integer> tempNameMap = new HashMap<String,Integer>();
    List<Map<String,Integer>> tempDupList = new ArrayList<Map<String,Integer>>();
      tempDupList.add(new HashMap<String,Integer>());
    try {
      if (progFrame != null) format.setProgressStatusComponent(progFrame, 10);
      format.parse(f, tempFieldList, tempSpoilerList);

      int fieldCount = tempFieldList.size();
      for (int i=0; i < fieldCount; i++) {
        if (tempFieldList.get(i).equals("Set")) setField = i;
      }

      int lineCount = tempSpoilerList.size();
      for (int i=0; i < lineCount; i += fieldCount) {
        String cleanName = sanitizeName(tempSpoilerList.get(i));
        tempSpoilerList.set(i, cleanName);

        if (tempNameMap.containsKey(cleanName) == false)
          tempNameMap.put(cleanName, new Integer(i));
        else {
          int j = 0;
          while (j < tempDupList.size() && tempDupList.get(j).containsKey(cleanName) == true) {
            j++;
          }
          if (j > tempDupList.size()-1) {
            tempDupList.add(new HashMap<String,Integer>());
          }
          tempDupList.get(j).put(cleanName, new Integer(i));
        }
      }

      fieldList.clear();
      elementList.clear();
      elementMap.clear();
      dupList.clear();
      fieldList.addAll(tempFieldList);
      elementList.addAll(tempSpoilerList);
      elementMap.putAll(tempNameMap);
      dupList.addAll(tempDupList);
    }
    catch (Exception e) {
      if (e instanceof FileNotFoundException) {
        ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "CardText file not found.");
      }
      else {
        //IllegalArgumentException
        //BufferUnderflowException
        //IOException
        ArcanistCCG.LogManager.write(e, "Couldn't parse CardText file.");
      }
    }
  }


  /**
   * Opens Expan.dat file and maps sets to abbreviations.
   *
   * @param expanPath path to Expan.dat
   */
  private synchronized void parseExpanDat(String expanPath) {
    textSetNameList.clear();
    textSetToAbbrevMap.clear();
    textAbbrevToSetMap.clear();

    if (expanPath == null || expanPath.length() == 0) {
      setField = -1;
      return;
    }

    if (getType() == TYPE_IMAGES) {
      // With ImageDat, we lied and gave the gamePath instead
      // Use subdirs' names for expansion info

      File gameDir = new File(expanPath);
      File[] setDirs = gameDir.listFiles(new DirFileFilter());
      Arrays.sort(setDirs);
      for (int i=0; i < setDirs.length; i++) {
        String setName = setDirs[i].getName();
        String setAbbrev = setName;

        textSetNameList.add(setName);
        textSetToAbbrevMap.put(setName, setAbbrev);
        textAbbrevToSetMap.put(setAbbrev, setName);
      }
      return;
    }


    BufferedReader inFile = null;

    try {
      FileReader fr = new FileReader(expanPath);
      inFile = new BufferedReader(fr);
      String tmpLine = inFile.readLine();

      int n=0;
      while (tmpLine != null) {
        n = tmpLine.indexOf("-");
        if (n<=0 || n+2 >= tmpLine.length())                //Also break if starts with "-" (Rarity)
          break;

        String setAbbrev = tmpLine.substring(0,n);
        String setName = tmpLine.substring(n+1, tmpLine.length());
        textSetNameList.add(setName);
        textSetToAbbrevMap.put(setName, setAbbrev);
        textAbbrevToSetMap.put(setAbbrev, setName);
        tmpLine = inFile.readLine();
      }

      inFile.close();
    }
    catch (FileNotFoundException e) {
      setField = -1;
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Expan.dat file not found.");
    }
    catch (IOException e) {
      ArcanistCCG.LogManager.write(e, "Couldn't parse Expan.dat file.");
    }
    finally {
      if (inFile != null) {
        try {inFile.close();}
        catch (IOException e) {}
      }
    }
  }


  /**
   * Gets an unsorted list of all the cards' names.
   */
  public synchronized Set<String> getAllCardNames() {
    return elementMap.keySet();
  }


  /**
   * Gets a list of full set names for which there are text reprints.
   *
   * @param cardName a card
   * @return a list of full set names
   */
  public synchronized List<String> getCardTextSets(String cardName) {
    List<String> result = new ArrayList<String>();
    if (setField == -1 || !cardExists(cardName)) return result;

    // Collect reprints' set fields in an array (unordered)
    List<String> textSetLineList = new ArrayList<String>();
    int n = ((Integer)elementMap.get(cardName)).intValue()+setField;
    textSetLineList.add( ((String)elementList.get(n)) );
    for (int k=0; k < dupList.size(); k++) {
      Map<String,Integer> tmpMap = dupList.get(k);
      if ( tmpMap.containsKey(cardName) == true) {
        n = tmpMap.get(cardName).intValue()+setField;
        textSetLineList.add( elementList.get(n) );
      }
    }

    String[] setAbbrevArray = getTextSetAbbrevs();

    // Build an ordered list of reprints' sets by finding each abbrev
    for(int i=0; i < textSetLineList.size(); i++) {
      String tmpSetLine = textSetLineList.get(i);
      for (int j=0; j < setAbbrevArray.length; j++) {
        if (tmpSetLine.matches("(^|,)"+ setAbbrevArray[j] +"(,|-|$)")) {
          result.add(textSetNameList.get(j));
        }
      }
    }

    return result;
  }


  /**
   * Returns an array of full set names.
   */
  public synchronized String[] getTextSetNames() {
    return textSetNameList.toArray(new String[textSetNameList.size()]);
  }

  /**
   * Returns an array of abbreviated set names.
   */
  public synchronized String[] getTextSetAbbrevs() {
    String[] result = new String[textSetNameList.size()];
    for (int i=0; i < textSetNameList.size(); i++) {
      result[i] = textSetToAbbrevMap.get(textSetNameList.get(i));
    }

    return result;
  }

  /**
   * Checks if a set exists.
   *
   * @param setName full set name
   * @return true if it exists
   */
  public synchronized boolean textSetExists(String setName) {
    return textSetToAbbrevMap.containsKey(setName);
  }


  /**
   * Checks if a card has text data.
   *
   * @param cardName a card name
   * @return true if a card with that exact name exists
   */
  public synchronized boolean cardExists(String cardName) {
    return elementMap.containsKey(cardName);
  }

  /**
   * Checks if a card has text data.
   *
   * @param cardName a card name or image filename
   * @return the case-insensitive match (with suffix trimming), or ""
   */
  public synchronized String isItThere(String cardName) {
    if (cardName.length() == 0) return "";

    if (elementMap.containsKey(cardName))
      return cardName;

    Set<String> knownCards = elementMap.keySet();

    cardName = CardFilenameFilter.trimExtension(cardName);
    if (cardName == null) return "";

    for (String knownCard : knownCards)
      if (knownCard.equalsIgnoreCase(cardName) == true)
        return knownCard;


    cardName = CardFilenameFilter.trimNumericSuffix(cardName);
    if (cardName == null) return "";

    for (String knownCard : knownCards) {
      if (knownCard.equalsIgnoreCase(cardName) == true)
        return knownCard;
    }

    return "";
  }


  /**
   * Fills a Set-ImageArray hashmap with available image names.
   *
   * @param gamePath path to the game dir
   */
  private void buildFileList(String gamePath) {
    File gameDir = new File(gamePath);
    if (!gameDir.exists()) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't find game folder.");
      return;
    }

    File[] setDirs = gameDir.listFiles(new DirFileFilter());
    File[] setCards;
    for (int i=0; i < setDirs.length; i++) {
      setCards = setDirs[i].listFiles();
      Arrays.sort(setCards);
      imageMap.put(setDirs[i].getName(), new ArrayList<String>());
        imageSetList.add(setDirs[i].getName());
      for (int j=0; j < setCards.length; j++) {
        imageMap.get(imageSetList.get(i)).add(setCards[j].getName());
      }
    }
    //Collections.sort(imageSetList);

    // The following lines sort by Expan.dat order, with unlisted dirs at the top
    List<String> tmpList = new ArrayList<String>(imageSetList.size());
    for (int i=imageSetList.size()-1; i >= 0; i--) {
      if (textSetNameList.contains(imageSetList.get(i)) == false) {
        tmpList.add(imageSetList.get(i));
        imageSetList.remove(i);
      }
    }
    Collections.sort(tmpList);  // Alpha-sort unlisted dirs
    // Add the dat's dirs
    for (int i=0; i < textSetNameList.size(); i++) {
      for (int j=0; j < imageSetList.size(); j++) {
        if (textSetNameList.get(i).equals(imageSetList.get(j))) {
          tmpList.add(imageSetList.get(j));
          imageSetList.remove(j);
          break;
        }
      }
    }
    imageSetList = tmpList;
  }


  /**
   * Gets the number of fields.
   */
  public synchronized int getFieldCount() {return fieldList.size();}

  /**
   * Gets the name of a field.
   */
  public synchronized String getFieldName(int n) {
    if (n < 0 || n >= fieldList.size()) return null;
    return (String)fieldList.get(n);
  }


  /**
   * Tests whether an image exists in a given set.
   * The matching is exact, not fuzzy.
   *
   * @param setName full set name
   * @param fileName explicit front or back filename
   * @return true if the file exists, false otherwise
   */
  public synchronized boolean imageExists(String setName, String fileName) {
    if (imageMap.containsKey(setName)) {
      List<String> setImageList = imageMap.get(setName);
      if (setImageList.contains(fileName)) return true;
    }
    return false;
  }

  /**
   * Finds images of a given card in a given set.
   *
   * @param setName full set name
   * @param cardName wanted card
   * @return an array of available image file names
   */
  public synchronized List<String> availableImages(String setName, String cardName) {
    List<String> resultList = new ArrayList<String>();
    int cardCount;
    if (imageMap.containsKey(setName))
      cardCount = imageMap.get(setName).size();
    else
      cardCount = 0;

    CardFilenameFilter nameFilter = new CardFilenameFilter(cardName);

    List<String> setImageList = imageMap.get(setName);
    for (int i=0; i < cardCount; i++) {
      String fileName = setImageList.get(i);
      if (nameFilter.accept(null, fileName) == true) {
        resultList.add(fileName);
      }
    }
    return resultList;
  }


  /**
   * Frontend for the other availableImages method.
   * This finds the most recent full set name with any images of a card.
   *
   * @param cardName wanted card
   * @return a full set name, or ""
   */
  public synchronized String availableImages(String cardName) {
    List<String> returnedList;

    // Check each set name for any reprints
    for (int i=imageSetList.size()-1; i >= 0; i--) {
      returnedList = availableImages((String)imageSetList.get(i), cardName);
      if (returnedList.size() > 0)
        return imageSetList.get(i);
    }
    return "";
  }


  /**
   * Get the abbreviated set name for a given full set name.
   *
   * @param setName full set name
   * @return the abbreviation, or ""
   */
  public synchronized String getSetAbbrevFromName(String setName) {
    if (textSetToAbbrevMap.containsKey(setName) == false)
      return "";

    return textSetToAbbrevMap.get(setName);
  }


  /**
   * Gets the full set name for a given abbreviated set name.
   *
   * @param setAbbrev set abbreviation
   * @return full set name, or ""
   */
  public synchronized String getSetNameFromAbbrev(String setAbbrev) {
    if (textAbbrevToSetMap.containsKey(setAbbrev) == false)
      return "";

    return textAbbrevToSetMap.get(setAbbrev);
  }


  /**
   * Finds a card's index within elementList.
   *
   * @param cardName card name
   * @param setAbbrev abbreviated set name, or "" for most recent
   * @return index of the card in elementList, or -1
   */
  public synchronized int findCard(String cardName, String setAbbrev) {
    if (cardName == null || cardName.length() == 0 || elementMap.containsKey(cardName) == false) {return -1;}
    if (setAbbrev == null || setAbbrev.length() == 0) return findCard(cardName);
    int index;

    if (setField != -1) {
      index = elementMap.get(cardName).intValue()+setField;
      if ( elementList.get(index).indexOf(setAbbrev) == -1) {
        index = -1;                                           //Prevents settling for any reprint
        for (int i=0; i < dupList.size(); i++) {
          if ( dupList.get(i).containsKey(cardName) == true) {
            index = dupList.get(i).get(cardName).intValue()+setField;
            if ( elementList.get(index).indexOf(setAbbrev) != -1 ) {
              index -= setField;
              break;
            }
            index = -1;                                      //This line's skipped on success
          }
          else {
            // No more dupList entries for this card, stop looking
            break;
          }
        }
      }
      else {
        index -= setField;
      }
    }
    else {
      index = elementMap.get(cardName).intValue();
    }
    return index;
  }

  /**
   * Frontend for the other findCard method.
   * This finds the card index of the most recent text.
   *
   * @param cardName card name
   * @return index of the card in elementList, or -1
   */
  public synchronized int findCard(String cardName) {
    if (cardName == null || cardName.length() == 0) return -1;
    String[] setAbbrevArray = getTextSetAbbrevs();
    int result = -1;

    // Check each set name for any reprints
    for (int i=setAbbrevArray.length-1; i >= 0; i--) {
      result = findCard(cardName, setAbbrevArray[i]);
      if (result != -1) return result;
    }

    return result;
  }


  /**
   * Returns the text for each field of a card, or null.
   *
   * @param index an index from a call to findCard()
   */
  public synchronized String[] getCardText(int index) {
    int fieldCount = fieldList.size();
    if (index < 0 || index+fieldCount-1 >= elementList.size()) return null;
    if (index % fieldCount != 0) return null;

    String[] result = new String[fieldCount];

    for (int i=0; i < fieldCount; i++) {
      result[i] = elementList.get(index+i);
    }

    return result;
  }


  /**
   * Adds a new card.
   * If the proposed card has the same name and set
   * as an existing one, creation is cancelled.
   * If the proposed card contains no valid set abbreviations,
   * as listed in Expan.dat, creation is cancelled.
   *
   * @param newData card text to be added
   */
  public synchronized void addCard(String[] newData) {
    int fieldCount = fieldList.size();
    if (newData.length != fieldCount) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't add card: Proposed text has incorrect field count ("+ newData.length +").");
      return;
    }
    if (!isWritable()) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't add card: Current dat is not writable.");
      return;
    }

    if (setField != -1) {
      String[] tokens = newData[setField].split(",");
      int t = 0;
      String tempLine; boolean validSet = false;
      while (t+1 <= tokens.length) {
        tempLine = tokens[t++];
        tempLine = tempLine.substring(0, (tempLine.indexOf("-")!=-1?tempLine.indexOf("-"):tempLine.length()));
        // If any set abbreviation is valid, continue
        if (getSetNameFromAbbrev(tempLine).length() > 0) {
          validSet = true;
        }
        // If another entry claims this set, cancel
        if (findCard(newData[0], tempLine) != -1) {return;}
      }
      if (validSet == false) {return;}
    }
    else {
      if (findCard(newData[0], "") != -1) {return;}
    }

    for (int i=0; i < fieldCount; i++) {
      elementList.add(newData[i]);
    }

    // Check for existing mapping, and add to dupList if necessary
    if (elementMap.containsKey(newData[0]) == false) {
      elementMap.put(newData[0], new Integer(elementList.size()-fieldCount));
    }
    else {
      boolean dupListFull = false;
      for (int i=0; i < dupList.size(); i++) {
        if ( dupList.get(i).containsKey(newData[0]) == false) {
          dupList.get(i).put(newData[0], new Integer(elementList.size()-fieldCount));
          break;
        }
        if (i == dupList.size()-1) {
          dupListFull = true;
        }
      }
      // In case there's already 1 reprint of this in every dupList map
      if (dupListFull == true) {
        dupList.add(new HashMap<String,Integer>());
        dupList.get(dupList.size()-1).put(newData[0], new Integer(elementList.size()-fieldCount));
      }
    }
  }


  /**
   * Deletes a card.
   *
   * @param index index of a card in elementList
   */
  public synchronized void delCard(int index) {
    int fieldCount = fieldList.size();
    if (!isWritable()) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't delete card: Current dat is not writable.");
      return;
    }
    if (index < 0 || index+fieldCount-1 >= elementList.size() || index % fieldCount != 0) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't delete card: Bad dat index ("+ index +").");
      return;
    }

    String doomedName = (String)elementList.get(index);

    for (int i=0; i < fieldCount; i++) {
      elementList.set(index+i, "");
    }

    int dupIndex = -1; boolean gotIt = false;
    for (int i=0; i < dupList.size(); i++) {
      if ( ((Map)dupList.get(i)).containsKey(doomedName) == true) {
        if (index == dupList.get(i).get(doomedName).intValue() ) {
          if (dupIndex != -1) {
            dupList.get(i).put(doomedName, new Integer(dupIndex) );
            // Shift a spare dup into matching dup
            dupIndex = -1;
          }
          else {
            dupList.get(i).remove(doomedName);
            // Remove matching dup
          }
          gotIt = true;
        }
        else if (gotIt == false && dupIndex == -1) {
          // Grab a spare dup
          dupIndex = dupList.get(i).get(doomedName).intValue();
        }
        if (i>0) {
          // Shift dup up in dupList
          dupList.get(i-1).put( doomedName, dupList.get(i).get(doomedName) );
        }
        if ( i==dupList.size()-1 || (i+1<dupList.size() && dupList.get(i+1).containsKey(doomedName) == false) ) {
          // Remove last dup in dupList
          dupList.get(i).remove(doomedName);
        }
      }
    }
    //Remove existing mapping, or move an entry from dupList if available
    if (gotIt == false && elementMap.get(doomedName).intValue() == index) {
      if (dupIndex != -1) {
        elementMap.put(doomedName, new Integer(dupIndex));
        // Put spare dup into elementMap
      }
      else {
        elementMap.remove(doomedName);
        // Completely remove card from elementMap
      }
    }
    // Nothing happens if not in dupList or elementMap
  }


  /**
   * Edits a card.
   *
   * @param newData card text to be used
   * @param index index of a card in elementList
   */
  public synchronized void editCard(String[] newData, int index) {
    int fieldCount = fieldList.size();
    if (newData.length != fieldCount) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't edit card: Proposed text has incorrect field count ("+ newData.length +").");
      return;
    }
    if (!isWritable()) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't edit card: Current dat is not writable.");
      return;
    }
    if (index < 0 || index+fieldCount-1 >= elementList.size() || index % fieldCount != 0) {
      ArcanistCCG.LogManager.write(LogManager.ERROR_LEVEL, "Couldn't edit card: Bad dat index ("+ index +").");
      return;
    }

    if (setField != -1) {
      String[] tokens = newData[setField].split(",");
      int t = 0;
      String tempLine; boolean validSet = false;
      while (t+1 <= tokens.length) {
        tempLine = tokens[t++];
        tempLine = tempLine.substring(0, (tempLine.indexOf("-")!=-1?tempLine.indexOf("-"):tempLine.length()));
        // If any set abbreviation is valid, continue
        if ( getSetNameFromAbbrev(tempLine).length()>0 ) {
          validSet = true;
        }
        // If another entry claims this set, cancel
        int match = findCard(newData[0], tempLine);
        if (match != -1 && match != index) {return;}
      }
      if (validSet == false) {return;}
    }
    else {
      if (findCard(newData[0], "") != -1) {return;}
    }

    if (elementList.get(index).equals(newData[0]) == false) {
      delCard(index);
    }

    for (int i=0; i < fieldCount; i++) {
      elementList.set(index+i, newData[i]);
    }

    // Check for existing mapping, and add to dupList if necessary
    if (elementMap.containsKey(newData[0]) == false) {
      elementMap.put(newData[0], new Integer(index));
    }
    else if ( elementMap.get(newData[0]).intValue() == index ) {
      // This is already listed
    }
    else {
      boolean dupDontAdd = false;
      for (int i=0; i < dupList.size(); i++) {
        if ( dupList.get(i).containsKey(newData[0]) == true) {
          if ( dupList.get(i).get(newData[0]).intValue() == index ) {
            // This is a reprint, and already listed
            dupDontAdd = true;
            break;
          }
        }
      }
      if (dupDontAdd == false) {
        // Find the next open dupList and add it
        boolean dupListFull = false;
        for (int i=0; i < dupList.size(); i++) {
          if ( dupList.get(i).containsKey(newData[0]) == false) {
            dupList.get(i).put(newData[0], new Integer(index));
            break;
          }
          if (i == dupList.size()-1) {
            dupListFull = true;
          }
        }
        // In case there's already 1 reprint of this in every dupList map
        if (dupListFull == true) {
          dupList.add(new HashMap<String,Integer>());
          dupList.get(dupList.size()-1).put(newData[0], new Integer(index));
        }
      }
    }
  }


  /**
   * Saves the elementList to the original data file.
   */
  public synchronized void save() {
    if (!isWritable() || datFile == null) return;

    datFormat.save(datFile, fieldList, elementList);
  }


  /**
   * Strips illegal characters from a card name.
   * This includes :?!%#+=&" and trailing periods.
   * When a space preceeds an ampersand, the space is removed as well.
   */
  public String sanitizeName(String tmp) {
    tmp = tmp.replaceAll("( &)|([:?!%#+=&\"])|([.]+$)", "");
    return tmp;
  }
}
