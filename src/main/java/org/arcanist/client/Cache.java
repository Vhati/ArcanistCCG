// http://download.oracle.com/javase/tutorial/2d/images/drawimage.html
// http://blog.codebeach.com/2008/03/convert-color-image-to-gray-scale-image.html

package org.arcanist.client;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;


/**
 * A central cache of loaded images.
 * ImageIO reads the file and stores a WeakReference to it in a HashMap.
 * The result is instant loading of similar decks.
 * When no cards are left on the table with a given image, it is garbage collected.
 */
public class Cache {

  // Maps String paths to WeakReference(BufferedImage)
  Map<String,WeakReference<BufferedImage>> originalCacheMap = new HashMap<String,WeakReference<BufferedImage>>(150);
  Map<String,WeakReference<BufferedImage>> scaledCacheMap = new HashMap<String,WeakReference<BufferedImage>>(150);


  public Cache() {
    ImageIO.setUseCache(false);
  }

  /**
   * Clears the backend HashMap and runs garbage collection.
   */
  public synchronized void setup() {
    originalCacheMap.clear();
    scaledCacheMap.clear();
    System.gc();
  }


  /**
   * Gets an uncached fullsize image.
   *
   * @param path Path to image file
   * @return an image, or null
   */
  public BufferedImage getOriginalImage(String path) {
    File imageFile = new File(path);
    if (imageFile.exists() == false) return null;

    BufferedImage tmpImage = null;
    try {tmpImage = ImageIO.read(imageFile);} catch (IOException e) {}

    if (tmpImage != null) return tmpImage;
    else return null;
  }

  /**
   * Gets a cached fullsize image.
   * Do NOT use this to initially load card images.
   * These images are cached separately from scaled ones.
   *
   * @param path path to image file
   * @return an image, or null
   */
  public synchronized BufferedImage getCachedOriginalImage(String path) {
    if (originalCacheMap.containsKey(path) && originalCacheMap.get(path).get() != null)
      return originalCacheMap.get(path).get();

    File imageFile = new File(path);
    if (imageFile.exists() == false) return null;

    BufferedImage tmpImage = null;
    try {tmpImage = ImageIO.read(imageFile);} catch (IOException e) {}

    if (tmpImage != null) {
      originalCacheMap.put(path, new WeakReference<BufferedImage>(tmpImage));
      return tmpImage;
    }
    else {
      return null;
    }
  }

  /**
   * Gets a card's cached scaled image.
   * If the image is wider than it is tall, it is pre-rotated left 90 degrees.
   *
   * @param path path to image file
   * @return an image of a card
   */
  public synchronized BufferedImage getImg(String path) {
    if (scaledCacheMap.containsKey(path) && scaledCacheMap.get(path).get() != null)
      return scaledCacheMap.get(path).get();

    File imageFile = new File(path);
    if (imageFile.exists() == false) {
      if (!path.equals(Prefs.defaultErrorPath))
        return getImg(Prefs.defaultErrorPath);
      else
        return null;
    }

    BufferedImage tmpImage = null;
    try {tmpImage = ImageIO.read(imageFile);} catch (IOException e) {}

    if (tmpImage != null) {
      if (tmpImage.getWidth() > tmpImage.getHeight()) {
        tmpImage = getScaledInstance(tmpImage, Prefs.defaultCardHeight, Prefs.defaultCardWidth, true);
        tmpImage = getRotatedInstance(tmpImage, -Math.PI/2);
      }
      else tmpImage = getScaledInstance(tmpImage, Prefs.defaultCardWidth, Prefs.defaultCardHeight, true);

      scaledCacheMap.put(path, new WeakReference<BufferedImage>(tmpImage));
      return tmpImage;
    }
    else {
      return getImg(Prefs.defaultErrorPath);
    }
  }


  /**
   * Rotates an image.
   * This is not cached.
   *
   * @param img an image
   * @param angle amount in radians (+ is clockwise)
   * @return a rotated version
   */
  public BufferedImage getRotatedInstance(BufferedImage img, double angle) {
    int w = img.getWidth();
    int h = img.getHeight();

    AffineTransform tx = new AffineTransform();
      tx.rotate(angle);
      tx.preConcatenate(getPositiveTranslation(tx, img));

    Dimension newSize = getTransformedSize(tx, img);
    int transparency = img.getColorModel().getTransparency();
    if (Math.abs(newSize.width-img.getWidth()) < 1 && Math.abs(newSize.height-img.getHeight()) < 1) {
      // 180 degrees
    } else if (Math.abs(newSize.height-img.getWidth()) < 1 && Math.abs(newSize.width-img.getHeight()) < 1) {
      // 90/270 degrees
    } else {
      // weird angle
      transparency = Transparency.TRANSLUCENT;
    }

    GraphicsConfiguration gc = img.createGraphics().getDeviceConfiguration();
    BufferedImage tmp = gc.createCompatibleImage(newSize.width, newSize.height, transparency);
    Graphics2D g2 = tmp.createGraphics();
    g2.setComposite(AlphaComposite.Src);
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.drawImage(img, tx, null);
    g2.dispose();

    return tmp;
  }

  /**
   * Get a transform to give pixels positive coords.
   * PreConcatenate() this onto a rotation to slide
   * everything down/right.
   *
   * @param tx a rotation transform to compensate for
   * @param img the image to be rotated
   * @return a transform to make the most negative x/y zero
   */
  private AffineTransform getPositiveTranslation(AffineTransform tx, BufferedImage img) {
    double lowestX = 0; double lowestY = 0;
    Point2D p2din, p2dout;

    p2din = new Point2D.Double(0.0, 0.0);
    p2dout = tx.transform(p2din, null);
    lowestX = Math.min(p2dout.getX(), lowestX);
    lowestY = Math.min(p2dout.getY(), lowestY);

    p2din = new Point2D.Double(img.getWidth(), 0.0);
    p2dout = tx.transform(p2din, null);
    lowestX = Math.min(p2dout.getX(), lowestX);
    lowestY = Math.min(p2dout.getY(), lowestY);

    p2din = new Point2D.Double(img.getWidth(), img.getHeight());
    p2dout = tx.transform(p2din, null);
    lowestX = Math.min(p2dout.getX(), lowestX);
    lowestY = Math.min(p2dout.getY(), lowestY);

    p2din = new Point2D.Double(0, img.getHeight());
    p2dout = tx.transform(p2din, null);
    lowestX = Math.min(p2dout.getX(), lowestX);
    lowestY = Math.min(p2dout.getY(), lowestY);

    AffineTransform tat = new AffineTransform();
    tat.translate(-lowestX, -lowestY);
    return tat;
  }

  /**
   * Determine the new size of a transformed image.
   *
   * @param tx the transform
   * @param img the original image
   * @return the new size
   */
  private Dimension getTransformedSize(AffineTransform tx, BufferedImage img) {
    double highestX = 0; double highestY = 0;
    Point2D p2din, p2dout;

    p2din = new Point2D.Double(0.0, 0.0);
    p2dout = tx.transform(p2din, null);
    highestX = Math.max(p2dout.getX(), highestX);
    highestY = Math.max(p2dout.getY(), highestY);

    p2din = new Point2D.Double(img.getWidth(), 0.0);
    p2dout = tx.transform(p2din, null);
    highestX = Math.max(p2dout.getX(), highestX);
    highestY = Math.max(p2dout.getY(), highestY);

    p2din = new Point2D.Double(img.getWidth(), img.getHeight());
    p2dout = tx.transform(p2din, null);
    highestX = Math.max(p2dout.getX(), highestX);
    highestY = Math.max(p2dout.getY(), highestY);

    p2din = new Point2D.Double(0, img.getHeight());
    p2dout = tx.transform(p2din, null);
    highestX = Math.max(p2dout.getX(), highestX);
    highestY = Math.max(p2dout.getY(), highestY);

    return new Dimension ((int)highestX, (int)highestY);
  }


  public BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, boolean higherQuality) {
    //int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
    BufferedImage ret = (BufferedImage)img;

    int w, h;
    if (higherQuality) {
      // Use multi-step technique: start with original size, then
      // scale down in multiple passes with drawImage()
      // until the target size is reached
      w = img.getWidth();
      h = img.getHeight();
    } else {
      // Use one-step technique: scale directly from original
      // size to target size with a single drawImage() call
      w = targetWidth;
      h = targetHeight;
    }

    do {
      if (higherQuality) {
        if (w > targetWidth) {
          w /= 2;
          if (w < targetWidth) w = targetWidth;
        }
        else if (w < targetWidth) {
          w *= 2;
          if (w > targetWidth) w = targetWidth;
        }

        if (h > targetHeight) {
          h /= 2;
          if (h < targetHeight) h = targetHeight;
        }
        else if (h < targetHeight) {
          h *= 2;
          if (h > targetHeight) h = targetHeight;
        }
      }
      //Slow
      //GraphicsConfiguration gc = ret.createGraphics().getDeviceConfiguration();
      //BufferedImage tmp = gc.createCompatibleImage(w, h, ret.getColorModel().getTransparency());
      //
      BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2 = tmp.createGraphics();
      g2.setComposite(AlphaComposite.Src);
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2.drawImage(ret, 0, 0, w, h, null);
      g2.dispose();

      ret = tmp;
    } while (w != targetWidth || h != targetHeight);

    return ret;
  }


/*
  // This method incurs Toolkit's caching (via ImageIcon(path))

  // <BR><BR>ImageIcon calls the following method, which does background caching:
  // <BR><UL>Toolkit.getDefaultToolkit()).getImage(urlString);</UL>

  public ImageIcon getImgOldWay(String path) {
    if (scaledCacheMap.containsKey(path) && scaledCacheMap.get(path).get() != null)
      return scaledCacheMap.get(path).get();

    if (new File(path).exists() == false)
      path = Prefs.defaultErrorPath;
                                                             //In case there are a lot of errors...
    if (scaledCacheMap.containsKey(path) && scaledCacheMap.get(path).get() != null)
      return scaledCacheMap.get(path).get();

    ImageIcon tmpIcon = null;
    Image tmpImage = null;
      tmpIcon = new ImageIcon(path);
      tmpImage = tmpIcon.getImage();
      if ( tmpIcon.getIconWidth() > tmpIcon.getIconHeight() ) {
        tmpImage = tmpImage.getScaledInstance(Prefs.defaultCardHeight, Prefs.defaultCardWidth, Prefs.cardScaleMethod);
        ImageFilter rotIt = new RotateFilter((float)Math.toRadians(90), true);
        tmpImage = new JPanel().createImage(new FilteredImageSource(tmpImage.getSource(), rotIt));
      }
      else
        tmpImage = tmpImage.getScaledInstance(Prefs.defaultCardWidth, Prefs.defaultCardHeight, Prefs.cardScaleMethod);
      tmpIcon = new ImageIcon(tmpImage);
      scaledCacheMap.put(path, new WeakReference<ImageIcon>(tmpIcon));
    return tmpIcon;
  }
*/
}
