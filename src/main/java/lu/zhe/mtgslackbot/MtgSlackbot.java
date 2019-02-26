package lu.zhe.mtgslackbot;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.function.Consumer;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main class that handles IO.
 */
public class MtgSlackbot {
  private static final Timer TIMER = new Timer(true);
  private static final String USER_AGENT = "MtgSlackbot";

  private final DataSources dataSources = new DataSources();
  private final int serverPort;
  private final Set<String> tokens = new HashSet<>();
  // Send a query to self in this many millis (if positive)
  private final int keepAliveMs;
  private final String keepAliveUrl;
  // Guarded by this.
  private TimerTask timerTask;

  private MtgSlackbot(int serverPort, String tokens, int keepAliveMs, String keepAliveUrl) {
    this.serverPort = serverPort;
    this.keepAliveMs = keepAliveMs;
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

    if (keepAliveMs > 0 && keepAliveUrl != null) {
      registerKeepAlive();
    }
  }

  private static Consumer<String> createConsumer(String responseHook) {
    return (String response) -> {
        System.out.println(responseHook);
        try {
          String r = "{\"response_type\": \"in_channel\", \"text\": \"asdf\"}";
          CloseableHttpClient client = HttpClients.createDefault();
          HttpPost httpPost = new HttpPost(responseHook);
          httpPost.setEntity(new StringEntity(r));
          httpPost.setHeader("Content-type", "application/json");
          CloseableHttpResponse httpResponse = client.execute(httpPost);
          System.out.println("Status code: " + httpResponse.getStatusLine().getStatusCode());
          client.close();
        } catch (Exception e) {
          // Nothing to do
          System.out.println(e);
        }
    };
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
    int keepaliveMs = System.getenv("keepalive") == null
        ? 0
        : Integer.valueOf(System.getenv("keepalive")) * 60 * 1000;
    String appname = System.getenv("appname") == null
        ? null
        : "http://" + System.getenv("appname") + ".herokuapp.com/keepalive";

    new MtgSlackbot(serverPort, tokens, keepaliveMs, appname).start();
  }
}
