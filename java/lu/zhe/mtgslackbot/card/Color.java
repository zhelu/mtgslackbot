package lu.zhe.mtgslackbot.card;

/**
 * Represents the 5 colors of Magic(tm) cards.
 *
 * <p>
 * <ul>
 * <li>white
 * <li>blue
 * <li>black
 * <li>red
 * <li>green
 * </ul>
 */
public enum Color {
  WHITE,
  BLUE,
  BLACK,
  RED,
  GREEN;

  public static Color fromString(String color) {
    switch (color.toUpperCase()) {
      case "B":
        // Fallthrough intended
      case "BLACK":
        return Color.BLACK;
      case "U":
        // Fallthrough intended
      case "BLUE":
        return Color.BLUE;
      case "R":
        // Fallthrough intended
      case "RED":
        return Color.RED;
      case "W":
        // Fallthrough intended
      case "WHITE":
        return Color.WHITE;
      case "G":
        // Fallthrough intended
      case "GREEN":
        return Color.GREEN;
      default:
        throw new IllegalArgumentException("Unknown color: " + color);
    }
  }
}
