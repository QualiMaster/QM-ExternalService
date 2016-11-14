package eu.qualimaster.comserver;

import eu.qualimaster.algorithms.imp.correlation.spring.DataConnector;
import eu.qualimaster.dataManagement.accounts.PasswordStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ap0n on 1/15/15.
 */
public class RequestHandler {

  private List<DataConsumerDataHandler> dataConsumers;
  private DataConnector springDataConnector;

  private Logger logger = LoggerFactory.getLogger(Server.class);

  private int globalAnalysisInterval;

  public RequestHandler() throws Exception {
    dataConsumers = new ArrayList<DataConsumerDataHandler>();
  }

  public void subscribeConsumer(DataConsumerDataHandler consumer) {
    synchronized (dataConsumers) {
      if (!dataConsumers.contains(consumer)) {
        dataConsumers.add(consumer);
      }
    }
  }

  public void unsubscribeConsumer(DataConsumerDataHandler consumer) {
    synchronized (dataConsumers) {
      dataConsumers.remove(consumer);
    }
  }

  public void publishCorrelationResult(String result) {
    synchronized (dataConsumers) {
      for (DataConsumerDataHandler s : dataConsumers) {
        s.consumeCorrelationResult(result);
      }
    }
  }

  public void publishUnfilteredResult(String result) {
    synchronized (dataConsumers) {
      for (DataConsumerDataHandler s : dataConsumers) {
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
        res = "1," + springDataConnector.fullQuoteListResponce;
        logger.info("Symbols fetched");
      } catch (IOException e) {
        logger.error("SERVER: Get Symbols Error, " + e.getMessage());
        res = "0,Error while getting symbols";
      }
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
      PasswordStore.PasswordEntry entry = PasswordStore.getEntry("katerina");
      loginToSpring(entry.getUserName(), entry.getPassword());
    }
  }
}
