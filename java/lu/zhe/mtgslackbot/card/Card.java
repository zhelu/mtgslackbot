package lu.zhe.mtgslackbot.card;

import com.google.auto.value.AutoValue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A representation of a Magic(tm) card.
 *
 * <p>Only real cards are represented. Excluded types include:
 * <ul>
 * <li>vanguard
 * <li>scheme
 * <li>token
 * </ul>
 */
@AutoValue
public abstract class Card implements Serializable {
  /** The name printed on the card. */
  public abstract String name();

  /** The layout of the card. */
  public abstract Layout layout();

  /** Canonicalized name for all faces / sides. */
  public abstract List<String> names();

  /** The official oracle rules. */
  public abstract String oracleText();

  /** Mana cost of this card. */
  public abstract String manaCost();

  /** Converted mana cost. */
  public abstract int cmc();

  /** The colors of this card. */
  public abstract Set<Color> colors();

  /** The color identity of this card for purposes of commander. */
  public abstract Set<Color> colorIdentity();

  /** The type line. */
  public abstract String type();

  /** The super types. */
  public abstract Set<String> supertypes();

  /** The types of the this card. */
  public abstract Set<String> types();

  /** The subtypes. */
  public abstract Set<String> subtypes();

  /** Card power if available. */
  public abstract @Nullable String power();

  /** Card toughness if available. */
  public abstract @Nullable String toughness();

  /** Card loyalty if available. */
  public abstract @Nullable Integer loyalty();

  /** True if this card is on the reserve list. */
  public abstract boolean reserved();

  /** Official rulings for this card. */
  public abstract List<String> rulings();

  /** All sets this card was in. */
  public abstract List<String> printings();

  /** Legalities by format. */
  public abstract Map<Format, Legality> legalities();

  public static Card create(
      String name,
      Layout layout,
      List<String> names,
      String oracleText,
      String manaCost,
      int cmc,
      Set<Color> color,
      Set<Color> colorIdentity,
      String type,
      Set<String> superTypes,
      Set<String> types,
      Set<String> subtypes,
      String power,
      String toughness,
      Integer loyalty,
      boolean reserved,
      List<String> rulings,
      List<String> printings,
      Map<Format, Legality> legalities) {
    return new AutoValue_Card(
        name,
        layout,
        names,
        oracleText,
        manaCost,
        cmc,
        color,
        colorIdentity,
        type,
        superTypes,
        types,
        subtypes,
        power,
        toughness,
        loyalty,
        reserved,
        rulings,
        printings,
        legalities);
  }
}
