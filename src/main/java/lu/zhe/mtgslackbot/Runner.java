package lu.zhe.mtgslackbot;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import lu.zhe.mtgslackbot.card.Card;
import lu.zhe.mtgslackbot.card.CardUtils;
import lu.zhe.mtgslackbot.card.Format;
import lu.zhe.mtgslackbot.card.Layout;
import lu.zhe.mtgslackbot.card.Legality;
import lu.zhe.mtgslackbot.parsing.Parsing;

/**
 * Main class that handles IO.
 */
public class Runner {
  private Runner() {
  }

  public static void main(String[] args) {
    Parsing.getParsedInput("!mtg search is:\"fun\" cmc=2");
  }
}
