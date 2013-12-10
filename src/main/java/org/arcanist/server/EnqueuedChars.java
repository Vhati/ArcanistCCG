package org.arcanist.server;

import java.net.Socket;

import org.arcanist.server.*;


/**
 * Wrapper to hold a socket and char array.
 */
public class EnqueuedChars {
  public Socket socket = null;
  public char[] message = null;


  public EnqueuedChars(Socket s, char[] msg) {
    socket = s;
    message = msg;
  }

  public EnqueuedChars(Socket s, String msg) {
    socket = s;
    message = msg.toCharArray();
  }

  public EnqueuedChars(Socket s) {
    socket = s;
    message = null;
  }
}
