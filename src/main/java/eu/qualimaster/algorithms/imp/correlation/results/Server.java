package eu.qualimaster.algorithms.imp.correlation.results;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by ap0n on 1/14/15.
 */
public class Server {
  int producerPort;
  int consumerPort;
  int soTimeout;
  RequestHandler requestHandler;
  ServerSocket serverProducerSocket;
  ServerSocket serverConsumerSocket;

  final static Logger logger = LoggerFactory.getLogger(Server.class);

  public Server(int producerPort, int consumerPort) throws IOException {

    this.producerPort = producerPort;
    this.consumerPort = consumerPort;
    soTimeout = 500;

    try {
      requestHandler = new RequestHandler();
    } catch (Exception e) {
      e.printStackTrace();
    }

    serverProducerSocket = new ServerSocket(producerPort);
    serverProducerSocket.setSoTimeout(soTimeout);
    serverConsumerSocket = new ServerSocket(consumerPort);
    serverConsumerSocket.setSoTimeout(soTimeout);
  }

  public void start() {
    Thread producerThread = new Thread(new ServerRunnable(true));
    producerThread.start();
    Thread consumerThread = new Thread(new ServerRunnable(false));
    consumerThread.start();
  }

  private class ServerRunnable implements Runnable {

    boolean isProducer;
    public ServerRunnable(boolean isProducer) {
      this.isProducer = isProducer;
    }

    public void run() {
      Socket clientSocket;
      logger.info("Server started. Waiting for connections");
      while (true) {
        try {
          if (isProducer) {
            clientSocket = serverProducerSocket.accept();
            Thread thread = new Thread(new DataProducerDataHandler(requestHandler,
                                                                   clientSocket));
            thread.start();
          } else {
            clientSocket = serverConsumerSocket.accept();
            Thread thread = new Thread(new DataConsumerDataHandler(requestHandler, clientSocket));
            thread.start();
          }
        } catch (SocketTimeoutException e) {
          // Ignore the exception and continue
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        }
      }
    }
  }

  public static void main(String[] args) {
    int producerPort = 8888;
    int consumerPort = 8889;

    try {
      Server server = new Server(producerPort, consumerPort);
      server.start();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }
}
