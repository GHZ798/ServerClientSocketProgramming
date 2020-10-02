import java.net.*;
import java.text.*;
import java.time.Year;
import java.util.*;
import java.io.*;
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
            Socket clientSocket = serverSocket.accept();
            Future<?> f = pool.submit(new ServerThread(clientSocket));
            futures.add(f);
            System.out.println(futures.size()); // gives the size of the list of threads
        }
    }
}

class ServerThread extends Thread {
    Socket socket;

    ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        System.out.println("IN THREAD");
        try (PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));) {
            String inputLine;
            String status;
            while ((inputLine = in.readLine()) != null) {
                String[] newInput = inputLine.trim().split("\\s+");
                for (int i = 0; i < newInput.length; i++) {
                    System.out.println(newInput[i]);
                }
                if (newInput[2].equals("HTTP") || newInput[2].equals("HTTP/")) {
                    out.println("HTTP/1.0" + response(0));
                } else if (newInput[2].split("/")[0].equals("HTTP")) {
                    if (0 <= Float.parseFloat(newInput[2].split("/")[1])
                            && 1.0 >= Float.parseFloat(newInput[2].split("/")[1])) {
                        System.out.println("yes 0 to 1.0 good"); // good

                        if (newInput[0].equals("GET")) {
                            // get method
                            // args newInput[1] and out
                            get(newInput[1], out);
                            System.out.println("GET METHOD");

                        } else if (newInput[0].equals("POST")) {
                            // post method
                            // args newInput[1] and out
                            post(newInput[1], out);
                            System.out.println("POSTMETHOD");

                        } else if (newInput[0].equals("HEAD")) {
                            // head method
                            // args newInput[1] and out
                            head(newInput[1], out);
                        }
                    } else {
                        // version not supported
                        status = response(9);
                    }
                } else {
                    // 400 Bad Request
                }
                // out.println(inputLine);
            }
            System.out.println("not here yea");
            socket.close();
        } catch (IOException e) {
            System.out.println("Exception caught when trying listen for a connection");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void get(String newInput, PrintWriter out) {
        System.out.println("GETMETHOD");

    }

    public void post(String newInput, PrintWriter out) {
        System.out.println("POSTMETHOD");

    }

    public void head(String newInput, PrintWriter out) {
        System.out.println("HEADMETHOD");
        System.out.println(newInput);

        try {
            File file = new File(newInput);
            if (file.exists()) {
                long timeStamp = file.lastModified();
                DateFormat formatter = new SimpleDateFormat("E, dd MM yyyy hh:mm:ss zzz");

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(timeStamp);
                // System.out.println(formatter.format(calendar.getTime()));
                String lastModified = formatter.format(calendar.getTime()); // last modified
                out.println("Last-Modified Since: " + lastModified);

                Calendar now = Calendar.getInstance();
                now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
                // System.out.println(formatter.format(now.getTime()));
                String expire = formatter.format(now.getTime()); // expire
                out.println("Expire: " + expire);

                long filelength = file.length(); // file length
                out.println("Content-length: " + filelength);

                String allow = "GET, POST, HEAD"; // allow
                out.println("Allow: " + allow);

                String contentIncoding = "Identity"; // content incoding
                out.println("Content-Encoding: " + contentIncoding);

                String filetype;
                String filename = file.getName();
                switch (filename.split(".")[1]) {
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
                out.println("Content-Type: " + filetype);

                // writing the file
                Scanner myReader = new Scanner(file);
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    System.out.println(data);
                }
                myReader.close();
            } else {
                // file dosent exist
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
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
                statusCode = "505 HTTP Version Nor Supported";
                break;
            default:
                statusCode = "ERR";
                break;
        }
        return statusCode;
    }
}

// try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));
// Socket clientSocket = serverSocket.accept();
// PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
// BufferedReader in = new BufferedReader(new
// InputStreamReader(clientSocket.getInputStream()));) {
// String inputLine;
// while ((inputLine = in.readLine()) != null) {
// out.println(inputLine);
// }
// } catch (IOException e) {
// System.out.println(
// "Exception caught when trying to listen on port " + portNumber + " or
// listening for a connection");
// System.out.println(e.getMessage());
// }

// java -cp . PartialHTTP1Server.java 8000
