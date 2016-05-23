package lu.zhe.mtgslackbot.parsing;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lu.zhe.mtgslackbot.card.Card;
import lu.zhe.mtgslackbot.card.CardUtils;
import lu.zhe.mtgslackbot.card.Color;
import lu.zhe.mtgslackbot.card.Layout;

import javax.annotation.Nullable;

/**
 * Class that performs input parsing.
 */
public class Parsing {
  private static final List<String> COMMANDS =
      ImmutableList.of(
          "card", "search", "count", "random", "set", "jhoira", "mojos", "momir", "help");
  private static final List<String> OPS =
      ImmutableList.of(":", "=", "<=", "<", ">", ">=", "!=", "~", "!~");
  private static final Joiner PIPE_JOINER = Joiner.on("|");
  private static final Pattern TOKENIZER =
      Pattern.compile(
          "s(?<command>" + PIPE_JOINER.join(COMMANDS) + ")\\s(?<args>.*)");
  private static final Pattern WHOLE_PREDICATE =
      Pattern.compile("([a-z!]+(" + PIPE_JOINER.join(OPS) + ")(\"?)(.*)(\\2)|" +
         "([a-z!]+(" + PIPE_JOINER.join(OPS) + ")[^ \"]+))");
  private static final Pattern PREDICATE_TOKENIZER =
      Pattern.compile(
          "(?:\\s*(?<var>[a-z!]+)"
          + "(?<op>" + PIPE_JOINER.join(OPS) + ")"
          + "(?<quote>\"?)(?<val>.+)\\k<quote>)");
  private static final List<String> INT_OPS = ImmutableList.of("=", "<=", "<", ">", ">=", "!=");
  private static final Map<String, Function<Card, Integer>> PROPERTY_FUNCS =
      ImmutableMap.of("cmc",
          new Function<Card, Integer>() {
            @Override
            public Integer apply(Card card) {
              return card.cmc();
            }
          },
          "pow",
          new Function<Card, Integer>() {
            @Override
            public Integer apply(Card card) {
              try {
                return Integer.parseInt(card.power());
              } catch (NumberFormatException e) {
                return null;
              }
            }
          },
          "tgh",
          new Function<Card, Integer>() {
            @Override
            public Integer apply(Card card) {
              try {
                return Integer.parseInt(card.toughness());
              } catch (NumberFormatException e) {
                return null;
              }
            }
          },
          "loyalty",
          new Function<Card, Integer>() {
            @Override
            public Integer apply(Card card) {
              return card.loyalty();
            }
          });

  /* Maps var and op to function from val to predicate. */
  private static final Table<String, String, Function<String, Predicate<Card>>> PREDICATE_TABLE;
  static {
    PREDICATE_TABLE = buildTable();
  }

  private Parsing() {}

  /**
   * Return a {@link ParsedInput} for the input.
   */
  public static ParsedInput getParsedInput(String input) {
    System.out.println(input);
    Matcher m = TOKENIZER.matcher(input);
    m.find();
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
          return ParsedInput.create(
              command,
              CardUtils.canonicalizeName(m.group("args")),
              ImmutableList.<Predicate<Card>>of());
        case SEARCH:
          // fall through intended
        case COUNT:
          // fall through intended
        case RANDOM:
          List<Predicate<Card>> predicates = new ArrayList<>();
          Matcher preds = WHOLE_PREDICATE.matcher(args);
          while (preds.find()) {
            Matcher argMatcher = PREDICATE_TOKENIZER.matcher(preds.group());
            argMatcher.find();
            String var = argMatcher.group("var");
            String op = argMatcher.group("op");
            String val = argMatcher.group("val");
            Function<String, Predicate<Card>> f = PREDICATE_TABLE.get(var, op);
            if (f == null) {
              throw new IllegalArgumentException("Unprocessed arg: " + var + op + val);
            }
            predicates.add(f.apply(val));
          }
          return ParsedInput.create(command, "", predicates);
        default:
          throw new IllegalArgumentException("Unprocessed command: " + command);
      }
    } catch (IllegalStateException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Invalid input", e);
    }
  }

  /** Creates a filter from the predicate group. */
  private Predicate<Card> parseFilter(final String var, final String op, final String val) {
    return new Predicate<Card>() {
      @Override
      public boolean apply(Card card) {
        return false;
      }
    };
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

  private static Table<String, String, Function<String, Predicate<Card>>> buildTable() {
    ImmutableTable.Builder<String, String, Function<String, Predicate<Card>>> builder
        = ImmutableTable.builder();
    builder.put("text", "~", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(String value) {
        return getTextPredicate(value);
      }
    });
    builder.put("text", "!~", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(String value) {
        return Predicates.not(getTextPredicate(value));
      }
    });
    builder.put("is", ":", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(String value) {
        return getIsPredicate(value);
      }
    });
    builder.put("!is", ":", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(String value) {
        return Predicates.not(getIsPredicate(value));
      }
    });
    builder.put("t", ":", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(String value) {
        return getTypePredicate(value);
      }
    });
    builder.put("!t", ":", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(String value) {
        return Predicates.not(getTypePredicate(value));
      }
    });
    // int comparing predicates
    addEntries("cmc", builder);
    addEntries("pow", builder);
    addEntries("tgh", builder);
    addEntries("loyalty", builder);
    builder.put("ci", ":", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(final String value) {
        return new Predicate<Card>() {
          @Override
          public boolean apply(Card card) {
            for (int i = 0; i < value.length(); ++i) {
              switch(value.charAt(i)) {
                case 'w':
                  if (!card.colorIdentity().contains(Color.WHITE)) {
                    return false;
                  }
                  continue;
                case 'u':
                  if (!card.colorIdentity().contains(Color.BLUE)) {
                    return false;
                  }
                  continue;
                case 'b':
                  if (!card.colorIdentity().contains(Color.BLACK)) {
                    return false;
                  }
                  continue;
                case 'r':
                  if (!card.colorIdentity().contains(Color.RED)) {
                    return false;
                  }
                  continue;
                case 'g':
                  if (!card.colorIdentity().contains(Color.GREEN)) {
                    return false;
                  }
                  continue;
              }
            }
            return true;
          }
        };
      }
    });
    builder.put("c", ":", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(final String value) {
        return new Predicate<Card>() {
          @Override
          public boolean apply(Card card) {
            for (int i = 0; i < value.length(); ++i) {
              switch(value.charAt(i)) {
                case 'w':
                  if (!card.colors().contains(Color.WHITE)) {
                    return false;
                  }
                  continue;
                case 'u':
                  if (!card.colors().contains(Color.BLUE)) {
                    return false;
                  }
                  continue;
                case 'b':
                  if (!card.colors().contains(Color.BLACK)) {
                    return false;
                  }
                  continue;
                case 'r':
                  if (!card.colors().contains(Color.RED)) {
                    return false;
                  }
                  continue;
                case 'g':
                  if (!card.colors().contains(Color.GREEN)) {
                    return false;
                  }
                  continue;
              }
            }
            return true;
          }
        };
      }
    });
    builder.put("!ci", ":", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(final String value) {
        return new Predicate<Card>() {
          @Override
          public boolean apply(Card card) {
            for (int i = 0; i < value.length(); ++i) {
              switch(value.charAt(i)) {
                case 'w':
                  if (card.colorIdentity().contains(Color.WHITE)) {
                    return false;
                  }
                  continue;
                case 'u':
                  if (card.colorIdentity().contains(Color.BLUE)) {
                    return false;
                  }
                  continue;
                case 'b':
                  if (card.colorIdentity().contains(Color.BLACK)) {
                    return false;
                  }
                  continue;
                case 'r':
                  if (card.colorIdentity().contains(Color.RED)) {
                    return false;
                  }
                  continue;
                case 'g':
                  if (card.colorIdentity().contains(Color.GREEN)) {
                    return false;
                  }
                  continue;
              }
            }
            return true;
          }
        };
      }
    });
    builder.put("!c", ":", new Function<String, Predicate<Card>>() {
      @Override
      public Predicate<Card> apply(final String value) {
        return new Predicate<Card>() {
          @Override
          public boolean apply(Card card) {
            for (int i = 0; i < value.length(); ++i) {
              switch(value.charAt(i)) {
                case 'w':
                  if (card.colors().contains(Color.WHITE)) {
                    return false;
                  }
                  continue;
                case 'u':
                  if (card.colors().contains(Color.BLUE)) {
                    return false;
                  }
                  continue;
                case 'b':
                  if (card.colors().contains(Color.BLACK)) {
                    return false;
                  }
                  continue;
                case 'r':
                  if (card.colors().contains(Color.RED)) {
                    return false;
                  }
                  continue;
                case 'g':
                  if (card.colors().contains(Color.GREEN)) {
                    return false;
                  }
                  continue;
              }
            }
            return true;
          }
        };
      }
    });
    return builder.build();
  }

  private static void addEntries(final String property,
      ImmutableTable.Builder<String, String, Function<String, Predicate<Card>>> table) {
    for (String op : INT_OPS) {
      switch (op) {
        case "=":
          table.put(
              property,
              op,
              new Function<String, Predicate<Card>>() {
                @Override
                public Predicate<Card> apply(final String value) {
                  return new Predicate<Card>() {
                    private final Integer comparison = parseIntSafe(value);

                    @Override
                    public boolean apply(Card card) {
                      if (comparison == null) {
                        return false;
                      }
                      Integer intValue = PROPERTY_FUNCS.get(property).apply(card);
                      return intValue != null && intValue == comparison;
                    }
                  };
                }
              });
          continue;
        case "<":
          table.put(
              property,
              op,
              new Function<String, Predicate<Card>>() {
                @Override
                public Predicate<Card> apply(String value) {
                  return new Predicate<Card>() {
                    private final Integer comparison = parseIntSafe(value);

                    @Override
                    public boolean apply(Card card) {
                      if (comparison == null) {
                        return false;
                      }
                      Integer intValue = PROPERTY_FUNCS.get(property).apply(card);
                      return intValue != null && intValue < comparison;
                    }
                  };
                }
              });
          continue;
        case "<=":
          table.put(
              property,
              op,
              new Function<String, Predicate<Card>>() {
                @Override
                public Predicate<Card> apply(String value) {
                  return new Predicate<Card>() {
                    private final Integer comparison = parseIntSafe(value);
                    @Override

                    public boolean apply(Card card) {
                      if (comparison == null) {
                        return false;
                      }
                      Integer intValue = PROPERTY_FUNCS.get(property).apply(card);
                      return intValue != null && intValue <= comparison;
                    }
                  };
                }
              });
          continue;
        case ">":
          table.put(
              property,
              op,
              new Function<String, Predicate<Card>>() {
                @Override
                public Predicate<Card> apply(String value) {
                  return new Predicate<Card>() {
                    private final Integer comparison = parseIntSafe(value);

                    @Override
                    public boolean apply(Card card) {
                      if (comparison == null) {
                        return false;
                      }
                      Integer intValue = PROPERTY_FUNCS.get(property).apply(card);
                      return intValue != null && intValue > comparison;
                    }
                  };
                }
              });
          continue;
        case ">=":
          table.put(
              property,
              op,
              new Function<String, Predicate<Card>>() {
                @Override
                public Predicate<Card> apply(String value) {
                  return new Predicate<Card>() {
                    private final Integer comparison = parseIntSafe(value);

                    @Override
                    public boolean apply(Card card) {
                      if (comparison == null) {
                        return false;
                      }
                      Integer intValue = PROPERTY_FUNCS.get(property).apply(card);
                      return intValue != null && intValue >= comparison;
                    }
                  };
                }
              });
          continue;
        case "!=":
          table.put(
              property,
              op,
              new Function<String, Predicate<Card>>() {
                @Override
                public Predicate<Card> apply(String value) {
                  return new Predicate<Card>() {
                    private final Integer comparison = parseIntSafe(value);

                    @Override
                    public boolean apply(Card card) {
                      if (comparison == null) {
                        return false;
                      }
                      Integer intValue = PROPERTY_FUNCS.get(property).apply(card);
                      return intValue != null && intValue != comparison;
                    }
                  };
                }
              });
          continue;
      }
    }
  }

  private static Integer parseIntSafe(String input) {
    try {
      return Integer.parseInt(input);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Predicate<Card> getTextPredicate(final String regexp) {
    return new Predicate<Card>() {
      private final Pattern p = Pattern.compile(".*" + regexp + ".*");
      @Override
      public boolean apply(Card card) {
        return p.matcher(card.oracleText().toLowerCase().replaceAll("\n", "")).matches();
      }
    };
  }

  private static Predicate<Card> getTypePredicate(final String type) {
    return new Predicate<Card>() {
      @Override
      public boolean apply(Card card) {
        return card.types().contains(type)
            || card.supertypes().contains(type)
            || card.subtypes().contains(type);
      }
    };
  }

  private static Predicate<Card> getIsPredicate(String property) {
    switch (property) {
      case "dfc":
        return new Predicate<Card>() {
          @Override
          public boolean apply(Card card) {
            return card.layout() == Layout.DOUBLE_FACED;
          }
        };
      case "split":
        return new Predicate<Card>() {
          @Override
          public boolean apply(Card card) {
            return card.layout() == Layout.SPLIT;
          }
        };
      case "flip":
        return new Predicate<Card>() {
          @Override
          public boolean apply(Card card) {
            return card.layout() == Layout.FLIP;
          }
        };
      case "reserved":
        return new Predicate<Card>() {
          @Override
          public boolean apply(Card card) {
            return card.reserved();
          }
        };
      case "colorless":
        return new Predicate<Card>() {
          @Override
          public boolean apply(Card card) {
            return card.colors().isEmpty();
          }
        };
      default:
        return Predicates.alwaysFalse();
    }
  }
}
