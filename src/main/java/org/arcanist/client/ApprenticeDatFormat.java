package org.arcanist.client;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.List;

import org.arcanist.client.*;
import org.arcanist.util.*;


public class ApprenticeDatFormat implements DatFormat {

  private DatParseProgressFrame parseStatusComponent = null;
  private int parseStatusUpdateInterval = -1;
  private Charset charset = null;
  private CharsetDecoder charsetDecoder = null;
  private CharsetEncoder charsetEncoder = null;


  public ApprenticeDatFormat() {
    charset = Charset.forName("windows-1252");

    charsetDecoder = charset.newDecoder();
    charsetDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

    charsetEncoder = charset.newEncoder();
    charsetEncoder.onUnmappableCharacter(CodingErrorAction.REPORT);
  }


  /**
   * Returns the name of this format.
   */
  @Override
  public String getName() {return "Apprentice";}


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

    if (f.getName().toLowerCase().endsWith(".dat") == true) {
      return true;
    }
    return false;
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
   * The following Exceptions may occur:
   *   IllegalArgumentException
   *   BufferUnderflowException
   *   FileNotFoundException
   *   IOException
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
        @Override
        public void run() {
          tmpComp.parsingStarted();
        }
      });
    }

    fieldList.add("Name");
    fieldList.add("Color");
    fieldList.add("Set");
    fieldList.add("Type");
    fieldList.add("Cost");

    fieldList.add("Stats");
    fieldList.add("Text");
    fieldList.add("Flavor");

    int cardCount = 0;
    FileInputStream br = null;
    String tmpLine;

    try {
      br = new FileInputStream(f);                           //For raw numbers
      byte[] databytes = new byte[br.available()];           //Make a byte[] to hold file
      br.read(databytes, 0, br.available());                 //Fill the array
      ByteBuffer bb = ByteBuffer.wrap(databytes);            //Make it a buffer
      bb.order(ByteOrder.LITTLE_ENDIAN);                     //Win32 byte order

      int mainOffset = bb.getInt();                          //Read offset to card total
      bb.position(mainOffset);
      int numcards = bb.getInt();                            //Read card total


      short setlength;                                       //Search for the card
      while (bb.hasRemaining()) {
        tmpLine = readString(bb);                            //Name
        spoilerList.add(tmpLine);
        if (parseStatusComponent != null && ++cardCount % parseStatusUpdateInterval == 0) {
          final String tmpName = tmpLine;
          final DatParseProgressFrame tmpComp = parseStatusComponent;
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              tmpComp.setCurrentCard(tmpName);
            }
          });
        }

        int bodyOffset = bb.getInt();                        //Offset to more of the card's data
        int tmpColor = (int)bb.get();                        //Color (Land = -128)
          if (tmpColor < 0) tmpColor *= -1;
          spoilerList.add(Integer.toString(tmpColor));
        spoilerList.add(readString(bb));                     //Set
        bb.position(bb.position()+2);                        //Skip the spacer
        int cardOffset = bb.position();                      //Leave breadcrumbs

        bb.position(bodyOffset);                             //Skip to rest of card
        spoilerList.add(readString(bb));                     //Type
        spoilerList.add(readString(bb));                     //Cost
        spoilerList.add(readString(bb));                     //Stats
        spoilerList.add(readString(bb));                     //CardText
        tmpLine = readString(bb);                           //FlavorText
        //Add linebreaks in conversations
        spoilerList.add(tmpLine.replaceAll("\" \"", "\"\\\\n\""));

        bb.position(cardOffset);                             //On to the next card...
      }
      br.close();
    }
    catch (IllegalArgumentException e) {
      exception = e;
    }
    catch (BufferUnderflowException e) {
      exception = e;
    }
    catch (FileNotFoundException e) {
      exception = e;
    }
    catch (IOException e) {
      exception = e;
    }
    finally {
      if (br != null) {
        try {br.close();}
        catch (IOException e) {}
      }
    }

    if (exception != null) throw exception;


    if (parseStatusComponent != null) {
      final DatParseProgressFrame tmpComp = parseStatusComponent;
      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          tmpComp.parsingCompleted();
        }
      });
    }
    setProgressStatusComponent(null, -1);
  }


  /**
   * Returns an array of of SearchFieldPanels.
   */
  @Override
  public SearchFieldPanel[] getSearchFieldPanels(List<String> fieldList) {
    SearchFieldPanel[] result = new SearchFieldPanel[fieldList.size()];

    int cols = 0;
    for (int i=0; i < fieldList.size(); i++) {
      String name = (String)fieldList.get(i);
      int n = 0;
      if (name.equals("Color")) {
        n = ColorBarSearchFieldPanel.MIN_COLUMNS;
      } else if (name.equals("Set")) {
        n = ExpanSearchFieldPanel.MIN_COLUMNS;
      } else {
        n = TextSearchFieldPanel.MIN_COLUMNS;
      }
      if (n > cols) cols = n;
    }

    for (int i=0; i < fieldList.size(); i++) {
      String name = (String)fieldList.get(i);
      if (name.equals("Color")) {
        result[i] = new ColorBarSearchFieldPanel(name, cols);
      } else if (name.equals("Set")) {
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
    String tmpLine;
    int headSize = 4;
    int tailSize = 4;
    int skippedCards = 0;

    for (int i=0; i < spoilerList.size(); i+=fieldCount) {
      //Skip empty cards
      if (((String)spoilerList.get(i)).length() == 0) {
        skippedCards++;
        continue;
      }

      tmpLine = (String)spoilerList.get(i);                  //Name
      tailSize += 2+tmpLine.length()+4+1;                    //+BodyOffset+Color
      tmpLine = (String)spoilerList.get(i+2);                //Set
      tailSize += 2+tmpLine.length()+2;                      //+Spacer

      tmpLine = (String)spoilerList.get(i+3);                //Type
      headSize += 2+tmpLine.length();
      tmpLine = (String)spoilerList.get(i+4);                //Cost
      headSize += 2+tmpLine.length();
      tmpLine = (String)spoilerList.get(i+5);                //Stats
      headSize += 2+tmpLine.length();
      tmpLine = (String)spoilerList.get(i+6);                //Text
      headSize += 2+tmpLine.length();
      tmpLine = (String)spoilerList.get(i+7);                //Flavor
      headSize += 2+tmpLine.length();
    }


    FileOutputStream bw = null;

    try {
      bw = new FileOutputStream(f);                          //For raw numbers

      byte[] databytes = new byte[headSize+tailSize];        //Make a byte[] to hold file
      ByteBuffer bb = ByteBuffer.wrap(databytes);            //Make it a buffer
      bb.order(ByteOrder.LITTLE_ENDIAN);                     //Win32 byte order

      bb.putInt(headSize);                                   //Write offset to card number
      int nextTitleOffset = headSize+4;                      //Save return offset to next card title

      for (int i=0; i < spoilerList.size(); i+=fieldCount) {
        if (((String)spoilerList.get(i)).length() == 0) {    //Skip empty cards
          continue;
        }
        //write head
        int cardOffset = bb.position();                      //Save offset to body text
        writeString(bb, (String)spoilerList.get(i+3));       //Write type
        writeString(bb, (String)spoilerList.get(i+4));       //Write cost
        writeString(bb, (String)spoilerList.get(i+5));       //Write stats
        writeString(bb, (String)spoilerList.get(i+6));       //Write cardtext
        writeString(bb, (String)spoilerList.get(i+7));       //Write flavor
        int nextCardOffset = bb.position();                  //Save return offset to next body text
        //write tail
        bb.position(nextTitleOffset);
        writeString(bb, (String)spoilerList.get(i));         //Write name
        bb.putInt(cardOffset);                               //Write offset to body text
        bb.put((byte) Integer.parseInt((String)spoilerList.get(i+1)));  //Write color
        writeString(bb, (String)spoilerList.get(i+2));       //Write set
        bb.putShort((short)0);                               //Write spacer
        nextTitleOffset = bb.position();                     //Save return offset to next card title
        bb.position(nextCardOffset);
      }

      bb.position(headSize);                                 //Jump to tail
      bb.putInt(spoilerList.size()/fieldCount-skippedCards); //Write card total


      bw.write(databytes);                                   //Write the buffer to the file
    }
    catch (IllegalArgumentException e) {
      ArcanistCCG.LogManager.write(e, "Couldn't save CardText file.");
    }
    catch (BufferUnderflowException e) {
      ArcanistCCG.LogManager.write(e, "Couldn't save CardText file.");
    }
    catch (IOException e) {
      ArcanistCCG.LogManager.write(e, "Couldn't save CardText file.");
    }
    finally {
      try {if (bw != null) bw.close();}
      catch (IOException e) {}
    }
  }


  /**
   * Convenience method for Apprentice parsing.
   * Apprentice stores strings C style: (5hello or 3bye) in binary form.
   *
   * @param bb Buffer containing string
   */
  private String readString(ByteBuffer bb) throws CharacterCodingException {
    short length;
    byte[] tmpbytes;
    String tmpstring;

    length = bb.getShort();
    tmpbytes = new byte[length];
    bb.get(tmpbytes);
    tmpstring = charsetDecoder.decode(ByteBuffer.wrap(tmpbytes)).toString();
    //tmpstring = tmpstring.replaceAll("\\r", "");           //Moved to dat saver
    //tmpstring = tmpstring.replaceAll("\\n", "\\\\n");      //for faster loading

    return tmpstring;
  }

  /**
   * Convenience method for Apprentice writing.
   * Apprentice stores strings C style: (5hello or 3bye) in binary form.
   *
   * @param bb Destination buffer
   * @param tmpstring String to be written
   */
  private void writeString(ByteBuffer bb, String tmpstring) throws CharacterCodingException {
    tmpstring = tmpstring.replaceAll("\\r", "");             //Moved from loader
    tmpstring = tmpstring.replaceAll("\\n", "\\\\n");        //for faster loading

    tmpstring = tmpstring.replaceAll("\\\\n", "\r\n");
    short length = (short)tmpstring.length();
    byte[] tmpbytes = charsetEncoder.encode(CharBuffer.wrap(tmpstring)).array();

    bb.putShort(length);
    bb.put(tmpbytes);
  }
}
