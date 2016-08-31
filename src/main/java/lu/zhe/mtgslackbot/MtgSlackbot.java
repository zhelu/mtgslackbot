package lu.zhe.mtgslackbot;

import static spark.Spark.port;
import static spark.Spark.post;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;
import org.json.JSONObject;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
  // Send a keep alive query here
  private final String selfHost;
  // Guarded by this.
  private TimerTask timerTask;

  private MtgSlackbot(int serverPort, String token, int keepAliveMs, String selfHost) {
    this.serverPort = serverPort;
    this.token = token;
    this.keepAliveMs = keepAliveMs;
    this.selfHost = selfHost;
  }

  private void start() {
    port(serverPort);

    post("/", (request, response) -> {
      if (!request.queryParams("token").equals(token)) {
        response.status(401);
        return "Not authorized";
      }
      response.type("application/json");
      if (keepAliveMs > 0 && selfHost != null) {
        registerKeepAlive();
      }
      return process(request.queryParams("text"));
    });
  }

  private synchronized void registerKeepAlive() {
    if (timerTask != null) {
      timerTask.cancel();
    }
    System.out.println("registering keep-alive. will send in " + keepAliveMs + " ms.");
    timerTask = new TimerTask() {
      @Override
      public void run() {
        try {
          URL url = new URL(selfHost);
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          connection.setRequestMethod("POST");
          connection.setRequestProperty("User-Agent", USER_AGENT);
          String urlParameters = "token=" + token + "&command=/mtg&command=card%20dispel";
          connection.setDoOutput(true);
          DataOutputStream out = new DataOutputStream(connection.getOutputStream());
          out.writeBytes(urlParameters);
          out.flush();
          out.close();
          System.out.println("sending keep-alive");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    TIMER.schedule(timerTask, keepAliveMs);
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
    String selfHost = System.getenv("selfhost");
    new MtgSlackbot(serverPort, token, keepaliveMs, selfHost).start();
  }
}
