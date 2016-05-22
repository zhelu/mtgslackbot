package lu.zhe.mtgslackbot;

import com.google.common.base.Joiner;

import java.io.IOException;
import java.util.Scanner;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;

/**
 * Main class that handles IO.
 */
public class MtgSlackbot {
  private DataSources dataSources;
  private boolean slack = true;
  private static Joiner SPACE_JOINER = Joiner.on(" ");

  private void init(boolean slack) {
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
    for (String arg : args) {
      switch (arg) {
        case "slack":
          slack = true;
          continue;
        default:
      }
    }
    try {
      MtgSlackbot servlet = new MtgSlackbot();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
