package lu.zhe.mtgslackbot.parsing;

import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lu.zhe.mtgslackbot.card.Card;

/**
 * Class that performs input parsing.
 */
public class Parsing {
  private static final List<String> COMMANDS =
      ImmutableList.of("card", "search", "count", "random", "set", "jhoira", "mojos", "momir", "help");
  private static final List<String> OPS =
      ImmutableList.of(":", "=", "<=", "<", ">", ">=", "!=", "~");
  private static final Joiner PIPE_JOINER = Joiner.on("|");
  private static final Pattern TOKENIZER =
      Pattern.compile(
          "!mtg\\s+(?<command>" + PIPE_JOINER.join(COMMANDS) + ")\\s+(?<args>.*)");
  private static final Pattern PREDICATE_TOKENIZER =
      Pattern.compile(
          "(?:\\s*(?<var>[a-z]+)"
          + "(?<op>" + PIPE_JOINER.join(OPS) + ")"
          + "(?<quote>\"?)(?<val>.+)\\k<quote>)");

  private Parsing() {}

  /**
   * Return a {@link ParsedInput} for the input.
   */
  public static ParsedInput getParsedInput(String input) {
    System.out.println(input);
    Matcher m = TOKENIZER.matcher(input);
    try {
      Command command = Command.valueOf(m.group("command").toUpperCase());
      String args = m.group("args");
      switch (command) {
        case CARD:
          // fall through intended
        case SET:
          // fall through intended
        case JHOIRA:
          // fall through intended
        case MOJOS:
          // fall through intended
        case MOMIR:
          // fall through intended
        case HELP:
          return ParsedInput.create(command, m.group("args"), ImmutableList.<Predicate<Card>>of());
        case SEARCH:
          // fall through intended
        case COUNT:
          // fall through intended
        case RANDOM:
          Matcher argMatcher = PREDICATE_TOKENIZER.matcher(args);
          while (argMatcher.find()) {
            System.out.println("var: " + argMatcher.group("var"));
            System.out.println("op: " + argMatcher.group("op"));
            System.out.println("val: " + argMatcher.group("val"));
          }
          return null;
        default:
          throw new IllegalArgumentException("Unprocessed command: " + command);
      }
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException("Invalid input", e);
    }
  }

  /** Creates a filter from the predicate group. */
  private Predicate<Card> parseFilter(String var, String op, String val) {
    return null;
  }

  /** Base commands. */
  public enum Command {
    CARD,
    SEARCH,
    COUNT,
    RANDOM,
    SET,
    JHOIRA,
    MOJOS,
    MOMIR,
    HELP;
  }

  /** Representation of parsed input. */
  @AutoValue
  public static abstract class ParsedInput {
    public abstract Command command();

    public abstract String arg();

    public abstract List<Predicate<Card>> filters();

    public static ParsedInput create(Command command, String arg, List<Predicate<Card>> filters) {
      return new AutoValue_Parsing_ParsedInput(command, arg, filters);
    }
  }
}
