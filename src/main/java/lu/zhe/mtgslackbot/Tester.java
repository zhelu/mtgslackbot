package lu.zhe.mtgslackbot;

import com.google.common.base.Joiner;

import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;

public class Tester {
  private static final Joiner JOINER = Joiner.on(" ");
  public static void main(String[] args) throws Exception {
    DataSources dataSources = new DataSources(true);
    ParsedInput parsedInput = Parsing.getParsedInput(JOINER.join(args));
    System.out.println(dataSources.processInput(parsedInput));
  }
}
