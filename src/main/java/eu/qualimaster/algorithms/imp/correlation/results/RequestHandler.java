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
  List<OutputStream> resultsBoard;
  Lock resultsBoardLock;
  List<String> allFinancialSymbols;
  List<String> allWebSymbols;
  Lock symbolsLock;

  Logger logger = LoggerFactory.getLogger(Server.class);

  public RequestHandler() {
    resultsBoard = new ArrayList<OutputStream>();
    resultsBoardLock = new ReentrantLock();

    symbolsLock = new ReentrantLock();
  }

  public void subscribeToResultsBoard(OutputStream consumerOutputStream) {
    resultsBoardLock.lock();
    if (!resultsBoard.contains(consumerOutputStream)) {
      resultsBoard.add(consumerOutputStream);
    }
    resultsBoardLock.unlock();
  }

  public void unsubscribeFromResultsBoard(OutputStream consumerOutputStream) {
    resultsBoardLock.lock();
    resultsBoard.remove(consumerOutputStream);
    resultsBoardLock.unlock();
  }

  public void publishToResultsBoard(String result) {
    resultsBoardLock.lock();
    for (OutputStream s : resultsBoard) {
      PrintWriter writer = new PrintWriter(s, true);
      writer.println("resultsSubscribe_response" + "," + result);
      writer.flush();
    }
    resultsBoardLock.unlock();
  }

  public void updateFinancialSymbolsList(List<String> newSymbolsList) {
    symbolsLock.lock();
    allFinancialSymbols = new ArrayList<String>(newSymbolsList);
//    allFinancialSymbols = newSymbolsList;
    symbolsLock.unlock();
  }

  public void updateWebSymbolsList(List<String> newSymbolsList) {
    symbolsLock.lock();
    allWebSymbols = newSymbolsList;
    symbolsLock.unlock();
  }

  public void sendAllSymbols(OutputStream consumerOutputStream) {
    PrintWriter writer = new PrintWriter(consumerOutputStream);
    symbolsLock.lock();
    String allSymbols = "f,";
    if (allFinancialSymbols != null) {
      for (String s : allFinancialSymbols) {
        allSymbols += s + "!";
      }
    }
    writer.println("quoteList_response," + allSymbols);
    writer.flush();

    allSymbols = "w,";
    if (allWebSymbols != null) {
      for (String s : allWebSymbols) {
        allSymbols += s + "!";
      }
    }
    writer.println("quoteList_response," + allSymbols);
    writer.flush();
    symbolsLock.unlock();
  }
}
