package org.arcanist.client;

import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;


/**
 * A popup help window.
 */
public class HelpWindow extends JInternalFrame {

  private ArcanistCCGFrame frame = null;

  private JPanel pane = new JPanel();


  public HelpWindow(ArcanistCCGFrame f) {
    super("Help",
      true, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);

    frame = f;

    int width = 390, height = 235;

    StringBuffer helpBuf = new StringBuffer();
    helpBuf.append(" ArcanistCCG Help\n");
    helpBuf.append("\n JumboView\n");
    helpBuf.append("  -Right-click a tab for extra options.\n");
    helpBuf.append("\n DeckFile Construction\n");
    helpBuf.append("  -Elements are separated by tabs.\n");
    helpBuf.append("  -Elements in brackets are optional.\n");
    helpBuf.append("    -[#] Name [Set] [FaceFile] [BackName [BackFile]]\n");
    helpBuf.append("    -// [Comment]\n");
    helpBuf.append("    -; [SideDeckComment]\n");
    helpBuf.append("    -> BackImageOverridePath (Relative to GameLocation)\n");
    helpBuf.append("    -< (Revert to game's default BackImage)\n");
    helpBuf.append("  -Never use the vertical bar \"|\" (pipe) character!\n");
    helpBuf.append("\n DeckBuilder\n");
    helpBuf.append("  -The dat file determines fields.\n");
    helpBuf.append("    -If using an Apprentice dat, there's a color filter.\n");
    helpBuf.append("      -Use X with others for specific multi-color costs.\n");
    helpBuf.append("  -The button next to each search field toggles search methods.\n");
    helpBuf.append("    -Basic searches are case-insensitive.\n");
    helpBuf.append("      -Keywords are separated by spaces.\n");
    helpBuf.append("        -Keywords with minuses are excluded.\n");
    helpBuf.append("      -Keyphrases are quoted.\n");
    helpBuf.append("    -Regex searches use regular expressions (advanced).\n");
    helpBuf.append("    -Enter starts searching.\n");
    helpBuf.append("  -While the cardList has focus, typing jumps through it.\n");
    helpBuf.append("    -Left-clicking or right-clicking shows the current pic/txt.\n");
    helpBuf.append("    -Enter/Left or Right keys also show the current pic/txt.\n");
    helpBuf.append("  -DeckTable changes aren't final until the cell is deselected.\n");
    helpBuf.append("    -If you have side decks, hover over the deck count for details.\n");
    helpBuf.append("  -Editing cards is dangerous!\n");
    helpBuf.append("    -Nothing is permanent until the dat is saved.\n");
    helpBuf.append("    -New adds a new card.\n");
    helpBuf.append("      -If there's a reprint from the same set, add's cancelled.\n");
    helpBuf.append("      -If the set abbrev isn't in Expan.dat, it's cancelled.\n");
    helpBuf.append("    -Delete removes text from selected expansion of card at left.\n");
    helpBuf.append("\n Objects\n");
    helpBuf.append("  -Resizing the table pads the outer edges.\n");
    helpBuf.append("     -Shrinking the table may cause item disappearance.\n");
    helpBuf.append("        -If so, just enlarge the table.\n");
    helpBuf.append("  -New items appear in the view's upper-left corner.\n");
    helpBuf.append("  -Cards\n");
    helpBuf.append("    -To rotate, drag a corner outward.\n");
    helpBuf.append("    -In NetPlay games, Local Flip doesn't flip for opponents.\n");
    helpBuf.append("      -Use it for cards only you can see, like your hand.\n");
    helpBuf.append("  -Decks\n");
    helpBuf.append("    -Click a card in a DeckList to draw that card.\n");
    helpBuf.append("    -Discard Piles are just empty decks.\n");
    helpBuf.append("    -If facedown & AutoFaced, drawn cards are flipped locally.\n");
    helpBuf.append("      -This can be suppressed by holding shift when drawing.\n");
    helpBuf.append("    -If you keep inadvertantly moving the deck, lock it.\n");
    helpBuf.append("    -Deck 'Add to Bottom' puts new cards under the deck.\n");
    helpBuf.append("      -Shift-Dragging a card or deck onto a deck adds to bottom.\n");
    helpBuf.append("    -Deck 'Card Offset' determines where new cards appear.\n");
    helpBuf.append("    -Stack a deck onto another by dragging.\n");
    helpBuf.append("      -The dragged deck goes onto the first deck it lands on.\n");
    helpBuf.append("  -Hands\n");
    helpBuf.append("    -Right-click the title bar for a popup menu.\n");
    helpBuf.append("    -Hands are initially locally revealed to their creators.\n");
    helpBuf.append("    -To enable draw/discard, click '...'.\n");
    helpBuf.append("    -Cards in the list can be rearranged by dragging.\n");
    helpBuf.append("    -Cards dragged onto the table are remote flipped.\n");
    helpBuf.append("      -This can be suppressed by holding shift when dragging.\n");
    helpBuf.append("    -Hover causes hands to move with the table when scrolling.\n");
    helpBuf.append("  -FloatingNotes.\n");
    helpBuf.append("    -Click once to edit; drag to move.\n");
    helpBuf.append("  -Groups.\n");
    helpBuf.append("    -Shift+Drag across the table to create a group.\n");
    helpBuf.append("    -The MiniMap can make groups that way too.\n");
    helpBuf.append("    -Right-click the group for more options.\n");
    helpBuf.append("    -A group checks top-left corners for contained objects.\n");
    helpBuf.append("  -There is no cure for 'Remove'.\n");
    helpBuf.append("    -If you 'Remove' a deck or hand, all cards in it go.\n");
    helpBuf.append("\n Other Windows\n");
    helpBuf.append("  -Timers\n");
    helpBuf.append("    -Timers count up by default.\n");
    helpBuf.append("    -If you set a start time, it will count down.\n");
    helpBuf.append("      -At zero, a message will appear in the chat.\n");


    pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

    JTextArea HelpArea = new JTextArea(helpBuf.toString(), 11, 31);
      HelpArea.setEditable(false);
    JScrollPane HelpScrollPane = new JScrollPane(HelpArea);
      pane.add(HelpScrollPane);

    this.setContentPane(pane);
    frame.getDesktop().add(this);

    if (frame.getDesktop().getSize().width/2-width/2 > 0 && frame.getDesktop().getSize().height/2-height/2 > 0)
      this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();

  }
}