package ConsumerTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by ap0n on 1/15/15.
 */
public class Client {

  Socket socket;
  PrintWriter writer;
  BufferedReader reader;
  Boolean readingResults;
  Boolean readingList;

  public Client() throws IOException {
    socket = new Socket("snf-618466.vm.okeanos.grnet.gr", 8889);
//    socket = new Socket("localhost", 8889);
//    socket = new Socket("clu01.softnet.tuc.gr", 8889);
    writer = new PrintWriter(socket.getOutputStream(), true);
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  public void stressTest() throws InterruptedException, IOException {
    Thread cliThread = new Thread(new Cli());
    cliThread.start();

    while (true) {
      String line = reader.readLine();
      if (line == null) {
        break;
      }
      System.out.println(line);
    }
  }

  private class Cli implements Runnable {

    public void run() {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      try {
        while (true) {
          String line = in.readLine();
          if (line.startsWith("s,")) {
            readingResults = true;
            String command = "resultsSubscribe" + line.substring(1) + "!";
            writer.println(command);
            writer.flush();
          } else if (line.startsWith("st,")) {
            String command = "resultsUnsubscribe" + line.substring(2) + "!";
            writer.println(command);
            writer.flush();
          } else if (line.equals("l")) {
            writer.println("quoteList!");
            writer.flush();
          } else if (line.startsWith("ap,")) {
            writer.println("addMarketplayer" + line.substring(2) + "!");
            writer.flush();
          } else if (line.startsWith("rp,")) {
            writer.println("removeMarketplayer" + line.substring(2) + "!");
            writer.flush();
          } else if (line.equals("x")) {  // close socket
            System.out.println("gracefully ending connection");
            socket.getOutputStream().close();
            socket.getInputStream().close();
            socket.close();
            break;
          } else if (line.equals("d")) {  // disconnect
            System.out.println("forcefully ending connection");
            System.exit(0);
          } else if (line.startsWith("cws,")) {
            writer.println("changewindowSize," + line.substring(4) + "!");
          } else if (line.startsWith("chs,")) {
            writer.println("changehubListSize," + line.substring(4) + "!");
          } else {
            writer.println(line + "!");
            writer.flush();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void initialTesting() throws IOException {
    while (true) {
      System.out.print("Consumer> ");
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String line = in.readLine();

      if (line.equals("s")) {  // start results
        System.out.println("Start results");
        writer.println("startResults");
        writer.flush();
        int countResult = 10;
        while (--countResult >= 0) {
          System.out.println(reader.readLine());
        }
      } else if (line.equals("st")) {  // stop results
        System.out.println("Stop results");
        writer.println("stopResults");
        writer.flush();
      } else if (line.equals("l1")) {  // send symbol list
        System.out.println("Send symbols list");
        writer.println("sendSymbolsList");
        writer.flush();
        int countResult = 10;
        while (--countResult >= 0) {
          System.out.println(reader.readLine());
        }
      } else if (line.equals("l2")) {  // send symbol list
        System.out.println("Send symbols list");
        writer.println("sendSymbolsList");
        writer.flush();
        int countResult = 20;
        while (--countResult >= 0) {
          System.out.println(reader.readLine());
        }
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

  public static void main(String[] args) throws IOException, InterruptedException {
    Client c = new Client();

    c.stressTest();
  }
}
