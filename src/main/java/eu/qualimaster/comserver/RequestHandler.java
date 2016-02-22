package eu.qualimaster.comserver;

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

  private List<DataConsumerDataHandler> dataConsumers;
  private DataConnector springDataConnector;

  private Logger logger = LoggerFactory.getLogger(Server.class);

  private int globalAnalysisInterval;
  // For dummy login demo
  private String username;
  private String password;
  // End dummy login demo

  public RequestHandler() throws Exception {
    dataConsumers = new ArrayList<DataConsumerDataHandler>();
    // For dummy login demo
    username = "";
    password = "";
    // End dummy login demo
  }

  public String loginUser(String received) {

    // TODO(npavlakis): Read users/passwords from a file

    String[] parts = received.split("/"); // e.g. "login/userA/qualimaster"
    String uname, passwd;
    if (parts.length == 3) {
      uname = parts[1];
      passwd = parts[2];
    } else {
      return "Invalid command structure.";
    }
    String
        reply =
        "Failed to authenticate user with username : " + uname + " and password : " + passwd;
    if (uname.equals("userA") && passwd.equals("qualimaster")) {
      reply =
          "Successfully authenticated user with username : " + uname + " and password : " + passwd;
      username = uname;
      password = passwd;
    } else {
      username = "";
      password = "";
    }
    return reply;
  }

  public String logoutUser() {
    String reply = "User already logged out.";
    if (!username.equals("")) {
      reply = "Successfully logged user with username : " + username + " out";
      username = "";
      password = "";
    }
    return reply;
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

  public void publishHubList(String hubList) {
    synchronized (dataConsumers) {
      for (DataConsumerDataHandler s : dataConsumers) {
        s.consumeHubList(hubList);
      }
    }
  }

  public void publishFocusResult(String result) {
    synchronized (dataConsumers) {
      for (DataConsumerDataHandler s : dataConsumers) {
        s.consumeHubList(result);
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
      res = springDataConnector.fullQuoteListResponce;
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
      loginToSpring("katerina", "test");
    }
  }

  //TODO Pass globalAnalysisInterval wherever needed.
  public String setGlobalAnalysisInterval(int globalAnalysisInterval) {
    this.globalAnalysisInterval = globalAnalysisInterval;
    return "Successfully set globalAnalysisInterval to " + globalAnalysisInterval + ".";
  }

  //TODO Pass adaptation parameter wherever needed.
  public String setAdaptationParameter(String received) {
    String[] parts = received.split("/"); // e.g. "setAdaptationParameter/someInterval/1000"
    if (parts.length == 3) {
      String parameter = parts[1];
      String value = parts[2];
      return "Successfully set " + parameter + " to " + value;
    } else {
      return "Invalid command structure.";
    }
  }

  public String requestDependencyAnalysis() {
    String reply = "Starting Dependency Analysis";
    return reply;
  }

  public String stopDependencyAnalysis() {
    String reply = "Stopping Dependency Analysis";
    return reply;
  }

  public String requestHistoricalDependency() {
    String reply = "Starting Historical Dependency Analysis";
    return reply;
  }
}
