package lu.zhe.mtgslackbot;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import lu.zhe.mtgslackbot.parsing.Parsing;

/**
 * Main class that handles IO.
 */
public class Runner {
  private final DataSources dataSources;

  private Runner(boolean debug, boolean slack) {
    this.dataSources = new DataSources(slack);
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
    Runner runner = new Runner(debug, slack);
  }
}
