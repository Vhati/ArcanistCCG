package org.arcanist.client;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;


/**
 * This is the image jumbo view.
 */
public class CardImagePanel extends JLabel implements ActionListener {
  private CardImagePanel pronoun = this;
  private JPanel imageToolboxPanel = new JPanel();
  private JButton imgLBtn = null;
  private JButton imgRBtn = null;
  private JButton imgFitBtn = null;
  private String path = "";
  private int rotation = 0;
  private BufferedImage originalImage = null;
  private BufferedImage currentImage = null;


  /**
   * Constructs a new CardImagePanel.
   * Technically it is a label, but that may change someday.
   */
  public CardImagePanel() {
  }

  /**
   * Constructs the image controls and adds listeners.
   * @param parent the parent frame
   */
  public void initialize(JInternalFrame parent) {
    imageToolboxPanel.setLayout(new BoxLayout(imageToolboxPanel, BoxLayout.X_AXIS));
    imgLBtn = new JButton("<");
      imgLBtn.setMargin(new Insets(1,0,1,0));
      imageToolboxPanel.add(imgLBtn);
    imgRBtn = new JButton(">");
      imgRBtn.setMargin(new Insets(1,0,1,0));
      imageToolboxPanel.add(imgRBtn);
    imgFitBtn = new JButton("F");
      imgFitBtn.setMargin(new Insets(1,0,1,0));
      imageToolboxPanel.add(imgFitBtn);
    int imageToolboxPanelW = imageToolboxPanel.getPreferredSize().width;
    int imageToolboxPanelH = imageToolboxPanel.getPreferredSize().height;
    int imageToolboxPanelX = parent.getSize().width - imageToolboxPanelW - 30;
    int imageToolboxPanelY = parent.getSize().height - imageToolboxPanelH - 32;
    imageToolboxPanel.setBounds(imageToolboxPanelX,imageToolboxPanelY, imageToolboxPanelW, imageToolboxPanelH);
    imageToolboxPanel.setVisible(false);
    parent.getLayeredPane().add(imageToolboxPanel, JLayeredPane.PALETTE_LAYER, 0);

    parent.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        int imageToolboxPanelW = imageToolboxPanel.getPreferredSize().width;
        int imageToolboxPanelH = imageToolboxPanel.getPreferredSize().height;
        int imageToolboxPanelX = ((Component)e.getSource()).getSize().width - imageToolboxPanelW - 30;
        int imageToolboxPanelY = ((Component)e.getSource()).getSize().height - imageToolboxPanelH - 32;
        imageToolboxPanel.setBounds(imageToolboxPanelX,imageToolboxPanelY, imageToolboxPanelW, imageToolboxPanelH);
      }
    });

    imgLBtn.addActionListener(this);
    imgRBtn.addActionListener(this);
    imgFitBtn.addActionListener(this);
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == imgLBtn) {
      rotImage(90);
    }
    else if (source == imgRBtn) {
      rotImage(-90);
    }
    else if (source == imgFitBtn) {
      fitImage();
    }
  }


  private  void rotImage(int degrees) {
    rotation = rotation + degrees;
    if (rotation <= -360) rotation += 360;
    double angle = Math.toRadians(rotation);

    if (angle != 0) {
      currentImage = Prefs.Cache.getRotatedInstance(originalImage, -angle);
    }
    else {
      currentImage = originalImage;
    }
    this.setIcon(new ImageIcon(currentImage));
  }


  private void fitImage() {
    if (this.getParent() == null) return;
    int pW = this.getParent().getWidth();
    int pH = this.getParent().getHeight();
    int cW = currentImage.getWidth();
    int cH = currentImage.getHeight();
    int dW=0, dH=0;
    if (cH > cW) {
      dH = pH;
      dW = (int)((float)pH/(float)cH*cW);
    } else {
      dW = pW;
      dH = (int)((float)pW/(float)cW*cH);
    }

    currentImage = Prefs.Cache.getScaledInstance(currentImage, dW, dH, true);
    this.setIcon(new ImageIcon(currentImage));
  }


  /**
   * Sets the image path.
   */
  public boolean setImagePath(String input) {
    path = input;
    rotation = 0;
    originalImage = Prefs.Cache.getCachedOriginalImage(path);
    currentImage = originalImage;
    if (currentImage != null) {
      this.setIcon(new ImageIcon(currentImage));
      return true;
    }
    return false;
  }

  /**
   * Gets the current image path.
   *
   * @return the path or a blank String
   */
  public String getImagePath() {return path;}


  /**
   * Shows or hides the image controls.
   *
   * @param b if true, shows the controls, otherwise, hides the controls
   */
  public void setToolboxVisible(boolean b) {
    imageToolboxPanel.setVisible(b);
  }
}