package org.arcanist.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * The JumboView.
 * This is a tabbed frame with multiple tools.
 */
public class JumboView extends JInternalFrame implements DockableParent, ActionListener {

  private static final int BLANK = -1;
  private static final int ERRORTEXT = 0;
  private static final int CARDIMAGE = 1;
  private static final int CARDTEXT = 2;
  private static final int CARDTEXT_EDITING = 3;

  private ArcanistCCGFrame frame = null;

  private JTabbedPane pane = new JTabbedPane(JTabbedPane.BOTTOM);
  private JPanel jumboPanel = new JPanel();
  private boolean preferTxt = false;
  public JPanel chatContainerPanel;

  private List<DockableChild> dockableChildren = new ArrayList<DockableChild>(1);
  private CardImagePanel imagePanel = new CardImagePanel();
  private MiniMap minimap = null;
  private int jumboStatus = BLANK;

  // *Grumble* 1.5 denies requestFocusInWindow for tabs mid-switch
  private Runnable chatFieldFocuser = new Runnable() {
    @Override
    public void run() {
      frame.getChatPanel().getInputField().requestFocusInWindow();
    }
  };


  private JRadioButtonMenuItem jumboPicMenuItem = new JRadioButtonMenuItem("Pic", true);
  private JRadioButtonMenuItem jumboTextMenuItem = new JRadioButtonMenuItem("Text");
  private JMenuItem chatCounterQueryMenuItem = new JMenuItem("List Counters");
  private JMenuItem chatAliasMenuItem = new JMenuItem("Set Your Alias...");
  private JMenuItem chatUndockMenuItem = new JMenuItem("Undock");
  private JMenuItem chatClearMenuItem = new JMenuItem("Clear");
  private JMenuItem chatSaveMenuItem = new JMenuItem("Save...");


  public JumboView(ArcanistCCGFrame f) {
    super("Jumbo Card View",
      true, //resizable
      true, //closable
      true, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);

    frame = f;

    int width = 325, height = 509;

    frame.getAddJumboMenuItem().setEnabled(false);

    minimap = new MiniMap(frame);


    this.addInternalFrameListener(new InternalFrameAdapter() {  //Enable the JumboMenuItem on close
      @Override
      public void internalFrameClosing(InternalFrameEvent e) {
        chatContainerPanel.remove(frame.getChatPanel());
        minimap.stopRepainter();
        frame.getAddJumboMenuItem().setEnabled(true);
        frame.setJumboFrame(null);
      }
    });

    pane.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        //Negate chatAlert() color change
        if (pane.getSelectedIndex() == pane.indexOfTab("Chat") && pane.indexOfTab("Chat") != -1) {
          pane.setBackgroundAt(pane.indexOfTab("Chat"), null);
          SwingUtilities.invokeLater(chatFieldFocuser);
        }

        //Hide/show the Jumbo <>F buttons
        if (pane.getSelectedIndex() == pane.indexOfTab("Jumbo") && pane.indexOfTab("Jumbo") != -1) {
          if (getJumboStatus() == CARDIMAGE) imagePanel.setToolboxVisible(true);
        } else {
          imagePanel.setToolboxVisible(false);
        }

        if (pane.getSelectedIndex() == pane.indexOfTab("MiniMap") && pane.indexOfTab("MiniMap") != -1) {
          minimap.startRepainter();
        } else {
          minimap.stopRepainter();                           //No need to force repaints
        }
      }
    });


    chatContainerPanel = new JPanel();
      chatContainerPanel.setLayout(new BorderLayout());


    //Declare menus
    final JPopupMenu jumboPopup = new JPopupMenu();
      ButtonGroup jumboGroup = new ButtonGroup();
        // jumboPicMenuItem
          jumboGroup.add(jumboPicMenuItem);
          jumboPopup.add(jumboPicMenuItem);
        // jumboTextMenuItem
          jumboGroup.add(jumboTextMenuItem);
          jumboPopup.add(jumboTextMenuItem);
    final JPopupMenu chatPopup = new JPopupMenu();
      // chatCounterQueryMenuItem
        chatPopup.add(chatCounterQueryMenuItem);
      // chatAliasMenuItem
        chatPopup.add(chatAliasMenuItem);
      // chatUndockMenuItem
        chatPopup.add(chatUndockMenuItem);
      // chatClearMenuItem
        chatPopup.add(chatClearMenuItem);
      // chatSaveMenuItem
        chatPopup.add(chatSaveMenuItem);


    jumboTextMenuItem.addActionListener(this);
    jumboPicMenuItem.addActionListener(this);
    chatCounterQueryMenuItem.addActionListener(this);
    chatAliasMenuItem.addActionListener(this);
    chatUndockMenuItem.addActionListener(this);
    chatClearMenuItem.addActionListener(this);
    chatSaveMenuItem.addActionListener(this);


    pane.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // see if the click was in one of tabs
        int tabcount = pane.getTabCount();
        for(int i=0; i < tabcount; i++) {
          javax.swing.plaf.TabbedPaneUI tpu = pane.getUI();
          Rectangle rect = tpu.getTabBounds(pane, i);
          int x = e.getX(); int y = e.getY();
          if (x < rect.x  ||  x > rect.x+rect.width  ||  y < rect.y  ||  y > rect.y+rect.height)
            continue;

          //pane.setSelectedIndex(i);                          //Technically optional
                                                             //setSelected trumps isEnabled()
                                                             //uncomment only if getSelected us used
          if (e.getButton() == 1) {
            if (i == pane.indexOfTab("Save Card")) {
              //The "Save Changes" button was clicked
              if (jumboPanel.getComponentCount()>0 && jumboPanel.getComponent(0).getClass().getName().equals("CardTextPanel") ) {
                ((CardTextPanel)jumboPanel.getComponent(0)).save(Prefs.textDat);
              }

              jumboPanel.removeAll();
              pane.remove(pane.indexOfTab("Save Card"));
              pane.setSelectedIndex(pane.indexOfTab("Jumbo"));
            }
          }

          //Right-Click Popup menu
          //  Technically This should be in both pressed and released
          //  Mac checks the trigger on press, Win on release
          //  But only macs have 1-button mice, which need the trigger check ;)
          if (e.getButton() == 3 || e.isPopupTrigger()) {
            if (i == pane.indexOfTab("Jumbo") && pane.isEnabledAt(i))
              jumboPopup.show(e.getComponent(), e.getX(), e.getY());
            else if (i == pane.indexOfTab("Chat") && pane.isEnabledAt(i))
              chatPopup.show(e.getComponent(), e.getX(), e.getY());
          }
          break;
        }
      }
    });
    //End Menus

    pane.add(jumboPanel, "Jumbo");
    pane.add(chatContainerPanel, "Chat");
    pane.add(minimap, "MiniMap");

    //This had to occur after the tab adding...
    if (frame.getChatPanel() == null)
      frame.setChatPanel(new ChatPanel());
    if (frame.getChatPanel().getParent() == null)
      addDockableChild(frame.getChatPanel());
    else
      pane.setEnabledAt(pane.indexOfTab("Chat"), false);

    this.setContentPane(pane);
    frame.getDesktop().add(this);
    int x = frame.getDesktop().getSize().width-width;
      if (x < 0) x=0;
    this.reshape(x, 0, width, height);
    this.show();

    imagePanel.initialize(this);
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == jumboTextMenuItem) {
      setPreferText(true);
    }
    else if (source == jumboPicMenuItem) {
      setPreferText(false);
    }
    else if (source == chatCounterQueryMenuItem) {
      ArcanistCCG.NetManager.chatCounterQuery();
    }
    else if (source == chatAliasMenuItem) {
      String prevAlias = Prefs.playerAlias;
      String reqAlias = null;
      String userinput = "";
      while (true) {
        userinput = JOptionPane.showInternalInputDialog(frame.getDesktop(), "Your new alias can be up to 20 characters including \"a-zA-Z0-9 ,-_'.\"\nIf blank, [P#] will be used.", "Set Your Alias", JOptionPane.PLAIN_MESSAGE);
        if (userinput == null)
          break;
        else if (userinput.length() == 0) {
          reqAlias = "";
          break;
        }
        else {
          try {
            if (Prefs.isValidPlayerAlias(userinput)) {
              reqAlias = userinput;
              break;
            }
          }
          catch (IllegalArgumentException f) {/* invalid alias */}
        }
      }
      if (reqAlias != null && prevAlias.equals(reqAlias) == false) {
        ArcanistCCG.NetManager.serverRequestAlias(reqAlias);
      }
    }
    else if (source == chatUndockMenuItem) {
      pane.setSelectedIndex(pane.indexOfTab("Jumbo"));
      pane.setEnabledAt(pane.indexOfTab("Chat"), false);
      removeDockableChild(frame.getChatPanel());

      new ChatFrame(frame);
    }
    else if (source == chatClearMenuItem) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Really clear the chat?", "Clear the Chat?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        frame.getChatPanel().clear();
      }
    }
    else if (source == chatSaveMenuItem) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Html Files (*.html)", new String[] {".html"});
      java.io.File file = frame.fileChooser(JFileChooser.SAVE_DIALOG, Prefs.homePath, filter);
      if (file == null) return;

      frame.getChatPanel().save(file);
    }
  }


  /**
   * Get the DockableChild objects attached to this.
   *
   * @return an array of children
   * @see DockableParent#getDockableChildren()
   */
  @Override
  public DockableChild[] getDockableChildren() {
    return dockableChildren.toArray(new DockableChild[dockableChildren.size()]);
  }

  /**
   * Determines whether a DockableChild is present here.
   *
   * @see DockableParent#hasDockableChild(DockableChild)
   */
  @Override
  public boolean hasDockableChild(DockableChild c) {
    return dockableChildren.contains(c);
  }

  /**
   * Test if a given DockableChild is allowed here.
   * It cannot already be present, and must be a ChatPanel.
   * And there cannot already be a ChatPanel.
   *
   * @return true if it can be added, false otherwise
   * @see DockableParent#isDockableChildValid(DockableChild)
   */
  @Override
  public boolean isDockableChildValid(DockableChild c) {
    if (c == null || dockableChildren.contains(c)) return false;
    if ((c instanceof JComponent) == false) return false;

    if (c.getClass() == ChatPanel.class) {
      for (int i=0; i < dockableChildren.size(); i++) {
        if (dockableChildren.get(i).getClass() == ChatPanel.class) return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Embed a DockableChild into this.
   * If it is a ChatPanel, it will be added within the Chat tab, which will be enabled.
   *
   * @throws IllegalArgumentException if the child is invalid
   * @see JumboView#isDockableChildValid(DockableChild)
   * @see DockableParent#addDockableChild(DockableChild)
   */
  @Override
  public void addDockableChild(DockableChild c) {
    if (!isDockableChildValid(c)) throw new IllegalArgumentException("Attempted to add an invalid DockableChild");

    if (c.getClass() == ChatPanel.class) {
      dockableChildren.add(c);
      chatContainerPanel.add((JComponent)c);
      chatContainerPanel.revalidate();
      chatContainerPanel.repaint();
      pane.setEnabledAt(pane.indexOfTab("Chat"), true);
    }
  }

  /**
   * Remove a DockableChild from this.
   *
   * @see DockableParent#removeDockableChild(DockableChild)
   */
  @Override
  public void removeDockableChild(DockableChild c) {
    if (c == null || !dockableChildren.contains(c)) return;

    if (c.getClass() == ChatPanel.class) {
      dockableChildren.remove(c);
      chatContainerPanel.remove((JComponent)c);
      chatContainerPanel.revalidate();
      chatContainerPanel.repaint();
      pane.setEnabledAt(pane.indexOfTab("Chat"), false);
      pane.setSelectedIndex(pane.indexOfTab("Jumbo"));
    }
  }


  /**
   * Sets whether the JumboView is set to show text.
   */
  public void setPreferText(boolean b) {preferTxt = b;}

  /**
   * Gets whether the JumboView is set to show text.
   */
  public boolean getPreferText() {return preferTxt;}


  /**
   * Select the Chat tab.
   * A docked ChatPanel will call this to show itself.
   *
   * @see ChatPanel#getInputField()
   */
  public void switchToChatTab() {
    int chatIndex = pane.indexOfTab("Chat");
    if (pane.isEnabledAt(chatIndex)) {
      this.requestFocusInWindow();
      if (pane.getSelectedIndex() != chatIndex) {
        pane.setSelectedIndex(chatIndex);
      } else {
        frame.getChatPanel().getInputField().requestFocusInWindow();
      }
    }
  }

  /**
   * Change the Chat tab color.
   */
  public void chatAlert() {
    int chatIndex = pane.indexOfTab("Chat");
    if (pane.getSelectedIndex() != chatIndex && pane.isEnabledAt(chatIndex))
      pane.setBackgroundAt(chatIndex, Color.yellow);
  }


  /**
   * Check if Jumbo tab is selected.
   */

  public boolean isJumboActive() {
    return (pane.getSelectedIndex() == 0);
  }


  /**
   * Find out what JumboView contains.
   *
   * @return BLANK, ERRORTEXT, IMAGE, CARDTEXT, CARDTEXT_EDITING
   */
  public int getJumboStatus() {
    return jumboStatus;
  }

  /**
   * Change the JumboView's contents.
   */
  public void updateJumboText(CardTextPanel cardPanel) {
    updateJumbo(cardPanel);
    if (cardPanel.isEditable() && pane.indexOfTab("Save Card") == -1) {
      pane.add("Save Card", null);
      jumboStatus = CARDTEXT_EDITING;
    } else {
      jumboStatus = CARDTEXT;
    }
  }


  /**
   * Change the JumboView's contents.
   */
  public void updateJumboImage(String path) {
    boolean result = imagePanel.setImagePath(path);
    if (result == true) {
      updateJumbo(imagePanel);
      jumboStatus = CARDIMAGE;
      imagePanel.setToolboxVisible(true);
    } else {
      String debugText = "";
      debugText += "Image not available:\n"+ path +"\n\n";
      debugText += "Either its Set or its ImageFile does not exist.\n";
      debugText += "This most often occurs when someone names set directories incorrectly, so they don't match.\n\n";
      debugText += "When naming dirs under .../GameName/...\n";
      debugText += "  -Remove spaces and punctuation.\n";
      debugText += "  -Capitalize each word.\n\n";
      debugText += "The deck this card is from will need editing too.\n";
      debugText += "Open it with any text editor to do a search/replace.\n\n";

      debugText += "If everyone's dirs are fine, the image may be corrupt.";
      JTextArea debugArea = new JTextArea(debugText, 10, 26);
        debugArea.setEditable(false);
        debugArea.setLineWrap(true);
        debugArea.setWrapStyleWord(true);
      updateJumbo(debugArea);
      jumboStatus = ERRORTEXT;
    }
  }


  /**
   * Repaint the JumboView.
   */
  public void jumboRefresh() {
    jumboPanel.revalidate();
    jumboPanel.repaint();
  }


  /**
   * Change the JumboView's contents.
   */
  public void updateJumbo(JComponent cardPanel) {
    clearJumbo();
    jumboPanel.add(cardPanel);
    jumboRefresh();
  }


  /**
   * Change the JumboView's contents.
   */
  public void clearJumbo() {
    jumboPanel.removeAll();
    if (pane.indexOfTab("Save Card") != -1) {
      pane.remove(pane.indexOfTab("Save Card"));
    }
    imagePanel.setToolboxVisible(false);
    jumboStatus = BLANK;
  }


  /**
   * Checks whether a card is currently being shown.
   */
  public boolean isShownInJumbo(String imagePath, String textName, String textExpansion) {
    if (imagePath != null && getJumboStatus() == CARDIMAGE) {
      if (imagePath.equals(imagePanel.getImagePath())) return true;
    }
    else if (textName != null && textExpansion != null && getJumboStatus() == CARDTEXT) {
      CardTextPanel ctp = (CardTextPanel)jumboPanel.getComponent(0);
      if (textName.equals(ctp.getName()) && textExpansion.equals(ctp.getSetAbbrev())) return true;
    }
    return false;
  }


  /**
   * Returns the current size of the content panel.
   */
  public Dimension getContentPanelSize() {return jumboPanel.getSize();}
}
