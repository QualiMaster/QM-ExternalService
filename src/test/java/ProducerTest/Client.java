package ProducerTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by ap0n on 1/15/15.
 */
public class Client {

  Socket socket;
  ObjectOutputStream objectOutputStream;
  Random random;

  public Client() throws IOException {
    socket = new Socket("localhost", 8888);
    objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    random = new Random(new Date().getTime());
  }

  public void stressTest(Boolean financial) throws IOException {

    List<String> symbols = new ArrayList<String>();
    if (financial) {
      for (int i = 0; i < 10; ++i) {
        symbols.add("fin" + String.valueOf(i));
      }
      symbols.add(0, "f");
    } else {
      for (int i = 0; i < 10; ++i) {
        symbols.add("w" + String.valueOf(i));
      }
      symbols.add(0, "w");
    }

    objectOutputStream.writeObject(symbols);

    while (true) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        // continue
      }

      int rounds = 10;
      while (rounds-- > 0) {
        if (financial) {
          objectOutputStream
              .writeObject("f,symbol1,symbol2," + String.valueOf(random.nextDouble()));
        } else {
          objectOutputStream
              .writeObject("w,symbol1,symbol2," + String.valueOf(random.nextDouble()));
        }
      }
    }
  }

  public void initialTest() throws IOException {

    while (true) {
      System.out.print("Producer> ");
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String line = in.readLine();

      if (line.equals("fr")) {  // financial result
        System.out.println("financial result");
        objectOutputStream.writeObject("f,symbol1,symbol2," + String.valueOf(random.nextDouble()));

      } else if (line.equals("wr")) {  // web result
        System.out.println("web result");
        objectOutputStream.writeObject("w,symbol1,symbol2," + String.valueOf(random.nextDouble()));

      } else if (line.equals("fs")) {  // financial symbols
        System.out.println("financial symbols");
        List<String> financialSymbols = new ArrayList<String>();
        for (int i = 0; i < 10; ++i) {
          financialSymbols.add("fin" + String.valueOf(i));
        }
        financialSymbols.add(0, "f");
        objectOutputStream.writeObject(financialSymbols);

      } else if (line.equals("ws")) {  // web symbols
        System.out.println("web symbols");
        List<String> webSymbols = new ArrayList<String>();
        for (int i = 0; i < 10; ++i) {
          webSymbols.add("w" + String.valueOf(i));
        }
        webSymbols.add(0, "w");

        objectOutputStream.writeObject(webSymbols);

      } else if (line.equals("x")) {  // close socket
        System.out.println("gracefully ending connection");
        socket.getOutputStream().close();
        socket.getInputStream().close();
        socket.close();
        break;
      } else if (line.equals("d")) {  // disconnect
        System.out.println("forcefully ending connection");
        break;
      }
    }
  }

  public static void main(String[] args) throws IOException {

    Client c = new Client();

//    c.initialTest();
    c.stressTest(Boolean.valueOf(args[0]));
  }
}
