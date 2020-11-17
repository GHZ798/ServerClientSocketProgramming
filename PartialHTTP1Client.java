import java.io.*;
import java.net.*;

public class PartialHTTP1Client {
  public static void main(String[] args)
    throws UnknownHostException, IOException {
    if (args.length != 2) {
      System.err.println(
        "Usage: java partialHTTPclient <host name> <port number>"
      );
      System.exit(1);
    }

    String hostName = args[0];
    int portNumber = Integer.parseInt(args[1]);

    try (
      Socket echoSocket = new Socket(hostName, portNumber);
      BufferedWriter out = new BufferedWriter(
        new OutputStreamWriter(echoSocket.getOutputStream())
      );
      BufferedReader in = new BufferedReader(
        new InputStreamReader(echoSocket.getInputStream())
      );
      BufferedReader stdIn = new BufferedReader(
        new InputStreamReader(System.in)
      )
    ) {
      String userInput;
      String line;
      while ((userInput = stdIn.readLine()) != null) {
        out.write(userInput);
        out.newLine();
        out.flush();
        System.out.println();
        while ((line = in.readLine()) != null) {
          if (line.equals("null")) {
            System.out.println();
            break;
          } else {
            System.out.println(line);
          }
        }
      }
      out.close();
    } catch (UnknownHostException e) {
      System.err.println("Don't know about host " + hostName);
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Couldn't get I/O for the connection to " + hostName);
      System.exit(1);
    }
  }
}
// java -cp . PartialHTTP1Client.java localhost 8000
// java -cp . PartialHTTP1Server.java 8000
// java -jar HTTPServerTester.jar localhost 8000
// HEAD text.txt HTTP/1.0
