package eu.qualimaster.comserver.adaptation;

import eu.qualimaster.comserver.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by ap0n on 31/1/2017.
 *
 * This class is only used when the TimeTravelPip is run as a storm topology.
 */
public class QueriesServer implements Runnable {

  private final static Logger logger = LoggerFactory.getLogger(QueriesServer.class);
  private final Object mutex = new Object();
  private int port;
  private ServerSocket serverSocket;
  private Socket clientSocket;
  private PrintWriter printWriter;
  private boolean connected;

  public QueriesServer(int port) {
    logger.info("Selected port: " + port);
    this.port = port;
    this.connected = false;

  }

  @Override
  public void run() {
    try {
      serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      return;
    }

    while (true) {
      try {
        logger.info("Accepting...");
        clientSocket = serverSocket.accept();
        logger.info("Accepted from: " + clientSocket.getInetAddress());
        synchronized (mutex) {
          printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
          connected = true;
        }
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  public boolean isConnected() {
    return connected;
  }

  public boolean sendQuery(String query) {
    synchronized (mutex) {
      if (!connected) return false;
      printWriter.println(query);
      if (printWriter.checkError()) {
        closeQuietly(printWriter);
        closeQuietly(clientSocket);
        connected = false;
        return false;
      }
      return true;
    }
  }

  private void closeQuietly(Closeable c) {
    try {
      c.close();
      c = null;
    } catch (IOException e) {
      // Ignore the exception
    }
  }
}
