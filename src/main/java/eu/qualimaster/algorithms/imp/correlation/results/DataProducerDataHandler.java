package eu.qualimaster.algorithms.imp.correlation.results;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;

/**
 * Created by ap0n on 1/14/15.
 */
public class DataProducerDataHandler implements IDataHandler {

  RequestHandler requestHandler;
  Socket socket;
  ObjectInputStream inputStream;

  Logger logger = LoggerFactory.getLogger(DataProducerDataHandler.class);

  public DataProducerDataHandler(RequestHandler requestHandler, Socket socket) throws IOException {
    this.socket = socket;
    this.requestHandler = requestHandler;
    inputStream = new ObjectInputStream(socket.getInputStream());
  }

  @Override
  public void run() {
    logger.info(this.getClass().getName() + " started.");
    while (true) {
      Object received = null;
      try {
        received = inputStream.readObject();
      } catch (IOException e) {
        e.printStackTrace();
        logger.error(e.getMessage(), e);
        break;
      } catch (ClassNotFoundException e) {
        logger.error(e.getMessage(), e);
        break;
      }
      if (received instanceof String) {  // Got result
        logger.info("[producer] Got result: " + received);
        requestHandler.publishToResultsBoard((String) received);

      } else if (received instanceof List) {  // Got symbolsList
        logger.info("[producer] Got symbols");
        List<String> symbols = (List<String>) received;
        String symbolsType = symbols.remove(0);
        if (symbolsType.equals("f")) {  // financial symbol list
          logger.info("financial");
          requestHandler.updateFinancialSymbolsList(symbols);
        } else if (symbolsType.equals("w")) {  // web symbol list
          logger.info("web");
          requestHandler.updateWebSymbolsList(symbols);
        }

      } else {  // Unknown
        logger.error("Unknown message type received.");
        break;
      }
    }

    try {
      socket.getInputStream().close();
      socket.getOutputStream().close();
      socket.close();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }
}
