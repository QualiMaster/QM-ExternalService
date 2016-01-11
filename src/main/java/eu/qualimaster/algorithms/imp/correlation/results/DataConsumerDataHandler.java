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
  private boolean useFilter;

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
    useFilter = true;
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

      if (received.startsWith("login/")) { // Login command
        synchronized (printWriter) {
          try {
            String reply = requestHandler.loginUser(received);
            logger.info("Sending login response");
            printWriter.println("login_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.";
            printWriter.println("login_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }
      } else if (received.startsWith("logout")) { // StartsWith used to avoid Windows socket problem
        synchronized (printWriter) {
          try {
            String reply = requestHandler.logoutUser();
            logger.info("Sending logout response");
            printWriter.println("logout_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.";
            printWriter.println("logout_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }
      } else if (received
          .startsWith("setGlobalAnalysisInterval/")) { // Set global analysis interval command
        synchronized (printWriter) {
          logger.info("Got setGlobalAnalysisInterval");
          try {
            String[] parts = received.split("/"); // e.g. "setGlobalAnalysisInterval/1000"
            int interval = Integer.parseInt(parts[1]);
            String reply = requestHandler.setGlobalAnalysisInterval(interval);
            printWriter.println("setGlobalAnalysisInterval_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.";
            printWriter.println("setGlobalAnalysisInterval_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }
      } else if (received.equals("requestDependencyAnalysis")) { // Request dependency analysis
        synchronized (printWriter) {
          logger.info("Got requestDependencyAnalysis");
          try {
            String reply = requestHandler.requestDependencyAnalysis();
            printWriter.println("requestDependencyAnalysis_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.";
            printWriter.println("requestDependencyAnalysis_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }
      } else if (received.equals("stopDependencyAnalysis")) { // Stop dependency analysis
        synchronized (printWriter) {
          logger.info("Got stopDependencyAnalysis");
          try {
            String reply = requestHandler.stopDependencyAnalysis();
            printWriter.println("stopDependencyAnalysis_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.";
            printWriter.println("stopDependencyAnalysis_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }
      } else if (received.equals("requestHistoricalDependency")) { // Request historical dependency
        synchronized (printWriter) {
          logger.info("Got requestHistoricalDependency");
          try {
            String reply = requestHandler.requestHistoricalDependency();
            printWriter.println("requestHistoricalDependency_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.";
            printWriter.println("requestHistoricalDependency_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }
      } else if (received.startsWith("setAdaptationParameter/")) { // Set adaptation parameter command
        synchronized (printWriter) {
          logger.info("Got setAdaptationParameter");
          try {
            String reply = requestHandler.setAdaptationParameter(received);
            printWriter.println("setAdaptationParameter_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.";
            printWriter.println("setAdaptationParameter_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }
      } else if (received.equals("quoteList")) {  // Send Symbols List command
        synchronized (printWriter) {
          logger.info("Got quoteList");
          try {
            String reply = requestHandler.getQuoteList();
            logger.info("Sending symbols");
            printWriter.print("quoteList_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.";
            printWriter.println("quoteList_response," + reply);
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
    if (!useFilter) {
      return true;
    }
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
        if (pair.equals("*")) {
          useFilter = false;
          continue;
        }
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
        if (pair.equals("*")) {
          useFilter = true;
          continue;
        }
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
