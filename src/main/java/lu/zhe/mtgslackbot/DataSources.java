package lu.zhe.mtgslackbot;

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import lu.zhe.mtgslackbot.parsing.Parsing.Command;
import lu.zhe.mtgslackbot.parsing.Parsing.ParsedInput;
import lu.zhe.mtgslackbot.shared.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Wrapper class that contains set and card data.
 *
 * <p>Loads data from resources.
 */
public class DataSources {
  @SuppressWarnings("unused") // May be used later for generating random messages.
  private static final SecureRandom random = new SecureRandom();
  private final ListeningExecutorService executor =
      MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

  private static final String CREATURE_FORMAT_STRING =
      "https://api.scryfall.com/cards/random?" +
          "q=cmc%%3A%d%%20t%%3Acreature+-is%%3Afunny+-is%%3Aextra";
  private static final String EQUIPMENT_FORMAT_STRING =
      "https://api.scryfall.com/cards/random?" +
          "q=cmc%%3C%d%%20t%%3Aequipment+-is%%3Afunny+-is%%3Aextra";
  private static final String INSTANT_SORCERY_FORMAT_STRING =
      "https://api.scryfall.com/cards/random?" +
          "q=t%%3A%s+-is%%3Afunny+-is%%3Aextra";


  private final Map<String, String> allRules;
  private final String abilityKeyWordsPattern;

  @SuppressWarnings("unchecked")
    // The cast is safe.
  DataSources() {
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
  JSONObject processInput(ParsedInput input, Consumer<String> responseHook) {
    String arg = input.arg();
    switch (input.command()) {
      case TEST: {
        return newTopJsonObj().put("text",
            "The test command is currently not bound to any feature.");
      }
      case JHOIRA: {
        String type = Ascii.toLowerCase(arg);
        if (!type.equals("instant") && !type.equals("sorcery")) {
          return newTopJsonObj().put("text",
              "argument must be either instant or sorcery, but got \"" + type + "\"");
        }
        List<ListenableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
          futures.add(executor.submit(() -> {
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
          }));
        }
        ListenableFuture<List<String>> liftedFuture = Futures.allAsList(futures);
        try {
          List<String> strings = liftedFuture.get(2250, TimeUnit.MILLISECONDS);
          return processListResponse(strings);
        } catch (InterruptedException | ExecutionException e) {
          return newTopJsonObj().put("text", "Internal server error");
        } catch (TimeoutException e) {
          Futures.addCallback(liftedFuture, new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> strings) {
              JSONObject response = processListResponse(strings);
              Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
              responseHook.accept(response.toString());
            }

            public void onFailure(Throwable t) {
              Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
              responseHook.accept(newTopJsonObj().put("text", "error").toString());
            }
          });
          return newTopJsonObj().put("text", "Randomizing...");
        }
      }
      case MOJOS: {
        int cmc;
        try {
          cmc = Integer.parseInt(arg);
          if (cmc < 0) {
            return newTopJsonObj().put("text", "argument must be a positive integer");
          }
        } catch (NumberFormatException e) {
          return newTopJsonObj().put("text", "argument must be a positive integer");
        }
        List<ListenableFuture<String>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
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
        }));
        if (cmc > 0) {
          futures.add(executor.submit(() -> {
            try {
              Scanner sc = new Scanner(
                  new URL(String.format(
                      EQUIPMENT_FORMAT_STRING,
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
          }));
        }
        ListenableFuture<List<String>> liftedFuture = Futures.allAsList(futures);
        try {
          List<String> strings = liftedFuture.get(2250, TimeUnit.MILLISECONDS);
          return processListResponse(strings);
        } catch (InterruptedException | ExecutionException e) {
          return newTopJsonObj().put("text", "Internal server error");
        } catch (TimeoutException e) {
          Futures.addCallback(liftedFuture, new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> strings) {
              JSONObject response = processListResponse(strings);
              Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
              responseHook.accept(response.toString());
            }

            public void onFailure(Throwable t) {
              Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
              responseHook.accept(newTopJsonObj().put("text", "error").toString());
            }
          });
          return newTopJsonObj().put("text", "Randomizing...");
        }
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
        ListenableFuture<String> future = executor.submit(() -> {
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
            return newTopJsonObj().put("text", "error").toString();
          }
        });
        try {
          String string = future.get(2250, TimeUnit.MILLISECONDS);
          return processListResponse(ImmutableList.of(string));
        } catch (InterruptedException | ExecutionException e) {
          return newTopJsonObj().put("text", "Internal server error");
        } catch (TimeoutException e) {
          Futures.addCallback(future, new FutureCallback<String>() {
            @Override
            public void onSuccess(String string) {
              JSONObject response = processListResponse(ImmutableList.of(string));
              Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
              responseHook.accept(response.toString());
            }

            public void onFailure(Throwable t) {
              Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
              responseHook.accept(newTopJsonObj().put("text", "error").toString());
            }
          });
          return newTopJsonObj().put("text", "Randomizing...");
        }
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

  private String getShortDisplayJson(JSONObject card, JSONObject response) {
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

  private JSONObject getGlossaryOrRuleEntry(String keywordOrParagraph) {
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

  private JSONObject processListResponse(List<String> strings) {
    JSONObject response = newTopJsonObj();
    for (String s : strings) {
      if (s == null) {
        return
            newTopJsonObj().put("text", "Unable to generate a random creature");
      }
      getShortDisplayJson(new JSONObject(s), response);
    }
    return response;
  }
}
