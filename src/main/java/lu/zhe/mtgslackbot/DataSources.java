package lu.zhe.mtgslackbot;

import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import lu.zhe.mtgslackbot.card.Card;
import lu.zhe.mtgslackbot.card.Format;
import lu.zhe.mtgslackbot.card.Layout;
import lu.zhe.mtgslackbot.card.Legality;
import lu.zhe.mtgslackbot.parsing.Parsing.Command;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;
import lu.zhe.mtgslackbot.shared.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Wrapper class that contains set and card data.
 *
 * <p>Loads data from resources.
 */
public class DataSources {
  private static final SecureRandom random = new SecureRandom();
  private final ListeningExecutorService executor =
      MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

  private static final Joiner SEMICOLON_JOINER = Joiner.on("; ");
  private static final Joiner NEWLINE_JOINER = Joiner.on("\n");
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
  private static final String CREATURE_FORMAT_STRING =
      "https://api.scryfall.com/cards/random?" +
          "q=cmc%%3A%d%%20t%%3Acreature+-is%%3Afunny+-is%%3Aextra";
  private static final String EQUIPMENT_FORMAT_STRING =
      "https://api.scryfall.com/cards/random?" +
          "q=cmc%%3A%d%%20t%%3Aequipment+-is%%3Afunny+-is%%3Aextra";
  private static final String INSTANT_SORCERY_FORMAT_STRING =
      "https://api.scryfall.com/cards/random?" +
          "q=t%%3A%s+-is%%3Afunny+-is%%3Aextra";


  private final Map<String, Card> allCards;
  private final Map<String, String> allSets;
  private final Map<String, String> allRules;
  private final String abilityKeyWordsPattern;

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

      System.out.println("Reading ability keywords...");
      String abilityWordGlossary = allRules.get("ability word");
      String abilityWordParagraph =
          allRules.get(
              abilityWordGlossary.substring(
                  abilityWordGlossary.lastIndexOf(" ") + 1,
                  abilityWordGlossary.lastIndexOf(".")));
      String abilityWordsJoined =
          abilityWordParagraph.substring(
              abilityWordParagraph.indexOf("The ability words are ") + 22,
              abilityWordParagraph.length() - 1).replaceAll(" and ", " ");
      String[] abilityWords = abilityWordsJoined.split(",");
      List<String> abilityWordList = new ArrayList<>();
      for (String abilityWord : abilityWords) {
        abilityWord = abilityWord.trim();
        abilityWordList.add(
            Character.toUpperCase(abilityWord.charAt(0)) + abilityWord.substring(1));
      }
      abilityKeyWordsPattern = "(" + Joiner.on("|").join(abilityWordList) + ")";

    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Error reading serialized rule data", e);
    }
    System.out.println("finished loading rule data...");
  }

  /** Get the display json for the parsed input. */
  public JSONObject processInput(ParsedInput input, Consumer<String> responseHook) {
    String arg = input.arg();
    List<Predicate<Card>> predicates = input.filters();
    Predicate<Card> predicate = Predicates.and(predicates);
    switch (input.command()) {
      case TEST: {
        return newTopJsonObj().put("text",
            "The test command is currently not bound to any feature.");
      }
      case CARD: {
        Card card = allCards.get(Utils.normalizeInput(arg));
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
        if (prefixMatch.isEmpty() && anyMatch.size() == 1) {
          return getDisplayJson(anyMatch.get(0));
        }
        anyMatch.addAll(prefixMatch);
        if (!anyMatch.isEmpty()) {
          return getTopList(anyMatch);
        }
        return newTopJsonObj().put("text", "No matches found");
      }
      case RULING: {
        Card card = allCards.get(arg);
        if (card != null) {
          return getRulingJson(card);
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
          return getRulingJson(prefixMatch.get(0));
        }
        if (anyMatch.size() == 1) {
          return getRulingJson(anyMatch.get(0));
        }
        if (!prefixMatch.isEmpty()) {
          return getTopList(prefixMatch);
        }
        if (!anyMatch.isEmpty()) {
          return getTopList(anyMatch);
        }
        return newTopJsonObj().put("text", "No matches found");
      }
      case SEARCH: {
        List<Card> matches = new ArrayList<>();
        for (Card candidate : allCards.values()) {
          if (predicate.apply(candidate)) {
            matches.add(candidate);
          }
        }
        if (!matches.isEmpty()) {
          return getTopList(matches);
        }
        return newTopJsonObj().put("text", "No matches found");
      }
      case COUNT: {
        int count = 0;
        for (Card candidate : allCards.values()) {
          if (predicate.apply(candidate)) {
            ++count;
          }
        }
        return newTopJsonObj().put("text", count + " matches");
      }
      case RANDOM: {
        Card c = getRandom(predicate);
        return c == null ? newTopJsonObj().put("text", "No matches found") : getDisplayJson(c);
      }
      case SET: {
        return getSet(arg);
      }
      case JHOIRA: {
        String type = Ascii.toLowerCase(arg);
        if (!type.equals("instant") && !type.equals("sorcery")) {
          return newTopJsonObj().put("text",
              "argument must be either instant or sorcery, but got \"" + type + "\"");
        }
        List<ListenableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
          futures.add(executor.submit(new Callable<String>() {
            @Override
            public String call() {
              try {
                Scanner sc = new Scanner(
                    new URL(String.format(
                        INSTANT_SORCERY_FORMAT_STRING,
                        type)).openStream(),
                    "UTF-8");
                StringBuilder result = new StringBuilder();
                while (sc.hasNextLine()) {
                  result.append(sc.nextLine());
                }
                return result.toString();
              } catch (Exception e) {
                e.printStackTrace();
                return newTopJsonObj().put("text", "error").toString();
              }
            }
          }));
        }
        ListenableFuture<List<String>> liftedFuture = Futures.allAsList(futures);
        Futures.addCallback(liftedFuture, new FutureCallback<List<String>>() {
          @Override
          public void onSuccess(List<String> strings) {
            JSONObject response = newTopJsonObj();
            for (String s : strings) {
              getShortDisplayJson(new JSONObject(s), response);
            }
            responseHook.accept(response.toString());
          }

          public void onFailure(Throwable t) {
            responseHook.accept(newTopJsonObj().put("text", "error").toString());
          }
        });
        return newTopJsonObj().put("text", "Randomizing...");
      }
      case MOJOS: {
        int cmc;
        try {
          cmc = Integer.parseInt(arg);
          if (cmc <= 0) {
            return newTopJsonObj().put("text", "argument must be a positive integer");
          }
        } catch (NumberFormatException e) {
          return newTopJsonObj().put("text", "argument must be a positive integer");
        }
        List<ListenableFuture<String>> futures = new ArrayList<>();
        futures.add(executor.submit(new Callable<String>() {
          @Override
          public String call() {
            try {
              Scanner sc = new Scanner(
                  new URL(String.format(
                      CREATURE_FORMAT_STRING,
                      cmc)).openStream(),
                  "UTF-8");
              StringBuilder result = new StringBuilder();
              while (sc.hasNextLine()) {
                result.append(sc.nextLine());
              }
              return result.toString();
            } catch (Exception e) {
              e.printStackTrace();
              return null;
            }
          }
        }));
        if (cmc > 0) {
          futures.add(executor.submit(new Callable<String>() {
            @Override
            public String call() {
              try {
                Scanner sc = new Scanner(
                    new URL(String.format(
                        EQUIPMENT_FORMAT_STRING,
                        cmc - 1)).openStream(),
                    "UTF-8");
                StringBuilder result = new StringBuilder();
                while (sc.hasNextLine()) {
                  result.append(sc.nextLine());
                }
                return result.toString();
              } catch (Exception e) {
                e.printStackTrace();
                return null;
              }
            }
          }));
        }
        ListenableFuture<List<String>> liftedFuture = Futures.allAsList(futures);
        Futures.addCallback(liftedFuture, new FutureCallback<List<String>>() {
          @Override
          public void onSuccess(List<String> strings) {
            JSONObject response = newTopJsonObj();
            for (String s : strings) {
              if (s == null) {
                responseHook.accept(
                    newTopJsonObj().put("text", "Unable to generate a random creature").toString());
                return;
              }
              getShortDisplayJson(new JSONObject(s), response);
            }
            responseHook.accept(response.toString());
          }

          public void onFailure(Throwable t) {
            responseHook.accept(newTopJsonObj().put("text", "error").toString());
          }
        });
        return newTopJsonObj().put("text", "Randomizing...");
      }
      case MOMIR: {
        int cmc;
        try {
          cmc = Integer.parseInt(arg);
          if (cmc <= 0) {
            return newTopJsonObj().put("text", "argument must be a positive integer");
          }
        } catch (NumberFormatException e) {
          return newTopJsonObj().put("text", "argument must be a positive integer");
        }
        ListenableFuture<String> future = executor.submit(new Callable<String>() {
          @Override
          public String call() {
            try {
              Scanner sc = new Scanner(
                  new URL(String.format(
                      CREATURE_FORMAT_STRING,
                      cmc)).openStream(),
                  "UTF-8");
              StringBuilder result = new StringBuilder();
              while (sc.hasNextLine()) {
                result.append(sc.nextLine());
              }
              return
                  getShortDisplayJson(new JSONObject(result.toString()), newTopJsonObj());
            } catch (Exception e) {
              e.printStackTrace();
              return newTopJsonObj().put("text", "error").toString();
            }
          }
        });
        Futures.addCallback(future, new FutureCallback<String>() {
          @Override
          public void onSuccess(String response) {
            responseHook.accept(response);
          }

          public void onFailure(Throwable t) {
            responseHook.accept(newTopJsonObj().put("text", "error").toString());
          }
        });
        return newTopJsonObj().put("text", "Randomizing...");
      }
      case HELP:
        switch (arg) {
          case "":
            StringBuilder commands = new StringBuilder();
            for (Command c : Command.values()) {
              commands.append(c.toString().toLowerCase() + " ");
            }
            return newTopJsonObj().put(
                "text", "/mtg <command>\ncommands are: " + commands.toString().trim()).put("mrkdwn",
                false);
          case "card":
            return newTopJsonObj().put("text", "/mtg card <name>").put("mrkdwn", false);
          case "ruling":
            return newTopJsonObj().put("text", "/mtg ruling <name>").put("mrkdwn", false);
          case "set":
            return newTopJsonObj().put("text", "/mtg set <set abbreviation>").put("mrkdwn", false);
          case "search":
            // fall through intended
          case "count":
            // fall through intended
          case "random":
            return newTopJsonObj().put("text",
                "/mtg {search|count|random} <predicate 1> ...\n"
                    + "predicates:\n"
                    + "text(~, !~) cmc,pow,tgh,loyalty(==, ~=,...) t c is s").put("mrkdwn", false);
        }
        return newTopJsonObj().put("text", "not implemented").put("mrkdwn", false);
      case RULE:
        return getGlossaryOrRuleEntry(arg);
      default:
        return newTopJsonObj().put("text", "not implemented");
    }
  }

  private Card getRandom(Predicate<Card> predicate) {
    List<Card> matches = new ArrayList<>();
    for (Card candidate : allCards.values()) {
      if (predicate.apply(candidate)) {
        matches.add(candidate);
      }
    }
    if (!matches.isEmpty()) {
      return matches.get(random.nextInt(matches.size()));
    }
    return null;
  }

  private JSONObject getTopList(List<Card> cards) {
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
    if (cards.size() == 1) {
      return getDisplayJson(cards.get(0));
    }
    String result = SEMICOLON_JOINER.join(Lists.transform(cards, NAME_GETTER));
    if (extras == 0) {
      return newTopJsonObj().put("text", result);
    }
    return newTopJsonObj().put("text", result + "; plus " + extras + " others");
  }

  /**
   * Gets the rulings for card in json form.
   */
  public JSONObject getRulingJson(Card card) {
    if (card.rulings().isEmpty()) {
      return newTopJsonObj().put("text", "no rulings");
    }
    return newTopJsonObj().put("text", NEWLINE_JOINER.join(card.rulings()));
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

  public String getShortDisplayJson(JSONObject card, JSONObject response) {
    try {
      switch (card.getString("layout")) {
        case "normal":
          processNormalCard(card, response);
          return response.toString();
        case "transform":
          processTransformCard(card, response);
          return response.toString();
        case "split":
          processSplitCard(card, response);
          return response.toString();
        case "flip":
          processFlipCard(card, response);
          return response.toString();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error trying to process:\n" + card, e);
    }
    return "unable to render card";
  }

  private static void addAttachment(JSONObject response, JSONObject attachment) {
    JSONArray attachments = response.optJSONArray("attachments");
    if (attachments == null) {
      attachments = new JSONArray();
      response.put("attachments", attachments);
    }
    attachments.put(attachment);
  }

  private void processNormalCard(JSONObject face, JSONObject response) {
    StringBuilder builder = new StringBuilder();

    builder.append(face.getString("name")).append(" ").append(
        Utils.substituteSymbols(face.getString("mana_cost"))).append(" || ").append(
        face.getString("type_line"));
    if (face.has("power") && face.has("toughness")) {
      builder.append(" ").append(Utils.substituteAsterisk(face.getString("power"))).append(
          "/").append(Utils.substituteAsterisk(face.getString("toughness")));
    }
    builder
        .append("\n")
        .append(Utils.substituteAbilityWords(Utils.substituteSymbols(face.getString("oracle_text")),
            abilityKeyWordsPattern));

    addAttachment(response,
        newAttachment().put("text", builder.toString()).put("color", getColor(face)));
  }

  private void processTransformCard(JSONObject card, JSONObject response) {
    StringBuilder builder = new StringBuilder();

    JSONObject face = card.getJSONArray("card_faces").getJSONObject(0);
    builder.append(face.getString("name")).append(" ").append(
        Utils.substituteSymbols(face.getString("mana_cost"))).append(" || ").append(
        face.getString("type_line"));
    if (face.has("power") && face.has("toughness")) {
      builder.append(" ").append(Utils.substituteAsterisk(face.getString("power"))).append(
          "/").append(Utils.substituteAsterisk(face.getString("toughness")));
    }
    builder
        .append("\n")
        .append(Utils.substituteAbilityWords(Utils.substituteSymbols(face.getString("oracle_text")),
            abilityKeyWordsPattern));

    addAttachment(response,
        newAttachment().put("text", builder.toString()).put("color", getColor(face)));
  }

  private void processSplitCard(JSONObject card, JSONObject response) {
    StringBuilder builder = new StringBuilder();

    JSONArray sides = card.getJSONArray("card_faces");
    builder.append(card.getString("name")).append(" (SPLIT CARD)\n");
    JSONObject side1 = sides.getJSONObject(0);
    JSONObject side2 = sides.getJSONObject(1);
    builder.append(side1.getString("name")).append(" ").append(
        Utils.substituteSymbols(side1.getString("mana_cost"))).append(" || ").append(
        side1.getString("type_line")).append("\n").append(Utils.substituteAbilityWords(
        Utils.substituteSymbols(side1.getString("oracle_text")), abilityKeyWordsPattern)).append(
        "\n\n").append(
        side2.getString("name")).append(" ").append(
        Utils.substituteSymbols(side2.getString("mana_cost"))).append(" || ").append(
        side2.getString("type_line")).append("\n").append(Utils.substituteAbilityWords(
        Utils.substituteSymbols(side2.getString("oracle_text")), abilityKeyWordsPattern));

    addAttachment(response,
        newAttachment().put("text", builder.toString()).put("color", getColor(card)));
  }

  private void processFlipCard(JSONObject card, JSONObject response) {
    StringBuilder builder = new StringBuilder();

    JSONArray sides = card.getJSONArray("card_faces");

    JSONObject side1 = sides.getJSONObject(0);
    JSONObject side2 = sides.getJSONObject(1);
    builder.append(side1.getString("name")).append(" ").append(
        Utils.substituteSymbols(side1.getString("mana_cost"))).append(" || ").append(
        side1.getString("type_line")).append("\n").append(Utils.substituteAbilityWords(
        Utils.substituteSymbols(side1.getString("oracle_text")), abilityKeyWordsPattern)).append(
        "\n\nFLIPS INTO:\n\n").append(
        side2.getString("name")).append(" ").append(
        Utils.substituteSymbols(side2.getString("mana_cost"))).append(" || ").append(
        side2.getString("type_line")).append("\n").append(Utils.substituteAbilityWords(
        Utils.substituteSymbols(side2.getString("oracle_text")), abilityKeyWordsPattern));

    addAttachment(response,
        newAttachment().put("text", builder.toString()).put("color", getColor(card)));
  }

  public JSONObject getShortDisplayJson(Card card) {
    Layout layout = card.layout();
    switch (layout) {
      case NORMAL:
        return getNormalDisplayJson(card);
      case DOUBLE_FACED:
        return getNormalDisplayJson(card);
      case SPLIT:
        return getSplitDisplayJson(card, "SPLIT");
      case AFTERMATH:
        return getSplitDisplayJson(card, "AFTERMATH");
      case FLIP:
        return getFlipDoubleDisplayJson(card, "FLIPS");
      case MELD:
        return getNormalDisplayJson(card);
      default:
        throw new IllegalArgumentException("Unknown layout: " + layout);
    }
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
        return getSplitDisplayJson(card, "SPLIT");
      case AFTERMATH:
        return getSplitDisplayJson(card, "AFTERMATH");
      case FLIP:
        return getFlipDoubleDisplayJson(card, "FLIPS");
      case MELD:
        return getMeldDisplayJson(card);
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
        newAttachment().put("text", builder.toString()).put("color", getColor(card));
    return newTopJsonObj().put("attachments", new JSONArray().put(cardJson));
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
        newAttachment().put("text", builder.toString()).put("color", getColor(front));
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
    JSONObject backJson = newAttachment()
        .put("text", builder.toString())
        .put("color", getColor(back))
        .put("pretext", flipsOrTransforms + " INTO:");
    return newTopJsonObj()
        .put("attachments", new JSONArray().put(frontJson).put(backJson));
  }

  /**
   * Returns json representation for a meld card.
   */
  private JSONObject getMeldDisplayJson(Card card) {
    List<String> names = card.names();
    Card card1 = allCards.get(names.get(0));
    Card card2 = allCards.get(names.get(1));
    Card meld = allCards.get(names.get(2));
    StringBuilder builder = new StringBuilder();
    builder
        .append(card1.name())
        .append(" ")
        .append(card1.manaCost())
        .append(" || ")
        .append(card1.type());
    if (!card1.power().isEmpty() && !card1.toughness().isEmpty()) {
      builder.append(" ").append(card1.power()).append("/").append(card1.toughness());
    }
    if (card1.loyalty() != null) {
      builder.append(" <").append(card1.loyalty()).append(">");
    }
    builder
        .append(" | ")
        .append(getLegality(card1))
        .append(" ")
        .append(COMMA_JOINER.join(card1.printings()))
        .append("\n")
        .append(card1.oracleText());
    JSONObject card1Json =
        newAttachment().put("text", builder.toString()).put("color", getColor(card1));
    builder = new StringBuilder()
        .append(card2.name())
        .append(" ")
        .append(card2.manaCost())
        .append(" || ")
        .append(card2.type());
    if (!card2.power().isEmpty() && !card2.toughness().isEmpty()) {
      builder.append(" ").append(card2.power()).append("/").append(card2.toughness());
    }
    if (card2.loyalty() != null) {
      builder.append(" <").append(card2.loyalty()).append(">");
    }
    builder
        .append("\n")
        .append(card2.oracleText());
    JSONObject card2Json = newAttachment()
        .put("text", builder.toString())
        .put("color", getColor(card2))
        .put("pretext", "AND");
    builder = new StringBuilder()
        .append(meld.name())
        .append(" || ")
        .append(meld.type());
    if (!meld.power().isEmpty() && !meld.toughness().isEmpty()) {
      builder.append(" ").append(meld.power()).append("/").append(meld.toughness());
    }
    if (meld.loyalty() != null) {
      builder.append(" <").append(meld.loyalty()).append(">");
    }
    builder
        .append("\n")
        .append(meld.oracleText());
    JSONObject meldJson = newAttachment()
        .put("text", builder.toString())
        .put("color", getColor(meld))
        .put("pretext", "MELD INTO:");
    return newTopJsonObj()
        .put("attachments", new JSONArray().put(card1Json).put(card2Json).put(meldJson));
  }

  /**
   * Formats a split card for display.
   *
   * <p>type should be "AFTERMATH" or "SPLIT".
   */
  private JSONObject getSplitDisplayJson(Card card, String type) {
    List<String> names = card.names();
    Card left = allCards.get(names.get(0));
    Card right = allCards.get(names.get(1));
    StringBuilder builder = new StringBuilder();
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
        newAttachment().put("text", builder.toString()).put("color", getColor(left));
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
        newAttachment().put("text", builder.toString()).put("color", getColor(right));
    JSONObject json = newTopJsonObj();
    builder = new StringBuilder()
        .append(left.name())
        .append(" // ")
        .append(right.name())
        .append(" (").append(type).append(" CARD)")
        .append(" | ")
        .append(getLegality(left))
        .append(" ")
        .append(COMMA_JOINER.join(left.printings()));
    JSONObject header = newAttachment().put("text", builder.toString());
    return json.put("attachments", new JSONArray().put(header).put(leftJson).put(rightJson));
  }

  /**
   * Gets the display text for a set given its set abbreviation.
   */
  public JSONObject getSet(String setAbbreviation) {
    String set = allSets.get(setAbbreviation.toLowerCase());
    if (set == null) {
      throw new NoSuchElementException(setAbbreviation + " is not a valid set abbreviation");
    }
    return newTopJsonObj().put("text", set).put("mrkdwn", false);
  }

  public JSONObject getGlossaryOrRuleEntry(String keywordOrParagraph) {
    keywordOrParagraph = keywordOrParagraph.toLowerCase();
    String entry = allRules.get(keywordOrParagraph);
    if (entry != null) {
      return newTopJsonObj().put("text", entry);
    }
    for (Entry<String, String> ruleEntry : allRules.entrySet()) {
      if (ruleEntry.getKey().contains(keywordOrParagraph)) {
        return newTopJsonObj().put("text", ruleEntry.getValue());
      }
    }
    throw new NoSuchElementException("No entry for " + keywordOrParagraph);
  }

  private static JSONObject newTopJsonObj() {
    return new JSONObject().put("response_type", "in_channel");
  }

  private static JSONObject newAttachment() {
    return new JSONObject()
        .put("mrkdwn_in", new JSONArray().put("text").put("pretext"))
        .put("fallback", "mtgslackbot");
  }

  private static String getColor(JSONObject card) {
    JSONArray colors = card.getJSONArray("colors");
    if (colors.length() > 1) {
      return "#EEEE00";
    } else if (colors.length() == 0) {
      return card.getString("type_line").contains("Artifact") ? "#884444" : "#BBBBBB";
    }
    switch (colors.getString(0)) {
      case "W":
        return "#EEEEEE";
      case "U":
        return "#3333EE";
      case "B":
        return "#000000";
      case "R":
        return "#BB0000";
      case "G":
        return "#00AA00";
    }
    throw new IllegalStateException("unknown color information");
  }

  private static String getColor(Card card) {
    if (card.colors().size() > 1) {
      return "#EEEE00";
    } else if (card.colors().isEmpty()) {
      return card.types().contains("artifact") ? "#884444" : "#BBBBBB";
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

  private static boolean isLegal(Card card) {
    for (Format format : FORMATS) {
      Legality legality = card.legalities().get(format);
      if (legality == Legality.LEGAL || legality == Legality.RESTRICTED) {
        return true;
      }
    }
    return false;
  }
}
