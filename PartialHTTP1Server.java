import java.net.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.io.*;
import java.lang.Object;
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
                Future<?> f = pool.submit(new ServerThread(clientSocket));
                futures.add(f);
                // System.out.println(futures.size()); // gives the size of the list of threads
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

    ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                // PrintWriter out = new PrintWriter(this.socket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));) {
            String inputLine;
            String status = "";
            List<String> response = new ArrayList<>();
            while ((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);
                String[] newInput = inputLine.trim().split("\\s+");
                System.out.println(newInput.length);
                if (newInput.length != 3 || (!"HTTP".equals(newInput[2].split("/")[0]))) {
                    System.out.println("1");

                    status = response(2);
                    response.add(status);
                } else if (newInput[2].split("/")[0].equals("HTTP")) {
                    System.out.println("2");

                    if (0 <= Float.parseFloat(newInput[2].split("/")[1])
                            && 1.0 >= Float.parseFloat(newInput[2].split("/")[1])) {
                                System.out.println("2");

                        if (newInput[0].equals(newInput[0].toUpperCase())) {
                            System.out.println("2");

                            if (newInput[0].equals("GET")) {
                                System.out.println("indside get ");

                                // get method
                                // args newInput[1] and out
                                System.out.println(newInput[1]);

                                response = get(newInput[1], status);

                            } else if (newInput[0].equals("POST")) {
                                // post method
                                // args newInput[1] and out
                                response = post(newInput[1], status);

                            } else if (newInput[0].equals("HEAD")) {
                                // head method
                                // args newInput[1] and out
                                response = head(newInput[1], status);

                            } else if (newInput[0].equals("PUT")) {
                                status = response(7);
                                response.add(status);
                            } else if (newInput[0].equals("DELETE")) {
                                status = response(7);
                                response.add(status);
                            } else if (newInput[0].equals("LINK")) {
                                status = response(7);
                                response.add(status);
                            } else if (newInput[0].equals("UNLINK")) {
                                status = response(7);
                                response.add(status);
                            } else {
                                status = response(2);
                                response.add(status);
                            }
                        } else {
                            System.out.println("2");

                            // 400 Bad Request
                            status = response(2);
                            response.add(status);
                        }
                    } else {
                        // version not supported
                        System.out.println("awiudiawjd");

                        status = response(9);
                        response.add(status);

                    }
                } else {
                    // bad request
                    System.out.println("3");

                    status = response(2);
                    response.add(status);
                }
                System.out.println("5");
                String str;
                System.out.println(response);

                try {
                    response.get(1);
                    str = "HTTP/1.0 " + response.get(0) + System.getProperty("line.separator") + response.get(1) + null;
                } catch (IndexOutOfBoundsException e) {
                    str = "HTTP/1.0 " + response.get(0) + System.getProperty("line.separator") + null;
                }
                out.write(str);
                out.newLine();
                out.flush();
                System.out.println("6");

                response.clear();
                // out.println(inputLine);
            }
            socket.close();
        } catch (IOException e) {
            System.out.println("Exception caught when trying listen for a connection");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public List<String> get(String newInput, String r) {

        List<String> response = new ArrayList<>();
        response.add(r);
        String para = "";
        String header = "";

        try {
            File file = new File("." + newInput);
            if (file.exists()) {
                if (file.canRead()) {
                    String filename = file.getName();
                    String filetype = "";
                    if(filename.split("\\.").length < 2){
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
                    header += "Content-Type: " + filetype + "\n";

                    long filelength = file.length(); // file length
                    header += "Content-Length: " + filelength + "\n";

                    long timeStamp = file.lastModified();
                    DateFormat formatter = new SimpleDateFormat("E, dd MM yyyy hh:mm:ss zzz");

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(timeStamp);
                    String lastModified = formatter.format(calendar.getTime()); // last modified
                    header += "Last-Modified: " + lastModified + "\n";

                    long IfModSince = file.lastModified();
                    Calendar newCalendar = Calendar.getInstance();
                    newCalendar.setTimeInMillis(IfModSince);
                    String LastMod = formatter.format(newCalendar.getTime());

                    if (lastModified.equals(LastMod)) {
                        response.set(0, response(0));
                    } else {
                        response.set(0, response(1));
                    }

                    String contentIncoding = "identity"; // content incoding
                    header += "Content-Encoding: " + contentIncoding + "\n";

                    String allow = "GET, POST, HEAD"; // allow
                    header += "Allow: " + allow + "\n";

                    Calendar now = Calendar.getInstance();
                    now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
                    // System.out.println(formatter.format(now.getTime()));
                    String expire = formatter.format(now.getTime()); // expire
                    header += "Expire: " + expire + "\n";

                    // writing the file
                    Scanner myReader = new Scanner(file);
                    while (myReader.hasNextLine()) {
                        String data = myReader.nextLine();
                        para += data + "\n";
                    }
                    header += "\n" + para;
                    response.add(header);
                    myReader.close();

                    // 200 ok
                    response.set(0, response(0));

                } else {
                    // Forbidden 403
                    response.set(0, response(3));

                }
            } else {
                // file dosent exist
                response.set(0, response(4));

            }

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();

        }
        return response;

    }

    public List<String> post(String newInput, String r) {

        List<String> response = new ArrayList<>();
        response.add(r);
        String para = "";
        String header = "";

        try {
            File file = new File("." + newInput);
            if (file.exists()) {
                if (file.canRead()) {
                    String filename = file.getName();
                    String filetype = "";
                    if(filename.split("\\.").length < 2){
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
                    header += "Content-Type: " + filetype + "\n";

                    long filelength = file.length(); // file length
                    header += "Content-Length: " + filelength + "\n";

                    long timeStamp = file.lastModified();
                    DateFormat formatter = new SimpleDateFormat("E, dd MM yyyy hh:mm:ss zzz");
                    
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(timeStamp);
                    String lastModified = formatter.format(calendar.getTime()); // last modified
                    header += "Last-Modified: " + lastModified + "\n";

                    long IfModSince = file.lastModified();
                    Calendar newCalendar = Calendar.getInstance();
                    newCalendar.setTimeInMillis(IfModSince);
                    String LastMod = formatter.format(newCalendar.getTime());

                    if (lastModified.equals(LastMod)) {
                        response.set(0, response(0));
                    } else {
                        response.set(0, response(1));
                    }

                    String contentIncoding = "identity"; // content incoding
                    header += "Content-Encoding: " + contentIncoding + "\n";

                    String allow = "GET, POST, HEAD"; // allow
                    header += "Allow: " + allow + "\n";

                    Calendar now = Calendar.getInstance();
                    now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
                    // System.out.println(formatter.format(now.getTime()));
                    String expire = formatter.format(now.getTime()); // expire
                    header += "Expire: " + expire + "\n";

                    // writing the file
                    Scanner myReader = new Scanner(file);
                    while (myReader.hasNextLine()) {
                        String data = myReader.nextLine();
                        para += data + "\n";
                    }
                    header += "\n" + para;
                    response.add(header);
                    myReader.close();

                    // 200 ok
                    response.set(0, response(0));

                } else {
                    // Forbidden 403
                    response.set(0, response(3));

                }
            } else {
                // file dosent exist
                response.set(0, response(4));

            }

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();

        }
        return response;

    }

    public List<String> head(String newInput, String r) {

        List<String> response = new ArrayList<>();
        response.add(r);
        String para = "";
        String header = "";

        try {
            File file = new File("." + newInput);
            if (file.exists()) {
                if (file.canRead()) {
                    String filename = file.getName();
                    String filetype = "";
                    if(filename.split("\\.").length < 2){
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
                    header += "Content-Type: " + filetype + "\n";

                    long filelength = file.length(); // file length
                    header += "Content-Length: " + filelength + "\n";

                    long timeStamp = file.lastModified();
                    DateFormat formatter = new SimpleDateFormat("E, dd MM yyyy hh:mm:ss zzz");
                    
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(timeStamp);
                    String lastModified = formatter.format(calendar.getTime()); // last modified
                    header += "Last-Modified: " + lastModified + "\n";

                    long IfModSince = file.lastModified();
                    Calendar newCalendar = Calendar.getInstance();
                    newCalendar.setTimeInMillis(IfModSince);
                    String LastMod = formatter.format(newCalendar.getTime());

                    if (lastModified.equals(LastMod)) {
                        response.set(0, response(0));
                    } else {
                        response.set(0, response(1));
                    }

                    String contentIncoding = "identity"; // content incoding
                    header += "Content-Encoding: " + contentIncoding + "\n";

                    String allow = "GET, POST, HEAD"; // allow
                    header += "Allow: " + allow + "\n";

                    Calendar now = Calendar.getInstance();
                    now.set(Calendar.YEAR, (now.get(Calendar.YEAR) + 10));
                    // System.out.println(formatter.format(now.getTime()));
                    String expire = formatter.format(now.getTime()); // expire
                    header += "Expire: " + expire + "\n";

                    // writing the file
                    Scanner myReader = new Scanner(file);
                    while (myReader.hasNextLine()) {
                        String data = myReader.nextLine();
                        para += data + "\n";
                    }
                    header += "\n" + para;
                    response.add(header);
                    myReader.close();

                    // 200 ok
                    response.set(0, response(0));

                } else {
                    // Forbidden 403
                    response.set(0, response(3));

                }
            } else {
                // file dosent exist
                response.set(0, response(4));

            }

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();

        }
        return response;

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
}
