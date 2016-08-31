package lu.zhe.mtgslackbot;

import static spark.Spark.port;
import static spark.Spark.post;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;

import org.json.JSONObject;

/**
 * Main class that handles IO.
 */
public class MtgSlackbot {
  private static final String RESPONSE_TEMPLATE =
      "{\"text\": \"%s\", \"response_type\": \"in_channel\"}";
  private final DataSources dataSources = new DataSources();
  private final int serverPort;
  private final String token;

  MtgSlackbot(int serverPort, String token) {
    this.serverPort = serverPort;
    this.token = token;
  }

  private void start() {
    port(serverPort);

    post("/", (request, response) -> {
      if (!request.queryParams("token").equals(token)) {
        response.status(401);
        return "Not authorized";
      }
      response.type("application/json");
      return process(request.queryParams("text"));
    });
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

    new MtgSlackbot(serverPort, token).start();
  }
}
