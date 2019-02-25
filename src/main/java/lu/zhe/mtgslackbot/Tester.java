package lu.zhe.mtgslackbot;

import com.google.common.base.Joiner;
import java.util.function.Consumer;
import lu.zhe.mtgslackbot.parsing.Parsing;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;

public class Tester {
  private static final Joiner JOINER = Joiner.on(" ");
  public static void main(String[] args) throws Exception {
    DataSources dataSources = new DataSources();
    ParsedInput parsedInput = Parsing.getParsedInput(JOINER.join(args));
    Consumer<String> consumer = response -> System.out.println(response);
    System.out.println(dataSources.processInput(parsedInput, consumer));
    Thread.sleep(10000);
    System.exit(0);
  }
}
