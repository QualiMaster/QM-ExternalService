package ProducerTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Random;

/**
* Created by ap0n on 1/15/15.
*/
public class Client {

  Socket socket;
  PrintWriter writer;
  Random random;

  public Client() throws IOException {
    socket = new Socket("localhost", 8888);
    writer = new PrintWriter(socket.getOutputStream(), true);
    random = new Random(new Date().getTime());
  }

  public void stressTest(Boolean financial) throws IOException {

    String symbols = "l,f,";
    if (financial) {
      for (int i = 0; i < 10; ++i) {
        symbols += "fs" + String.valueOf(i) + "!";
      }
    } else {
      symbols = "l,w,";
      for (int i = 0; i < 10; ++i) {
        symbols += "ws" + String.valueOf(i) + "!";
      }
    }
    writer.println(symbols);

    while (true) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        // continue
      }

      int rounds = 10;
      while (rounds-- > 0) {
        if (financial) {
          writer.println("f,symbol1,symbol2," + String.valueOf(random.nextDouble()));
        } else {
          writer.println("w,symbol1,symbol2," + String.valueOf(random.nextDouble()));
        }
      }
    }
  }

  public void initialTest() throws IOException {

//    while (true) {
//      System.out.print("Producer> ");
//      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//      String line = in.readLine();
//
//      if (line.equals("fr")) {  // financial result
//        System.out.println("financial result");
//        writer.println("f,symbol1,symbol2," + String.valueOf(random.nextDouble()));
//
//      } else if (line.equals("wr")) {  // web result
//        System.out.println("web result");
//        writer.println("w,symbol1,symbol2," + String.valueOf(random.nextDouble()));
//
//      } else if (line.equals("fs")) {  // financial symbols
//        System.out.println("financial symbols");
//        List<String> financialSymbols = new ArrayList<String>();
//        for (int i = 0; i < 10; ++i) {
//          financialSymbols.add("fin" + String.valueOf(i));
//        }
//        financialSymbols.add(0, "f");
//        writer.println(financialSymbols);
//
//      } else if (line.equals("ws")) {  // web symbols
//        System.out.println("web symbols");
//        List<String> webSymbols = new ArrayList<String>();
//        for (int i = 0; i < 10; ++i) {
//          webSymbols.add("w" + String.valueOf(i));
//        }
//        webSymbols.add(0, "w");
//
//        writer.println(webSymbols);
//
//      } else if (line.equals("x")) {  // close socket
//        System.out.println("gracefully ending connection");
//        socket.getOutputStream().close();
//        socket.getInputStream().close();
//        socket.close();
//        break;
//      } else if (line.equals("d")) {  // disconnect
//        System.out.println("forcefully ending connection");
//        break;
//      }
//    }
  }

  public static void main(String[] args) throws IOException {

    Client c = new Client();

//    c.initialTest();
    c.stressTest(Boolean.valueOf(args[0]));
  }
}
