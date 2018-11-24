import java.io.File;
import java.util.Date;
import java.net.Socket;
import java.io.FileWriter;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;

// @author author Anush Shrestha
// @TODO multi thread, no access above root folder public_html, test get and head  

public class HttpServer {
  ServerSocket serverSocket;
  Socket client;

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Invalid Invocation. HttpServer must be started with portNumber as the only argument.");
    }
    try {
      int portNumber = Integer.parseInt(args[0]);
      new HttpServer().start(portNumber);

    } catch (NumberFormatException e) {
      System.out.println("Invalid port number. HttpServer must be started with valid portNumber");
    } catch (IOException e) {
      System.err.println("Socket occured. Re-run server" + e.getMessage());
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void start(int portNumber) throws Exception {
    try {
      serverSocket = new ServerSocket(portNumber);
      System.out.println("Server started at port : " + portNumber);
      while (true) {
        client = serverSocket.accept();
        if (client.isConnected()) {
          System.out.println("Connected to server.");
          HttpHandler connection = new HttpHandler(client);
          Thread request = new Thread(connection);
          request.start();
        }
      }
    } catch (Exception e) {
      System.out.println("Please start the Server with a valid, unused portNumber");
    }
  }
}

class HttpHandler implements Runnable {
  Socket client;

  public HttpHandler(Socket client) {
    this.client = client;
  }

  @Override
  public void run() {
    try {
      BufferedReader request = new BufferedReader(new InputStreamReader(client.getInputStream()));
      OutputStream responder = client.getOutputStream();
      // BufferedWriter response = new BufferedWriter(new
      // OutputStreamWriter(responder));
      String requestHeader = "";
      boolean isHost = false;
      String HTTPMethod = "", requestedFile = "";
      int lineNumber = 1;

      String line = ".";
      while (!line.equals("")) {
        line = request.readLine();
        requestHeader += line + "\n";
        // System.out.println("entry : " + line);
        if (line.length() > 5 && !isHost) {
          isHost = line.substring(0, 5).equals("Host:");
        }

        if (lineNumber == 1) {
          HTTPMethod = line.split(" ")[0].toString();
          requestedFile = line.split(" ")[1].toString();
          System.out.println("HTTPMethod :" + HTTPMethod);
          System.out.println("Requested file :" + requestedFile);
        }

        // System.out.println("Is host : " + isHost);
        lineNumber++;
      }
      System.out.println(requestHeader);

      int statusCode = 400, fileSize = 0;
      byte[] fileContent = null;

      // check for HOST request field
      if (isHost) {
        if (HTTPMethod.equals("HEAD")) {
          fileContent = getFileContent(requestedFile);
          fileSize = getContentLength(fileContent);
          statusCode = (fileContent == null) ? 404 : 200;
        } else if (HTTPMethod.equals("GET")) {
          fileContent = getFileContent(requestedFile);
          fileSize = getContentLength(fileContent);
          statusCode = (fileContent == null) ? 404 : 200;
        } else {
          statusCode = 501;
        }
      }

      // response.write(sb.toString());
      String responseHeader = buildResponseHeader(statusCode, new StringBuilder(), fileSize,
          getContentType(requestedFile));
      System.out.println(responseHeader);
      // response.flush();
      byte[] responseMessage = null;
      byte[] responderHeaderBytes = responseHeader.getBytes();
      if (statusCode == 200 && HTTPMethod.equals("GET")) {
        responseMessage = new byte[responderHeaderBytes.length + fileSize];
        ByteBuffer buf = ByteBuffer.wrap(responseMessage);
        buf.put(responderHeaderBytes);
        buf.put(fileContent);

      } else {
        responseMessage = responderHeaderBytes; // "HEAD"
      }

      responder.write(responseMessage);

      responder.close();
      request.close();
      // response.close();
      // thread.close();
      // client.close();
      // return;

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String getContentType(String requestedFile) {
    try {
      if (requestedFile.matches(".*\\.html") || requestedFile.matches(".*\\.htm"))
        return "text/html";
      if (requestedFile.matches(".*\\.gif"))
        return "image/gif";
      if (requestedFile.matches(".*\\.jpeg") || requestedFile.matches(".*\\.jpg"))
        return "image/jpeg";
      if (requestedFile.matches(".*\\.pdf"))
        return "application/pdf";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "text";
  }

  private String buildResponseHeader(int responseCode, StringBuilder sb, int requestedFileSize,
      String requestedFileType) {
    if (responseCode == 200) {
      sb.append("HTTP/1.1 200 OK\r\n");
      sb.append("Server:CS4333HttpServer/1.0.2\r\n");
      sb.append("Content-Length: " + requestedFileSize + "\r\n");
      sb.append("Content-Type: " + requestedFileType + "\r\n");
    } else if (responseCode == 404) {
      sb.append("HTTP/1.1 404 Not Found\n");
      sb.append("Server:CS4333HttpServer/1.0.2\n");
      sb.append("Content-Length: " + requestedFileSize + "\n");
      sb.append("Content-Type: " + requestedFileType + "\n");
      sb.append("\n");
    } else if (responseCode == 400) {
      sb.append("HTTP/1.1 400 Bad Request\n");
      sb.append("Server:CS4333HttpServer/1.0.2\n");
      sb.append("Content-Length: " + requestedFileSize + "\n");
      sb.append("Content-Type: " + requestedFileType + "\n");
      sb.append("\n");
    } else if (responseCode == 501) {
      sb.append("HTTP/1.1 501 Not Implemented\n");
      sb.append("Server:CS4333HttpServer/1.0.2\n");
      sb.append("Content-Length: " + requestedFileSize + "\n");
      sb.append("Content-Type: " + requestedFileType + "\n");
      sb.append("\n");
    }
    return sb.toString();
  }

  public int getContentLength(byte[] fileContent) {
    return (fileContent == null) ? 0 : fileContent.length;
  }

  private byte[] getFileContent(String requestedFile) {
    if (requestedFile.equals("/")) {
      System.out.println("Access Denied. Please mention resource name.");
      return null;
    } else {
      // String responseToClient = "";
      // BufferedReader reader;
      try {

        // String filePath = System.getProperty("user.dir") + "\\public_html\\";
        // filePath = filePath + requestedFile.split("/")[1].toString();
        // System.out.println("Requested file : " + filePath);

        FileInputStream file = new FileInputStream("public_html" + requestedFile);
        // System.out.println("buffer size " + requestedFile);
        byte[] myFile = new byte[file.available()];
        file.read(myFile);
        file.close();
        // System.out.println("Response data : Sending");
        return myFile;
        // File myFile = new File(System.getProperty("user.dir") + "\\public_html\\" +
        // requestedFile);
        // System.out.println(myFile.getCanonicalPath());
        // reader = new BufferedReader(new FileReader(myFile));
        // String line = null;
        // while ((line = reader.readLine()) != null) {
        // responseToClient += line;
        // }
        // responseToClient += line;
        // reader.close();

      } catch (Exception e) {
        System.out.println("No permission / File not found.");
        // e.printStackTrace();
        return null;
      }
    }
  }

}