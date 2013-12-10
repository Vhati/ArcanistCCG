package org.arcanist.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import javax.swing.JComponent;

import org.arcanist.client.*;


/**
 * A table component for Cards.
 * It has a serial number for uniqueness.
*/
public class CardComponent extends JComponent {

  private static final Font OVERLAY_FONT = new Font("SansSerif", Font.PLAIN, 10);

  private ArcanistCCGFrame frame = null;

  private int id = -1;
  private BufferedImage frontImage = null;
  private BufferedImage backImage = null;
  private volatile String frontName = null;
  private volatile String backName = null;
  private volatile String frontStats = null;
  private volatile String backStats = null;
  private volatile boolean showText = false;
  private volatile boolean flipState = true;
  private volatile boolean paintLock;
  private volatile int rotation = 0;


  public CardComponent(ArcanistCCGFrame f, BufferedImage image) {
    frame = f;
    setFrontImage(image);
  }


  /**
   * Sets the serial number.
   *
   * @param n New id
   */
  public void setId(int n) {
    id = n;
  }

  /**
   * Gets the serial number.
   *
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * Sets the faceup state.
   *
   * @param b face up? (T/F)
   */
  public void setFlipState(boolean b) {
    if (b == flipState) return;

    flipState = b;
    repaint();
  }

  /**
   * Gets the faceup state.
   */
  public boolean getFlipState() {
    return flipState;
  }


  public synchronized void setFrontImage(BufferedImage image) {
    frontImage = image;
  }

  public synchronized BufferedImage getFrontImage() {
    return frontImage;
  }

  public synchronized void setBackImage(BufferedImage image) {
    backImage = image;
  }

  public synchronized BufferedImage getBackImage() {
    return backImage;
  }


  /**
   * Sets whether to always show text.
   *
   * @param b true to always show text, false to use global defaults
   */
  public void setShowText(boolean b) {showText = b;}

  /**
   * Sets name text to be painted over the image.
   *
   * @param frontName string to show when enabled, null for none
   * @param backName string to show when disabled, null for none
   */
  public void setNameText(String frontName, String backName) {
    if (frontName != null && frontName.length() == 0) frontName = null;
    if (backName != null && backName.length() == 0) backName = null;

    this.frontName = frontName;
    this.backName = backName;
  }

  /**
   * Sets stat text to be painted over the image.
   *
   * @param frontStats string to show when faceup, null for none
   * @param backStats string to show when facedown, null for none
   */
  public void setStatsText(String frontStats, String backStats) {
    if (frontStats != null && frontStats.length() == 0) frontStats = null;
    if (backStats != null && backStats.length() == 0) backStats = null;

    this.frontStats = frontStats;
    this.backStats = backStats;
  }


  /**
   * Set the rotation.
   * This doesn't actually rotate the image.
   * This affects decorations.
   *
   * @param n the rotation
   */
  public void setRotation(int n) {
    rotation = n;
  }


  /**
   * Sets whether to paint a lock.
   *
   * @param b true to paint a lock, false otherwise
   */
  public void setPaintLock(boolean b) {paintLock = b;}

  /**
   * Gets whether to paint a lock.
   *
   * @return true to paint a lock, false otherwise
   */
  public boolean getPaintLock() {return paintLock;}


  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);                                 //paint normally
    Graphics2D g2 = null;

    // Draw the image
    BufferedImage displayedImage = null;
    if (flipState == true && frontImage != null) displayedImage = frontImage;
    else if (flipState == false && backImage != null) displayedImage = backImage;
    if (displayedImage != null) {
      g2 = (Graphics2D)g.create();
      int xAdj = 0, yAdj = 0;
      switch (rotation) {
        case 0:
          xAdj = 0;
          yAdj = 0;
          break;
        case -90:
          xAdj = 0;
          yAdj = -(Prefs.defaultCardHeight-0);
          break;
        case -180:
          xAdj = -(Prefs.defaultCardWidth-0);
          yAdj = -(Prefs.defaultCardHeight-0);
          break;
        case -270:
          xAdj = -(Prefs.defaultCardWidth-0);
          yAdj = 0;
          break;
      }
      g2.drawImage(displayedImage, 0, 0, null);
    }

    if (frame.isDragging() == false) {
      if (showText == true || Prefs.textOnImages == true) {
        // Draw overlay text
        String nameText = null;
        if (flipState == true) nameText = frontName;
        else nameText = backName;
        if (nameText != null) {
          g2 = (Graphics2D)g.create();
          paintText(g2, nameText, 5, 11, 0);
        }

        String statsText = null;
        if (flipState == true) statsText = frontStats;
        else statsText = backStats;
        if (statsText != null) {
          g2 = (Graphics2D)g.create();
          paintText(g2, statsText, 5, Prefs.defaultCardHeight/2, 0);
        }
      }
    }
    if (frame.isDragging() == false && paintLock == true) {
      // Draw the lock
      g2 = (Graphics2D)g.create();
      g2.setColor(Color.ORANGE);
      g2.fillRect(16, 4, 2, 6);
      g2.fillRect(16, 4, 6, 2);
      g2.fillRect(20, 4, 2, 5);
      g2.fillRect(14, 9, 10, 9);
    }
  }


  /**
   * Paint text on a card's image.
   * This is called by paintComponent.
   */
  private void paintText(Graphics2D g2, String text, int x, int y, int lineSpacing) {
    if (text.length() == 0 || x < 0 || y < 0)
      return;

    int xAdj = 0, yAdj = 0;
    g2.addRenderingHints( new RenderingHints( RenderingHints.KEY_ANTIALIASING , RenderingHints.VALUE_ANTIALIAS_OFF ));
    AttributedString attribString = new AttributedString(text);
      attribString.addAttribute(TextAttribute.FONT, OVERLAY_FONT);
    AttributedCharacterIterator iterator = attribString.getIterator();
    LineBreakMeasurer measurer;
    TextLayout textFragment;

    measurer = new LineBreakMeasurer(iterator, g2.getFontRenderContext());
    textFragment = null;

    g2.rotate(Math.toRadians(-rotation));                    //Spins opposite cardRot
    while (measurer.getPosition() < iterator.getEndIndex()) {
      switch (rotation) {
        case 0:
          xAdj = x;
          yAdj = y;
          break;
        case -90:
          xAdj = x;
          yAdj = -(Prefs.defaultCardHeight-y);
          break;
        case -180:
          xAdj = -(Prefs.defaultCardWidth-x);
          yAdj = -(Prefs.defaultCardHeight-y);
          break;
        case -270:
          xAdj = -(Prefs.defaultCardWidth-x);
          yAdj = y;
          break;
      }
      textFragment = measurer.nextLayout(Prefs.defaultCardWidth-2*x);

      g2.setColor(Color.BLACK);
      Rectangle bkgBounds = textFragment.getBounds().getBounds();
      g2.fillRect(xAdj+bkgBounds.x-2, yAdj+bkgBounds.y-2, bkgBounds.width+4, bkgBounds.height+4);

      g2.setColor(Color.WHITE);
      textFragment.draw(g2, xAdj, yAdj);
      y += (bkgBounds.height+4) + lineSpacing;
    }
  }
}
