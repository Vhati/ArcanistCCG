package org.arcanist.client;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A popup for Tokens.
 */
public class TokensFrame extends NerfableInternalFrame implements ActionListener {

  private ArcanistCCGFrame frame = null;

  private Map<JButton,String> tokenMap = new HashMap<JButton,String>();


  /**
   * Constructs and shows the window.
   */
  public TokensFrame(ArcanistCCGFrame f) {
    super("Tokens",
      true, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);
    if (Prefs.usePaletteFrames) this.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);

    frame = f;

    JPanel pane = new JPanel();

    File[] dirListing = new File(Prefs.tokensPath).listFiles();
    for (int i=0; i < dirListing.length; i++) {
      if (dirListing[i].isFile()) {
        JButton tmpBtn = new JButton(new ImageIcon(Prefs.tokensPath + dirListing[i].getName()));
          tmpBtn.setBorder(null);
          tmpBtn.addActionListener(this);
          tokenMap.put(tmpBtn, dirListing[i].getName());
          pane.add(tmpBtn);
      }
    }

    this.setContentPane(pane);
    pane.setPreferredSize(new Dimension(pane.getPreferredSize().width/2, pane.getPreferredSize().height*2-4));
    this.pack();
    frame.getDesktop().add(this);

    int width = this.getWidth();
    int height = this.getHeight();
    if (frame.getDesktop().getSize().width/2-width/2 > 0 && frame.getDesktop().getSize().height/2-height/2 > 0)
      this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    String fileName = tokenMap.get(source);
    if (fileName != null) {
      Rectangle tableView = frame.getTableView();
      ArcanistCCG.NetManager.tokenAdd(tableView.x, tableView.y, fileName);
    }
  }
}
