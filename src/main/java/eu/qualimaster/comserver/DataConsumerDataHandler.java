package eu.qualimaster.comserver;

import eu.qualimaster.ExternalHBaseConnector.TweetSentimentConnector;
import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.adaptation.external.ChangeParameterRequest;
import eu.qualimaster.adaptation.external.ClientEndpoint;
import eu.qualimaster.adaptation.external.ReplayMessage;
import eu.qualimaster.adaptation.external.ResponseMessage;
import eu.qualimaster.adaptation.external.UsualMessage;
import eu.qualimaster.comserver.adaptation.Dispatcher;
import eu.qualimaster.dataManagement.accounts.PasswordStore;
import eu.qualimaster.events.ResponseStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static eu.qualimaster.comserver.Cmd.ADD_MARKET_PLAYER;
import static eu.qualimaster.comserver.Cmd.CHANGE_CORRELATION_THRESHOLD;
import static eu.qualimaster.comserver.Cmd.CHANGE_DENSITY_SIZE;
import static eu.qualimaster.comserver.Cmd.CHANGE_HUBLIST_SIZE;
import static eu.qualimaster.comserver.Cmd.CHANGE_WINDOW_ADVANCE;
import static eu.qualimaster.comserver.Cmd.CHANGE_WINDOW_SIZE;
import static eu.qualimaster.comserver.Cmd.LOGIN;
import static eu.qualimaster.comserver.Cmd.LOGOUT;
import static eu.qualimaster.comserver.Cmd.QUOTE_LIST;
import static eu.qualimaster.comserver.Cmd.REMOVE_MARKET_PLAYER;
import static eu.qualimaster.comserver.Cmd.REQUEST_FINANCIAL_REPLAY;
import static eu.qualimaster.comserver.Cmd.REQUEST_HISTORICAL_SENTIMENT;
import static eu.qualimaster.comserver.Cmd.REQUEST_SNAPSHOTS;
import static eu.qualimaster.comserver.Cmd.RESULT_SUBSCRIBE;
import static eu.qualimaster.comserver.Cmd.RESULT_UNSUBSCRIBE;

/**
 * Created by ap0n on 1/14/15.
 */
public class DataConsumerDataHandler implements IDataHandler {

  private final Object printWriterLock;
  private RequestHandler requestHandler;
  private Socket socket;
  private OutputStream outputStream;
  private BufferedInputStream bufferedInputStream;
  private InputStreamReader inputStreamReader;
  private PrintWriter printWriter;
  private Map<String, Integer> filter;
  private boolean useFilter;

  private boolean loggedIn;
  private String userName;
  private String role;

  private ResponseStore<UsualMessage, ChangeParameterRequest, ResponseMessage> responseStore;
//  private ResponseStore<UsualMessage, RequestMessage, ResponseMessage> responseStore;

  // *Warning* Lock clientEntpoint before using it!
  private ClientEndpoint clientEndpoint;  // For sending user commands to the infrastructure.

  private TweetSentimentConnector tweetSentimentConnector;
  private Logger logger = LoggerFactory.getLogger(DataConsumerDataHandler.class);
  private DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
  private boolean isReplay;
  private Map<String, Set<String>> commandPipelineMap;
  private Map<String, String> identifierPipelineMap;
  private Map<String, String> commandPipelineToComponentMap;  // cmd,pipeline -> compName

  public DataConsumerDataHandler(final RequestHandler requestHandler, Socket socket,
                                 boolean isReplay)
      throws IOException {
    printWriterLock = new Object();
    this.requestHandler = requestHandler;
    this.socket = socket;
    this.isReplay = isReplay;

    outputStream = socket.getOutputStream();
    printWriter = new PrintWriter(outputStream, true);

    bufferedInputStream = new BufferedInputStream(socket.getInputStream());
    inputStreamReader = new InputStreamReader(bufferedInputStream);
    filter = new HashMap<>();
    logger.info("Consumer connected from: " + socket.getInetAddress().getHostAddress());
    useFilter = true;

    loggedIn = false;
    userName = "";

    ResponseStore.IStoreHandler<UsualMessage, ChangeParameterRequest, ResponseMessage>
        handler =
        new ResponseStore.IStoreHandler<UsualMessage, ChangeParameterRequest, ResponseMessage>() {
          @Override
          public String getRequestMessageId(ChangeParameterRequest requestMessage) {
            return requestMessage.getMessageId();
          }

          @Override
          public String getResponseMessageId(ResponseMessage responseMessage) {
            return responseMessage.getMessageId();
          }

          @Override
          public ChangeParameterRequest castRequest(UsualMessage usualMessage) {
            return ResponseStore.cast(ChangeParameterRequest.class, usualMessage);
          }

          @Override
          public ResponseMessage castResponse(UsualMessage usualMessage) {
            return ResponseStore.cast(ResponseMessage.class, usualMessage);
          }
        };

    responseStore = new ResponseStore<>(0, handler);

    Dispatcher dispatcher = new Dispatcher(printWriter, responseStore);
    clientEndpoint =
        new ClientEndpoint(dispatcher,
                           InetAddress.getByName(AdaptationConfiguration.getAdaptationHost()),
                           AdaptationConfiguration.getAdaptationPort());

    initializeMappings();

//    tweetSentimentConnector = new TweetSentimentConnector();
  }

  String[] checkCommand(String cmd) {
    String[] args = cmd.split(",", 3);
    if (commandPipelineMap.get(args[0]).contains(args[1])) {
      return args;
    }
    synchronized (printWriterLock) {
      printWriter.println(args[0] + "_response,0,wrong_pipeline!");
    }
    return null;
  }

  public void run() {

    requestHandler.subscribeConsumer(this);
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
      // -- Server Commands
      if (received.startsWith(LOGIN + ",")) { // Login command
        synchronized (printWriterLock) {
          try {
            String reply;
            if (loggedIn) {
              reply = "0,already logged in!";
            } else {
              reply = login(received.substring(6, received.length())) + "!";
            }
            logger.info("Sending login response");
            printWriter.println("login_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "0,error: " + e.getMessage() + ". Please try again.!";
            printWriter.println("login_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }
      } else if (!loggedIn) {  // Only login is allowed while not logged-in
        synchronized (printWriterLock) {
          printWriter.println(received + "_response,0,not logged in!");
          printWriter.flush();
        }
      } else if (received.startsWith(LOGOUT)) {
        synchronized (printWriterLock) {
          String reply = logout() + "!";
          printWriter.println("logout_response," + reply);
          printWriter.flush();
        }
      } else if (received.equals(QUOTE_LIST)) {  // Send Symbols List command
        logger.info("Got quoteList");
        synchronized (printWriterLock) {
          try {
            String reply = requestHandler.getQuoteList() + "!";
            logger.info("Sending symbols");
            printWriter.print("quoteList_response," + reply);
            printWriter.flush();
          } catch (Exception e) {
            String reply = "error: " + e.getMessage() + ". Please try again.!";
            printWriter.println("quoteList_response," + reply);
            logger.error(e.getMessage(), e);
          }
        }
      } else if (received.startsWith(REQUEST_HISTORICAL_SENTIMENT + ",")) {
        logger.info("[consumer] got resultsHistoricalSentiment. Cmd = " + received);
        try {

          synchronized (printWriterLock) {
            printWriter.println("historicalSentiment_response,1!");
          }

          String[] reply = requestHistoricalSentiment(received.substring(27));

          synchronized (printWriterLock) {
            for (String s : reply) {
              printWriter.println(s);
            }
            printWriter.println("!");
          }
        } catch (ParseException e) {
          logger.error(e.getMessage(), e);

          synchronized (printWriterLock) {
            printWriter.println("historicalSentiment_response,0, " + e.getMessage() + "!");
          }
        }
        // --
      } else if (received.startsWith(RESULT_SUBSCRIBE + ",")) {  // Start sending results command
        // Pipelines: priority
        logger.info("[consumer] got resultsSubscribe");
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          addToFilter(cmd_args[2]);
          synchronized (printWriterLock) {
            printWriter.println("resultsSubscribe_response,1!");
          }
        }
      } else if (received.startsWith(RESULT_UNSUBSCRIBE + ",")) {  // Stop sending results command
        // Pipelines: priority
        logger.info("[consumer] got resultsUnsubscribe");
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          removeFromFilter(cmd_args[2]);
          synchronized (printWriterLock) {
            printWriter.println("resultsUnsubscribe_response,1!");
          }
        }
      } else if (received.startsWith(ADD_MARKET_PLAYER + ",")
                 || received.startsWith(REMOVE_MARKET_PLAYER + ",")) {
        // Pipelines: focus, transfer entropy
        logger.info("Got " + received);
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          changeParameter(cmd_args[0], cmd_args[1], cmd_args[0] + "/" + cmd_args[2]);
        }
      } else if (received.startsWith(CHANGE_WINDOW_SIZE + ",")) {
        // Pipelines: all
        logger.info("[consumer] got changewindowSize. Cmd = " + received);
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          changeParameter(cmd_args[0], cmd_args[1], Integer.parseInt(cmd_args[2]));
        }
      } else if (received.startsWith(CHANGE_HUBLIST_SIZE + ",")) {
        // Pipelines: dynamic graph
        logger.info("[consumer] got changehubListSize. Cmd = " + received);
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          changeParameter(cmd_args[0], cmd_args[1], Integer.parseInt(cmd_args[2]));
        }
      } else if (received.startsWith(CHANGE_CORRELATION_THRESHOLD + ",")) {
        // Pipelines: focus & dynamic graph & time travel
        logger.info("[consumer] got changeDynamicCorrelationThreshold. Cmd = " + received);
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          changeParameter(cmd_args[0], cmd_args[1], Double.parseDouble(cmd_args[2]));
        }
      } else if (received.startsWith(REQUEST_SNAPSHOTS + ",")) {
        // Pipelines: time travel
        logger.info("[consumer] got requestSnapshots. Cmd = " + received);
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          changeParameter(cmd_args[0], cmd_args[1], cmd_args[2]);
          changeParameter(cmd_args[0], cmd_args[1], "");
        }
      } else if (received.startsWith(REQUEST_FINANCIAL_REPLAY + ",")) {
        // Pipelines: transfer entropy
        logger.info("[consumer] got request financial replay");
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          requestFinancialReplay(cmd_args[0], cmd_args[1], cmd_args[2]);
        }
      } else if (received.startsWith(CHANGE_WINDOW_ADVANCE + ",")) {
        // Pipelines: transfer entropy
        logger.info("[consumer] got changeWindowAdvance. Cmd = " + received);
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          changeParameter(cmd_args[0], cmd_args[1], Integer.parseInt(cmd_args[2]));
        }
      } else if (received.startsWith(CHANGE_DENSITY_SIZE + ",")) {
        // Pipelines: transfer entropy
        logger.info("[consumer] got changeDensitySize. Cmd = " + received);
        String[] cmd_args = checkCommand(received);
        if (cmd_args != null) {
          changeParameter(cmd_args[0], cmd_args[1], Integer.parseInt(cmd_args[2]));
        }
      } else {
        logger.error("Unknown command received: " + received);
        synchronized (printWriterLock) {
          printWriter.println(received + "_response,0,Unknown command!");
        }
      }
    }

    try {
      requestHandler.unsubscribeConsumer(this);
      socket.getInputStream().close();
      socket.getOutputStream().close();
      socket.close();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  private String login(String credentials) {
    String[] parts = credentials.split(","); // e.g. "userA,qualimaster"
    String user, password;
    if (parts.length == 2) {
      user = parts[0];
      password = parts[1];
    } else {
      return "0,Invalid command";
    }

    PasswordStore.PasswordEntry entry = PasswordStore.getEntry("serviceUsers/" + user);

    String reply = "0,failed";
    if (user.equals(entry.getUserName()) && password.equals(entry.getPassword())) {
      loggedIn = true;
      userName = user;
      role = entry.getValue("role");
      logger.info("User " + userName + " logged-in");
      reply = "1," + role;
    }
    return reply;
  }

  private String logout() {
    logger.info("User " + userName + " logged-out");
    loggedIn = false;
    userName = "";
    return "1";
  }

  public void consumeResult(String hubList) {
    synchronized (printWriterLock) {
      printWriter.println(hubList + "!");
      printWriter.flush();
    }
  }

  public void consumeCorrelationResult(String result) {

    if (!filterResult(result)) {
      return;
    }

    synchronized (printWriterLock) {
      printWriter.println("resultsSubscribe_response," + result + "!");
      printWriter.flush();
    }
  }

  private String readString() throws IOException {
    StringBuilder response = new StringBuilder();
    int c;
    while (inputStreamReader != null) {
      c = inputStreamReader.read();
      if (c == -1) {
        return null;
      }
      if ((char) c == '!') {  // All messages are separated by !
        break;
      }
      response.append((char) c);
    }
    String res = response.toString();

    if (res.startsWith("\n")) {
      return res.substring(1);
    }
    return res;
  }

  private boolean filterResult(String result) {  // Runs from different thread
    if (!useFilter) {
      return true;
    }
    String[] splitResult = result.split(",");

    if (splitResult[0].compareTo("f") != 0 && splitResult[0].compareTo("w") != 0) {
      return true;  // filtering applies to priority pipeline only!
    }

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
        } else {
          filter.put(pair, ++ctr);
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
          logger.warn("Trying to remove non existent pair (" + pair + ")!");
          continue;
        } else if (ctr == 1) {
          filter.remove(pair);
        } else {
          filter.put(pair, --ctr);
        }
      }
    }
  }

  public <T extends Serializable> void changeParameter(String cmd, String pipeline, T args) {
    logger.info("Sending: " + cmd + ". Value = " + args);
    String[] dest = commandPipelineToComponentMap.get(cmd + "," + pipeline).split(",");
    ChangeParameterRequest<T> request =
        new ChangeParameterRequest<>(identifierPipelineMap.get(pipeline), dest[0], dest[1], args);

    logger.info("cmd: " + cmd + " pipeline: " + identifierPipelineMap.get(pipeline) + " component: "
                + dest[0] + " param: " + dest[1] + " value:" + args);

    synchronized (clientEndpoint) {
      clientEndpoint.schedule(request);
      responseStore.sentEvent(request);
    }
  }

  public void requestFinancialReplay(String cmd, String pipeline, String args) {

    // cmd = start,ticket,MM/dd/yyyy,HH:mm:ss,MM/dd/yyyy,HH:mm:ss,speed,query
    DateFormat df = new SimpleDateFormat("MM/dd/yyyy,HH:mm:ss");
    String[] argElements = args.split(",");

    boolean start = false;
    int ticket = -1;
    Date start_date = null;
    Date end_date = null;
    int speed = -1;

    try {
      start = argElements[0].equals("1");
      ticket = Integer.parseInt(argElements[1]);
      start_date = df.parse(argElements[2] + "," + argElements[3]);
      end_date = df.parse(argElements[4] + "," + argElements[5]);
      speed = Integer.parseInt(argElements[6]);
    } catch (ParseException e) {
      e.printStackTrace();
    }

    String query = "";

    if (argElements.length > 7) {
      for (int i = 7; i < argElements.length; i++) {
        query += ",";
        query += argElements[i];
      }
      query = query.substring(1);
    }

    ReplayMessage msg = new ReplayMessage(identifierPipelineMap.get(pipeline),
                                          commandPipelineToComponentMap.get(cmd + "," + pipeline),
                                          start,
                                          ticket);
    msg.setReplayStartInfo(start_date, end_date, speed, query);

    logger.info("start: " + start + " ticket: " + ticket + " start_date=" + start_date
                + " end_date: " + end_date + " speed: " + speed + " query: " + query);

    synchronized (clientEndpoint) {
      clientEndpoint.schedule(msg);
//      responseStore.sent(msg);
    }
  }

  public String[] requestHistoricalSentiment(String request) throws ParseException {

    // request format: MM/dd/YYYY,HH:mm:ss,MM/dd/YYYY,HH:mm:ss,player1,player2,...
    // response format: player,date,sentiment

    // parse request
    String[] args = request.split(",");
    Date startDate = dateFormat.parse(args[0]);
    Date endDate = dateFormat.parse(args[1]);

    String[] result = new String[args.length - 2];

    for (int i = 2; i < args.length; i++) {
      int playerId = Integer.parseInt(args[i]);
      TreeMap<Long, Integer[]> r = tweetSentimentConnector
          .getSentimentForMarketplayerDail(playerId, startDate, endDate);
      result[i - 2] = args[i];
      if (r.size() == 0) {
        result[i - 2] += "|" + null;
      }
      for (Map.Entry<Long, Integer[]> entry : r.entrySet()) {
        Date d = new Date(entry.getKey());

        result[i - 2] += "|" + dateFormat.format(d);
        for (int j = 0; j < entry.getValue().length; j++) {
          result[i - 2] += "," + entry.getValue()[j];
        }
      }
    }
    return result;
  }

  private void initializeMappings() {
    commandPipelineMap = new HashMap<>();
    commandPipelineMap.put(RESULT_SUBSCRIBE, new HashSet<String>());
    commandPipelineMap.get(RESULT_SUBSCRIBE).add("p");

    commandPipelineMap.put(RESULT_UNSUBSCRIBE, new HashSet<String>());
    commandPipelineMap.get(RESULT_UNSUBSCRIBE).add("p");

    commandPipelineMap.put(ADD_MARKET_PLAYER, new HashSet<String>());
    commandPipelineMap.get(ADD_MARKET_PLAYER).add("f");
    commandPipelineMap.get(ADD_MARKET_PLAYER).add("te");

    commandPipelineMap.put(REMOVE_MARKET_PLAYER, new HashSet<String>());
    commandPipelineMap.get(REMOVE_MARKET_PLAYER).add("f");
    commandPipelineMap.get(REMOVE_MARKET_PLAYER).add("te");

    commandPipelineMap.put(CHANGE_WINDOW_SIZE, new HashSet<String>());
    commandPipelineMap.get(CHANGE_WINDOW_SIZE).add("tt");
    commandPipelineMap.get(CHANGE_WINDOW_SIZE).add("te");
    commandPipelineMap.get(CHANGE_WINDOW_SIZE).add("f");
    commandPipelineMap.get(CHANGE_WINDOW_SIZE).add("d");
    commandPipelineMap.get(CHANGE_WINDOW_SIZE).add("p");

    commandPipelineMap.put(CHANGE_HUBLIST_SIZE, new HashSet<String>());
    commandPipelineMap.get(CHANGE_HUBLIST_SIZE).add("d");

    commandPipelineMap.put(CHANGE_CORRELATION_THRESHOLD, new HashSet<String>());
    commandPipelineMap.get(CHANGE_CORRELATION_THRESHOLD).add("f");
    commandPipelineMap.get(CHANGE_CORRELATION_THRESHOLD).add("d");
    commandPipelineMap.get(CHANGE_CORRELATION_THRESHOLD).add("tt");

    commandPipelineMap.put(REQUEST_SNAPSHOTS, new HashSet<String>());
    commandPipelineMap.get(REQUEST_SNAPSHOTS).add("tt");

    commandPipelineMap.put(REQUEST_FINANCIAL_REPLAY, new HashSet<String>());
    commandPipelineMap.get(REQUEST_FINANCIAL_REPLAY).add("te");

    commandPipelineMap.put(CHANGE_WINDOW_ADVANCE, new HashSet<String>());
    commandPipelineMap.get(CHANGE_WINDOW_ADVANCE).add("te");

    commandPipelineMap.put(CHANGE_DENSITY_SIZE, new HashSet<String>());
    commandPipelineMap.get(CHANGE_DENSITY_SIZE).add("te");

    identifierPipelineMap = new HashMap<>();
    identifierPipelineMap.put("p", "PriorityPip");
    identifierPipelineMap.put("d", "DynamicGraphPip");
    identifierPipelineMap.put("f", "FocusPip");
    identifierPipelineMap.put("te", "TransferPip");
    identifierPipelineMap.put("tt", "TimeTravelPip");

    commandPipelineToComponentMap = new HashMap<>();
    commandPipelineToComponentMap.put(ADD_MARKET_PLAYER + ",f", "SpringDataSource,playerList");
    commandPipelineToComponentMap.put(ADD_MARKET_PLAYER + ",te", "SpringDataSource,playerList");

    commandPipelineToComponentMap.put(REMOVE_MARKET_PLAYER + ",f", "SpringDataSource,playerList");
    commandPipelineToComponentMap.put(REMOVE_MARKET_PLAYER + ",te", "SpringDataSource,playerList");

    commandPipelineToComponentMap.put(CHANGE_WINDOW_SIZE + ",tt", "FinancialCorrelation,widowSize");
    commandPipelineToComponentMap
        .put(CHANGE_WINDOW_SIZE + ",te", "TransferEntropyCalculation,widowSize");
    commandPipelineToComponentMap.put(CHANGE_WINDOW_SIZE + ",f", "correlation,widowSize");
    commandPipelineToComponentMap
        .put(CHANGE_WINDOW_SIZE + ",d", "CorrelationComputation,widowSize");
    commandPipelineToComponentMap.put(CHANGE_WINDOW_SIZE + ",p", "FinancialCorrelation,widowSize");

    commandPipelineToComponentMap
        .put(CHANGE_HUBLIST_SIZE + ",d", "DynamicHubComputation,hubListSize");

    commandPipelineToComponentMap
        .put(CHANGE_CORRELATION_THRESHOLD + ",f", "DynamicGraphCompilation,correlationThreshold");
    commandPipelineToComponentMap
        .put(CHANGE_CORRELATION_THRESHOLD + ",d", "DynamicGraphCompilation,correlationThreshold");
    commandPipelineToComponentMap
        .put(CHANGE_CORRELATION_THRESHOLD + ",tt", "DynamicGraphCompilation,correlationThreshold");

    commandPipelineToComponentMap.put(REQUEST_SNAPSHOTS + ",tt", "queries,snapshotQuery");

    commandPipelineToComponentMap.put(REQUEST_FINANCIAL_REPLAY + ",te", "ReplaySink");

    commandPipelineToComponentMap
        .put(CHANGE_WINDOW_ADVANCE + ",te", "TransferEntropyCalculation,windowAdvance");
    commandPipelineToComponentMap
        .put(CHANGE_DENSITY_SIZE + ",te", "TransferEntropyCalculation,densitySize");
  }
}
