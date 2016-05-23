package lu.zhe.mtgslackbot;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import java.io.IOException;
import java.util.Scanner;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;

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
      return dataSources.processInput(parsedInput);
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public static void main(String[] args) {
    final MtgSlackbot server = new MtgSlackbot();

    int serverPort = System.getenv("PORT") == null ? 8080 : Integer.valueOf(System.getenv("PORT"));

    port(serverPort);

    get("/", (request, response) -> {
      System.out.println(request.raw().getQueryString());
      if (!request.params("token").equals(System.getenv("token"))) {
        response.status(401);
        return "Not authorized";
      }
      response.type("text/json");
      return String.format(RESPONSE_TEMPLATE, server.process(request.body()));
    });

    post("/", (request, response) -> {
      System.out.println(request.raw());
      if (!request.queryParams("token").equals(System.getenv("token"))) {
        response.status(401);
        return "Not authorized";
      }
      response.type("text/json");
      return String.format(RESPONSE_TEMPLATE, server.process(request.body()));
    });
  }
}
