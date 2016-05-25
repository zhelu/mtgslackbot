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
  private final DataSources dataSources = new DataSources(true);
  private static final String RESPONSE_TEMPLATE =
      "{\"text\": \"%s\", \"response_type\": \"in_channel\"}";

  private String process(String input) {
    try {
      ParsedInput parsedInput = Parsing.getParsedInput(input);
      return dataSources.processInput(parsedInput).toString();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public static void main(String[] args) {
    final MtgSlackbot server = new MtgSlackbot();

    int serverPort = System.getenv("PORT") == null ? 8080 : Integer.valueOf(System.getenv("PORT"));

    port(serverPort);

    post("/", (request, response) -> {
      if (!request.queryParams("token").equals(System.getenv("token"))) {
        response.status(401);
        return "Not authorized";
      }
      response.type("application/json");
      return server.process(request.queryParams("text"));
    });
  }
}
