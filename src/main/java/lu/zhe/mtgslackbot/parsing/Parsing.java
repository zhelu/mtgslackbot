package lu.zhe.mtgslackbot.parsing;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that performs input parsing.
 */
public class Parsing {
  private static final List<String> COMMANDS =
      Lists.transform(ImmutableList.copyOf(Command.values()),
          input -> input.toString().toLowerCase());
  private static final Joiner PIPE_JOINER = Joiner.on("|");
  private static final Pattern TOKENIZER =
      Pattern.compile(
          "(?<command>" + PIPE_JOINER.join(COMMANDS) + ")(\\s(?<args>.*))?");

  private Parsing() {}

  /**
   * Return a {@link ParsedInput} for the input.
   */
  public static ParsedInput getParsedInput(String input) {
    System.out.println(input);
    // Normalize all whitespace.
    input = input.replaceAll("\\s+", " ").trim();
    Matcher m = TOKENIZER.matcher(input);
    m.find();
    try {
      Command command = Command.valueOf(m.group("command").toUpperCase());
      String rawArgs = m.group("args");
      String args = rawArgs == null ? null : rawArgs.toLowerCase();
      switch (command) {
        case JHOIRA:
          // fall through intended
        case MOJOS:
          // fall through intended
        case MOMIR:
          // fall through intended
        case RULE:
          // fall through intended
        case TEST:
          // fall through intended
        case HELP:
          return ParsedInput.create(
              command,
              args == null ? "" : args);
        default:
          throw new IllegalArgumentException("Unprocessed command: " + command);
      }
    } catch (IllegalStateException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Invalid input", e);
    }
  }

  /** Base commands. */
  public enum Command {
    JHOIRA,
    MOJOS,
    MOMIR,
    RULE,
    TEST,
    HELP
  }

  /** Representation of parsed input. */
  @AutoValue
  public static abstract class ParsedInput {
    public abstract Command command();

    public abstract String arg();

    static ParsedInput create(Command command, String arg) {
      return new AutoValue_Parsing_ParsedInput(command, arg);
    }
  }
}
