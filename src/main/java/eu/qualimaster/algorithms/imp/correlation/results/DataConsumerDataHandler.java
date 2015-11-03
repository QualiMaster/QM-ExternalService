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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ap0n on 1/14/15.
 */
public class DataConsumerDataHandler implements IDataHandler {

  private RequestHandler requestHandler;
  private Socket socket;
  private OutputStream outputStream;
  private InputStream inputStream;

  private BufferedInputStream bufferedInputStream;
  private InputStreamReader inputStreamReader;

  private PrintWriter printWriter;

  private Map<String, Integer> filter;

  private Logger logger = LoggerFactory.getLogger(DataConsumerDataHandler.class);

  public DataConsumerDataHandler(RequestHandler requestHandler, Socket socket) throws IOException {
    this.requestHandler = requestHandler;
    this.socket = socket;
    outputStream = socket.getOutputStream();
    inputStream = socket.getInputStream();
    printWriter = new PrintWriter(outputStream, true);

    bufferedInputStream = new BufferedInputStream(socket.getInputStream());
    inputStreamReader = new InputStreamReader(bufferedInputStream);
    filter = new HashMap<String, Integer>();
    logger.info("Consumer connected from: " + socket.getInetAddress().getHostAddress());
  }

  public void run() {

    requestHandler.subscribeToResultsBoard(this);
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
        logger.info("Got quoteList");

        synchronized (printWriter) {
          try {
            String reply = requestHandler.getQuoteList();
            logger.info("Sending symbols");
            printWriter.print("quoteList_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.";
            printWriter.print("quoteList_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }

      } else if (received.startsWith("resultsSubscribe/")) {  // Start sending results command

        logger.info("[consumer] got resultsSubscribe");

          // "resultsSubscribe/".length = 17
          addToFilter(received.substring(17));
          synchronized (printWriter) {
            printWriter.println("resultsSubscribe_response, resultsSubscribe ok");
        }

      } else if (received.startsWith("resultsUnsubscribe/")) {  // Stop sending results command
        logger.info("[consumer] got resultsUnsubscribe");

        // "resultsUnsubscribe/".length = 19
        removeFromFilter(received.substring(19));

        synchronized (printWriter) {
          printWriter.println("resultsUnsubscribe_response, resultsUnsubscribe ok");
        }

      } else {
        logger.error("Unknown command received: " + received);
        synchronized (printWriter) {
          printWriter.println("Unknown command received: " + received);
        }
      }
    }

    try {
      requestHandler.unsubscribeFromResultsBoard(this);
      socket.getInputStream().close();
      socket.getOutputStream().close();
      socket.close();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  public void consumeResult(String result) {

    if (!filterResult(result)) {
      return;
    }

    synchronized (printWriter) {
      printWriter.println("resultsSubscribe_response," + result + "!");
      printWriter.flush();
    }
  }

  private String readString() throws IOException {
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

  private boolean filterResult(String result) {  // Runs from different thread
    String[] splitResult = result.split(",");
    boolean ret;
    synchronized (filter) {
      ret = filter.containsKey(splitResult[1] + "," + splitResult[2]);
    }
    return ret;
  }

  private void addToFilter(String addMe) {
    String[] pairs = addMe.split("\\|");
    synchronized (filter) {
      for (String pair : pairs) {
        if (pair.length() == 0) {
          continue;
        }
        logger.info("Adding " + pair + " to filter");
        Integer ctr = filter.get(pair);
        if (ctr == null) {
          filter.put(pair, 1);
          logger.info(pair + " added to filter. Current count is " + 1);
        } else {
          filter.put(pair, ++ctr);
          logger.info(pair + " was already in filter. Current count is " + ctr);
        }
      }
    }
  }

  private void removeFromFilter(String removeMe) {
    String[] pairs = removeMe.split("\\|");
    synchronized (filter) {
      for (String pair : pairs) {
        if (pair.length() == 0) {
          continue;
        }
        logger.info("Removing " + pair + " from filter");
        Integer ctr = filter.get(pair);
        if (ctr == null) {
          logger.warn("Removing non existent pair (" + pair + ")!");
          continue;
        } else if (ctr == 1) {
          filter.remove(pair);
          logger.info("Removed " + pair + " from filter completely");
        } else {
          filter.put(pair, --ctr);
          logger.info("Removed " + pair + " from filter. Current count is " + ctr);
        }
      }
    }
  }
}
