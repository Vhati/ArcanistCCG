package org.arcanist.server;

import java.net.Socket;

import org.arcanist.server.*;


/**
 * Wrapper to hold a socket and an int flag.
 */
public class EnqueuedInt {
  public Socket socket = null;
  public int message = -1;


  public EnqueuedInt(Socket s, int msg) {
    socket = s;
    message = msg;
  }
}
