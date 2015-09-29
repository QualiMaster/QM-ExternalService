package eu.qualimaster.algorithms.imp.correlation.results;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by ap0n on 1/14/15.
 */
public class DataConsumerDataHandler implements IDataHandler {

  RequestHandler requestHandler;
  Socket socket;
  OutputStream outputStream;
  InputStream inputStream;

  BufferedInputStream bufferedInputStream;
  InputStreamReader inputStreamReader;

  PrintWriter printWriter;

  Logger logger = LoggerFactory.getLogger(DataConsumerDataHandler.class);

  public DataConsumerDataHandler(RequestHandler requestHandler, Socket socket) throws IOException {
    this.requestHandler = requestHandler;
    this.socket = socket;
    outputStream = socket.getOutputStream();
    inputStream = socket.getInputStream();
    printWriter = new PrintWriter(outputStream, true);

    bufferedInputStream = new BufferedInputStream(socket.getInputStream());
    inputStreamReader = new InputStreamReader(bufferedInputStream);
    logger.info("Consumer connected from: " + socket.getInetAddress().getHostAddress());
  }

  String readString() throws IOException {
    StringBuilder response = new StringBuilder();
    int c;
    while (inputStreamReader != null && (c = inputStreamReader.read()) != -1) {
      if ((char) c == '!') {  // All messages are separated by !
        break;
      }
      response.append((char) c);
    }
    String res = response.toString();
    if (res.equals("")) {
      return null;
    } else if (res.startsWith("\n")) {
      return res.substring(1);
    }
    return res;
  }

  public void run() {
    logger.info(this.getClass().getName() + " started.");
    while (true) {
      String received;
      try {
        received = readString();
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
        break;
      }

      if (received == null) {
        break;  // socket has been closed
      }

      if (received.equals("quoteList")) {  // Send Symbols List command
        logger.info("[consumer] got quoteList");
        requestHandler.sendAllSymbols(outputStream);
      } else if (received.equals("resultsSubscribe")) {  // Start sending results command
        logger.info("[consumer] got resultsSubscribe");
        requestHandler.subscribeToResultsBoard(outputStream);
        printWriter.println("resultsSubscribe_response, resultsSubscribe ok");
      } else if (received.equals("resultsUnsubscribe")) {  // Stop sending results command
        logger.info("[consumer] got resultsUnsubscribe");
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
