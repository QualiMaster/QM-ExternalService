package eu.qualimaster.comserver;

import eu.qualimaster.Configuration;
import eu.qualimaster.ExternalHBaseConnector.TweetSentimentConnector;
import eu.qualimaster.adaptation.external.ClientEndpoint;
import eu.qualimaster.comserver.adaptation.Dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

/**
 * Created by ap0n on 1/14/15.
 */
public class Server {

  final static Logger logger = LoggerFactory.getLogger(Server.class);
  int producerPort;
  int consumerPort;
  int soTimeout;
  RequestHandler requestHandler;
  ServerSocket serverProducerSocket;
  ServerSocket serverConsumerSocket;

  public Server(int producerPort, int consumerPort, String adaptationConfigurationFile)
      throws IOException {

    this.producerPort = producerPort;
    this.consumerPort = consumerPort;
    soTimeout = 500;

    initializeEndpoint(adaptationConfigurationFile);

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

  public static void main(String[] args) {

    // TODO(ap0n): Add a file configuration for server ports, etc.

    int producerPort = 8888;
    int consumerPort = 8889;

    String adaptationConfigurationFile = args.length == 0
                                         ? "/var/nfs/qm/qm.infrastructure.cfg"
                                         : args[0];

    try {
      Server server = new Server(producerPort, consumerPort, adaptationConfigurationFile);
      server.start();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

//  public static void main(String[] args) throws ParseException {
//
//    TweetSentimentConnector connector = new TweetSentimentConnector();
//    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
//    Date startDate = dateFormat.parse("09/01/2015");
//    Date endDate = dateFormat.parse("01/01/2016");
//    TreeMap<Long, Integer[]> r = connector.getSentimentForMarketplayerDail(1416, startDate, endDate);
//    System.out.println(r.size());
//  }

  public void start() {
    Thread producerThread = new Thread(new ServerRunnable(true));
    producerThread.start();
    Thread consumerThread = new Thread(new ServerRunnable(false));
    consumerThread.start();
  }

  /**
   * Read the configuration and initialize the client endpoint so that user commands can be
   * forwarded to the infrastructure.
   */
  private void initializeEndpoint(String configurationFile) throws IOException {

    Configuration.configure(new File(configurationFile));

    logger.info("Host: " + Configuration.getAdaptationHost());
    logger.info("Port: " + Configuration.getAdaptationPort());
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
}
