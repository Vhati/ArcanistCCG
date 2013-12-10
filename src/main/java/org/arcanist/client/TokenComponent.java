package org.arcanist.client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

import org.arcanist.client.*;


/**
 * This is the ancestor of all draggable images.
 * It has a serial number for uniqueness.
*/
public class TokenComponent extends JComponent {

  private ArcanistCCGFrame frame = null;

  private int id = -1;
  private BufferedImage tokenImage = null;
  private volatile boolean paintLock;


  public TokenComponent(ArcanistCCGFrame f, BufferedImage image) {
    frame = f;
    setImage(image);
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


  public synchronized void setImage(BufferedImage image) {
    tokenImage = image;
  }

  public synchronized BufferedImage getImage() {
    return tokenImage;
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
    if (tokenImage != null) {
      g2 = (Graphics2D)g.create();
      g2.drawImage(tokenImage, 0, 0, null);
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
