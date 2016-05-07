package lu.zhe.mtgslackbot.card;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for parsing card data.
 */
public class ParseUtils {
  private ParseUtils() {
    // Disable instantiation
  }

  /**
   * Parses a mapping of {@link Card} objects mapped to a canonical name.
   */
  public static Map<String, Card> parseCards(InputStream is, boolean useSlackSymbols)
      throws IOException {
    ImmutableMap.Builder<String, Card> allCards = ImmutableMap.builder();
    JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName().toLowerCase();
      String canonicalName = CardUtils.canonicalizeName(name);
      try {
        Card card = readCard(reader, useSlackSymbols);
        if (card != null) {
          allCards.put(canonicalName, card);
        }
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
    }
    reader.endObject();
    return allCards.build();
  }

  private static Card readCard(JsonReader reader, boolean useSlackSymbols) throws IOException {
    String name = null;
    String layout = null;
    List<String> sets = ImmutableList.of();
    List<String> names = ImmutableList.of();
    String manaCost = "";
    int cmc = 0;
    Set<Color> colors = ImmutableSet.of();
    Set<Color> colorIdentity = ImmutableSet.of();
    String typeLine = "";
    Set<String> superTypes = ImmutableSet.of();
    Set<String> types = ImmutableSet.of();
    Set<String> subtypes = ImmutableSet.of();
    String oracleText = "";
    String power = "";
    String toughness = "";
    Integer loyalty = null;
    boolean reserved = false;
    List<String> rulings = ImmutableList.of();
    Map<Format, Legality> legalities = ImmutableMap.of();
    reader.beginObject();
    while (reader.hasNext()) {
      switch (reader.nextName()) {
        case "name":
          name = reader.nextString();
          break;
        case "names":
          names = readNames(reader);
          break;
        case "layout":
          layout = reader.nextString();
          break;
        case "printings":
          sets = readSets(reader);
          break;
        case "cmc":
          try {
            cmc = reader.nextInt();
          } catch (NumberFormatException e) {
            // ignore
            reader.skipValue();
          }
          break;
        case "colors":
          colors = readColorArray(reader);
          break;
        case "colorIdentity":
          colorIdentity = readColorArray(reader);
          break;
        case "type":
          typeLine = reader.nextString();
          break;
        case "manaCost":
          manaCost = substituteSymbols(reader.nextString(), useSlackSymbols);
          break;
        case "text":
          oracleText = substituteSymbols(reader.nextString(), useSlackSymbols);
          break;
        case "power":
          power = reader.nextString();
          break;
        case "toughness":
          toughness = reader.nextString();
          break;
        case "loyalty":
          loyalty = reader.nextInt();
          break;
        case "reserved":
          reserved = reader.nextBoolean();
          break;
        case "rulings":
          rulings = parseRulings(reader);
          break;
        case "legalities":
          legalities = parseLegalities(reader);
          break;
        default:
          reader.skipValue();
      }
    }
    reader.endObject();
    if (sets == null || sets.isEmpty()) {
      return null;
    }
    if (layout == null) {
      return null;
    }
    try {
      return Card.create(
          name,
          Layout.fromString(layout),
          names,
          oracleText,
          manaCost,
          cmc,
          colors,
          colorIdentity,
          typeLine,
          superTypes,
          types,
          subtypes,
          power,
          toughness,
          loyalty,
          reserved,
          rulings,
          sets,
          legalities);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static List<String> readNames(JsonReader reader) throws IOException {
    ImmutableList.Builder<String> names = ImmutableList.builder();
    reader.beginArray();
    while (reader.hasNext()) {
      names.add(CardUtils.canonicalizeName(reader.nextString().toLowerCase()));
    }
    reader.endArray();
    return names.build();
  }

  private static Set<String> readTypes(JsonReader reader) throws IOException {
    ImmutableSet.Builder<String> names = ImmutableSet.builder();
    reader.beginArray();
    while (reader.hasNext()) {
      names.add(reader.nextString());
    }
    reader.endArray();
    return names.build();
  }

  private static Set<String> parseStringArray(JsonReader reader) throws IOException {
    ImmutableSet.Builder<String> strings = ImmutableSet.builder();
    reader.beginArray();
    while (reader.hasNext()) {
      strings.add(reader.nextString());
    }
    reader.endArray();
    return strings.build();
  }

  private static List<String> readSets(JsonReader reader) throws IOException {
    ImmutableList.Builder<String> sets = ImmutableList.builder();
    reader.beginArray();
    while (reader.hasNext()) {
      String setName = reader.nextString();
      if (!setName.equals("UGL") && !setName.equals("UNH") && setName.length() == 3) {
        sets.add(setName);
      }
    }
    reader.endArray();
    return sets.build();
  }

  private static Set<Color> readColorArray(JsonReader reader) throws IOException {
    ImmutableSet.Builder<Color> colors = ImmutableSet.builder();
    reader.beginArray();
    while (reader.hasNext()) {
      colors.add(Color.fromString(reader.nextString()));
    }
    reader.endArray();
    return colors.build();
  }

  private static List<String> parseRulings(JsonReader reader) throws IOException {
    ImmutableList.Builder<String> rulings = ImmutableList.builder();
    reader.beginArray();
    while (reader.hasNext()) {
      reader.beginObject();
      while (reader.hasNext()) {
        switch (reader.nextName()) {
          case "text":
            rulings.add(reader.nextString());
            break;
          default:
            reader.skipValue();
        }
      }
      reader.endObject();
    }
    reader.endArray();
    return rulings.build();
  }

  private static String substituteSymbols(String text, boolean useSlackSymbols) {
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
    } else {
      return text;
    }
  }

  private static Map<Format, Legality> parseLegalities(JsonReader reader) throws IOException {
    ImmutableMap.Builder<Format, Legality> legalities = ImmutableMap.builder();
    reader.beginArray();
    while (reader.hasNext()) {
      Format format = null;
      Legality legality = null;
      reader.beginObject();
      while (reader.hasNext()) {
        switch (reader.nextName()) {
          case "format":
            try {
              format = Format.valueOf(reader.nextString().toUpperCase());
            } catch (IllegalArgumentException e) {
              format = null;
            }
            break;
          case "legality":
            try {
              legality = Legality.valueOf(reader.nextString().toUpperCase());
            } catch (IllegalArgumentException e) {
              legality = null;
            }
            break;
        }
      }
      if (format != null && legality != null) {
        legalities.put(format, legality);
      }
      reader.endObject();
    }
    reader.endArray();
    return legalities.build();
  }
}
