package lu.zhe.mtgslackbot.set;

import com.google.common.collect.ImmutableMap;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Utilities for reading data from sets.
 */
public class SetUtils {
  private SetUtils() {
    // Disallow instantiation
  }

  public static Map<String, String> parseSets(InputStream is) throws IOException {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
    reader.beginArray();
    while (reader.hasNext()) {
      readSet(reader, builder);
    }
    reader.endArray();
    return builder.build();
  }

  private static void readSet(JsonReader reader, ImmutableMap.Builder<String, String> builder)
      throws IOException {
    String code = "";
    String name = "";
    String releaseDate = "";
    reader.beginObject();
    while (reader.hasNext()) {
      switch (reader.nextName()) {
        case "name":
          name = reader.nextString();
          break;
        case "code":
          code = reader.nextString();
          break;
        case "releaseDate":
          releaseDate = reader.nextString();
          break;
      }
    }
    if (code.length() == 3 && !name.isEmpty() && !releaseDate.isEmpty()) {
      builder.put(code.toLowerCase(), name + " " + releaseDate);
    }
    reader.endObject();
  }
}
