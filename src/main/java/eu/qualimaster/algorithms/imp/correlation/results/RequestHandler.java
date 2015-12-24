package eu.qualimaster.algorithms.imp.correlation.results;

import eu.qualimaster.algorithms.imp.correlation.spring.DataConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by ap0n on 1/15/15.
 */
public class RequestHandler {
  private List<DataConsumerDataHandler> resultsBoard;
  private DataConnector springDataConnector;

  private Logger logger = LoggerFactory.getLogger(Server.class);

  public RequestHandler() throws Exception {
    resultsBoard = new ArrayList<DataConsumerDataHandler>();
  }

  public void subscribeToResultsBoard(DataConsumerDataHandler consumer) {
    synchronized (resultsBoard) {
      if (!resultsBoard.contains(consumer)) {
        resultsBoard.add(consumer);
      }
    }
  }

  public void unsubscribeFromResultsBoard(DataConsumerDataHandler consumer) {
    synchronized (resultsBoard) {
      resultsBoard.remove(consumer);
    }
  }

  public void publishToResultsBoard(String result) {
    synchronized (resultsBoard) {
      for (DataConsumerDataHandler s : resultsBoard) {
        s.consumeResult(result);
      }
    }
  }

  public String getQuoteList() throws Exception {

    String res;
    synchronized (this) {
      initSpringDataConnector();

      springDataConnector.fullQuoteListResponce = "";
      try {
        logger.info("Fetching symbols");
        springDataConnector.getSymbols();
        while (springDataConnector.fullQuoteListResponce == "") {
          springDataConnector.execute();
        }
        logger.info("Symbols fetched");
      } catch (IOException e) {
        try {
          logger.info("Error getting symbols. Closing connection and retrying");
          springDataConnector.stopRunning();
          initSpringDataConnector();
          springDataConnector.getSymbols();
        } catch (Exception ex) {
          logger.error("SERVER: Get Symbols Error, " + ex.getMessage());
          throw new Exception("SERVER: Get Symbols Error : " + ex.getMessage());
        }
      }
      res =  springDataConnector.fullQuoteListResponce;
      springDataConnector.stopRunning();
      springDataConnector = null;
    }
    return res;
  }

  private void loginToSpring(String username, String password) throws Exception {
    int result = springDataConnector.connect();
    switch (result) {
      case DataConnector.OK: {
        logger.info("Connected to Spring API");
        try {
          springDataConnector.login(username, password);
        } catch (IOException ex) {
          logger.error("Login Error: " + ex.getMessage());
          throw new Exception("Login Error: " + ex.getMessage());
        }
        break;
      }
      case DataConnector.CONNECTION_ERROR: {
        logger.error("SERVER: Connection Error");
        throw new Exception("SERVER: Connection Error");
      }
      case DataConnector.NO_INTERNET: {
        logger.error("SERVER: Connection Error, Check your internet connection");
        throw new Exception("SERVER: Connection Error, Check your internet connection");
      }
      default: {
        logger.error("SERVER: Connection Error, Unknown error");
        throw new Exception("SERVER: Connection Error, Unknown error");
      }
    }

    while (!springDataConnector.isLoggedIn()) {
      springDataConnector.execute();
    }
  }

  private void initSpringDataConnector() throws Exception {
    synchronized (this) {
      springDataConnector = new DataConnector();
      loginToSpring("katerina2", "password");
    }
  }
}
