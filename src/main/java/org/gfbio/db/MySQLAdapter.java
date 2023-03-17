package org.gfbio.db;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.gfbio.util.AppProperties;
import org.gfbio.util.MappingParser;
import org.json.JSONArray;
import org.json.JSONObject;

public class MySQLAdapter {

  private static final Logger LOGGER = Logger.getLogger(MySQLAdapter.class);

  private AppProperties props = AppProperties.getInstance();

  public ResultSet select(String fromTable, String forAcronym, String offset) {

    try {

      Map<String, String> kvMap = MappingParser.getKeysValues(
          new String(Files.readAllBytes(
              Paths.get(props.getModelDir() + forAcronym + File.separator + "mappingConfig.json"))),
          "queries");

      StringBuilder query = new StringBuilder().append(kvMap.get(fromTable))
          .append(" FROM " + fromTable).append(" LIMIT ").append(props.getQueryLimit());

      if (offset != null)
        query.append(" OFFSET ").append(offset);

      ResultSet set = DBConnection.execQuery(query.toString());

      return set;

    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public ResultSet select(String fromTable, String forAcronym) {
    return select(fromTable, forAcronym, null);
  }

  public void exportToJSON(ResultSet rs, String fromTable, String toFile, String forAcronym,
      boolean append) {

    LOGGER
        .info("exporting database records as JSON from table=" + fromTable + " to file=" + toFile);

    // JSONObject parent = new JSONObject();
    JSONArray array = new JSONArray();

    // exports to .json
    FileWriter file = null;

    Entry<String, String> e = null;

    try {

      file = new FileWriter(toFile, append);

      // load respective column names for database table
      Map<String, String> columns = MappingParser.getKeysValues(
          new String(Files.readAllBytes(
              Paths.get(props.getModelDir() + forAcronym + File.separator + "mappingConfig.json"))),
          fromTable);

      LOGGER.info("loaded columns mapping=" + columns);

      while (rs.next()) {
        JSONObject record = new JSONObject();

        Iterator<Entry<String, String>> kv = columns.entrySet().iterator();
        while (kv.hasNext()) {
          e = kv.next();

          switch (e.getValue()) {
            case "String":
              record.put(e.getKey(), rs.getString(e.getKey()));
              break;

            case "int":
              record.put(e.getKey(), rs.getInt(e.getKey()));
              break;

            case "float":
              record.put(e.getKey(), rs.getFloat(e.getKey()));
              break;

            default:
              break;
          }
        }
        array.put(record);
      }

      // export JSON to file
      file.write(array.toString());
      file.close();

    } catch (SQLException | IOException ex) {
      ex.printStackTrace();
    } catch (NumberFormatException ne) {
      LOGGER.error("NumberFormatException");
      LOGGER.error(e.getKey());
      LOGGER.error(e.getValue());

      // just debugging stuff
      try {
        rs.moveToCurrentRow();
        LOGGER.error(rs.getRow());
        LOGGER.error(rs.getObject(e.getKey()));
        LOGGER.error(rs.getObject("geonameid"));
      } catch (SQLException e1) {

        e1.printStackTrace();
      }


      throw new RuntimeException();
    }
  }

  public int countRows(String fromTable) {

    String query = new StringBuilder().append("SELECT COUNT(*)").append(" AS total ")
        .append(" FROM " + fromTable).toString();
    ResultSet set = DBConnection.execQuery(query);

    int total = 0;

    try {

      while (set.next()) {
        total = set.getInt("total");
        LOGGER.info("number of rows=" + total);
      }

      set.close();

    } catch (SQLException e) {
      e.printStackTrace();
    }

    return total;
  }

}
