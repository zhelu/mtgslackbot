package lu.zhe.mtgslackbot;

import com.google.common.base.Joiner;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;

/**
 * Main class that handles IO.
 */
public class Runner {
  private static final Joiner JOINER = Joiner.on(" ");
  private final DataSources dataSources;

  private Runner(boolean slack) {
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
    try {
      String input = JOINER.join(args);
      Runner runner = new Runner(true);
      System.out.println(runner.process(input));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
