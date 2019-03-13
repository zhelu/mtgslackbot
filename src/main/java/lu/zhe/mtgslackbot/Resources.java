package lu.zhe.mtgslackbot;

import java.io.IOException;
import java.util.Map;

import lu.zhe.mtgslackbot.rule.RuleUtils;

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
      ".*(?<url>http.?://media.wizards.com/\\d+/[^/]*/MagicCompRules.+\\.txt).*");

  private final boolean debug;
  private final String path;

  private Resources(String path, boolean debug) {
    System.setProperty("http.agent", "mtgslackbot");
    this.path = path;
    this.debug = debug;
  }

  private void write() {
    Map<String, String> allRules;
    // parse rules (need these to parse ability words)
    try {
      long start = System.currentTimeMillis();
      Scanner pageScanner =
          new Scanner(new URL(
              "https://magic.wizards.com/en/game-info/gameplay/rules-and-formats/rules")
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
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println(
          "Expected path argument. Should reference src/main/resources/lu/zhe/mtgslackbot/");
      System.exit(1);
    }
    System.out.println("Building resources...");
    new Resources(args[0], args.length == 2).write();
  }
}
