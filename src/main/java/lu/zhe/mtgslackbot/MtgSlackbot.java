package lu.zhe.mtgslackbot;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;
import org.json.JSONObject;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main class that handles IO.
 */
public class MtgSlackbot {
  private static final String RESPONSE_TEMPLATE =
      "{\"text\": \"%s\", \"response_type\": \"in_channel\"}";
  private static final Timer TIMER = new Timer(true);
  private static final String USER_AGENT = "MtgSlackbot";

  private final DataSources dataSources = new DataSources();
  private final int serverPort;
  private final String token;
  // Send a query to self in this many millis (if positive)
  private final int keepAliveMs;
  private final String keepAliveUrl;
  // Guarded by this.
  private TimerTask timerTask;

  private MtgSlackbot(int serverPort, String token, int keepAliveMs, String selfhost) {
    this.serverPort = serverPort;
    this.token = token;
    this.keepAliveMs = keepAliveMs;
    this.keepAliveUrl = selfhost;
  }

  private void start() {
    port(serverPort);

    get("/keepalive", (request, response) -> {
      response.status(200);
      return "keepalive";
    });

    post("/", (request, response) -> {
      if (!request.queryParams("token").equals(token)) {
        response.status(401);
        return "Not authorized";
      }
      response.type("application/json");
      return process(request.queryParams("text"));
    });

    if (keepAliveMs > 0 && keepAliveUrl != null) {
      registerKeepAlive();
    }
  }

  private synchronized void registerKeepAlive() {
    System.out.println(
        "Register keep alive to: " + keepAliveUrl + " every " + keepAliveMs + " ms.");
    TIMER.schedule(buildTimerTask(), keepAliveMs);
  }

  private TimerTask buildTimerTask() {
    return new TimerTask() {
      @Override
      public void run() {
        try {
          System.out.println("Sending keep alive");
          URL url = new URL(keepAliveUrl);
          URLConnection connection = url.openConnection();
          Scanner sc = new Scanner(connection.getInputStream());
          System.out.println("Keep alive response:");
          while (sc.hasNextLine()) {
            System.out.println(sc.nextLine());
          }
          TIMER.schedule(buildTimerTask(), keepAliveMs);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
  }

  private String process(String input) {
    try {
      ParsedInput parsedInput = Parsing.getParsedInput(input);
      return dataSources.processInput(parsedInput).toString();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public static void main(String[] args) {
    int serverPort = System.getenv("PORT") == null
        ? 8080
        : Integer.valueOf(System.getenv("PORT"));
    String token = System.getenv("token");
    int keepaliveMs = System.getenv("keepalive") == null
        ? 0
        : Integer.valueOf(System.getenv("keepalive")) * 60 * 1000;
    String selfhost = System.getenv("selfhost");

    new MtgSlackbot(serverPort, token, keepaliveMs, selfhost).start();
  }
}
