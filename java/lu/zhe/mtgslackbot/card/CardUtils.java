package lu.zhe.mtgslackbot.card;

import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * General utilities for processing cards.
 */
public class CardUtils {
  private CardUtils() {
    // Disable instantiation
  }

  /**
   * Normalize a name.
   *
   * <p>Strips out symbols and numbers. Removes diacritcal marks and splits ae and oe ligatures.
   */
  public static String canonicalizeName(String name) {
    return Normalizer.normalize(name.toLowerCase(), Form.NFD)
        .replaceAll("[0-9\\-,'\".:?!\\p{InCombiningDiacriticalMarks}]", "")
        .replaceAll("æ", "ae")
        .replaceAll("œ", "oe");
  }
}
