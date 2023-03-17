package org.gfbio.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.gfbio.util.AppProperties;

public class DBConnection {

  private static final Logger LOGGER = Logger.getLogger(DBConnection.class);

  private static AppProperties properties = AppProperties.getInstance();

  public static ResultSet execQuery(String query) {

    PreparedStatement stmt;
    ResultSet set = null;

    try {

      // FIXME
      Class.forName("com.mysql.cj.jdbc.Driver");

      Connection conn = DriverManager.getConnection(properties.getJdbcUrl(),
          properties.getUsername(), properties.getPassword());
      stmt = conn.prepareStatement(query);

      LOGGER.info("executing query " + query);

      set = stmt.executeQuery();

    } catch (SQLException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    return set;
  }
}
