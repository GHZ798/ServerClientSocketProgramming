import java.io.*;
import java.lang.Object;
import java.net.*;
import java.security.cert.CRL;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class PartialHTTP1Server {

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

  public void run() {
    try (
      BufferedReader in = new BufferedReader(
        new InputStreamReader(this.socket.getInputStream())
      );
    ) {
      String inputLine;
      String status = "";
      List<String> response = new ArrayList<>();
      List<String> inputLines = new ArrayList<>();

      IP = this.socket.getInetAddress().getHostAddress();
      while ((inputLine = in.readLine()) != null) {
        if (inputLine.isEmpty()) {
          break;
        }
        inputLines.add(inputLine);
      }

      String[] newInput = inputLines.get(0).trim().split("\\s+");
      try {
        String method = newInput[0];
        String location = newInput[1];
        String version = newInput[2];

        if (version.split("/")[0].equals("HTTP")) {
          // btween - check
          if (
            0 <= Float.parseFloat(version.split("/")[1]) &&
            1.0 >= Float.parseFloat(version.split("/")[1])
          ) {
            switch (method) {
              case "GET":
                get(location);
                break;
              case "HEAD":
                head(location);
                break;
              case "POST":
                post(location, status);
                break;
              case "PUT":
                response.add("HTTP/1.0 " + response(7) + CRLF);
                Response(response);
                break;
              case "DELETE":
                response.add("HTTP/1.0 " + response(7) + CRLF);
                Response(response);
                break;
              case "LINK":
                response.add("HTTP/1.0 " + response(7) + CRLF);
                Response(response);
                break;
              case "UNLINK":
                response.add("HTTP/1.0 " + response(7) + CRLF);
                Response(response);
                break;
              default:
                response.add("HTTP/1.0 " + response(2) + CRLF);
                Response(response);
                break;
            }
          } else {
            response.add("HTTP/1.0 " + response(9) + CRLF);
            Response(response);
          }
        } else {
          response.add("HTTP/1.0 " + response(2) + CRLF);
          Response(response);
        }
      } catch (IndexOutOfBoundsException e) {
        response.add("HTTP/1.0 " + response(2) + CRLF);
        Response(response);
      }

      socket.close();
    } catch (IOException e) {
      System.out.println(
        "Exception caught when trying listen for a connection"
      );
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  // /index.html
  // status -> r

  public void get(String location) {
    List<String> response = new ArrayList<>();
    try {
      File file = new File(WORKING_DIR, location);

      if (!file.exists()) {
        // file dosent exist 404
        response.add("HTTP/1.0 " + response(4) + CRLF);
        Response(response);
      } else if (!file.canRead()) {
        // Forbidden 403
        response.add("HTTP/1.0 " + response(3) + CRLF);
        Response(response);
      } else {
        response.add("HTTP/1.0 " + response(0) + CRLF);

        String filetype = fileType(file.getName());
        response.add("Content-Type: " + filetype + CRLF);

        int filelength = (int) file.length(); // file length
        response.add("Content-Length: " + filelength + CRLF);

        long timeStamp = file.lastModified();
        DateFormat formatter = new SimpleDateFormat(
          "E, dd MMM yyyy hh:mm:ss zzz"
        );
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        String lastModified = formatter.format(calendar.getTime()); // last modified
        response.add("Last-Modified: " + lastModified + CRLF);

        System.out.println("Last-Modified: " + lastModified + CRLF);

        String contentIncoding = "identity"; // content incoding
        response.add("Content-Encoding: " + contentIncoding + CRLF);

        String allow = "GET, POST, HEAD"; // allow
        response.add("Allow: " + allow + CRLF);

        Calendar now = Calendar.getInstance();
        now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
        // System.out.println(formatter.format(now.getTime()));
        String expire = formatter.format(now.getTime()); // expire
        response.add("Expires: " + expire + CRLF);

        // writing the file
        byte[] payload = new byte[filelength];
        BufferedInputStream bis = new BufferedInputStream(
          new FileInputStream(file)
        );
        if (filelength != bis.read(payload)) {
          throw new IOException("Error reading requested file");
        }
        bis.close();

        Response(response, payload);
      }
    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  public List<String> post(String newInput, String r) {
    return null;
  }

  public void head(String location) {
    List<String> response = new ArrayList<>();
    try {
      File file = new File(WORKING_DIR, location);
      if (file.exists()) {
        if (file.canRead()) {
          response.add("HTTP/1.0 " + response(0) + CRLF);

          String filetype = fileType(file.getName());
          response.add("Content-Type: " + filetype + CRLF);

          int filelength = (int) file.length(); // file length
          response.add("Content-Length: " + filelength + CRLF);

          long timeStamp = file.lastModified();
          DateFormat formatter = new SimpleDateFormat(
            "E, dd MMM yyyy hh:mm:ss zzz"
          );
          formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
          Calendar calendar = Calendar.getInstance();
          calendar.setTimeInMillis(timeStamp);

          String lastModified = formatter.format(calendar.getTime()); // last modified
          response.add("Last-Modified: " + lastModified + CRLF);

          System.out.println("Last-Modified: " + lastModified + CRLF);

          String contentIncoding = "identity"; // content incoding
          response.add("Content-Encoding: " + contentIncoding + CRLF);

          String allow = "GET, POST, HEAD"; // allow
          response.add("Allow: " + allow + CRLF);

          Calendar now = Calendar.getInstance();
          now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
          // System.out.println(formatter.format(now.getTime()));
          String expire = formatter.format(now.getTime()); // expire
          response.add("Expires: " + expire + CRLF);
          Response(response);
        } else {
          // Forbidden 403
          response.add("HTTP/1.0 " + response(3) + CRLF);
          Response(response);
        }
      } else {
        // file dosent exist 404
        response.add("HTTP/1.0 " + response(4) + CRLF);
        Response(response);
      }
    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  //HEAD
  public void Response(List<String> response) {
    try {
      PrintStream ps = new PrintStream(this.socket.getOutputStream());
      if (response.size() < 6) {
        System.out.println(response.size());
        int curr = 0;
        while (!response.isEmpty()) {
          ps.printf("%s", response.get(curr));
          curr++;
        }
      } else {
        int curr = 0;
        while (curr < 7) {
          ps.printf("%s", response.get(curr));
          curr++;
        }
        ps.printf("%s", CRLF);
      }
    } catch (IOException e) {
      System.out.println("IO EXCEPTION");
      e.printStackTrace();
    }
  }

  //GET
  public void Response(List<String> response, byte[] payload) {
    try {
      PrintStream ps = new PrintStream(this.socket.getOutputStream());
      for (String x : response) {
        ps.printf("%s", x);
      }
      ps.printf("%s", CRLF);

      // String payload = response.get(curr);
      // byte[] content = response.get(curr);
      ps.write(payload);

      ps.printf("%s", CRLF);
      ps.flush();
    } catch (IOException e) {
      System.out.println("IO EXCEPTION");
      e.printStackTrace();
    }
  }

  public String response(int i) {
    String statusCode;
    switch (i) {
      case 0:
        statusCode = "200 OK";
        break;
      case 1:
        statusCode = "304 Not Modified";
        break;
      case 2:
        statusCode = "400 Bad Request";
        break;
      case 3:
        statusCode = "403 Forbidden";
        break;
      case 4:
        statusCode = "404 Not Found";
        break;
      case 5:
        statusCode = "408 Request Timeout";
        break;
      case 6:
        statusCode = "500 Internal Server Error";
        break;
      case 7:
        statusCode = "501 Not Implemented";
        break;
      case 8:
        statusCode = "503 Service Unavailable";
        break;
      case 9:
        statusCode = "505 HTTP Version Not Supported";
        break;
      default:
        statusCode = "ERR";
        break;
    }
    return statusCode;
  }

  public String fileType(String filename) {
    String filetype = "";
    if (filename.split("\\.").length < 2) {
      filetype = "application/octet-stream";
    } else {
      switch (filename.split("\\.")[1]) {
        case "txt":
          filetype = "text/plain";
          break;
        case "html":
          filetype = "text/html";
          break;
        case "gif":
          filetype = "image/gif";
          break;
        case "jpeg":
          filetype = "image/jpeg";
          break;
        case "png":
          filetype = "image/png";
          break;
        case "ocetet-stream":
          filetype = "application/octet-stream";
          break;
        case "pdf":
          filetype = "application/pdf";
          break;
        case "x-gzip":
          filetype = "application/x-gzip";
          break;
        case "zip":
          filetype = "application/zip";
          break;
        default:
          filetype = "application/octet-stream";
          break;
      } // content-type is working
    }
    return filetype;
  }
}
// StringBuilder sb = new StringBuilder();
// Scanner myReader = new Scanner(file);
// while (myReader.hasNextLine()) {
//   sb.append(myReader.nextLine()).append('\n');
// }
// String payload = sb.toString();
// response.add(payload);
// myReader.close();
// long IfModSince = file.lastModified();
// Calendar newCalendar = Calendar.getInstance();
// newCalendar.setTimeInMillis(IfModSince);
// String LastMod = formatter.format(newCalendar.getTime());
// if (lastModified.equals(LastMod)) {
//   response.set(0, response(0));
// } else {
//   response.set(0, response(1));
// }
// -----------------------------------------------------------
// the final output to the client.
// 0 -> status
// 1 -> header everthing // get post head
// String outputR;
// try {
//   // -- if no index 1 exists skip
//   System.out.println("Inside TRY");
//   response.get(1);
//   outputR = "HTTP/1.0 " + response.get(0) + CRLF + response.get(1) + CRLF;
// } catch (IndexOutOfBoundsException e) {
//   System.out.println("Inside CATCH");
//   outputR = "HTTP/1.0 " + response.get(0) + CRLF;
// }
// System.out.println("OUTPUT: " + outputR);
// Buff  VVV
// buffout.write(outputR);
// buffout.write(CRLF);
// in RESPONSE
// if (response.size() < 6) {
//   System.out.println(response.size());
//   int curr = 0;
//   while (!response.isEmpty()) {
//     ps.printf("%s", response.get(curr));
//     curr++;
//   }
// } else {
//   int curr = 0;
//   while (curr < 7) {
//     ps.printf("%s", response.get(curr));
//     curr++;
//   }
//   ps.printf("%s", CRLF);
//   // String payload = response.get(curr);
//   // byte[] content = response.get(curr);
//   ps.write(content);
// }
// ps.printf("%s", CRLF);
// ps.flush();
// response.clear(); // clear the storage
