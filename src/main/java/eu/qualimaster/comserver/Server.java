package eu.qualimaster.comserver;

import eu.qualimaster.adaptation.AdaptationConfiguration;

import eu.qualimaster.dataManagement.DataManagementConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;

/**
 * Created by ap0n on 1/14/15.
 */
public class Server {

  final static Logger logger = LoggerFactory.getLogger(Server.class);
  private static final String DEFAULT_PROPERTIES_PATH = "/var/nfs/qm/tsi";
  int producerPort;
  int consumerPort;
  int soTimeout;
  RequestHandler requestHandler;
  ServerSocket serverProducerSocket;
  ServerSocket serverConsumerSocket;
  boolean isReplay;

  public Server(boolean isReplay, int producerPort, int consumerPort,
                String adaptationConfigurationFile)
      throws IOException {
    this.isReplay = isReplay;
    this.producerPort = producerPort;
    this.consumerPort = consumerPort;
    soTimeout = 500;

    logger.info("Producer port: " + producerPort);
    logger.info("Consumer port: " + consumerPort);
    logger.info("Is replay: " + isReplay);

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

    boolean isReplay = false;
    String adaptationConfigurationFile = "/var/nfs/qm/qm.infrastructure.cfg";
    if (args.length > 0) {
      // Usage: java -jar <name>.jar [adaptationConfigurationPath] [replay]  two optional args
      if (args[args.length - 1].equals("replay")) {
        isReplay = true;
      }

      if (!args[0].equals("replay")) {
        adaptationConfigurationFile = args[0];
      }
    }

    // Default ports
    int producerPort = isReplay ? 7788 : 8888;
    int consumerPort = isReplay ? 7789 : 8889;

    String properties_path = DataManagementConfiguration.getExternalServicePath();
    if (properties_path.equals("")) {
      properties_path = DEFAULT_PROPERTIES_PATH;
      logger.warn("externalService.path is empty. Using default: " + properties_path);
    } else {
      logger.info("Configured externalService.path: " + properties_path);
    }
    properties_path += "/external-service.properties";

    Properties properties = new Properties();
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(properties_path);
      properties.load(inputStream);
      if (isReplay) {
        producerPort = Integer.parseInt(properties.getProperty("REPLAY_PORT"));
        consumerPort = Integer.parseInt(properties.getProperty("REPLAY_CONSUMER_PORT"));
      } else {
        producerPort = Integer.parseInt(properties.getProperty("PORT"));
        consumerPort = Integer.parseInt(properties.getProperty("CONSUMER_PORT"));
      }
    } catch (IOException ioex) {
      // Ignore exception, means file not found or something similar. Fall back to defaults.
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException ex) {
          // Ignore exception, means file not found or something similar. Fall back to defaults.
          //          ex.printStackTrace();
        }
      }
    }

    try {
      Server server = new Server(isReplay, producerPort, consumerPort, adaptationConfigurationFile);
      server.start();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  private void readPropertiesFile() {

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

//    Configuration.configure(new File(configurationFile));
    AdaptationConfiguration.configure(new File(configurationFile));
//    DataManagementConfiguration.configure(new File(configurationFile));

    logger.info("Adaptation Host: " + AdaptationConfiguration.getAdaptationHost());
    logger.info("Adaptation Port: " + AdaptationConfiguration.getAdaptationPort());
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
            Thread
                thread =
                new Thread(new DataConsumerDataHandler(requestHandler, clientSocket, isReplay));
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
