import java.io.*;
import java.net.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class TestServer {

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: java EchoServer <port number>");
      System.exit(1);
    }
    List<Future<?>> futures = new ArrayList<Future<?>>();
    ExecutorService pool = Executors.newFixedThreadPool(50);
    ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));
    System.out.println("Server ready for connection ...");
    while (true) {
      if (futures.size() < 50) {
        Socket clientSocket = serverSocket.accept();
        Future<?> f = pool.submit(
          new ServerThread(clientSocket, Integer.parseInt(args[0]))
        );
        futures.add(f);
      } else {
        Socket clientSocket = serverSocket.accept();
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("HTTP/1.0 503 Service Unavailable");
      }
    }
  }
}

class ServerThread extends Thread {

  Socket socket;
  final char CR = (char) 0x0D;
  final char LF = (char) 0x0A;
  final String CRLF = "" + CR + LF; // "" forces conversion to string
  private static final File WORKING_DIR = new File(
    System.getProperty("user.dir")
  );
  public String IP;
  public int PORT;

  // Constructor
  ServerThread(Socket socket, int port) {
    this.socket = socket;
    this.PORT = port;
    try {
      socket.setSoTimeout(5);
    } catch (SocketException e) {
      System.err.println("Error setting timeout on socket: ");
      e.printStackTrace();
    }
  }

  // Run method where all the computation happens.
  public void run() {
    try (
      BufferedReader in = new BufferedReader(
        new InputStreamReader(this.socket.getInputStream())
      );
    ) {
      String inputLine;
      List<String> response = new ArrayList<>();
      List<String> inputLines = new ArrayList<>();
      IP = this.socket.getInetAddress().getHostAddress();

      // reads all the lines sent in from the client
      while ((inputLine = in.readLine()) != null) {
        System.out.println(inputLine);
        if (inputLine.isEmpty()) {
          break;
        }
        inputLines.add(inputLine);
      }
    }
    catch(IOException e){
        e.printStackTrace();
    }
  }
}
