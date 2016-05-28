package lu.zhe.mtgslackbot;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Map.Entry;

import lu.zhe.mtgslackbot.card.Card;
import lu.zhe.mtgslackbot.card.CardUtils;
import lu.zhe.mtgslackbot.card.Format;
import lu.zhe.mtgslackbot.card.Layout;
import lu.zhe.mtgslackbot.card.Legality;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Wrapper class that contains set and card data.
 *
 * <p>Loads data from resources.
 */
public class DataSources {
  private static final SecureRandom random = new SecureRandom();

  private static final Joiner SEMICOLON_JOINER = Joiner.on("; ");
  private static final Function<Card, String> NAME_GETTER = new Function<Card, String>() {
    @Override
    public String apply(Card card) {
      return card.name();
    }
  };
  private static final Joiner COMMA_JOINER = Joiner.on(",");
  private static final List<Format> FORMATS = ImmutableList.of(
                                                  Format.STANDARD,
                                                  Format.MODERN,
                                                  Format.LEGACY,
                                                  Format.VINTAGE,
                                                  Format.COMMANDER);
  private final Map<String, Card> allCards;
  private final Map<String, String> allSets;
  private final Map<String, String> allRules;

  @SuppressWarnings("unchecked")
  public DataSources() {
    System.out.println("loading card data...");
    try (ObjectInputStream ois =
        new ObjectInputStream(
            DataSources.class.getClassLoader().getResourceAsStream(
                "lu/zhe/mtgslackbot/Cards.ser"))) {
      long start = System.currentTimeMillis();
      allCards = (Map<String, Card>) ois.readObject();
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Error reading serialized card data", e);
    }
    System.out.println("finish loading card data...");
    System.out.println("loading set data...");
    try (ObjectInputStream ois =
        new ObjectInputStream(
            DataSources.class.getClassLoader().getResourceAsStream(
                "lu/zhe/mtgslackbot/Sets.ser"))) {
      long start = System.currentTimeMillis();
      allSets = (Map<String, String>) ois.readObject();
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Error reading serialized set data", e);
    }
    System.out.println("finished loading set data...");
    System.out.println("loading rule data...");
    try (ObjectInputStream ois =
        new ObjectInputStream(
            DataSources.class.getClassLoader().getResourceAsStream(
                "lu/zhe/mtgslackbot/Rules.ser"))) {
      long start = System.currentTimeMillis();
      allRules = (Map<String, String>) ois.readObject();
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Error reading serialized rule data", e);
    }
    System.out.println("finished loading rule data...");
  }

  /** Get the display json for the parsed input. */
  public JSONObject processInput(ParsedInput input) {
    String arg = input.arg();
    List<Predicate<Card>> predicates = input.filters();
    Predicate<Card> predicate = Predicates.and(predicates);
    switch (input.command()) {
      case CARD:
        {
          Card card = allCards.get(arg);
          if (card != null) {
            return getDisplayJson(card);
          }
          List<Card> prefixMatch = new ArrayList<>();
          List<Card> anyMatch = new ArrayList<>();
          for (Entry<String, Card> candidate : allCards.entrySet()) {
            if (candidate.getKey().startsWith(arg)) {
              prefixMatch.add(candidate.getValue());
            } else if (candidate.getKey().contains(arg)) {
              anyMatch.add(candidate.getValue());
            }
          }
          if (prefixMatch.size() == 1) {
            return getDisplayJson(prefixMatch.get(0));
          }
          if (anyMatch.size() == 1) {
            return getDisplayJson(anyMatch.get(0));
          }
          if (!prefixMatch.isEmpty()) {
            return getTopList(prefixMatch);
          }
          if (!anyMatch.isEmpty()) {
            return getTopList(anyMatch);
          }
          return newJsonObject().put("text", "No matches found");
        }
      case SEARCH:
        {
          List<Card> matches = new ArrayList<>();
          for (Card candidate : allCards.values()) {
            if (predicate.apply(candidate)) {
              matches.add(candidate);
            }
          }
          if (!matches.isEmpty()) {
            return getTopList(matches);
          }
          return newJsonObject().put("text", "No matches found");
        }
      case COUNT:
        {
          int count = 0;
          for (Card candidate : allCards.values()) {
            if (predicate.apply(candidate)) {
              ++count;
            }
          }
          return newJsonObject().put("text", count + " matches");
        }
      case RANDOM:
        {
          List<Card> matches = new ArrayList<>();
          for (Card candidate : allCards.values()) {
            if (predicate.apply(candidate)) {
              matches.add(candidate);
            }
          }
          if (!matches.isEmpty()) {
            Card c = matches.get(random.nextInt(matches.size()));
            return getDisplayJson(c);
          }
          return newJsonObject().put("text", "No matches found");
        }
      case SET:
        {
          return getSet(arg);
        }
      case JHOIRA:
        return newJsonObject().put("text", "not implemented");
      case MOJOS:
        return newJsonObject().put("text", "not implemented");
      case MOMIR:
        return newJsonObject().put("text", "not implemented");
      case HELP:
        return newJsonObject().put("text", "not implemented");
      case RULE:
        return getGlossaryOrRuleEntry(arg);
      default:
        return newJsonObject().put("text", "not implemented");
    }
  }

  private static JSONObject getTopList(List<Card> cards) {
    int extras = 0;
    if (cards.size() > 10) {
      extras = cards.size() - 10;
      cards = cards.subList(0, 10);
    }
    Collections.sort(cards, new Comparator<Card>() {
      @Override
      public int compare(Card c1, Card c2) {
        return c1.name().compareTo(c2.name());
      }
    });
    String result = SEMICOLON_JOINER.join(Lists.transform(cards, NAME_GETTER));
    if (extras == 0) {
      return newJsonObject().put("text", result);
    }
    return newJsonObject().put("text", result + " plus " + extras + " others");
  }

  /**
   * Gets the display json for the canonical name of a card.
   */
  public JSONObject getDisplayJson(String canonicalName) {
    Card card = allCards.get(canonicalName);
    if (card == null) {
      throw new NoSuchElementException(canonicalName + " is not the canonical name for a card");
    }
    return getDisplayJson(card);
  }

  /**
   * Gets the display json for a {@link Card}.
   */
  public JSONObject getDisplayJson(Card card) {
    Layout layout = card.layout();
    switch (layout) {
      case NORMAL:
        return getNormalDisplayJson(card);
      case DOUBLE_FACED:
        return getFlipDoubleDisplayJson(card, "TRANSFORMS");
      case SPLIT:
        return getSplitDisplayJson(card);
      case FLIP:
        return getFlipDoubleDisplayJson(card, "FLIPS");
      default:
        throw new IllegalArgumentException("Unknown layout: " + layout);
    }
  }

  private String getLegality(Card card) {
    StringBuilder builder = new StringBuilder();
    Map<Format, Legality> legalities = card.legalities();
    boolean legal = false;
    for (Format format : FORMATS) {
      Legality legality = legalities.get(format);
      if (legality == Legality.LEGAL || legality == Legality.RESTRICTED) {
        legal = true;
        char formatChar = format.toString().toLowerCase().charAt(0);
        builder.append(formatChar);
        if (legality == Legality.RESTRICTED) {
          builder.append("*");
        }
      }
    }
    if (!legal) {
      builder.append("BANNED");
    }
    if (card.reserved()) {
      builder.append(" :(");
    }
    return builder.toString();
  }

  /**
   * Return json representation for normal card.
   */
  private JSONObject getNormalDisplayJson(Card card) {
    StringBuilder builder = new StringBuilder();
    builder
        .append(card.name())
        .append(" ")
        .append(card.manaCost())
        .append(" || ")
        .append(card.type());
    if (!card.power().isEmpty() && !card.toughness().isEmpty()) {
      builder.append(" ").append(card.power()).append("/").append(card.toughness());
    }
    if (card.loyalty() != null) {
      builder.append(" <").append(card.loyalty()).append(">");
    }
    builder
        .append(" | ")
        .append(getLegality(card))
        .append(" ")
        .append(COMMA_JOINER.join(card.printings()))
        .append("\n")
        .append(card.oracleText());
    JSONObject cardJson =
        new JSONObject().put("text", builder.toString()).put("color", getColor(card));
    return newJsonObject().put("attachments", new JSONArray().put(cardJson));
  }

  /**
   * Returns json representation for a double-sided or flip card.
   *
   * <p>flipsOrTransforms should be "FLIPS" or "TRANSFORMS."
   */
  private JSONObject getFlipDoubleDisplayJson(Card card, String flipsOrTransforms) {
    List<String> names = card.names();
    Card front = allCards.get(names.get(0));
    Card back = allCards.get(names.get(1));
    StringBuilder builder = new StringBuilder();
    builder
        .append(front.name())
        .append(" ")
        .append(front.manaCost())
        .append(" || ")
        .append(front.type());
    if (!front.power().isEmpty() && !front.toughness().isEmpty()) {
      builder.append(" ").append(front.power()).append("/").append(front.toughness());
    }
    if (front.loyalty() != null) {
      builder.append(" <").append(front.loyalty()).append(">");
    }
    builder
        .append(" | ")
        .append(getLegality(front))
        .append(" ")
        .append(COMMA_JOINER.join(front.printings()))
        .append("\n")
        .append(front.oracleText());
    JSONObject frontJson =
        new JSONObject().put("text", builder.toString()).put("color", getColor(front));
    builder = new StringBuilder()
        .append(back.name())
        .append(" || ")
        .append(back.type());
    if (!back.power().isEmpty() && !back.toughness().isEmpty()) {
      builder.append(" ").append(back.power()).append("/").append(back.toughness());
    }
    if (back.loyalty() != null) {
      builder.append(" <").append(back.loyalty()).append(">");
    }
    builder
        .append("\n")
        .append(back.oracleText());
    JSONObject backJson = new JSONObject()
        .put("text", builder.toString())
        .put("color", getColor(back))
        .put("pretext", flipsOrTransforms + " INTO:");
    return newJsonObject()
        .put("attachments", new JSONArray().put(frontJson).put(backJson));
  }

  private JSONObject getSplitDisplayJson(Card card) {
    List<String> names = card.names();
    Card left = allCards.get(names.get(0));
    Card right = allCards.get(names.get(1));
    StringBuilder builder = new StringBuilder();
    JSONObject json = newJsonObject();
    builder
        .append(left.name())
        .append(" // ")
        .append(right.name())
        .append(" (SPLIT CARD)")
        .append(" | ")
        .append(getLegality(left))
        .append(" ")
        .append(COMMA_JOINER.join(left.printings()));
    json.put("text", builder.toString());
    builder = new StringBuilder()
        .append(left.name())
        .append(" ")
        .append(left.manaCost())
        .append(" || ")
        .append(left.type());
    if (!left.power().isEmpty() && !left.toughness().isEmpty()) {
      builder.append(" ").append(left.power()).append("/").append(left.toughness());
    }
    if (left.loyalty() != null) {
      builder.append(" <").append(left.loyalty()).append(">");
    }
    builder
        .append("\n")
        .append(left.oracleText());
    JSONObject leftJson =
        new JSONObject().put("text", builder.toString()).put("color", getColor(left));
    builder = new StringBuilder()
        .append(right.name())
        .append(" ")
        .append(right.manaCost())
        .append(" || ")
        .append(right.type());
    if (!right.power().isEmpty() && !right.toughness().isEmpty()) {
      builder.append(" ").append(right.power()).append("/").append(right.toughness());
    }
    if (right.loyalty() != null) {
      builder.append(" <").append(right.loyalty()).append(">");
    }
    builder
        .append("\n")
        .append(right.oracleText());
    JSONObject rightJson =
        new JSONObject().put("text", builder.toString()).put("color", getColor(right));
    return json.put("attachments", new JSONArray().put(leftJson).put(rightJson));
  }

  /**
   * Gets the display text for a set given its set abbreviation.
   */
  public JSONObject getSet(String setAbbreviation) {
    String set = allSets.get(setAbbreviation.toLowerCase());
    if (set == null) {
      throw new NoSuchElementException(setAbbreviation + " is not a valid set abbreviation");
    }
    return newJsonObject().put("text", set);
  }

  public JSONObject getGlossaryOrRuleEntry(String keywordOrParagraph) {
    keywordOrParagraph = keywordOrParagraph.toLowerCase();
    String entry = allRules.get(keywordOrParagraph);
    if (entry != null) {
      return newJsonObject().put("text", entry);
    }
    for (Entry<String, String> ruleEntry : allRules.entrySet()) {
      if (ruleEntry.getKey().contains(keywordOrParagraph)) {
        return newJsonObject().put("text", ruleEntry.getValue());
      }
    }
    throw new NoSuchElementException("No entry for " + keywordOrParagraph);
  }

  private static JSONObject newJsonObject() {
    return new JSONObject().put("response_type", "in_channel");
  }

  private static String getColor(Card card) {
    if (card.colors().size() > 1) {
      return "#EEEE00";
    } else if (card.colors().isEmpty()) {
      return card.types().contains("equipment") ? "#884444" : "#BBBBBB";
    }
    switch (card.colors().iterator().next()) {
      case WHITE:
        return "#EEEEEE";
      case BLUE:
        return "#3333EE";
      case BLACK:
        return "#000000";
      case RED:
        return "#BB0000";
      case GREEN:
        return "#00AA00";
    }
    throw new IllegalStateException("unknown color information");
  }
}
