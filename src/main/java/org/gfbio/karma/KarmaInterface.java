package org.gfbio.karma;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.gfbio.util.AppProperties;

public class KarmaInterface implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(KarmaInterface.class.getName());

  // DO NOT CHANGE
  private static final String KARMA_SHADED_JAR = "karma-offline-batch.jar";

  // DO NOT CHANGE
  private static final String KARMA_RDF_GENERATOR = "edu.isi.karma.rdf.OfflineRdfGenerator";

  private static AppProperties properties = AppProperties.getInstance();

  private List<String> jsonFileNames;

  private String acronym, table, type;

  public KarmaInterface() {}

  /**
   * Maps (semi-)structured data to semantic triples using Karma
   * 
   * @param acronym Terminology acronym (e.g. ITIS)
   * @param table Terminology table to map (e.g. vernaculars)
   * @param type Type of files which contain data to map (e.g. .json)
   */
  public KarmaInterface(String acronym, String table, String type) {
    this.acronym = acronym;
    this.table = table;
    this.type = type;
  }

  @Deprecated
  public String createTTLForCSV(String onto, String graph, String csv)
      throws IOException, InterruptedException {

    if (checkForFile(graph, properties.getWorkDir())) {
      LOGGER.info("File already exists");
      return properties.getWorkDir() + "out/" + graph + "-triples-out.ttl";
    }
    if (!checkForModel(onto, graph, properties.getModelDir())) {
      throw new FileNotFoundException("Model for graph " + graph + " could not be found!");
    }
    LOGGER.info("Creating TTL for graph " + graph);

    Process p = triplesFromCSV(onto, graph, csv);
    LOGGER.info("final Karma command: " + p.info());

    int rc = p.waitFor();
    LOGGER.info("Execution completed; rc=" + rc);

    if (rc == 0) {
      return properties.getWorkDir() + "out/" + graph + "-triples-out.ttl";
    }

    return null;
  }

  /**
   * 
   * @param onto
   * @param tableName
   * @param type
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  private void createTTLForTable() throws IOException, InterruptedException {

    properties = AppProperties.getInstance();

    // createWorkFolder(properties);

    // if (checkForFile(tableName, properties.getWorkDir())) {
    // LOGGER.info("File already exists");
    // return properties.getWorkDir() + "/out/" + tableName + "-triples-out.ttl";
    // }
    // if (!checkForModel(onto, tableName, properties.getModelDir())) {
    // throw new FileNotFoundException("Model for table " + tableName + " could not be found!");
    // }

    List<String> ttlFilenames = new ArrayList<String>();

    try {

      if (jsonFileNames == null) {

        jsonFileNames = new ArrayList<String>();

        DirectoryStream<Path> directoryStream =
            Files.newDirectoryStream(Paths.get(properties.getWorkDir() + this.acronym));

        for (Path path : directoryStream) {
          // path : absolute path to file
          jsonFileNames.add(path.toString());
        }

        directoryStream.close();

      }

      LOGGER.info("Creating TTL for table " + this.table);

      short filePart = 1;

      Predicate<String> containsTablename = s -> s.contains(this.table);

      var result = jsonFileNames.stream().filter(containsTablename).collect(Collectors.toList());

      for (String file : result) {

        LOGGER.info("reading in file " + file);

        Process p = null;

        StringBuilder ttlOut = new StringBuilder();

        switch (type.toLowerCase()) {

          case ("db"):

            p = triplesFromDB(this.acronym, this.table);
            // LOGGER.info("final Karma command: " + p.info());

            break;

          case ("json"):

            if (file.contains(this.table)) {

              if (result.size() > 1) {
                p = triplesFromJSON(this.table, this.acronym, file, String.valueOf(filePart));

                ttlOut.append(properties.getWorkDir()).append(this.acronym).append(File.separator)
                    .append("out/").append(this.table).append("-").append(filePart)
                    .append("-triples-out.ttl");

              } else {
                p = triplesFromJSON(this.table, this.acronym, file, null);

                ttlOut.append(properties.getWorkDir()).append(this.acronym).append(File.separator)
                    .append("out/").append(this.table).append("-triples-out.ttl");
              }


            } else
              continue;

            break;
        }

        int rc = p.waitFor();
        LOGGER.info("Execution completed; rc=" + rc + "\n");

        ttlFilenames.add(ttlOut.toString());

        p.destroy();

        filePart++;
      }

    } catch (IOException ex) {
      ex.printStackTrace();
    }

    if (ttlFilenames.size() > 1) {

      String rdfFileName = properties.getWorkDir() + this.acronym + File.separator + "out/"
          + this.table + "-triples-out.ttl";

      LOGGER.info("writing merged .ttl to " + rdfFileName);

      Model merge = ModelFactory.createDefaultModel();

      for (String ttlFile : ttlFilenames) {
        merge.add(RDFDataMgr.loadModel(ttlFile));
      }

      merge.write(new FileOutputStream(rdfFileName), "N-TRIPLE");

      merge.close();

    }

    // if (rc == 0) {
    // return properties.getWorkDir() + onto.toUpperCase() + "/out/" + tableName
    // + "-triples-out.ttl";
    // }

  }

  /**
   * 
   * @param tableName
   * @param onto
   * @param fromJSON
   * @return
   * @throws IOException
   */
  private Process triplesFromJSON(String tableName, String onto, String fromJSON, String dataPart)
      throws IOException {
    List<String> command = new ArrayList<String>();

    command.add("java");
    // Specifies a list of directories, JAR files, and ZIP archives to search for class files
    command.add("-cp");
    command.add(properties.getModelDir() + KARMA_SHADED_JAR);
    command.add(KARMA_RDF_GENERATOR);
    command.add("--sourcetype");
    command.add("JSON");
    command.add("--filepath");
    command.add(fromJSON);
    command.add("--sourcename");
    command.add(tableName);
    command.add("--modelfilepath");
    command.add(properties.getModelDir() + onto + File.separator + tableName + "-model.ttl");
    command.add("--outputfile");
    if (dataPart != null) {
      command.add(properties.getWorkDir() + onto + File.separator + "out/" + tableName + "-"
          + dataPart + "-triples-out.ttl");
    } else {
      command.add(properties.getWorkDir() + onto + File.separator + "out/" + tableName
          + "-triples-out.ttl");
    }

    // add DB cli params
    command.addAll(properties.makeCLIParams());

    LOGGER.info("final command=" + command);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.inheritIO();

    return pb.start();
  }

  /**
   * 
   * @param onto
   * @param graph
   * @param csv
   * @return
   * @throws IOException
   */
  private Process triplesFromCSV(String onto, String graph, String csv) throws IOException {
    AppProperties properties = AppProperties.getInstance();
    List<String> command = new ArrayList<String>();

    command.add("java");
    // Specifies a list of directories, JAR files, and ZIP archives to search for class files
    command.add("-cp");
    command.add(properties.getKarmaHome() + KARMA_SHADED_JAR);
    command.add(KARMA_RDF_GENERATOR);
    command.add("--modelfilepath");
    command.add(properties.getModelDir() + onto + "/" + graph + "-model.ttl");
    command.add("--outputfile");
    command.add(properties.getWorkDir() + onto + "/out/" + graph + "-triples-out.ttl");
    command.add("--sourcetype");
    command.add("CSV");
    command.add("--headerindex");
    command.add("1");
    command.add("--dataindex");
    command.add("2");
    command.add("--delimiter");
    command.add(",");
    command.add("--filepath");
    command.add(csv);
    command.add("--sourcename");
    command.add("geonames");

    // add DB cli params
    // command.addAll(properties.makeCLIParams());

    LOGGER.info("executing command " + command);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.inheritIO();

    return pb.start();
  }

  /**
   * 
   * @param onto
   * @param tableName
   * @return
   * @throws IOException
   */
  @Deprecated
  private Process triplesFromDB(String onto, String tableName) throws IOException {

    AppProperties properties = AppProperties.getInstance();
    List<String> command = new ArrayList<String>();

    command.add("java");
    // Specifies a list of directories, JAR files, and ZIP archives to search for class files
    command.add("-cp");
    command.add(properties.getKarmaHome() + "/" + KARMA_SHADED_JAR);
    command.add(KARMA_RDF_GENERATOR);
    command.add("--modelfilepath");
    command.add(properties.getModelDir() + "/models/" + onto + "/" + tableName + "-model.ttl");
    command.add("--outputfile");
    command.add(properties.getWorkDir() + "/out/" + tableName + "-triples-out.ttl");
    command.add("--tablename");
    command.add(tableName);

    // add DB cli params
    command.addAll(properties.makeCLIParams());

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.inheritIO();

    return pb.start();
  }

  private boolean checkForModel(String onto, String tableName, String modelDir) {

    Path file = Paths.get(modelDir + onto.toUpperCase() + "/" + tableName + "-model.ttl");

    return file.toFile().exists();
  }

  private boolean checkForFile(String tableName, String workDir) throws IOException {
    Path file = Paths.get(workDir + "/out/" + tableName + "-triples-out.ttl");
    System.out.println("checking if file " + file.getFileName() + " already exists. \n");

    if (file.toFile().exists()) {

      BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);

      LOGGER.info("creationTime: " + attr.creationTime());
      LOGGER.info("lastAccessTime: " + attr.lastAccessTime());
      LOGGER.info("lastModifiedTime: " + attr.lastModifiedTime());
      LOGGER.info("\n");

      return true;
    } else {
      return false;
    }
  }

  @Override
  public void run() {
    try {
      createTTLForTable();
    } catch (IOException | InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
