package lu.zhe.mtgslackbot.shared;

/**
 * Shared utilities.
 */
public class Utils {
  private Utils() {
    // Disable instantiation
  }

  /**
   * Replace standard Gatherer text symbols with Slack emojis.
   */
  public static String substituteSymbols(String text) {
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
        .replaceAll("\\{B\\}", ":bk:")
        .replaceAll("\\{E\\}", ":e:")
        .replaceAll("\\(", "(_")
        .replaceAll("\\)", "_)")
        .replaceAll("\\{P\\}", ":p:");
  }

  public static String substituteAsterisk(String text) {
    return text.replaceAll("\\*", "\u2217");
  }

  /** Canonicalize input for card searching. */
  public static String normalizeInput(String input) {
    return input.replaceAll("\\s+", " ").toLowerCase();
  }
}
