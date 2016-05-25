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
  private final boolean useSlackSymbols;

  @SuppressWarnings("unchecked")
  public DataSources(boolean useSlackSymbols) {
    this.useSlackSymbols = useSlackSymbols;
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

  /** Get the display string for the parsed input. */
  public String processInput(ParsedInput input) {
    String arg = input.arg();
    List<Predicate<Card>> predicates = input.filters();
    Predicate<Card> predicate = Predicates.and(predicates);
    switch (input.command()) {
      case CARD:
        {
          Card card = allCards.get(arg);
          if (card != null) {
            return getDisplayString(card);
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
            return getDisplayString(prefixMatch.get(0));
          }
          if (anyMatch.size() == 1) {
            return getDisplayString(anyMatch.get(0));
          }
          if (!prefixMatch.isEmpty()) {
            return getTopList(prefixMatch);
          }
          if (!anyMatch.isEmpty()) {
            return getTopList(anyMatch);
          }
          return "No matches found";
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
          return "No matches found";
        }
      case COUNT:
        {
          int count = 0;
          for (Card candidate : allCards.values()) {
            if (predicate.apply(candidate)) {
              ++count;
            }
          }
          return count + " matches";
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
            return getDisplayString(c);
          }
          return "No matches found";
        }
      case SET:
        {
          return getSet(arg);
        }
      case JHOIRA:
        return "not implemented";
      case MOJOS:
        return "not implemented";
      case MOMIR:
        return "not implemented";
      case HELP:
        return "not implemented";
      case RULE:
        return getGlossaryOrRuleEntry(arg);
      default:
        return "not implemented";
    }
  }

  private static String getTopList(List<Card> cards) {
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
      return result;
    }
    return result + " plus " + extras + " others";
  }

  /**
   * Gets the display string for the canonical name of a card.
   */
  public String getDisplayString(String canonicalName) {
    Card card = allCards.get(canonicalName);
    if (card == null) {
      throw new NoSuchElementException(canonicalName + " is not the canonical name for a card");
    }
    return getDisplayString(card);
  }

  /**
   * Gets the display string for a {@link Card}.
   */
  public String getDisplayString(Card card) {
    Layout layout = card.layout();
    switch (layout) {
      case NORMAL:
        return getNormalDisplayString(card);
      case DOUBLE_FACED:
        return getFlipDoubleDisplayString(card, "TRANSFORMS");
      case SPLIT:
        return getSplitDisplayString(card);
      case FLIP:
        return getFlipDoubleDisplayString(card, "FLIPS");
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

  private String getNormalDisplayString(Card card) {
    StringBuilder builder = new StringBuilder();
    builder
        .append(card.name())
        .append(" ")
        .append(substituteSymbols(card.manaCost()))
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
        .append(substituteSymbols(card.oracleText()));
    return builder.toString();
  }

  /**
   * Returns a string for a double-sided or flip card.
   *
   * <p>flipsOrTransforms should be "FLIPS" or "TRANSFORMS."
   */
  private String getFlipDoubleDisplayString(Card card, String flipsOrTransforms) {
    List<String> names = card.names();
    Card front = allCards.get(names.get(0));
    Card back = allCards.get(names.get(1));
    StringBuilder builder = new StringBuilder();
    builder
        .append(front.name())
        .append(" ")
        .append(substituteSymbols(front.manaCost()))
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
        .append(substituteSymbols(front.oracleText()))
        .append("\n")
        .append(flipsOrTransforms)
        .append(" INTO:\n");
    builder
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
        .append(substituteSymbols(back.oracleText()));
    return builder.toString();
  }

  private String getSplitDisplayString(Card card) {
    List<String> names = card.names();
    Card left = allCards.get(names.get(0));
    Card right = allCards.get(names.get(1));
    StringBuilder builder = new StringBuilder();
    builder
        .append(left.name())
        .append(" // ")
        .append(right.name())
        .append(" (SPLIT CARD)")
        .append(" | ")
        .append(getLegality(left))
        .append(" ")
        .append(COMMA_JOINER.join(left.printings()))
        .append("\n")
        .append(left.name())
        .append(" ")
        .append(substituteSymbols(left.manaCost()))
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
        .append(substituteSymbols(left.oracleText()))
        .append("\n")
        .append(right.name())
        .append(" ")
        .append(substituteSymbols(right.manaCost()))
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
        .append(substituteSymbols(right.oracleText()));
    return builder.toString();
  }

  /**
   * Gets the display text for a set given its set abbreviation.
   */
  public String getSet(String setAbbreviation) {
    String set = allSets.get(setAbbreviation);
    if (set == null) {
      throw new NoSuchElementException(set + " is not a valid set abbreviation");
    }
    return set;
  }

  public String getGlossaryOrRuleEntry(String keywordOrParagraph) {
    String entry = allRules.get(keywordOrParagraph);
    if (entry == null) {
      throw new NoSuchElementException("No entry for " + keywordOrParagraph);
    }
    return entry;
  }

  private String substituteSymbols(String text) {
    if (useSlackSymbols) {
      return text
          .replaceAll("\\{2/B\\}", ":2b:")
          .replaceAll("\\{2/G\\}", ":2g:")
          .replaceAll("\\{2/R\\}", ":2r:")
          .replaceAll("\\{2/U\\}", ":2u:")
          .replaceAll("\\{2/W\\}", ":2w:")
          .replaceAll("\\{B/G\\}", ":bg:")
          .replaceAll("\\{B/P\\}", ":bp:")
          .replaceAll("\\{B/R\\}", ":br:")
          .replaceAll("\\{C\\}", ":c:")
          .replaceAll("\\{G\\}", ":g:")
          .replaceAll("\\{G/P\\}", ":gp:")
          .replaceAll("\\{G/U\\}", ":gu:")
          .replaceAll("\\{G/W\\}", ":gw:")
          .replaceAll("\\{Q\\}", ":q:")
          .replaceAll("\\{R\\}", ":r:")
          .replaceAll("\\{R/G\\}", ":rg:")
          .replaceAll("\\{R/P\\}", ":rp:")
          .replaceAll("\\{R/W\\}", ":rw:")
          .replaceAll("\\{S\\}", ":s:")
          .replaceAll("\\{T\\}", ":t:")
          .replaceAll("\\{U\\}", ":u:")
          .replaceAll("\\{U/P\\}", ":u-p:")
          .replaceAll("\\{U/B\\}", ":ub:")
          .replaceAll("\\{U/R\\}", ":ur:")
          .replaceAll("\\{W/U\\}", ":wu:")
          .replaceAll("\\{W/P\\}", ":wp:")
          .replaceAll("\\{W\\}", ":w:")
          .replaceAll("\\{0\\}", ":0:")
          .replaceAll("\\{1\\}", ":1:")
          .replaceAll("\\{2\\}", ":2:")
          .replaceAll("\\{3\\}", ":3:")
          .replaceAll("\\{4\\}", ":4:")
          .replaceAll("\\{5\\}", ":5:")
          .replaceAll("\\{6\\}", ":6:")
          .replaceAll("\\{7\\}", ":7:")
          .replaceAll("\\{8\\}", ":8:")
          .replaceAll("\\{9\\}", ":9:")
          .replaceAll("\\{10\\}", ":10:")
          .replaceAll("\\{11\\}", ":11:")
          .replaceAll("\\{12\\}", ":12:")
          .replaceAll("\\{13\\}", ":13:")
          .replaceAll("\\{14\\}", ":14:")
          .replaceAll("\\{15\\}", ":15:")
          .replaceAll("\\{16\\}", ":16:")
          .replaceAll("\\{17\\}", ":17:")
          .replaceAll("\\{18\\}", ":18:")
          .replaceAll("\\{19\\}", ":19:")
          .replaceAll("\\{20\\}", ":20:")
          .replaceAll("\\{X\\}", ":xx:")
          .replaceAll("\\{W/B\\}", ":wb:")
          .replaceAll("\\{B\\}", ":bk:");
    }
    return text;
  }
}
