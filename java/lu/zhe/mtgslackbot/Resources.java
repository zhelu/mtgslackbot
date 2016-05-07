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
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that builds serialized data structures from JSON inputs.
 */
public class Resources {
  private static final Pattern RULES_URL_PATTERN = Pattern.compile(
      ".*(?<url>http://media.wizards.com/\\d+/docs/MagicCompRules_\\d+.txt).*");

  private Resources(String path) {
    System.setProperty("http.agent", "mtgslackbot");
    Map<String, Card> allCards;
    Map<String, String> allSets;
    Map<String, String> allRules;
    try {
      long start = System.currentTimeMillis();
      InputStream is =
          new URL("http://mtgjson.com/json/AllCards-x.json").openStream();
      allCards = ParseUtils.parseCards(is, true);
      System.out.println("Cards processed: " + allCards.size());
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      throw new RuntimeException("Error parsing cards", e);
    }
    try (ObjectOutputStream oos =
        new ObjectOutputStream(
            new FileOutputStream(path + "/Cards.ser"))) {
      oos.writeObject(allCards);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't serialize card data", e);
    }
    try {
      long start = System.currentTimeMillis();
      InputStream is =
          new URL("http://mtgjson.com/json/SetList.json").openStream();
      allSets = SetUtils.parseSets(is);
      System.out.println("Sets processed: " + allSets.size());
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      throw new RuntimeException("Error parsing cards", e);
    }
    try (ObjectOutputStream oos =
        new ObjectOutputStream(
            new FileOutputStream(path + "/Sets.ser"))) {
      oos.writeObject(allSets);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't serialize set data", e);
    }
    try {
      long start = System.currentTimeMillis();
      Scanner pageScanner =
          new Scanner(new URL(
              "http://magic.wizards.com/en/gameinfo/gameplay/formats/comprehensiverules")
              .openStream());
      String rulesUrl = "";
      while (pageScanner.hasNextLine()) {
        String line = pageScanner.nextLine();
        Matcher matcher = RULES_URL_PATTERN.matcher(line);
        if (matcher.matches()) {
          rulesUrl = matcher.group("url");
          break;
        }
      }
      pageScanner.close();
      Scanner sc = new Scanner(new URL(rulesUrl).openStream());
      allRules = RuleUtils.parseRules(sc);
      System.out.println("Rule entries: " + allRules.size());
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      throw new RuntimeException("Error parsing comprehensive rules", e);
    }
    try (ObjectOutputStream oos =
        new ObjectOutputStream(
            new FileOutputStream(path + "/Rules.ser"))) {
      oos.writeObject(allRules);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't serialize rule data", e);
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println(
          "Expect single path argument. Should reference java/lu/zhe/mtgslackbot/res");
      System.exit(1);
    }
    System.out.println("Building resources...");
    Resources setupTask = new Resources(args[0]);
  }
}
