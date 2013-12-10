package org.arcanist.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.arcanist.client.*;
import org.arcanist.util.*;


/**
 * A Timer popup.
 */
public class TimerFrame extends JInternalFrame implements ActionListener {

  private ArcanistCCGFrame frame = null;

  private JPanel pane = new JPanel();

  private JTextField clockField = new JTextField("00:00:00");
  private JButton ctrlBtn = new JButton(">");

  private boolean running = false;
  private int countdown = -1;
  private int currentTime = 0;
  private long lastClickTime = 0;

  private TimerWorker worker = new TimerWorker();

  private JPanel opPanel = new JPanel();
  private JButton cfgBtn = new JButton("...");
  private JButton resetBtn = new JButton("Rst");
  private JPanel cfgPanel = new JPanel();
  private JButton opBtn = new JButton("<");
  private JTextField hourField = new JTextField(new RegexDocument("[0-5]?[0-9]?"), "00", 2);
  private JTextField minField = new JTextField(new RegexDocument("[0-5]?[0-9]?"), "00", 2);
  private JTextField secField = new JTextField(new RegexDocument("[0-5]?[0-9]?"), "00", 2);
  private JButton setBtn = new JButton("Set");


  /**
   * Constructs and shows the window.
   */
  public TimerFrame(ArcanistCCGFrame f) {
    super("Timer",
      false, //resizable
      true, //closable
      false, //maximizable
      false); //iconifiable
    this.setFrameIcon(null);
    if (Prefs.usePaletteFrames) this.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);

    frame = f;

    this.addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameClosing(InternalFrameEvent e) {
        stopTimer();
      }
    });

    // opPanel
      opPanel.setLayout(new BoxLayout(opPanel, BoxLayout.X_AXIS));
      // cfgBtn
        cfgBtn.setMargin(new Insets(1,2,1,2));
        opPanel.add(cfgBtn);
      // clockField
        clockField.setEditable(false);
        clockField.setHorizontalAlignment(JTextField.RIGHT);
        opPanel.add(clockField);
      // resetBtn
        resetBtn.setMargin(new Insets(1,2,1,2));
        opPanel.add(resetBtn);
      // ctrlBtn
        ctrlBtn.setMargin(new Insets(1,6,1,6));
        opPanel.add(ctrlBtn);

    // cfgPanel
      cfgPanel.setLayout(new BoxLayout(cfgPanel, BoxLayout.X_AXIS));
      // opBtn
        opBtn.setMargin(new Insets(1,2,1,2));
        cfgPanel.add(opBtn);
      // hourField
        hourField.setMargin(new Insets(1,2,1,2));
        cfgPanel.add(hourField);
      // minField
        minField.setMargin(new Insets(1,2,1,2));
        cfgPanel.add(minField);
      // secField
        secField.setMargin(new Insets(1,2,1,2));
        cfgPanel.add(secField);
      // setBtn
        setBtn.setMargin(new Insets(1,2,1,2));
        cfgPanel.add(setBtn);


    //Set up listeners
    FocusListener selectText = new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        ((JTextField)e.getSource()).selectAll();
      }
    };
    hourField.addFocusListener(selectText);
    minField.addFocusListener(selectText);
    secField.addFocusListener(selectText);

    cfgBtn.addActionListener(this);
    resetBtn.addActionListener(this);
    ctrlBtn.addActionListener(this);
    opBtn.addActionListener(this);
    setBtn.addActionListener(this);


    pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
    pane.add(opPanel);

    this.setContentPane(pane);
    pane.setPreferredSize(new Dimension(pane.getPreferredSize().width, pane.getPreferredSize().height-7));
    this.pack();
    frame.getDesktop().add(this);

    int width = this.getWidth();
    int height = this.getHeight();
    if (frame.getDesktop().getSize().width/2-width/2 > 0 && frame.getDesktop().getSize().height/2-height/2 > 0)
      this.reshape(frame.getDesktop().getSize().width/2-width/2, frame.getDesktop().getSize().height/2-height/2, width, height);
    else this.reshape(0, 0, width, height);

    this.show();

    ctrlBtn.requestFocusInWindow();
  }


  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == cfgBtn) {
      if (countdown == -1) {
        hourField.setText("00");
        minField.setText("00");
        secField.setText("00");
      } else {
        int h = countdown/60/60;
        int m = (countdown-(h*60*60)) / 60;
        int s = countdown - (h*60*60) - (m*60);
        hourField.setText((h<10?"0":"")+ h);
        minField.setText((m<10?"0":"")+ m);
        secField.setText((s<10?"0":"")+ s);
      }
      pane.removeAll();
      pane.add(cfgPanel);
      pane.revalidate();
      pane.repaint();
    }
    else if (source == resetBtn) {
      reset();
    }
    else if (source == ctrlBtn) {
      if (running) stopTimer();
      else startTimer();
    }
    else if (source == opBtn) {
      pane.removeAll();
      pane.add(opPanel);
      pane.revalidate();
      pane.repaint();
    }
    else if (source == setBtn) {
      int h = 0; int m = 0; int s = 0;
      if (hourField.getText().length() > 0) {
        try {
          h = Integer.parseInt(hourField.getText());
        }
        catch (NumberFormatException f) {}
      }
      if (minField.getText().length() > 0) {
        try {
          m = Integer.parseInt(minField.getText());
        }
        catch (NumberFormatException f) {}
      }
      if (secField.getText().length() > 0) {
        try {
          s = Integer.parseInt(secField.getText());
        }
        catch (NumberFormatException f) {}
      }
      int newCount = h*60*60 + m*60 + s;
      if (newCount == 0) setCountdown(-1);
      else setCountdown(newCount);
      //A stop-before and reset-after are implied with the countdown

      pane.removeAll();
      pane.add(opPanel);
      pane.revalidate();
      pane.repaint();
    }
  }


  /**
   * Start the timer.
   * The play/stop button will be changed.
   * If the worker thread hasn't died since the last stop, it will continue counting.
   * Otherwise a new worker thread will be created and started.
   */
  public void startTimer() {
    running = true;

    //If the last pause was briefer than the worker's sleep, let it keep running
    if (!worker.stillAlive) {
      Thread thread = new Thread(worker);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }
    ctrlBtn.setText("ll");
  }

  /**
   * Stop the timer.
   * The play/stop button will be changed.
   */
  public void stopTimer() {
    running = false;
    ctrlBtn.setText(">");
  }

  /**
   * Reset the timer to the initial time.
   * The timer will be stopped.
   * The clock's background will revert to the default color.
   * The play/stop button will be enabled.
   */
  public void reset() {
    stopTimer();
    clockField.setBackground(null);
    ctrlBtn.setEnabled(true);
    if (countdown == -1) {
      setCurrentTime(0);
    }
    else setCurrentTime(countdown);
  }

  /**
   * Set the currently displayed time.
   *
   * @param t the new time in seconds
   */
  public void setCurrentTime(int t) {
    if (t < 0) t = 0;
    currentTime = t;

    int h = t/60/60;
    int m = (t-(h*60*60)) / 60;
    int s = t - (h*60*60) - (m*60);
    String result = (h<10?"0":"")+ h +":"+ (m<10?"0":"")+ m +":"+ (s<10?"0":"")+ s;
    clockField.setText(result);
  }

  /**
   * Get the currently displayed time.
   *
   * @return the current time in seconds
   */
  public int getCurrentTime() {
    return currentTime;
  }

  /**
   * Set the initial countdown time.
   * The timer will be stopped, adjusted, and reset.
   *
   * @param t starting time, or -1 for a countup from 0
   */
  public void setCountdown(int t) {
    stopTimer();
    if (t < -1) t = -1;
    countdown = t;
    reset();
  }

  /**
   * Get the initial countdown time.
   *
   * @return the time in seconds, or -1 for a countup from 0
   */
  public int getCountdown() {
    return countdown;
  }



  private class TimerWorker implements Runnable {
    public boolean stillAlive = false;

    public TimerWorker() {}

    public void run() {
      stillAlive = true;
      int lastCount = getCurrentTime();
      lastClickTime = System.currentTimeMillis();

      while (running) {
        long now = System.currentTimeMillis();
        int elapsed = (int)((now-lastClickTime)/1000);

        if (countdown == -1) {
          if (elapsed > 0) setCurrentTime(lastCount + elapsed);
        }
        else {
          if (elapsed > 0) setCurrentTime(lastCount - elapsed);
          if (getCurrentTime() == 0) {
            String chatAlias = ArcanistCCG.NetManager.getPlayerAlias();

            int h = countdown/60/60;
            int m = (countdown-(h*60*60)) / 60;
            int s = countdown - (h*60*60) - (m*60);
            String countdownString = "";
            if (h > 0) countdownString += (h<10?"0":"")+ h;
            if (countdownString.length() > 0) countdownString += ":";
            if (h > 0 || m > 0) countdownString += (m<10?"0":"")+ m;
            if (countdownString.length() > 0) countdownString += ":";
            countdownString += (s<10?"0":"")+ s;

            ArcanistCCG.NetManager.chatNotice(ChatPanel.STYLE_NOTICE1, "--"+ chatAlias +": Ding! ("+  countdownString +")--");
            clockField.setBackground(Color.YELLOW);
            ctrlBtn.setEnabled(false);
            break;
          }
        }
        try {Thread.currentThread().sleep(500);}
        catch(InterruptedException e) {}
      }
      stillAlive = false;
      stopTimer();
    }
  }
}