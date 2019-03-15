package lu.zhe.mtgslackbot;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.SparkBase.port;


/**
 * Main class that handles IO.
 */
public class MtgSlackbot {
  private static final Timer TIMER = new Timer(true);

  private final DataSources dataSources = new DataSources();
  private final int serverPort;
  private final Set<String> tokens = new HashSet<>();
  // Send a query to self in this many millis (if positive)
  private final Duration keepAlive;
  private final String keepAliveUrl;

  private MtgSlackbot(int serverPort, String tokens, Duration keepAlive, String keepAliveUrl) {
    this.serverPort = serverPort;
    this.keepAlive = keepAlive;
    this.keepAliveUrl = keepAliveUrl;
    for (String token : tokens.split(",")) {
      this.tokens.add(token);
    }
  }

  private void start() {
    port(serverPort);

    get("/keepalive", (request, response) -> {
      response.status(200);
      return "keepalive";
    });

    post("/", (request, response) -> {
      if (!tokens.contains(request.queryParams("token"))) {
        response.status(401);
        return "Not authorized";
      }
      response.type("application/json");
      return process(
          request.queryParams("text"),
          createConsumer(request.queryParams("response_url")));
    });

    if (!keepAlive.isNegative() && !keepAlive.isZero() && keepAliveUrl != null) {
      registerKeepAlive();
    }
  }

  private static Consumer<String> createConsumer(String responseHook) {
    return (String response) -> {
      try {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(responseHook);
        httpPost.setEntity(new StringEntity(response));
        httpPost.setHeader("Content-type", "application/json");
        client.execute(httpPost);
        client.close();
      } catch (Exception e) {
        // Nothing to do
        System.out.println(e);
      }
    };
  }

  private synchronized void registerKeepAlive() {
    System.out.println(
        "Register keep alive to: " + keepAliveUrl + " every " + keepAlive.toMillis() + " ms.");
    TIMER.schedule(buildTimerTask(), keepAlive.toMillis());
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
          TIMER.schedule(buildTimerTask(), keepAlive.toMillis());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
  }

  private String process(String input, Consumer<String> responseHook) {
    try {
      ParsedInput parsedInput = Parsing.getParsedInput(input);
      return dataSources.processInput(
          parsedInput,
          responseHook).toString();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public static void main(String[] args) {
    int serverPort = System.getenv("PORT") == null
        ? 8080
        : Integer.valueOf(System.getenv("PORT"));
    String tokens = System.getenv("tokens");
    Duration keepalive = System.getenv("keepalive") == null
        ? Duration.ZERO
        : Duration.ofMillis(Integer.valueOf(System.getenv("keepalive")) * 60 * 1000);
    String appname = System.getenv("appname") == null
        ? null
        : "http://" + System.getenv("appname") + ".herokuapp.com/keepalive";

    new MtgSlackbot(serverPort, tokens, keepalive, appname).start();
  }
}
