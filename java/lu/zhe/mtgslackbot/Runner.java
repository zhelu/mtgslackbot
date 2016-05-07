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

  private Runner() {
    this.dataSources = new DataSources();
  }

  public static void main(String[] args) {
    Runner runner = new Runner();
  }
}
