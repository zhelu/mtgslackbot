package lu.zhe.mtgslackbot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import lu.zhe.mtgslackbot.card.Card;
import lu.zhe.mtgslackbot.card.ParseUtils;
import lu.zhe.mtgslackbot.rule.RuleUtils;
import lu.zhe.mtgslackbot.set.SetUtils;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Scanner;

/**
 * Class that builds serialized data structures from JSON inputs.
 */
public class Resources {
  private Resources() {
    Map<String, Card> allCards;
    Map<String, String> allSets;
    Map<String, String> allRules;
    try {
      long start = System.currentTimeMillis();
      InputStream is =
          Resources.class.getClassLoader().getResourceAsStream(
              "lu/zhe/mtgslackbot/AllCards-x.json");
      allCards = ParseUtils.parseCards(is, true);
      System.out.println("Cards processed: " + allCards.size());
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      throw new RuntimeException("Error parsing cards", e);
    }
    try (ObjectOutputStream oos =
        new ObjectOutputStream(
            new FileOutputStream("src/main/resources/lu/zhe/mtgslackbot/Cards.ser"))) {
      oos.writeObject(allCards);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't serialize card data", e);
    }
    try {
      long start = System.currentTimeMillis();
      InputStream is =
          Resources.class.getClassLoader().getResourceAsStream(
              "lu/zhe/mtgslackbot/SetList.json");
      allSets = SetUtils.parseSets(is);
      System.out.println("Sets processed: " + allSets.size());
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      throw new RuntimeException("Error parsing cards", e);
    }
    try (ObjectOutputStream oos =
        new ObjectOutputStream(
            new FileOutputStream("src/main/resources/lu/zhe/mtgslackbot/Sets.ser"))) {
      oos.writeObject(allSets);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't serialize set data", e);
    }
    try {
      long start = System.currentTimeMillis();
      Scanner sc =
          new Scanner(Resources.class.getClassLoader().getResourceAsStream(
              "lu/zhe/mtgslackbot/rules.txt"));
      allRules = RuleUtils.parseRules(sc);
      System.out.println("Rule entries: " + allRules.size());
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      throw new RuntimeException("Error parsing comprehensive rules", e);
    }
    try (ObjectOutputStream oos =
        new ObjectOutputStream(
            new FileOutputStream("src/main/resources/lu/zhe/mtgslackbot/Rules.ser"))) {
      oos.writeObject(allRules);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't serialize rule data", e);
    }
  }

  public static void main(String[] args) {
    System.out.println("Building resources...");
    Resources setupTask = new Resources();
  }
}
