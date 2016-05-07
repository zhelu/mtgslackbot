package lu.zhe.mtgslackbot.rule;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utilities for parsing the comprehensive rules.
 */
public class RuleUtils {
  private static final Pattern RULE_PATTERN =
      Pattern.compile("(?<p>\\d+\\.(\\d+(\\.|[a-z]))?) (?<rule>.*)");

  private RuleUtils() {}

  /**
   * Reads a map from key words or paragraphs to text from the input {@link Scanner}.
   */
  public static Map<String, String> parseRules(Scanner sc) throws IOException {
    // State for parsing
    boolean readRules = false;
    boolean readGlossary = false;
    String lastParagraph = null;
    String glossaryEntry = null;

    Map<String, String> builder = new HashMap<>();
    while (sc.hasNextLine()) {
      String line = sc.nextLine();
      if (line.equals("Credits")) {
        if (!readGlossary) {
          readRules = true;
          continue;
        } else {
          readGlossary = false;
          continue;
        }
      }
      if (line.equals("Glossary") && readRules) {
        readRules = false;
        readGlossary = true;
        continue;
      }
      if (line.isEmpty()) {
        lastParagraph = null;
        continue;
      }
      if (readRules) {
        Matcher m = RULE_PATTERN.matcher(line);
        if (!m.matches() && !line.startsWith("Example: ") && !line.startsWith("  ")) {
          System.out.println(line);
        }
        if (line.startsWith("Example: ") || line.startsWith("  ")) {
          if (lastParagraph == null) {
            throw new IllegalStateException("lastParagraph state variable is null");
          }
          builder.put(lastParagraph, builder.get(lastParagraph) + "\n" + line.trim());
        } else {
          String paragraph = m.group("p");
          String rule = m.group("rule");
          lastParagraph = paragraph;
          builder.put(paragraph, rule);
        }
      }
      if (readGlossary) {
        String text = sc.nextLine();
        if (glossaryEntry == null) {
          glossaryEntry = text.replaceAll("(Obsolete)", "");
        } else {
          if (glossaryEntry.contains(",")) {
            for (String item : glossaryEntry.split(",")) {
              builder.put(item.trim(), text);
            }
          } else {
            builder.put(glossaryEntry.trim(), text);
          }
          glossaryEntry = null;
        }
      }
    }
    return ImmutableMap.copyOf(builder);
  }
}
