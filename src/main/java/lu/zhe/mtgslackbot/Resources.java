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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that builds serialized data structures from JSON inputs.
 */
public class Resources {
  private static final Pattern RULES_URL_PATTERN = Pattern.compile(
      ".*(?<url>http://media.wizards.com/\\d+/[^/]*/MagicCompRules_.+\\.txt).*");

  private Resources(String path, boolean debug) {
    System.setProperty("http.agent", "mtgslackbot");
    Map<String, Card> allCards;
    Map<String, String> allSets;
    Map<String, String> allRules;
    // parse rules (need these to parse ability words)
    try {
      long start = System.currentTimeMillis();
      Scanner pageScanner =
          new Scanner(new URL(
              "http://magic.wizards.com/en/game-info/gameplay/rules-and-formats/rules")
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
      System.out.println("Rules url: " + rulesUrl);
      pageScanner.close();
      Scanner sc = new Scanner(new URL(rulesUrl).openStream(), "ISO8859-1");
      allRules = RuleUtils.parseRules(sc);
      System.out.println("Rule entries: " + allRules.size());
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      throw new RuntimeException("Error parsing comprehensive rules", e);
    }
    String abilityWordGlossary = allRules.get("ability word");
    String abilityWordParagraph =
        allRules.get(
            abilityWordGlossary.substring(
                abilityWordGlossary.lastIndexOf(" ") + 1,
                abilityWordGlossary.lastIndexOf(".")));
    String abilityWordsJoined =
        abilityWordParagraph.substring(abilityWordParagraph.indexOf("The ability words are ") + 22,
            abilityWordParagraph.length() - 1).replaceAll(" and ", " ");
    String[] abilityWords = abilityWordsJoined.split(",");
    List<String> abilityWordSet = new ArrayList<>();
    for (String abilityWord : abilityWords) {
      abilityWord = abilityWord.trim();
      abilityWordSet.add(
          Character.toUpperCase(abilityWord.charAt(0)) + abilityWord.substring(1, abilityWord.length()));
    }
    if (!debug) {
      try (ObjectOutputStream oos =
          new ObjectOutputStream(
              new FileOutputStream(path + "/Rules.ser"))) {
        oos.writeObject(allRules);
        System.out.println("Wrote out Rules.ser");
      } catch (IOException e) {
        throw new RuntimeException("Couldn't serialize rule data", e);
      }
    }
    // parse cards
    try {
      long start = System.currentTimeMillis();
      InputStream is =
          new URL("http://mtgjson.com/json/AllCards-x.json").openStream();
      allCards = ParseUtils.parseCards(is, abilityWordSet);
      System.out.println("Cards processed: " + allCards.size());
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      throw new RuntimeException("Error parsing cards", e);
    }
    if (!debug) {
      try (ObjectOutputStream oos =
          new ObjectOutputStream(
              new FileOutputStream(path + "/Cards.ser"))) {
        oos.writeObject(allCards);
        System.out.println("Wrote out Cards.ser");
      } catch (IOException e) {
        throw new RuntimeException("Couldn't serialize card data", e);
      }
    }
    // parse sets
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
    if (!debug) {
      try (ObjectOutputStream oos =
          new ObjectOutputStream(
              new FileOutputStream(path + "/Sets.ser"))) {
        oos.writeObject(allSets);
        System.out.println("Wrote out Sets.ser");
      } catch (IOException e) {
        throw new RuntimeException("Couldn't serialize set data", e);
      }
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println(
          "Expected path argument. Should reference src/main/resources/lu/zhe/mtgslackbot/");
      System.exit(1);
    }
    System.out.println("Building resources...");
    Resources setupTask = new Resources(args[0], args.length == 2);
  }
}
