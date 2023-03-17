/**
 *
 */
package org.gfbio.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import com.google.gson.Gson;

/**
 * Access database properties saved in a file and make them accessible via
 * getter
 *
 */
public class AppProperties {

  private String username, password, dbtype, host, port, dbname, karmaHome, workDir, modelDir,
      jdbcUrl, queryLimit, pythonInterpreter;

  private String geonamesHeaders, geonamesHeadersLite;

  // FIXME set properties accordingly
  private static final String filename = "db.properties";

  private static AppProperties instance;

  private AppProperties() {
    readProperties();
  }

  public static AppProperties getInstance() {
    if (AppProperties.instance == null) {
      AppProperties.instance = new AppProperties();
    }
    return AppProperties.instance;
  }

  public List<String> makeCLIParams() {

    List<String> params = new ArrayList<String>();
    params.add("--hostname");
    params.add(getHost());
    params.add("--username");
    params.add(getUsername());
    params.add("--dbname");
    params.add(getDbname());
    params.add("--password");
    params.add(getPassword());
    params.add("--dbtype");
    params.add(getDbtype());
    params.add("--sourcetype");
    params.add("DB");
    params.add("--portnumber");
    params.add(getPort());

    System.out.println("Karma Parameters: ");
    System.out.println(params.toString());

    return params;

  }

  public MappingBasic readMappingConfig(String acronym) {
    Gson gson = new Gson();
    MappingBasic mappingConfig = new MappingBasic();

    try {
      Reader reader = new InputStreamReader(new FileInputStream(
          new File(this.getModelDir() + acronym + File.separator + "mappingConfig.json")));
      mappingConfig = gson.fromJson(reader, MappingBasic.class);
      reader.close();
    } catch (IOException e) {
      System.err.println(e);
      // LOGGER.error(e.getMessage());
    }
    return mappingConfig;
  }

  public void readProperties() {

    // try ... with automatically closes resources after use
    // searches for the file in the classpath, i.e., /src/main/resources
    try (InputStream input = AppProperties.class.getClassLoader().getResourceAsStream(filename)) {

      Properties prop = new Properties();

      // load a properties file
      prop.load(input);

      // get the property value and print it out
      username = prop.getProperty("username");
      password = prop.getProperty("password");
      dbtype = prop.getProperty("dbtype");
      host = prop.getProperty("hostname");
      port = prop.getProperty("portnumber");
      dbname = prop.getProperty("dbname");

      // directory containing Karma models
      modelDir = prop.getProperty("modelDir");
      // where to put output files
      workDir = prop.getProperty("workDir");
      // location of karma-offline-0.0.1-SNAPSHOT-shaded.jar
      karmaHome = prop.getProperty("karmaHome");

      pythonInterpreter = prop.getProperty("python.interpreter");

      jdbcUrl = "jdbc:" + dbtype.toLowerCase() + "://" + host + ":" + port + "/" + dbname;

      queryLimit = prop.getProperty("query.limit");
      // setGeonamesHeaders(prop.getProperty("columns").split("\\|"));
      setGeonamesHeaders(prop.getProperty("columns.full"));
      geonamesHeadersLite = prop.getProperty("columns.lite");

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getDbtype() {
    return dbtype;
  }

  public String getHost() {
    return host;
  }

  public String getPort() {
    return port;
  }

  public String getDbname() {
    return dbname;
  }

  public String getKarmaHome() {
    return karmaHome;
  }

  public String getWorkDir() {
    return workDir;
  }

  public String getModelDir() {
    return modelDir;
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public String getQueryLimit() {
    return queryLimit;
  }

  public String getGeonamesHeaders() {
    return geonamesHeaders;
  }

  public void setGeonamesHeaders(String geonamesHeaders) {
    this.geonamesHeaders = geonamesHeaders;
  }

  public String getGeonamesHeadersLite() {
    return geonamesHeadersLite;
  }

  public void setGeonamesHeadersLite(String geonamesHeadersLite) {
    this.geonamesHeadersLite = geonamesHeadersLite;
  }

  public String getPythonInterpreter() {
    return pythonInterpreter;
  }

  public void setPythonInterpreter(String pythonInterpreter) {
    this.pythonInterpreter = pythonInterpreter;
  }
}
