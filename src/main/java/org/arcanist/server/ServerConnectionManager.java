package org.arcanist.server;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderMalfunctionError;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.swing.SwingUtilities;

import org.arcanist.server.*;
import org.arcanist.util.*;


public class ServerConnectionManager implements Runnable {

  private static final byte[][] BOMS = {
    {},                                                //No BOM, US-ASCII
    {(byte)0xef, (byte)0xbb, (byte)0xbf},              //UTF-8
    {(byte)0xfe, (byte)0xff},                          //UTF-16BE
    {(byte)0xff, (byte)0xfe},                          //UTF-16LE
    {(byte)0x00, (byte)0x00, (byte)0xfe, (byte)0xff},  //UTF-32BE
    {(byte)0xff, (byte)0xfe, (byte)0x00, (byte)0x00}   //UTF-32LE
  };
  private static final byte[] CHARSET_BOM = BOMS[0];   //Hardcoded to match
  private static final String CHARSET = "US-ASCII";
  private static final String NEWLINE_STRING = "\n";   //Can be \r\n

  public static final int ACTION_START_ACCEPTING = 0;
  public static final int ACTION_STOP_ACCEPTING = 1;
  public static final int ACTION_KICK_SOCKET = 2;
  public static final int ACTION_SET_PORT = 3;

  private static final int SELECTOR_NOT_OPENED = 0;
  private static final int SELECTOR_NOT_READY = 1;
  private static final int SELECTOR_READY = 2;
  private static final int SELECTOR_CLOSED = 3;
  private static final int SELECTOR_FAILED = 4;

  private ServerConnectionManager pronoun = this;

  private LinkedList<ServerConnectionListener> connectionListeners = new LinkedList<ServerConnectionListener>();
  private LinkedList<ServerProtocolListener> protocolListeners = new LinkedList<ServerProtocolListener>();
  private LinkedList<Runnable> pendingActions = new LinkedList<Runnable>();

  /** Temporary read buffers */
  private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
  private CharBuffer lineBuf = CharBuffer.allocate(8192);

  /**
   * A Map to look up ServerConnectionInMiscs from Sockets.
   * As sockets are added/removed, miscs are added/removed here too.
   */
  private Map<Socket,ServerConnectionInMisc> miscMap = new WeakHashMap<Socket,ServerConnectionInMisc>();

  private int serverPort = 6775;
  private volatile boolean keepRunning = false;
  private volatile int maxClients = 0;
  private volatile int selectorState = SELECTOR_NOT_OPENED;
  private volatile boolean accepting = false;
  private volatile LogManager logManager = null;

  private final Object warmupLock = new Object();
  private Selector selector = null;
  private byte[] newlineBytes = null;


  public ServerConnectionManager() {
    ByteBuffer newlineBuf = Charset.forName(CHARSET).encode(CharBuffer.wrap(NEWLINE_STRING));
    newlineBytes = new byte[newlineBuf.limit()];
    newlineBuf.get(newlineBytes);
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
   * Spawns a thread to listen on a port.
   * Repeated calling will have no effect while a thread is running.
   */
  public void startThread() {
    if (keepRunning == true) return;

    keepRunning = true;
    selectorState = SELECTOR_NOT_READY;
    Thread t = new Thread(this);
    t.setPriority(Thread.NORM_PRIORITY);
    t.setDaemon(true);
    t.setName("ServerConnectionManager ("+ t.getName() +")");
    t.start();
  }

  /**
   * Kills the watcher thread.
   */
  public void killThread() {
    keepRunning = false;
  }


  /**
   * Enqueues actions to run under the ConnectionManager thread.
   *
   * @param n one of: ACTION_START_LISTENING,
   *                  ACTION_STOP_LISTENING,
   *                  ACTION_KICK_SOCKET (o = a Socket)
   *                  ACTION_SET_PORT (o = an Integer)
   * @param o related object, or null
   */
  public void enqueueAction(int n, final Object o) {
    Runnable r = null;
    if (n == ACTION_START_ACCEPTING) {
      r = new Runnable() {
        public void run() {
          if (accepting == true) return;
          if (maxClients != 0 && miscMap.size() >= maxClients) return;

          ServerSocketChannel ssc = null;
          try {
            ssc = ServerSocketChannel.open();
              ssc.configureBlocking(false);
            ServerSocket pubSock = ssc.socket();
              pubSock.setReuseAddress(true);
            pubSock.bind(new InetSocketAddress(serverPort));
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            accepting = true;
            ServerConnectionEvent sce = new ServerConnectionEvent(pronoun, ServerConnectionEvent.ACCEPTING_STARTED, null);
            processServerConnectionEvent(sce);
          }
          catch(IOException e) {
            logException(e, "An error occurred setting up the server socket.");
            if (ssc != null) {
              try {ssc.close();}
              catch (IOException f) {}
            }
          }
        }
      };
    }
    else if (n == ACTION_STOP_ACCEPTING) {
      r = new Runnable() {
        public void run() {
          if (accepting == false) return;

          closeAcceptingSocket();
          ServerConnectionEvent sce = new ServerConnectionEvent(pronoun, ServerConnectionEvent.ACCEPTING_STOPPED, null);
          processServerConnectionEvent(sce);
        }
      };
    }
    else if (n == ACTION_KICK_SOCKET) {
      r = new Runnable() {
        public void run() {
          kickSocket((Socket)o);
        }
      };
    }
    else if (n == ACTION_SET_PORT) {
      r = new Runnable() {
        public void run() {
          int newPort = ((Integer)o).intValue();
          if (serverPort != newPort) {
            serverPort = newPort;
            ServerConnectionEvent sce = new ServerConnectionEvent(pronoun, ServerConnectionEvent.PORT_CHANGED, o);
            processServerConnectionEvent(sce);
          }
        }
      };
    }
    if (r != null) enqueueAction(r);
  }

  /**
   * Enqueues actions to run under the ConnectionManager thread.
   *
   * @param r the action, such as close, register, accept, etc
   */
  public void enqueueAction(Runnable r) {
    synchronized (pendingActions) {
      pendingActions.add(r);
      if (selectorState == SELECTOR_NOT_OPENED || selectorState == SELECTOR_NOT_READY) {
        synchronized (warmupLock) {/* wait if racing to warmup */}
      }
      if (selectorState == SELECTOR_READY) selector.wakeup();
    }
  }

  private void processActions() {
    Runnable r = null;
    synchronized (pendingActions) {
      while (pendingActions.size() > 0) {
        r = (Runnable)pendingActions.removeFirst();
        r.run();
      }
    }
  }


  /**
   * Gets the listening port.
   */
  public int getPort() {return serverPort;}

  /**
   * Sets the listening port.
   * Accepting will be stopped and restarted, if necessary.
   */
  public void setPort(int n) {
    if (serverPort == n) return;
    enqueueAction(ACTION_STOP_ACCEPTING, null);
    enqueueAction(ACTION_SET_PORT, new Integer(n));
    enqueueAction(ACTION_START_ACCEPTING, null);
  }


  /**
   * Gets the maximum client count.
   */
  public int getMaxClients() {return maxClients;}

  /**
   * Sets the maximum numbers of clients.
   * If/when the limit is met further accepting ceases.
   *
   * @param n the nth client that will stop further accepts (0 for no limit)
   */
  public void setMaxClients(int n) {
    if (maxClients == n) return;

    if (n != 0 && n <= miscMap.size()) {
      enqueueAction(ACTION_STOP_ACCEPTING, null);
    }
    maxClients = n;
    ServerConnectionEvent sce = new ServerConnectionEvent(pronoun, ServerConnectionEvent.MAX_CLIENTS_CHANGED, new Integer(maxClients));
    processServerConnectionEvent(sce);
  }


  /**
   * Reterns whether this thread is running.
   */
  public boolean isRunning() {return keepRunning;}


  /**
   * Returns whether new sockets will be accepted.
   */
  public boolean isAccepting() {return accepting;}


  @Override
  public void run() {
    synchronized (warmupLock) {
      try {
        selector = Selector.open();
        selectorState = SELECTOR_READY;
      }
      catch(IOException e) {
        logException(e, "An error occurred setting up the socket selector.");
        keepRunning = false;
        selectorState = SELECTOR_FAILED;
      }
    }

    while (keepRunning) {
      processActions();

      // select will block, so no sleeping needed
      try {
        if (selector.select(800) == 0) {continue;}
      }
      catch (ClosedSelectorException e) {
        keepRunning = false;
        break;
      }
      catch (IOException e) {
        logException(e, null);
        keepRunning = false;
        break;
      }

      Set<SelectionKey> keys = selector.selectedKeys();
      Iterator<SelectionKey> it = keys.iterator();
      while (keepRunning && it.hasNext()) {
        SelectionKey key = it.next();
        it.remove();
        if (!key.isValid()) continue;

        if (key.isAcceptable()) {
          //Someone has connected

          acceptNewConnection(key);
        }
        else if (key.isReadable()) {
          //A socket has something to read

          readFromConnection(key);
        }
      }
    }

    synchronized (pendingActions) {
      pendingActions.clear();
    }
    closeAcceptingSocket();

    try {selector.close();}
    catch (IOException e) {logException(e, null);}
    finally {closeSilently(selector);}
    selectorState = SELECTOR_CLOSED;

    //Closing the selector doesn't close the channels
    Socket[] sockets = miscMap.keySet().toArray(new Socket[miscMap.size()]);
    for (int i=sockets.length-1; i >= 0; i--) kickSocket(sockets[i]);

    ServerConnectionEvent sce = new ServerConnectionEvent(this, ServerConnectionEvent.SERVER_STOPPED, null);
    processServerConnectionEvent(sce);
  }


  private void acceptNewConnection(SelectionKey key) {
    try {
      //For an accept to be pending the channel must be a server socket channel
      ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();

      //Add it to the selector
      SocketChannel newChannel = serverSocketChannel.accept();
      Socket newSocket = newChannel.socket();
      newChannel.configureBlocking(false);
      newChannel.register(key.selector(), SelectionKey.OP_READ);

      ServerConnectionInMisc newScm = new ServerConnectionInMisc();
      miscMap.put(newSocket, newScm);

      ServerConnectionEvent sce = new ServerConnectionEvent(this, ServerConnectionEvent.SOCKET_ADDED, newSocket);
      processServerConnectionEvent(sce);


      if (maxClients != 0 && miscMap.size() >= maxClients) {
        try {serverSocketChannel.close();}
        catch (IOException e) {logException(e, null);}
        finally {closeSilently(serverSocketChannel);}
        //closing the channel cancels the key too
      }
    }
    catch (IOException e) {
      logException(e, "A recoverable error encountered while a user tried to connect.");
    }
  }


  /**
   * Appends incoming bytes to a socket-dedicated buffer.
   * If any newlines are present, chars preceeding them
   * are removed from the buffer and passed on as a String,
   * and the newlines are removed.
   *
   * Disconnects are detected here as well.
   */
  private void readFromConnection(SelectionKey key) {
    SocketChannel tmpChannel = (SocketChannel)key.channel();
    readBuffer.clear();
    int numRead = -1;

    try {
      numRead = tmpChannel.read(readBuffer);
    }
    catch(IOException e) {System.out.println(e.toString());}
    if (numRead == -1) {
      kickSocket(tmpChannel.socket());
      return;
    }
    readBuffer.limit(numRead);
    readBuffer.rewind();

    ServerConnectionInMisc scm = miscMap.get(tmpChannel.socket());

    //If first read has BOM, trim it off
    if (CHARSET_BOM.length > 0 && scm.checkedBom == false) {
      scm.checkedBom = true;
      if (arrayStartsAtCurrentPosition(readBuffer, CHARSET_BOM)) {
        numRead -= CHARSET_BOM.length;
        readBuffer.position(CHARSET_BOM.length);
        readBuffer.compact();
        readBuffer.flip();  //limit=pos and rewind
      }
    }

    ByteBuffer inBuf = scm.makeBuffRoomFor(numRead);
    int oldPos = inBuf.position();
    int newPos = oldPos + numRead;
    inBuf.limit(newPos);
    inBuf.put(readBuffer);
    inBuf.position(oldPos);

    boolean atNewline = false;
    int lineStart = 0;
    int lastNewline = -1;

    for (int i=oldPos; i < newPos; i++) {
      //Peek ahead for a newline
      atNewline = false;
      if (arrayStartsAtCurrentPosition(inBuf, newlineBytes)) atNewline = true;

      if (atNewline) {
        if (lastNewline == -1) lineStart = oldPos;
        else lineStart = lastNewline + newlineBytes.length;

        //Decode from lineStart until i (length = i-lineStart)
        inBuf.position(lineStart);
        inBuf.limit(i);
        try {
          int maxChars = (int)(scm.decoder.maxCharsPerByte() * inBuf.remaining())+1;
          if (lineBuf.capacity() >= maxChars) lineBuf.clear();
          else lineBuf = CharBuffer.allocate(maxChars);

          CoderResult cr = scm.decoder.decode(inBuf, lineBuf, true);
          lineBuf.flip();
          char[] line = new char[lineBuf.limit()];
          lineBuf.get(line);

          EnqueuedChars es = new EnqueuedChars(tmpChannel.socket(), line);
          ServerProtocolEvent e = new ServerProtocolEvent(this, ServerProtocolEvent.LINE_ADDED, es);
          processServerProtocolEvent(e);
        }
        catch (CoderMalfunctionError e) {logException(e, null);}

        inBuf.limit(newPos);
        lastNewline = i;
        i += newlineBytes.length;
        inBuf.position(i);
      }
      else {
        //This byte wasn't a newline, move along...
        inBuf.position(i+1);
      }
    }
    if (lastNewline != -1) {
      inBuf.position(lastNewline + newlineBytes.length);
      inBuf.limit(newPos);
      inBuf.compact();

      ServerProtocolEvent e = new ServerProtocolEvent(this, ServerProtocolEvent.WAKE_UP, null);
      processServerProtocolEvent(e);
    }
  }


  public void addServerConnectionListener(ServerConnectionListener l) {
    synchronized (connectionListeners) {
      connectionListeners.add(l);
    }
  }

  public void removeServerConnectionListener(ServerConnectionListener l) {
    synchronized (connectionListeners) {
      connectionListeners.remove(l);
    }
  }

  public void processServerConnectionEvent(final ServerConnectionEvent e) {
    synchronized (connectionListeners) {
      final Object[] listeners = connectionListeners.toArray();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          for (int i=0; i < listeners.length; i++) {
            ((ServerConnectionListener)listeners[i]).fireServerConnectionEvent(e);
          }
        }
      });
    }
  }


  public void addServerProtocolListener(ServerProtocolListener l) {
    synchronized (protocolListeners) {
      protocolListeners.add(l);
    }
  }

  public void removeServerProtocolListener(ServerProtocolListener l) {
    synchronized (protocolListeners) {
      protocolListeners.remove(l);
    }
  }

  public void processServerProtocolEvent(final ServerProtocolEvent e) {
    synchronized (protocolListeners) {
      final Object[] listeners = protocolListeners.toArray();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          for (int i=0; i < listeners.length; i++) {
            ((ServerProtocolListener)listeners[i]).fireServerProtocolEvent(e);
          }
        }
      });
    }
  }


  private void closeAcceptingSocket() {
    if (accepting == false) return;

    Iterator it = selector.keys().iterator();
    while (it.hasNext()) {
      Object tmpObj = it.next();
      if (tmpObj instanceof SelectionKey) {
        SelectionKey key = (SelectionKey)tmpObj;
        if (key.isValid() && key.isAcceptable()) {
          try {key.channel().close();}
          catch (IOException f) {}
          key.cancel();
          if (key.channel() instanceof ServerSocketChannel) {
            try {((ServerSocketChannel)key.channel()).socket().close();}
            catch (IOException e) {logException(e, null);}
          }
        }
      }
    }
    accepting = false;
  }


  private void kickSocket(Socket s) {
    char[] address = s.getInetAddress().toString().toCharArray();

    miscMap.remove(s);
    try {s.close();}
    catch (IOException e) {logException(e, null);}
    finally {closeSilently(s);}
    //closing the socket, closes the channel, cancels the key too

    ServerConnectionEvent sce = new ServerConnectionEvent(this, ServerConnectionEvent.SOCKET_REMOVED, new EnqueuedChars(s, address));
    processServerConnectionEvent(sce);
  }


  //Closeable was introduced in 1.5...

  private void closeSilently(Socket c) {
    try {c.close();}
    catch (IOException e) {}
  }

  private void closeSilently(Channel c) {
    try {c.close();}
    catch (IOException e) {}
  }

  private void closeSilently(Selector c) {
    try {c.close();}
    catch (IOException e) {}
  }


  /**
   * Searches a ByteBuffer for the given subsequence.
   * It compares bytes from the current buffer position and the key
   * until either a pair isn't equal or the whole array is matched.
   * Then the buffer is returned to its original position.
   * If the buffer has insufficient remaining bytes, this returns false.
   */
  private boolean arrayStartsAtCurrentPosition(ByteBuffer b, byte[] key) {
    if (b.remaining() < key.length) return false;

    int herePos = b.position();
    int cmpPos = 0;
    boolean foundKey = false;
    byte tmpByte = b.get();
    while (tmpByte == key[cmpPos]) {
      if (cmpPos == key.length-1) {
        foundKey = true;
        break;
      }
      if (b.position() == b.limit()) {
        break;
      }
      cmpPos++;
      tmpByte = b.get();
    }
    if (cmpPos > 0) b.position(herePos);

    return foundKey;
  }


  private static class ServerConnectionInMisc {
    public ByteBuffer buff = ByteBuffer.allocate(8192);
    public boolean checkedBom = false;
    public CharsetDecoder decoder = Charset.forName(CHARSET).newDecoder().onUnmappableCharacter(CodingErrorAction.REPLACE);


    public ByteBuffer makeBuffRoomFor(int pendingBytes) {
      int oldCapacity = buff.capacity();
      int oldLimit = buff.limit();
      int oldPos = buff.position();
      int oldMax = (oldLimit!=oldCapacity?oldLimit:oldPos);  //if no limit, use pos
      int newCapacity = oldCapacity;

      while (newCapacity-oldMax < pendingBytes) newCapacity *= 2;

      if (newCapacity != oldCapacity) {
        buff.rewind();
        ByteBuffer newBuf = ByteBuffer.allocate(newCapacity);
        newBuf.put(buff);
        newBuf.limit(oldLimit);
        newBuf.position(oldPos);
        buff = newBuf;
      }
      return buff;
    }
  }
}
