package org.arcanist.server;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderMalfunctionError;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnmappableCharacterException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.WeakHashMap;

import org.arcanist.server.*;
import org.arcanist.util.*;


public class SocketOutQueue implements Runnable, ServerConnectionListener {

  private static final String CHARSET = "US-ASCII";

  private ServerConnectionManager connectionManager = null;

  /** Temporary write buffer */
  private ByteBuffer writeBuffer = ByteBuffer.allocate(8192);

  //Vectors are synchronized, so it's okay that two threads use it.
  private Vector<EnqueuedChars> queue = new Vector<EnqueuedChars>();
  private Object waitLock = new Object();

  /**
   * A Map to look up OutputStreams from Sockets.
   * As sockets are enqueued, new streams are added to this Map.
   * When a socket is abandoned elsewhere, it disappears from here too.
   */
  private Map<Socket,CharsetEncoder> socketMap = new WeakHashMap<Socket,CharsetEncoder>();
  private volatile boolean keepRunning = false;
  private Selector selector = null;
  private volatile LogManager logManager = null;


  public SocketOutQueue(ServerConnectionManager scm) {
    connectionManager = scm;
    connectionManager.addServerConnectionListener(this);
  }


  /**
   * Sets the log manager.
   * Use null to remove.
   */
  public void setLogManager(LogManager lm) {logManager = lm;}

  public void logException(Throwable e, String message) {
    if (logManager != null) {
      logManager.write(e, message);
    } else {
      if (message != null) System.out.println(message);
      e.printStackTrace();
    }
  }


  /**
   * Ensures a socket isknown.
   * If the socket isn't already in the internal
   * WeakHashMap and isn't closed, the socket's
   * OutputStream will be added to the Map; this
   * avoids a race condition among threads reacting to new
   * connections. Otherwise unmapped sockets are ignored.
   */
  public void includeSocket(Socket s) {
    synchronized (socketMap) {
      if (!socketMap.containsKey(s)) {
        if (s.isClosed()) return;

        CharsetEncoder encoder = Charset.forName(CHARSET).newEncoder().onUnmappableCharacter(CodingErrorAction.REPLACE);
        socketMap.put(s, encoder);
      }
    }
  }


  /**
   * Adds a kick to the queue.
   * When the queue is processed, the socket will
   * be promptly removed from the map of available
   * outbound recipients and later disconnected
   * by the ServerConnectionManager queue.
   */
  public void enqueueKick(Socket s) {
    // Using a null message to mean kick
    synchronized (socketMap) {
      if (!socketMap.containsKey(s)) return;
      queue.add(new EnqueuedChars(s));
    }
    wakeup();
  }

  /**
   * Adds a message to the queue.
   */
  public void enqueueMessage(Socket s, char[] message) {
    if (s == null | message == null) return;
    synchronized (socketMap) {
      if (!socketMap.containsKey(s)) return;
      queue.add(new EnqueuedChars(s, message));
    }
    wakeup();
  }


  /**
   * Enqueues a message to all sockets.
   */
  public void enqueueToAll(char[] message) {
    synchronized (socketMap) {
      for (Socket s : socketMap.keySet()) {
        queue.add(new EnqueuedChars(s, message));
      }
    }
    wakeup();
  }


  /**
   * Spawns a thread to watch this queue and act on it.
   * Repeated calling will have no effect while a thread is running.
   */
  public void startThread() {
    if (keepRunning == true) return;

    keepRunning = true;
    Thread t = new Thread(this);
    t.setPriority(Thread.NORM_PRIORITY);
    t.setDaemon(true);
    t.setName("OutQueue ("+ t.getName() +")");
    t.start();
  }


  public void wakeup() {
    synchronized(waitLock) {
      waitLock.notify();
    }
  }


  /**
   * Kills the thread.
   */
  public void killThread() {
    keepRunning = false;
  }


  @Override
  public void run() {
    try {
      selector = Selector.open();
    }
    catch(IOException e) {
      logException(e, "An error occurred setting up the outbound socket selector.");
      keepRunning = false;
    }

    while(keepRunning) {
      if (queue.size() > 0) {
        EnqueuedChars es = (EnqueuedChars)queue.remove(0);
        CharsetEncoder encoder = null;
        synchronized (socketMap) {encoder = socketMap.get(es.socket);}
        if (encoder == null) continue;

        if (es.socket.isClosed()) {
          synchronized (socketMap) {socketMap.remove(es.socket);}
          continue;
        }

        if (es.message == null) {
          // Used null message to mean kick
          abortSocket(es.socket);
        }
        else {
          // Forward it
          pseudoBlockingWrite(es, (CharsetEncoder)encoder);
        }
      } else {
        synchronized(waitLock) {
          try {waitLock.wait();}
          catch (InterruptedException e) {}
        }
      }
    }

    try {selector.close();}
    catch (IOException e) {logException(e, null);}
    finally {closeSilently(selector);}
  }


  @Override
  public void fireServerConnectionEvent(ServerConnectionEvent e) {
    if (e.getSource() == this) return;
    if (e.getID() == ServerConnectionEvent.SOCKET_ADDED) {
      Socket s = (Socket)e.getContent();
      synchronized (socketMap) {
        if (!socketMap.containsKey(s)) {
          CharsetEncoder encoder = Charset.forName(CHARSET).newEncoder().onUnmappableCharacter(CodingErrorAction.REPLACE);
          socketMap.put(s, encoder);
        }
      }
    }
    else if (e.getID() == ServerConnectionEvent.SOCKET_REMOVED) {
      Socket s = ((EnqueuedChars)e.getContent()).socket;
      synchronized (socketMap) {socketMap.remove(s);}
    }
  }


  private boolean pseudoBlockingWrite(EnqueuedChars es, CharsetEncoder encoder) {
    SocketChannel channel = es.socket.getChannel();
    if (channel == null) return false;

    int maxBytes = (int)(encoder.maxBytesPerChar() * es.message.length)+1;
    if (writeBuffer.capacity() >= maxBytes) writeBuffer.clear();
    else writeBuffer = ByteBuffer.allocate(maxBytes);
    try {
      CoderResult cr = encoder.encode(CharBuffer.wrap(es.message), writeBuffer, false);
    }
    catch (CoderMalfunctionError e) {logException(e, null); return false;}
    if (writeBuffer.position() == 0) return false;
    writeBuffer.flip();

    boolean doneWriting = false;
    while (keepRunning && !doneWriting) {
      try {
        channel.register(selector, SelectionKey.OP_WRITE);
        if (selector.select()==0) {return false;}
      }
      catch (ClosedSelectorException e) {
        keepRunning = false;
        return false;
      }
      catch (ClosedChannelException e) {
        logException(e, null);
        abortSocket(es.socket);
        return false;
      }
      catch (IOException e) {
        logException(e, null);
        abortSocket(es.socket);
        //closing the socket, closes the channel, cancels the key too
        return false;
      }

      Set<SelectionKey> keys = selector.selectedKeys();
      Iterator<SelectionKey> it = keys.iterator();
      while (keepRunning && it.hasNext()) {
        SelectionKey key = it.next();
        it.remove();
        if (!key.isValid()) continue;
        if (key.isWritable()) {
          if (writeBuffer.hasRemaining()) {
            try {
              channel.write(writeBuffer);
            }
            catch(IOException e) {
              System.out.println("Error sending outgoing message.");
              logException(e, null);
              abortSocket(es.socket);
              return false;
            }
          }
          if (!writeBuffer.hasRemaining()) {
            key.interestOps(0); doneWriting = true;
          }
        }
      }
    }
    SelectionKey key = channel.keyFor(selector);
    if (key != null) key.interestOps(0);
    return true;
  }


  private void abortSocket(Socket s) {
    synchronized (socketMap) {socketMap.remove(s);}
    connectionManager.enqueueAction(ServerConnectionManager.ACTION_KICK_SOCKET, s);
  }


  //Closeable was introduced in 1.5...

  private void closeSilently(Selector c) {
    try {c.close();}
    catch (IOException e) {}
  }
}
