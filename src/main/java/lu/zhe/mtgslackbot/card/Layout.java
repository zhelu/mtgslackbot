package lu.zhe.mtgslackbot.card;

/**
 * Represents the layouts of supported Magic(tm) cards.
 *
 * <p>
 * <ul>
 * <li>normal / leveler
 * <li>flip
 * <li>split
 * <li>double-faced
 * </ul>
 */
public enum Layout {
  NORMAL,
  FLIP,
  SPLIT,
  DOUBLE_FACED,
  MELD,
  AFTERMATH;

  public static Layout fromString(String layout) {
    switch (layout) {
      case "normal":
        // fall through intended
      case "leveler":
        return NORMAL;
      case "flip":
        return FLIP;
      case "split":
        return SPLIT;
      case "double-faced":
        return DOUBLE_FACED;
      case "meld":
        return MELD;
      case "aftermath":
        return AFTERMATH;
      default:
        throw new IllegalArgumentException("Unknown layout: " + layout);
    }
  }
}
