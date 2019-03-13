package lu.zhe.mtgslackbot.rule;

import com.google.common.collect.ImmutableMap;
import lu.zhe.mtgslackbot.shared.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for parsing the comprehensive rules.
 */
public class RuleUtils {
  private static final Pattern RULE_PATTERN =
      Pattern.compile("(?<p>\\d+(\\.\\d+([a-z])?)?)\\.? (?<rule>.*)");

  private RuleUtils() {}

  /**
   * Reads a map from key words or paragraphs to text from the input {@link Scanner}.
   */
  public static Map<String, String> parseRules(Scanner sc) {
    // State for parsing
    boolean readRules = false;
    boolean readGlossary = false;
    String lastParagraph = null;
    String glossaryEntry = null;

    Map<String, String> builder = new HashMap<>();
    while (sc.hasNextLine()) {
      String line = Utils.substituteSymbols(
          sc.nextLine()
              .replaceAll("\u0093", "\"")
              .replaceAll("\u0094", "\"")
              .replaceAll("\u0097", "-")
              .replaceAll("\u0092", "'")
              .replaceAll("\\*", "\u2217"));
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
        glossaryEntry = null;
        continue;
      }
      if (readRules) {
        Matcher m = RULE_PATTERN.matcher(line);
        if (line.isEmpty()) {
          lastParagraph = null;
          continue;
        }
        if (line.startsWith("Example: ") || line.startsWith("  ")) {
          if (lastParagraph == null) {
            throw new IllegalStateException("lastParagraph state variable is null");
          }
          builder.put(lastParagraph, builder.get(lastParagraph) + "\n" + line.trim());
        } else {
          try {
            String paragraph = m.group("p");
            String rule = m.group("rule");
            lastParagraph = paragraph;
            builder.put(paragraph, rule);
          } catch (Exception e) {
            // Rule not formatted properly. Just skip.
          }
        }
      }
      if (readGlossary) {
        if (glossaryEntry == null) {
          glossaryEntry = line.replaceAll("(Obsolete)", "");
          lastParagraph = null;
        } else if (!line.trim().isEmpty()) {
          if (lastParagraph == null) {
            lastParagraph = line;
          } else {
            lastParagraph += "\n" + line;
          }
        } else {
          if (glossaryEntry.contains(",")) {
            for (String item : glossaryEntry.split(",")) {
              builder.put(item.trim(), lastParagraph);
            }
          } else {
            builder.put(glossaryEntry.trim().toLowerCase(), lastParagraph);
          }
          glossaryEntry = null;
        }
      }
    }
    return ImmutableMap.copyOf(builder);
  }
}
