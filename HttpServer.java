import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

// @author Anush Shrestha
// @TODO multi thread, no access above root folder public_html, test get and head  

class HttpServer {
  public static void main(String[] args) throws InterruptedException {

    if (args.length == 0 || args[0] == null || args[0].isEmpty()) {
      System.out.println(" Invalid invocation. Missing port number. ");
      System.exit(-1);
    }
    int portNumber = Integer.parseInt(args[0]);

    try {

      MultiThreadedServer socketServer = new MultiThreadedServer(portNumber);
      socketServer.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

class MultiThreadedServer {
  ServerSocket serverSocket;
  int portNumber;

  public MultiThreadedServer(int portNumber) {
    this.portNumber = portNumber;
  }

  public void start() throws IOException, InterruptedException {
    serverSocket = new ServerSocket(portNumber);
    System.out.println("Starting server at port : " + portNumber);

    Socket client = null;

    System.out.println("Waiting client.");
    while (true) {
      client = serverSocket.accept();
      System.out.println("Client IP : " + client.getInetAddress().getCanonicalHostName() + " connected.");

      Thread thread = new Thread(new SocketClientHandler(client));
      thread.start();
    }
  }

}

class SocketClientHandler implements Runnable {
  Socket client;

  public SocketClientHandler(Socket client) {
    this.client = client;
  }

  @Override
  public void run() {
    try {
      System.out.println("Thread started : " + Thread.currentThread().getName());
      readRequest();
      return;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void readRequest() throws IOException, InterruptedException {
    try {

      BufferedReader request = new BufferedReader(new InputStreamReader(client.getInputStream()));
      BufferedWriter response = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
      String requestHeader = "";
      String temp = ".";

      while (!temp.equals("")) {
        temp = request.readLine();
        System.out.println("Request : " + temp);
        requestHeader += temp + "\n";
      }

      // get method
      StringBuilder sb = new StringBuilder();
      if (requestHeader.split("\n")[0].split(" ")[1].toString().equals("/")) {
        System.out.println("Invalid Invocation. Please mention requested file.");
        System.exit(-1);
      }

      String file = requestHeader.split("\n")[0].split(" ")[1].split("/")[1];
      String filePath = System.getProperty("user.dir") + "\\public_html\\";
      filePath = filePath + file;
      System.out.println("Requested file : " + filePath);

      if (!checkURL(filePath)) {
        System.out.println("Requested file : " + filePath + " doesn't exist. ");
        System.exit(-1);
      }

      if (requestHeader.split("\n")[0].contains("HEAD")) {
        buildResponseHeader(200, sb);
        System.out.println("Response : " + sb.toString());
        response.write(sb.toString());
        sb.setLength(0);
        response.flush();
      } else if (requestHeader.split("\n")[0].contains("GET")) {
        buildResponseHeader(200, sb);
        response.write(sb.toString());
        System.out.println("Response : " + sb.toString());
        response.write(getData(file));
        sb.setLength(0);
        response.flush();
      } else {
        System.out.println("System only supports GET and HEAD.");
        // Enter the error code
        // 404 page not found
        buildResponseHeader(404, sb);
        response.write(sb.toString());
        sb.setLength(0);
        response.flush();
      }

      request.close();
      response.close();
      // thread.close();
      client.close();
      return;

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean checkURL(String file) {
    File myFile = new File(file);
    return myFile.exists() && !myFile.isDirectory();
  }

  private static void buildResponseHeader(int responseCode, StringBuilder sb) {
    if (responseCode == 200) {
      sb.append("HTTP/1.1 200 OK\r\n");
      sb.append("Date:" + getTimeStamp() + "\r\n");
      sb.append("Server:localhost\r\n");
      sb.append("Content-Type: text/html\r\n");
      sb.append("Connection: Closed\r\n\r\n");
    } else if (responseCode == 404) {
      sb.append("HTTP/1.1 404 Not Found\r\n");
      sb.append("Date:" + getTimeStamp() + "\r\n");
      sb.append("Server:localhost\r\n");
      sb.append("\r\n");
    } else if (responseCode == 400) {
      sb.append("HTTP/1.1 400 Bad Request\r\n");
      sb.append("Date:" + getTimeStamp() + "\r\n");
      sb.append("Server:localhost\r\n");
      sb.append("\r\n");
    } else if (responseCode == 304) {
      sb.append("HTTP/1.1 501 Not Implemented\r\n");
      sb.append("Date:" + getTimeStamp() + "\r\n");
      sb.append("Server:localhost\r\n");
      sb.append("\r\n");
    }
  }

  private static String getData(String file) {

    String responseToClient = "";
    BufferedReader reader;

    try {
      File myFile = new File(System.getProperty("user.dir") + "\\public_html\\" + file);
      // System.out.println(myFile.getCanonicalPath());
      reader = new BufferedReader(new FileReader(myFile));
      String line = null;
      while ((line = reader.readLine()) != null) {
        responseToClient += line;
      }
      // responseToClient += line;
      // System.out.println("Response data : " + responseToClient);
      reader.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
    return responseToClient;
  }

  // TimeStamp
  private static String getTimeStamp() {
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
    String formattedDate = sdf.format(date);
    return formattedDate;
  }
}