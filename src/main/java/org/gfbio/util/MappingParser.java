package org.gfbio.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MappingParser {

  private static final Logger LOGGER = Logger.getLogger(MappingParser.class);

  /**
   * This parses and returns only the keys of the JsonElements inside the array.
   * 
   * @return
   */
  public static List<String> getKeys(String json, String jsonArrayName) {

    List<String> keys = new ArrayList<String>();

    Iterator<Entry<String, JsonElement>> iter = parse(json, jsonArrayName);

    while (iter.hasNext()) {
      keys.add(iter.next().getKey());
    }

    return keys;

  }

  /**
   * This parses and returns keys and values of the JsonElements inside the array.
   * 
   * @return
   */
  public static Map<String, String> getKeysValues(String json, String jsonArrayName) {

    Map<String, String> keysValues = new HashMap<String, String>();

    Iterator<Entry<String, JsonElement>> iter = parse(json, jsonArrayName);

    while (iter.hasNext()) {

      Entry<String, JsonElement> entry = iter.next();

      keysValues.put(entry.getKey(), entry.getValue().getAsString());
    }

    return keysValues;
  }

  /**
   * Parse a valid mappingConfig.json to retrieve codified information about tables and their
   * respective columns.
   * 
   * @param json
   * @param jsonArrayName
   */
  private static Iterator<Entry<String, JsonElement>> parse(String json, String jsonArrayName) {

    LOGGER.debug("parsing mapping for array " + jsonArrayName);

    Iterator<Entry<String, JsonElement>> iter = null;

    JsonParser parser = new JsonParser();

    JsonElement jsonTree = parser.parse(json);

    // traverse tree
    //
    // We look for, e.g., "kingdoms": [ { "kingdom_id": "int", "kingdom_name": "kingdoms" } ],
    // which is an object, which itself contains an array which holds column names

    if (jsonTree.isJsonObject()) {

      JsonObject jsonObject = jsonTree.getAsJsonObject();
      JsonArray tables = jsonObject.getAsJsonArray(jsonArrayName);

      // for (JsonElement e : tables) {
      JsonObject obj_ = tables.get(0).getAsJsonObject(); // JSON contains only one JsonElement

      LOGGER.debug(obj_);

      iter = obj_.entrySet().iterator();
      // while (iter.hasNext()) {
      // System.out.println(iter.next().getKey());
      // }

      // }

    }

    return iter;

  }
}
