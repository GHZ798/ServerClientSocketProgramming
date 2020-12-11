import java.io.*;
import java.net.*;
import java.text.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import javax.sound.midi.SysexMessage;

// Server class when it accepts the connection gets a socket which it passes on to the Thread
public class HTTP3Server {

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

// This is the Thread Class which does most of the computing
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

      // first line --
      String[] newInput = inputLines.get(0).trim().split("\\s+");
      try {
        String method = newInput[0];
        String location = newInput[1];
        String version = newInput[2];

        // depenfing on the method name GET HEAD OR POST it send the location of the file and req data to the appropriate function
        if (version.split("/")[0].equals("HTTP")) {
          if (
            0 <= Float.parseFloat(version.split("/")[1]) &&
            1.0 >= Float.parseFloat(version.split("/")[1])
          ) {
            // sends them to respective functions
            switch (method) {
              case "GET":
                if (inputLines.size() < 5) {
                  inputLines.remove(0);
                  getCookies(location, inputLines);
                  // getWiInputs(location, inputLines);
                } else {
                  System.out.println("IN ELSE");
                  getCookiesStored(location, inputLines);
                }
                break;
              case "HEAD":
                head(location);

                break;
              case "POST":
                inputLines.remove(0);
                post(location, inputLines, in);
                break;
              case "PUT":
                response.add("HTTP/1.0 " + Status(7) + CRLF);
                Response(response);
                break;
              case "DELETE":
                response.add("HTTP/1.0 " + Status(7) + CRLF);
                Response(response);
                break;
              case "LINK":
                response.add("HTTP/1.0 " + Status(7) + CRLF);
                Response(response);
                break;
              case "UNLINK":
                response.add("HTTP/1.0 " + Status(7) + CRLF);
                Response(response);
                break;
              default:
                response.add("HTTP/1.0 " + Status(2) + CRLF);
                Response(response);
                break;
            }
          } else {
            response.add("HTTP/1.0 " + Status(9) + CRLF);
            Response(response);
          }
        } else {
          response.add("HTTP/1.0 " + Status(2) + CRLF);
          Response(response);
        }
      } catch (IndexOutOfBoundsException e) {
        response.add("HTTP/1.0 " + Status(2) + CRLF);
        Response(response);
      }
      socket.close(); // sockets is closed
    } catch (IOException e) {
      System.out.println(
        "Exception caught when trying listen for a connection"
      );
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  // POST Function
  public void post(String location, List<String> inputLines, BufferedReader in)
    throws IOException {
    List<String> response = new ArrayList<>();
    int CONTENT_LENGTH = 0;
    String CONTENT_TYPE = "";

    // Checks for Content Type
    try {
      // Content-Type
      String ct = inputLines.get(2);
      String ctHeader = ct.split(":", 2)[0];
      String ctValue = ct.split(":", 2)[1].trim();

      if (ctHeader.equals("Content-Type")) {
        CONTENT_TYPE = ctValue;
      } else {
        response.add("HTTP/1.0 " + Status(10) + CRLF);
        Response(response); // Response function send the data from response to the client
        System.out.println("in else CT");

        return;
      }
    } catch (IndexOutOfBoundsException e) {
      response.add("HTTP/1.0 " + Status(6) + CRLF);
      Response(response);
      System.out.println("in else CT");
    }

    // Checks for Content Length
    try {
      // Content-Length
      String cl = inputLines.get(3);
      String clHeader = cl.split(":", 2)[0];
      String clValue = cl.split(":", 2)[1].trim();

      if (clHeader.equals("Content-Length")) {
        CONTENT_LENGTH = Integer.parseInt(clValue);
      } else {
        response.add("HTTP/1.0 " + Status(10) + CRLF);
        Response(response);
        System.out.println("in else CL");

        return;
      }
    } catch (IndexOutOfBoundsException e) {
      response.add("HTTP/1.0 " + Status(10) + CRLF);
      Response(response);
      System.out.println("in else CT");
    }

    // Deals with the CGI and payload for the POST
    File file = new File(WORKING_DIR, location);
    char[] data = new char[CONTENT_LENGTH * 2];
    in.read(data);
    String param = (new String(data)).trim().replaceAll("!(.)", "$1");
    String cmd = WORKING_DIR.getAbsolutePath() + File.separatorChar + location;

    // variables to be used for exec
    String[] vars = new String[] {
      "CONTENT_LENGTH=" + CONTENT_LENGTH,
      "SCRIPT_NAME=" + file.getName(),
      "SERVER_NAME=" + IP,
      "SERVER_PORT=" + PORT,
      "HTTP_FROM=" + inputLines.get(0).split(":", 2)[1].trim(),
      "HTTP_USER_AGENT=" + inputLines.get(1).split(":", 2)[1].trim(),
    };

    // checks if the file ext is cgi
    if (!"cgi".equals(file.getName().split("\\.")[1])) {
      System.out.println("INSIDE IF !!");

      response.add("HTTP/1.0 " + Status(11) + CRLF);
      Response(response);
      return;
    }
    // checks if the file can be exec
    else if (!file.canExecute()) {
      System.out.println("INSIDE ELSE IF !!");
      System.out.println(file.getName());

      response.add("HTTP/1.0 " + Status(3) + CRLF);
      Response(response);
      return;
    }
    // case when the extention is cgi and the file can be exec
    else {
      System.out.println("INSIDE ELSE !!");
      response.add("HTTP/1.0 " + Status(0) + CRLF);
      response.add("Content-Length: " + CONTENT_LENGTH + CRLF);
      response.add("Content-Type: " + "text/html" + CRLF);
      String allow = "GET, POST, HEAD"; // allow
      response.add("Allow: " + allow + CRLF);

      DateFormat formatter = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss z",
        Locale.US
      );
      formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
      Calendar now = Calendar.getInstance();
      now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
      String expire = formatter.format(now.getTime()); // expire
      response.add("Expires: " + expire + CRLF);

      StringBuilder sb = new StringBuilder();
      Process process = Runtime.getRuntime().exec(cmd, vars, WORKING_DIR);

      try (
        BufferedWriter bw = new BufferedWriter(
          new OutputStreamWriter(process.getOutputStream())
        );
        BufferedReader br = new BufferedReader(
          new InputStreamReader(process.getInputStream())
        );
      ) {
        bw.write(param, 0, param.length());
        bw.flush();
        bw.close();
        process.waitFor();
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line).append('\n');
        }
      } catch (InterruptedException e) {
        response.clear();
        response.add("HTTP/1.0 " + Status(6) + CRLF);
        Response(response);
        return;
      }
      String payload = sb.toString();
      if (payload.trim().isEmpty()) {
        response.clear();
        response.add("HTTP/1.0 " + Status(12) + CRLF);
        Response(response);
        return;
      }

      if (payload.trim().isEmpty()) {
        response.clear();
        response.add("HTTP/1.0 " + Status(6) + CRLF);
        Response(response);
        return;
      }

      byte[] responseBytes = payload.getBytes();
      //finaly the result from the cgi is saved as bytes and sent to the Response function to send to the client
      Response(response, responseBytes);
    }
  }

  // GET Function
  public void get(String location) {
    List<String> response = new ArrayList<>();
    try {
      File file = new File(WORKING_DIR, location);

      if (!file.exists()) {
        // file dosent exist 404
        response.add("HTTP/1.0 " + Status(4) + CRLF);
        Response(response);
        return;
      } else if (!file.canRead()) {
        // file cant be read forbidden 403
        response.add("HTTP/1.0 " + Status(3) + CRLF);
        Response(response);
        return;
      } else {
        // the file exists and can be read
        response.add("HTTP/1.0 " + Status(0) + CRLF);

        // Content Type
        String filetype = fileType(file.getName());
        response.add("Content-Type: " + filetype + CRLF);

        // Content Length
        int filelength = (int) file.length(); // file length
        response.add("Content-Length: " + filelength + CRLF);

        long timeStamp = file.lastModified();
        DateFormat formatter = new SimpleDateFormat(
          "EEE, dd MMM yyyy HH:mm:ss z",
          Locale.US
        );
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        // Last Modified
        String lastModified = formatter.format(calendar.getTime()); // last modified
        response.add("Last-Modified: " + lastModified + CRLF);

        // Content-Encoding
        String contentIncoding = "identity"; // content incoding
        response.add("Content-Encoding: " + contentIncoding + CRLF);

        // Allowed methods
        String allow = "GET, POST, HEAD"; // allow
        response.add("Allow: " + allow + CRLF);

        // Expire date
        Calendar now = Calendar.getInstance();
        now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
        String expire = formatter.format(now.getTime()); // expire
        response.add("Expires: " + expire + CRLF);

        // getting the paylod for the get
        byte[] payload = new byte[filelength];
        BufferedInputStream bis = new BufferedInputStream(
          new FileInputStream(file)
        );
        if (filelength != bis.read(payload)) {
          throw new IOException("Error reading requested file");
        }
        bis.close();

        // sending the header and the payload to the Response function
        Response(response, payload);
      }
    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  // GET method with header inputs from the Client
  public void getWiInputs(String location, List<String> inputLines) {
    List<String> response = new ArrayList<>();
    try {
      File file = new File(WORKING_DIR, location);

      if (!file.exists()) {
        // file dosent exist 404
        response.add("HTTP/1.0 " + Status(4) + CRLF);
        Response(response);
      } else if (!file.canRead()) {
        // Forbidden 403
        response.add("HTTP/1.0 " + Status(3) + CRLF);
        Response(response);
      } else {
        response.add("HTTP/1.0 " + Status(0) + CRLF);

        String filetype = fileType(file.getName());
        response.add("Content-Type: " + filetype + CRLF);

        int filelength = (int) file.length(); // file length
        response.add("Content-Length: " + filelength + CRLF);

        long timeStamp = file.lastModified();
        DateFormat formatter = new SimpleDateFormat(
          "EEE, dd MMM yyyy HH:mm:ss z",
          Locale.US
        );
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        String lastModified = formatter.format(calendar.getTime()); // last modified
        response.add("Last-Modified: " + lastModified + CRLF);

        // System.out.println("Last-Modified: " + lastModified + CRLF);

        String contentIncoding = "identity"; // content incoding
        response.add("Content-Encoding: " + contentIncoding + CRLF);

        String allow = "GET, POST, HEAD"; // allow
        response.add("Allow: " + allow + CRLF);

        Calendar now = Calendar.getInstance();
        now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
        String expire = formatter.format(now.getTime()); // expire
        response.add("Expires: " + expire + CRLF);

        // input HEADERS

        Date lastmod = Date.from(Instant.ofEpochMilli(file.lastModified()));

        // Looks at the inputed lines from the client
        for (String x : inputLines) {
          String header = x.split(":", 2)[0];
          String date = x.split(":", 2)[1];
          try {
            if (header.equals("If-Modified-Since")) {
              Date ifmod = formatter.parse(date.trim());
              if (lastmod.before(ifmod)) {
                response.clear();
                response.add("HTTP/1.0 " + Status(1) + CRLF);
                response.add("Expires: " + expire + CRLF);
                Response(response);
                return;
              }
            }
          } catch (ParseException ignore) {}
        }

        // getting the paylod for the get
        byte[] payload = new byte[filelength];
        BufferedInputStream bis = new BufferedInputStream(
          new FileInputStream(file)
        );
        if (filelength != bis.read(payload)) {
          throw new IOException("Error reading requested file");
        }
        bis.close();

        // sends the response and the payload to the Response method to send it to the Client
        Response(response, payload);
      }
    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  public void getCookies(String location, List<String> lines) {
    List<String> response = new ArrayList<>();
    try {
      File file = new File(WORKING_DIR, location + "index.html");
      response.add("HTTP/1.0 " + Status(0) + CRLF);
      String filetype = fileType(file.getName());
      response.add("Content-Type: " + filetype + CRLF);
      LocalDateTime myDateObj = LocalDateTime.now();
      DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(
        "Y-MM-d HH:mm:ss"
      );
      String formattedDate = myDateObj.format(myFormatObj);
      // System.out.printf("Formatted date+time %s \n",formattedDate);

      String encodedDateTime = URLEncoder.encode(formattedDate, "UTF-8");
      // System.out.printf("URL encoded date-time %s \n",encodedDateTime);

      String decodedDateTime = URLDecoder.decode(encodedDateTime, "UTF-8");
      // System.out.printf("URL decoded date-time %s \n",decodedDateTime);

      response.add("Set-Cookie: lasttime=" + encodedDateTime + CRLF);
      int filelength = (int) file.length(); // file length

      byte[] payload = new byte[filelength];
      BufferedInputStream bis = new BufferedInputStream(
        new FileInputStream(file)
      );
      if (filelength != bis.read(payload)) {
        throw new IOException("Error reading requested file");
      }
      bis.close();

      Response(response, payload);
      // for(String x: response){
      //   System.out.println(x);
      // }

    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  public void getCookiesStored(String location, List<String> lines) {
    List<String> response = new ArrayList<>();
    try {
      File file = new File(WORKING_DIR, location + "index_seen.html");
      response.add("HTTP/1.0 " + Status(0) + CRLF);
      String filetype = fileType(file.getName());
      response.add("Content-Type: " + filetype + CRLF);
      LocalDateTime myDateObj = LocalDateTime.now();
      DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(
        "Y-MM-d HH:mm:ss"
      );

      String encodedDateTimeCookie = "";
      for(String x: lines){
        if(x.split(" ")[0].equals("Cookie:")){
          encodedDateTimeCookie = x.split(" ")[1].split("=")[1];
        }
      }

      String formattedDate = myDateObj.format(myFormatObj);
      // System.out.printf("Formatted date+time %s \n",formattedDate);

      String encodedDateTime = URLEncoder.encode(formattedDate, "UTF-8");
      // System.out.printf("URL encoded date-time %s \n",encodedDateTime);

      String decodedDateTime = URLDecoder.decode(encodedDateTimeCookie, "UTF-8");
      System.out.printf("URL decoded date-time %s \n",decodedDateTime);

      response.add("Set-Cookie: lasttime=" + encodedDateTime + CRLF);
      int filelength = (int) file.length(); // file length

      byte[] payload = new byte[filelength];
      BufferedInputStream bis = new BufferedInputStream(
        new FileInputStream(file)
      );
      if (filelength != bis.read(payload)) {
        throw new IOException("Error reading requested file");
      }
      bis.close();

      Response(response, payload);
      // for(String x: response){
      //   System.out.println(x);
      // }

    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  // HEAD method
  public void head(String location) {
    List<String> response = new ArrayList<>();
    try {
      File file = new File(WORKING_DIR, location);

      if (!file.exists()) {
        // file dosent exist 404
        response.add("HTTP/1.0 " + Status(4) + CRLF);
        Response(response);
      } else if (!file.canRead()) {
        // Forbidden 403
        response.add("HTTP/1.0 " + Status(3) + CRLF);
        Response(response);
      } else {
        response.add("HTTP/1.0 " + Status(0) + CRLF);

        String filetype = fileType(file.getName());
        response.add("Content-Type: " + filetype + CRLF);

        int filelength = (int) file.length(); // file length
        response.add("Content-Length: " + filelength + CRLF);

        long timeStamp = file.lastModified();
        DateFormat formatter = new SimpleDateFormat(
          "EEE, dd MMM yyyy HH:mm:ss z",
          Locale.US
        );
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        String lastModified = formatter.format(calendar.getTime()); // last modified
        response.add("Last-Modified: " + lastModified + CRLF);

        String contentIncoding = "identity"; // content incoding
        response.add("Content-Encoding: " + contentIncoding + CRLF);

        String allow = "GET, POST, HEAD"; // allow
        response.add("Allow: " + allow + CRLF);

        Calendar now = Calendar.getInstance();
        now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
        String expire = formatter.format(now.getTime()); // expire
        response.add("Expires: " + expire + CRLF);

        // sends the response to the Response method to send it to the Client
        Response(response);
      }
    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  // Response Function deals with sending the header items to the Client
  public void Response(List<String> response) {
    try {
      System.out.println("INSIDE REGUALR OUTPUT: ");

      for (String x : response) {
        System.out.println(x);
      }
      PrintStream ps = new PrintStream(this.socket.getOutputStream());
      for (String x : response) {
        ps.printf("%s", x);
      }
      ps.printf("%s", CRLF);
      // if (response.size() < 6) {
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
      // }
    } catch (IOException e) {
      System.out.println("IO EXCEPTION");
      e.printStackTrace();
    }
  }

  // Response Function deals with sending the header items and the payload to the Client
  public void Response(List<String> response, byte[] payload) {
    try {
      PrintStream ps = new PrintStream(this.socket.getOutputStream());

      // System.out.println();
      // System.out.println("INSIDE PAYLOAD OUTPUT: ");
      // System.out.println("SIZE: " + response.size());

      // for (String x : response) {
      //   System.out.print(x);
      // }
      for (String x : response) {
        ps.printf("%s", x);
      }
      ps.printf("%s", CRLF);

      // String payload = response.get(curr);
      // byte[] content = response.get(curr);
      if (payload != null) {
        ps.write(payload);
      }

      ps.flush();
    } catch (IOException e) {
      System.out.println("IO EXCEPTION");
      e.printStackTrace();
    }
  }

  // gives the status code
  public String Status(int i) {
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
      case 10:
        statusCode = "411 Length Required";
        break;
      case 11:
        statusCode = "405 Method Not Allowed";
        break;
      case 12:
        statusCode = "204 No Content";
        break;
      default:
        statusCode = "ERR";
        break;
    }
    return statusCode;
  }

  // gives file type, takes in the name of the file
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
