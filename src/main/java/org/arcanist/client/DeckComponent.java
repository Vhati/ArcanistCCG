package org.arcanist.client;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.font.*;
import javax.swing.*;
import javax.swing.event.*;
import java.text.*;

import org.arcanist.client.*;


/**
 * A table component for Decks.
 * It has a serial number for uniqueness.
*/
public class DeckComponent extends JComponent {
  private ArcanistCCGFrame frame = null;

  private int id = -1;
  private BufferedImage cardImage = null;
  private BufferedImage emptyImage = null;
  private volatile boolean cardShown = false;
  private volatile boolean paintLock;


  public DeckComponent(ArcanistCCGFrame f, BufferedImage image) {
    frame = f;
    setEmptyImage(image);
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
   * Sets whether to show the card image, or the empty image.
   */
  public synchronized void setCardShown(boolean b) {
    if (b == cardShown) return;

    cardShown = b;
    repaint();
  }

  /**
   * Gets whether the card image is shown.
   */
  public boolean getCardShown() {
    return cardShown;
  }


  public synchronized void setCardImage(BufferedImage image) {
    cardImage = image;
  }

  public synchronized BufferedImage getCardImage() {
    return cardImage;
  }

  public synchronized void setEmptyImage(BufferedImage image) {
    emptyImage = image;
  }

  public synchronized BufferedImage getEmptyImage() {
    return emptyImage;
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


  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = null;

    // Draw the dashed border
    g2 = (Graphics2D)g.create();
    g2.setColor(Color.YELLOW);
    float dashes[] = {5f,15f};
    g2.setStroke(new BasicStroke(8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,dashes,0));
    g2.draw(new Rectangle(-2, 3, Prefs.defaultCardWidth+2, Prefs.defaultCardHeight+2));

    // Draw the image
    BufferedImage displayedImage = null;
    if (cardShown == true && cardImage != null) displayedImage = cardImage;
    else if (cardShown == false && emptyImage != null) displayedImage = emptyImage;
    if (displayedImage != null) {
      g2 = (Graphics2D)g.create();
      g2.drawImage(displayedImage, 2, 2, null);
    }

    if (frame.isDragging() == false && paintLock == true) {
      // Draw the lock
      g2 = (Graphics2D)(g.create());
      g2.setColor(Color.ORANGE);
      g2.fillRect(16, 4, 2, 6);
      g2.fillRect(16, 4, 6, 2);
      g2.fillRect(20, 4, 2, 5);
      g2.fillRect(14, 9, 10, 9);
    }
  }
}
