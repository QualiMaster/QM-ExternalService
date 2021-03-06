package eu.qualimaster.comserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by ap0n on 1/14/15.
 */
public class DataProducerDataHandler implements IDataHandler {

  RequestHandler requestHandler;
  Socket socket;
  BufferedReader reader;

  Logger logger = LoggerFactory.getLogger(DataProducerDataHandler.class);

  public DataProducerDataHandler(RequestHandler requestHandler, Socket socket) throws IOException {
    this.socket = socket;
    this.requestHandler = requestHandler;
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    logger.info("Producer connected from: " + socket.getInetAddress().getHostAddress());
  }

  public void run() {
    logger.info(this.getClass().getName() + " started.");
    while (true) {
      String received;
      try {
        received = reader.readLine();
      } catch (IOException e) {
        e.printStackTrace();
        logger.error(e.getMessage(), e);
        break;
      }
      if (received == null) {
        break;
      }
      if (received.startsWith("f,") || (received.startsWith("w,"))) {  // result
        requestHandler.publishCorrelationResult(received);
//        requestHandler.publishFocusResult(received);
      } else if (received.startsWith("hubList,")) {
        requestHandler.publishUnfilteredResult(received);
      } else if (received.startsWith("focusPip,")) {
        requestHandler.publishUnfilteredResult(received.substring(9));
      } else if (received.startsWith("snapshots,")) {
        requestHandler.publishUnfilteredResult(received.substring(10));
      } else if (received.startsWith("te,")) {
        requestHandler.publishUnfilteredResult(received.substring(3));
      } else {  // Unknown
        logger.error("Unknown message type received: " + received);
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
