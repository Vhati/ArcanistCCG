package org.arcanist.client;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A GUI for designing text layouts.
 * A region will be given a preferred size to
 * match the current JumboView content, or a
 * similar default if the JumboView isn't open.
 */
public class PrefsLayoutPanel extends JPanel implements ActionListener {

  private ArcanistCCGFrame frame = null;
  private List<int[]> textArray = null;

  private int width = 325;
  private int height = 485;
  private JPanel previewPanel = new JPanel();

  JButton editGameBtn = null;
  JButton addBtn = null;
  JButton clearBtn = null;
  JButton importBtn = null;
  JButton exportBtn = null;


  public PrefsLayoutPanel(ArcanistCCGFrame f, List<int[]> textArray) {
    this.frame = f;
    this.textArray = textArray;

    GridBagLayout layoutGridbag = new GridBagLayout();
    GridBagConstraints layoutC = new GridBagConstraints();
      layoutC.fill = GridBagConstraints.BOTH;
      layoutC.weightx = 1.0;
      layoutC.weighty = 0;
      layoutC.gridwidth = GridBagConstraints.REMAINDER;  //End Row
    this.setLayout(layoutGridbag);

    JPanel layoutCtlPanel = new JPanel();
      editGameBtn = new JButton("Back to Games");
        editGameBtn.setMargin(new Insets(0,1,0,1));
        layoutCtlPanel.add(editGameBtn);
      addBtn = new JButton("Add Field");
        addBtn.setMargin(new Insets(0,1,0,1));
        layoutCtlPanel.add(addBtn);
      clearBtn = new JButton("Clear");
        clearBtn.setMargin(new Insets(0,1,0,1));
        layoutCtlPanel.add(clearBtn);
      importBtn = new JButton("Import");
        importBtn.setMargin(new Insets(0,1,0,1));
        layoutCtlPanel.add(importBtn);
      exportBtn = new JButton("Export");
        exportBtn.setMargin(new Insets(0,1,0,1));
        layoutCtlPanel.add(exportBtn);

    editGameBtn.addActionListener(this);
    addBtn.addActionListener(this);
    clearBtn.addActionListener(this);
    importBtn.addActionListener(this);
    exportBtn.addActionListener(this);


    previewPanel.setBorder(BorderFactory.createEtchedBorder());
    previewPanel.setLayout(null);

    for (int i=0; i < textArray.size(); i++) {
      previewPanel.add(new PrefsLayoutArea(i, textArray.get(i) ));
    }

    this.add(layoutCtlPanel, layoutC);
    layoutC.gridy = 1;
    layoutC.weighty = 1;
    this.add(previewPanel, layoutC);

    if (frame.getJumboFrame() != null) {
      Dimension jumboSize = frame.getJumboFrame().getContentPanelSize();
      previewPanel.setPreferredSize(new Dimension(jumboSize.width+10, jumboSize.height));
    } else {
      previewPanel.setPreferredSize(new Dimension(310+10, 448));
    }
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == editGameBtn) {
      if (JOptionPane.showInternalConfirmDialog(frame.getDesktop(), "Accept this new layout?", "Layout Changed", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        textArray.clear();
        for (int i=0; i < previewPanel.getComponentCount(); i++) {
          textArray.add(((PrefsLayoutArea)previewPanel.getComponent(i)).proposedArray);
        }
      }
      Container ancestorFrame = SwingUtilities.getAncestorOfClass(PrefsPathsFrame.class, this);
      if (ancestorFrame != null && ancestorFrame instanceof PrefsPathsFrame) ((PrefsPathsFrame)ancestorFrame).showPanel("games");
    }
    else if (source == addBtn) {
      previewPanel.add(new PrefsLayoutArea(previewPanel.getComponentCount(), new int[] {1,0,0,20,30}));
      previewPanel.revalidate();
      previewPanel.repaint();
    }
    else if (source == clearBtn) {
      previewPanel.removeAll();
      previewPanel.revalidate();
      previewPanel.repaint();
    }
    else if (source == importBtn) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = frame.fileChooser(JFileChooser.OPEN_DIALOG, Prefs.homePath, filter);
      if (file == null) return;

      try {
        previewPanel.removeAll();

        FileReader fr = new FileReader(file);
        BufferedReader inFile = new BufferedReader(fr);

        int first=0, second=0, third=0, fourth=0, fifth=0;
        String tempLine = inFile.readLine();
        for (int i=0; tempLine != null && tempLine.length() > 1; i++) {
          String[] tokens = tempLine.split(",");
          int t = 0;
          first=0; second=0; third=0; fourth=0; fifth=0;
          first = Integer.parseInt(tokens[t++]);
          second = Integer.parseInt(tokens[t++]);
          third = Integer.parseInt(tokens[t++]);
          fourth = Integer.parseInt(tokens[t++]);
          fifth = Integer.parseInt(tokens[t++]);
          previewPanel.add(new PrefsLayoutArea(i, new int[]{first,second,third,fourth,fifth} ));
          tempLine = inFile.readLine();
        }
      }
      catch (FileNotFoundException exception) {
        ArcanistCCG.LogManager.write(exception, "Couldn't import text layout.");
      }
      catch (IOException exception) {
        ArcanistCCG.LogManager.write(exception, "Couldn't import text layout.");
      }
      previewPanel.revalidate();
      previewPanel.repaint();
    }
    else if (source == exportBtn) {
      ExtensionFileFilter filter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
      File file = frame.fileChooser(JFileChooser.SAVE_DIALOG, Prefs.homePath, filter);
      if (file == null) return;

      try {
        FileWriter fw = new FileWriter(file);
        BufferedWriter outFile = new BufferedWriter(fw);

        for (int i=0; i < previewPanel.getComponentCount(); i++) {
          outFile.write( Integer.toString(((PrefsLayoutArea)previewPanel.getComponent(i)).proposedArray[0])+"," );
          outFile.write( Integer.toString(((PrefsLayoutArea)previewPanel.getComponent(i)).proposedArray[1])+"," );
          outFile.write( Integer.toString(((PrefsLayoutArea)previewPanel.getComponent(i)).proposedArray[2])+"," );
          outFile.write( Integer.toString(((PrefsLayoutArea)previewPanel.getComponent(i)).proposedArray[3])+"," );
          outFile.write( Integer.toString(((PrefsLayoutArea)previewPanel.getComponent(i)).proposedArray[4]) );
          outFile.newLine();
        }
        outFile.close();
      }
      catch (FileNotFoundException exception) {
        ArcanistCCG.LogManager.write(exception, "Couldn't export text layout.");
      }
      catch (IOException exception) {
        ArcanistCCG.LogManager.write(exception, "Couldn't export text layout.");
      }
    }
  }
}
