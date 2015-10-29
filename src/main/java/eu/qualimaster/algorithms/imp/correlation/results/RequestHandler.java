package eu.qualimaster.algorithms.imp.correlation.results;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ap0n on 1/15/15.
 */
public class RequestHandler {
  List<DataConsumerDataHandler> resultsBoard;
  String allFinancialSymbols;  // TODO(ap0n): Remove this one
  String allWebSymbols;  // TODO(ap0n): Remove this one
  Lock symbolsLock;  // TODO(ap0n): Remove this one

  Logger logger = LoggerFactory.getLogger(Server.class);

  public RequestHandler() {
    resultsBoard = new ArrayList<DataConsumerDataHandler>();

    symbolsLock = new ReentrantLock();
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

  public void updateFinancialSymbolsList(String newSymbolsList) {
    symbolsLock.lock();
    allFinancialSymbols = newSymbolsList;
    symbolsLock.unlock();
  }

  public void updateWebSymbolsList(String newSymbolsList) {
    symbolsLock.lock();
    allWebSymbols = newSymbolsList;
    symbolsLock.unlock();
  }

  public void sendAllSymbols(OutputStream consumerOutputStream) {
    PrintWriter writer = new PrintWriter(consumerOutputStream);
    symbolsLock.lock();
    writer.println("quoteList_response," + allFinancialSymbols);
    writer.flush();
    writer.println("quoteList_response," + allWebSymbols);
    writer.flush();
    symbolsLock.unlock();
  }
}
