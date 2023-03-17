package org.gfbio.rdb2rdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;
import org.gfbio.db.MySQLAdapter;
import org.gfbio.karma.KarmaInterface;
import org.gfbio.util.AppProperties;
import org.gfbio.util.MappingBasic;
import org.gfbio.util.MappingResource;
import com.mockrunner.mock.jdbc.MockResultSet;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class GEONAMESMapper implements Mapper {

  private static final String ONTO = "GEONAMES";

  private static final Logger LOGGER = Logger.getLogger(GEONAMESMapper.class);

  private Path csv, txt, path;

  private String countryCode;

  private AppProperties props = AppProperties.getInstance();

  private List<Model> editedGraphs = new ArrayList<Model>();

  public GEONAMESMapper(String countryCode) {
    this.countryCode = countryCode;

    // initialise variables pointing to our files
    path = Paths.get(props.getWorkDir() + ONTO);
    txt = path.resolve(this.countryCode + ".txt");
    csv = path.resolve(this.countryCode + ".csv");
  }

  @Override
  public void run() {

  }


  /**
   * Converts XX.txt file to a .csv
   * <p>
   * https://stackoverflow.com/questions/22526679/parse-txt-to-csv
   */
  @Override
  public short preprocess() {

    short rc = 0;

    LOGGER.info("start preprocessing");

    try {

      // creates new XX.csv file, opens it and writes the header as first row, then closes the
      // handle
      String headers = props.getGeonamesHeaders();
      FileWriter writer = new FileWriter(this.csv.toString());
      writer.append(headers);
      writer.append("\n");
      writer.close();


      // this opens the XX.txt file, reads it in line for line and writes them into the CSV as a new
      // row, using '|' as a column separator
      try (final Stream<String> lines = Files.lines(this.txt);

          final PrintWriter pw =
              new PrintWriter(Files.newBufferedWriter(this.csv, StandardOpenOption.APPEND))) {
        lines.map((line) -> line.split("\\t"))
            .map((line) -> Stream.of(line).collect(Collectors.joining("|"))).forEach(pw::println);

        lines.close();
      }
      LOGGER.info("converted raw " + this.countryCode + ".txt to " + this.countryCode + ".csv");
      rc = buildGEONAMESHierarchy(this.csv.toString());

      if (rc == 0)
        this.csv = path.resolve(countryCode + "_MOD.csv");
      else
        throw new RuntimeException(
            "Please try another python interpreter. If the problem persist, try to run the preprocessing script manually, e.g., via python shell, and skip the preprocessing step");

    } catch (Exception e) {
      e.printStackTrace();
    }

    return rc;

  }

  @Override
  public void createTriples() {

    MappingBasic mappingConfig = props.readMappingConfig(ONTO);

    ExecutorService execSvc = Executors.newCachedThreadPool();

    for (MappingResource resource : mappingConfig.getResources()) {

      LOGGER.info("Perform JSON to RDF mapping for the artifact " + resource.getModelName() + ".");

      if (resource.isExportAsTTL() == true) {
        KarmaInterface karma = new KarmaInterface(ONTO, resource.getTable(), "JSON");
        execSvc.execute(karma);
      }
    }

    execSvc.shutdown();

    while (!execSvc.isTerminated()) {
      // empty body
    }
  }

  @Override
  public void exportTablesToJSON() {

    // to use a unified API we'll export GEONAMES CSV to multiple JSON
    // those JSON files will be converted by Karma afterwards

    MySQLAdapter mysqlIf = new MySQLAdapter();

    // if we did not preprocess with Java code set the correct CSV path here
    this.csv = path.resolve(countryCode + "_MOD.csv");
    if (!Files.exists(csv)) {
      throw new RuntimeException("CSV " + this.csv.toString()
          + " could not be found! You must execute preprocess() first!");
    }

    MappingBasic mappingConfig = props.readMappingConfig(ONTO);
    for (MappingResource resource : mappingConfig.getResources()) {

      try {

        // file counter
        short i = 1;
        long linesRead = 0;

        // reads in a CSV file and tries to deduce Beans from headers represent in the CSV file
        BufferedReader reader_ = new BufferedReader(new FileReader(this.csv.toString()));
        CSVReader reader = new CSVReaderBuilder(reader_).withSkipLines(1)
            .withCSVParser(new CSVParserBuilder().withSeparator('|').build()).build();


        // JSON filename to write to
        StringBuilder jsonFile =
            new StringBuilder().append(props.getWorkDir()).append(ONTO).append(File.separator)
                .append(resource.getModelName()).append("-part-" + i).append(".json");

        String[] nextRecord;
        MockResultSet set = createNewMockSet();

        LOGGER.info("reading rows from " + this.csv.toString());

        while ((nextRecord = reader.readNext()) != null) {

          // since values can be missing, check if they're empty and replace respectively
          Arrays.asList(nextRecord)
              .replaceAll(e -> e.isEmpty() == false ? e : String.valueOf(java.sql.Types.NULL));

          set.addRow(nextRecord);
          linesRead = reader.getLinesRead();
          if (Math.floorMod(linesRead, 5000) == 0) {
            LOGGER.info("writing batch #" + i + " of records now");
            i++;
            // write batch of JSON
            mysqlIf.exportToJSON(set, resource.getModelName(), jsonFile.toString(), ONTO, true);

            // update file reference name
            jsonFile =
                new StringBuilder().append(props.getWorkDir()).append(ONTO).append(File.separator)
                    .append(resource.getModelName()).append("-part-" + i).append(".json");

            // clean up a bit
            set.close();
            set = null;
            set = createNewMockSet();
            LOGGER.info(set.getRowCount()); // should be 0
          }

          if (Math.floorMod(linesRead, 1000) == 0)
            LOGGER.info("read " + linesRead + " records");
        }

        // export remaining
        mysqlIf.exportToJSON(set, resource.getModelName(), jsonFile.toString(), ONTO, true);

        // CsvToBean converter = new CsvToBean<T>();
        // converter.setCsvReader(reader);
        // converter.setMappingStrategy();
        // converter.parse();

        if (set != null)
          set.close();
        if (reader_ != null)
          reader_.close();

        reader.close();


      } catch (Exception e) {
        LOGGER.error("CSV could not be parsed due to " + e.getLocalizedMessage());
        e.printStackTrace();
        throw new RuntimeException();
      }

    }
    LOGGER.info("CSV to JSON export done");

  }

  @Override
  public boolean createOutDir() {

    String outDir = props.getWorkDir() + ONTO + "/out";

    LOGGER.info("checking if directory " + outDir + " exists");

    try {
      if (Files.exists(Paths.get(outDir)) == false) {
        Files.createDirectory(Paths.get(outDir));
      }
    } catch (IOException e) {
      e.printStackTrace();

      return false;
    }
    LOGGER.info(outDir + " created/existing");


    return true;
  }

  @Override
  public void generateRDFModel() {
    try {

      LOGGER.info("generating RDF model from sub models now");
      LOGGER.info("collecting data ...");

      // for (String table : tableToClass.keySet()) {


      MappingBasic mappingConfig = props.readMappingConfig(ONTO);

      for (MappingResource resource : mappingConfig.getResources()) {

        if (resource.isExportAsTTL() == true) {

          String table = resource.getTable();

          String tableTTLFile = props.getWorkDir() + ONTO + File.separator + "out" + File.separator
              + table + "-triples-out.ttl";

          LOGGER.info("loading model from file " + tableTTLFile + " \n");
          Model tableTTL = RDFDataMgr.loadModel(tableTTLFile);

          // add the label to the uri of these concepts, e.g.,
          // http://terminologies.gfbio.org/ITIS/Kingdom_2 ->
          // http://terminologies.gfbio.org/ITIS/Kingdom_Protozoa
          // String[] concepts = new String[] {"kingdoms", "taxon_unit_types", "strippedauthor"};
          //
          // if (Arrays.asList(concepts).contains(table)) {
          // addStatementsWithLabel(table, tableTTL);
          // }

          // adds, e.g., http://terminologies.gfbio.org/ITIS/Kingdom
          // http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.w3.org/2002/07/owl#Class to
          // the
          // respective graph
          // addTypeOwlClassStatement(table, tableTTL);
          // removeTypeClassStatement(table, tableTTL);


          LOGGER.info("Writing final Statements to new file from graph");

          tableTTL.write(new FileOutputStream(props.getWorkDir() + ONTO + File.separator + "out"
              + File.separator + table + "_edit.ttl"), "N-TRIPLES");


          editedGraphs.add(RDFDataMgr.loadModel(props.getWorkDir() + ONTO + File.separator + "out"
              + File.separator + table + "_edit.ttl"));
        }

      }

      // exportFinalRDFModel();

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public void exportFinalRDFModel() {

    String rdfFileName = props.getWorkDir() + ONTO + File.separator + ONTO + ".owl";

    LOGGER.info("exporting final RDF file to " + rdfFileName);
    try {

      Model baseModel = ModelFactory.createOntologyModel();

      for (Model m : editedGraphs) {
        baseModel.add(m);
      }
      baseModel.setNsPrefix("omv", "http://omv.ontoware.org/2005/05/ontology#");
      baseModel.setNsPrefix("gfbio", "http://terminologies.gfbio.org/terms/");
      baseModel.setNsPrefix("ts-schema", "http://terminologies.gfbio.org/terms/ts-schema/");
      baseModel.setNsPrefix("wgs84_pos", "http://www.w3.org/2003/01/geo/wgs84_pos#");
      baseModel.setNsPrefix("gn", "http://www.geonames.org/ontology#");
      baseModel.setNsPrefix("dcterms", "http://purl.org/dc/terms/");

      // removeTypeClassStatement("taxonomic_units", baseModel);

      baseModel.write(new FileOutputStream(rdfFileName), "RDF/XML");

      baseModel.close();

      // close models after final export
      editedGraphs.forEach(m -> m.close());

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException();
    }

  }

  private MockResultSet createNewMockSet() throws Exception {

    // we kind of fake a java.sql.ResultSet here
    MockResultSet set = new MockResultSet("GEONAMESResultSet");
    LinkedHashSet<String> columns = new LinkedHashSet<String>();

    // add columns in order of how they were read in
    columns.addAll(Arrays.asList(props.getGeonamesHeadersLite().split("\\|")));
    set.addColumns(columns);

    return set;
  }

  /**
   * Open the XX.CSV file which was created beforehand. It should contain the contains of a raw
   * XX.txt file converted into a proper CSV sheet format.
   * 
   * Its purpose is the following: every entry in GEONAMES belongs to a certain hierarchy (ADMs),
   * e.g., village, district, federal state, state (from bottom to top). These hierarchy data is not
   * encoded in the raw data, but must be inferred from so-called admin_code(s). Using these
   * admin_code properties, one can infer parent(s) and chil(dren) of an entry and set their
   * respective geonameid accordingly. This must be done for every entry in the CSV! (this could
   * take up to 2h, depending on the system)
   * 
   * @param csvFilePath
   * @return
   */
  private short buildGEONAMESHierarchy(String csvFilePath) {

    // 0: everything terminated just fine
    // !0: error
    short exitCode = 0;

    try {

      LOGGER.info("building GEONAMES hierarchy");

      List<String> command = new ArrayList<String>();
      // TODO could either be python (which equals to v2) or python3 or something else (set via
      // db.properties)
      command.add(props.getPythonInterpreter());
      command.add(props.getKarmaHome() + "buildGEONAMESHierarchy.py");
      command.add(csvFilePath);
      command.add(this.countryCode);
      command.add(props.getWorkDir() + ONTO + File.separator);
      // command.add(Paths.get(xmlFilePath).getParent() + "/" + "output_diff_"
      // + config.getOntologyAcronym() + ".json");

      LOGGER.info("parsing command " + command);

      ProcessBuilder pb = new ProcessBuilder(command);
      pb.inheritIO();
      Process process = pb.start();

      exitCode = (short) process.waitFor();
      if (exitCode == 0)
        LOGGER.info("process completed");

    } catch (IOException | InterruptedException e) {
      LOGGER.error("Conversion failed due to " + e.getLocalizedMessage());
      // if (e.getLocalizedMessage().contains("error=2")) {
      // try {
      // // presumably python3 is not installed, but python(2) should be
      // // Cannot run program "python3": error=2, Datei oder Verzeichnis nicht gefunden
      // LOGGER.info("running with python(2) again");
      // List<String> command = new ArrayList<String>();
      // command.add("python");
      // command.add(configSvc.getWorkDir() + "XML2JSON.py");
      // command.add(xmlFilePath);
      // command.add(Paths.get(xmlFilePath).getParent() + "/" + "output_diff_"
      // + config.getOntologyAcronym() + ".json");
      //
      // LOGGER.info("parsing command " + command);
      //
      // ProcessBuilder pb = new ProcessBuilder(command);
      // pb.inheritIO();
      // Process process;
      //
      // process = pb.start();
      //
      // exitCode = process.waitFor();
      // if (exitCode == 0)
      // LOGGER.info("process completed");
      // } catch (IOException | InterruptedException e2) {
      // // now it must be something else
      // LOGGER.error("Conversion failed due to " + e2.getLocalizedMessage());
      // }
      // }
    }

    return exitCode;
  }

  @Override
  public void cleanTempFiles() throws RuntimeException {
    try {

      DirectoryStream<Path> directoryStream =
          Files.newDirectoryStream(Paths.get(props.getWorkDir() + ONTO));

      for (Path path : directoryStream) {
        // path: absolute path to file
        if (path.toString().contains(".json"))
          Files.delete(path);
      }

      directoryStream.close();

      directoryStream =
          Files.newDirectoryStream(Paths.get(props.getWorkDir() + ONTO + File.separator + "out"));

      for (Path path : directoryStream) {
        // path: absolute path to file
        if (path.toString().contains(".ttl"))
          Files.delete(path);
      }

      directoryStream.close();

    } catch (IOException e) {
      LOGGER.error("exception during cleanTempFiles:" + e.getLocalizedMessage());
      throw new RuntimeException();
    }

  }


}
