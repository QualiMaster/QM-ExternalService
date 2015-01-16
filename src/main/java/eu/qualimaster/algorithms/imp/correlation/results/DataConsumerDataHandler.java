package eu.qualimaster.algorithms.imp.correlation.results;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;

/**
 * Created by ap0n on 1/14/15.
 */
public class DataConsumerDataHandler implements IDataHandler {

  RequestHandler requestHandler;
  Socket socket;
  OutputStream outputStream;

  BufferedReader reader;
  PrintWriter printWriter;

  Logger logger = LoggerFactory.getLogger(DataProducerDataHandler.class);

  public DataConsumerDataHandler(RequestHandler requestHandler, Socket socket) throws IOException {
    this.requestHandler = requestHandler;
    this.socket = socket;
    outputStream = socket.getOutputStream();
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    printWriter = new PrintWriter(outputStream, true);
  }

  @Override
  public void run() {
    logger.info(this.getClass().getName() + " started.");
    while (true) {
      String received;
      try {
        received = reader.readLine();
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
        break;
      }

      if (received == null) {
        break;  // socket has been closed
      }

      if (received.equals("quoteList")) {  // Send Symbols List command
        logger.info("[consumer] got sendSymbolsList");
        requestHandler.sendAllSymbols(outputStream);
      } else if (received.equals("resultsSubscribe")) {  // Start sending results command
        logger.info("[consumer] got startResults");
        requestHandler.subscribeToResultsBoard(outputStream);
        printWriter.println("resultsSubscribe_response, resultsSubscribe ok");
      } else if (received.equals("resultsUnsubscribe")) {  // Stop sending results command
        logger.info("[consumer] got stopResults");
        requestHandler.unsubscribeFromResultsBoard(outputStream);
        printWriter.println("resultsUnsubscribe_response, resultsUnsubscribe ok");
      } else {
        logger.error("Unknown command received: " + received);
        printWriter.println("Unknown command received: " + received);
      }
    }

    try {
      requestHandler.unsubscribeFromResultsBoard(outputStream);
      socket.getInputStream().close();
      socket.getOutputStream().close();
      socket.close();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }
}
