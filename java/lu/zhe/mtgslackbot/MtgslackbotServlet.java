package lu.zhe.mtgslackbot;

import java.util.Scanner;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Main class that handles IO.
 */
public class MtgslackbotServlet extends HttpServlet {
  private DataSources dataSources;
  private boolean debug = false;
  private boolean slack = true;

  @Override
  public void init() throws ServletException {
    init(
        Boolean.getBoolean(getInitParameter("debug")),
        Boolean.getBoolean(getInitParameter("slack")));
  }

  private void init(boolean debug, boolean slack) {
    this.debug = debug;
    this.slack = slack;
    this.dataSources = new DataSources(slack);
  }

  private String process(String input) {
    try {
      ParsedInput parsedInput = Parsing.getParsedInput(input);
      return dataSources.processInput(parsedInput);
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public static void main(String[] args) {
    boolean slack = false;
    boolean debug = false;
    for (String arg : args) {
      switch (arg) {
        case "slack":
          slack = true;
          continue;
        case "debug":
          debug = true;
          continue;
        default:
      }
    }
    try {
      MtgslackbotServlet servlet = new MtgslackbotServlet();
      servlet.init(debug, slack);
      Scanner sc = new Scanner(System.in);
      while (sc.hasNextLine()) {
        System.out.println(servlet.process(sc.nextLine()));
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
