package lu.zhe.mtgslackbot;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;

import lu.zhe.mtgslackbot.card.Card;
import lu.zhe.mtgslackbot.card.CardUtils;
import lu.zhe.mtgslackbot.card.Format;
import lu.zhe.mtgslackbot.card.Layout;
import lu.zhe.mtgslackbot.card.Legality;

/**
 * Wrapper class that contains set and card data.
 *
 * <p>Loads data from resources.
 */
public class DataSources {
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
                "lu/zhe/mtgslackbot/res/Cards.ser"))) {
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
                "lu/zhe/mtgslackbot/res/Sets.ser"))) {
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
                "lu/zhe/mtgslackbot/res/Rules.ser"))) {
      long start = System.currentTimeMillis();
      allRules = (Map<String, String>) ois.readObject();
      System.out.println("\tTook " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Error reading serialized rule data", e);
    }
    System.out.println("finished loading rule data...");
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
    for (Format format : FORMATS) {
      Legality legality = legalities.get(format);
      if (legality == Legality.LEGAL || legality == Legality.RESTRICTED) {
        char formatChar = format.toString().toLowerCase().charAt(0);
        builder.append(formatChar);
        if (legality == Legality.RESTRICTED) {
          builder.append("*");
        }
      }
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
        .append(front.oracleText())
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
        .append(back.oracleText());
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
        .append(left.oracleText())
        .append("\n")
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
}
