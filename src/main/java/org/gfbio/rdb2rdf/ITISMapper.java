package org.gfbio.rdb2rdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;
import org.gfbio.db.MySQLAdapter;
import org.gfbio.karma.KarmaInterface;
import org.gfbio.util.AppProperties;
import org.gfbio.util.MappingBasic;
import org.gfbio.util.MappingParser;
import org.gfbio.util.MappingResource;
import org.gfbio.util.WorkerThread;

/**
 * https://jena.apache.org/documentation/io/rdf-input.html
 * 
 */
public class ITISMapper implements Mapper {

  private static final String ONTO = "ITIS";

  private static final Logger LOGGER = Logger.getLogger(ITISMapper.class);

  private static final String baseUri = "http://terminologies.gfbio.org/ITIS";

  private List<Model> editedGraphs = new ArrayList<Model>();

  private AppProperties props = AppProperties.getInstance();

  private Map<String, String> tableToClass = null;

  public ITISMapper() {

    try {

      tableToClass = MappingParser.getKeysValues(
          new String(Files.readAllBytes(
              Paths.get(props.getModelDir() + ONTO + File.separator + "mappingConfig.json"))),
          "tables");
    } catch (IOException e) {
      e.printStackTrace();
      LOGGER.error("error loading mappingConfig.json");
    }

    LOGGER.info(" +++++++++++++++++++ ");
    LOGGER.info(" ITISMapper created ");
    LOGGER.info("Currently supported Tables/Classes: ");
    LOGGER.info(tableToClass.toString() + "\n");

  }

  @Override
  public void run() {
    // this does nothing - yet
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

    MySQLAdapter mysqlIf = new MySQLAdapter();

    MappingBasic mappingConfig = props.readMappingConfig(ONTO);

    ExecutorService execSvc = Executors.newCachedThreadPool();

    for (MappingResource resource : mappingConfig.getResources()) {

      WorkerThread wThread = new WorkerThread(resource, mysqlIf, ONTO);
      // wThread.run();
      execSvc.execute(wThread);

      /*
       * try {
       * 
       * ResultSet set = null;
       * 
       * LOGGER.info("exporting table " + resource.getTable() + " to JSON");
       * 
       * int rows = mysqlIf.countRows(resource.getTable());
       * 
       * if (rows > Integer.valueOf(props.getQueryLimit())) {
       * 
       * double batches = Math.ceil(rows / Integer.valueOf(props.getQueryLimit())) + 1;
       * 
       * LOGGER.info("preparing " + batches + " batches");
       * 
       * int offset = 0;
       * 
       * for (short i = 1; i <= batches; i++) {
       * 
       * StringBuilder filename = new
       * StringBuilder().append(props.getWorkDir()).append(ONTO).append(File.separator)
       * .append(resource.getTable()).append("-part-" + i + "-").append(".json");
       * 
       * set = mysqlIf.select(resource.getTable(), ONTO, String.valueOf(offset));
       * 
       * mysqlIf.exportToJSON(set, resource.getTable(), filename.toString(), ONTO, false);
       * 
       * set.close();
       * 
       * offset = i * Integer.valueOf(props.getQueryLimit());
       * 
       * LOGGER.info("\n"); }
       * 
       * } else {
       * 
       * // if table contains <10000 rows // write everything into one JSON file
       * 
       * set = mysqlIf.select(resource.getTable(), ONTO);
       * 
       * if (set != null) {
       * 
       * StringBuilder filename = new StringBuilder().append(props.getWorkDir()).append(ONTO)
       * .append(File.separator).append(resource.getTable()).append(".json");
       * 
       * mysqlIf.exportToJSON(set, resource.getTable(), filename.toString(), ONTO, false);
       * 
       * } else { LOGGER.warn("ResultSet is null"); }
       * 
       * set.close();
       * 
       * } } catch (Exception e) { e.printStackTrace(); }
       * 
       * LOGGER.info("\n");
       */
    }

    execSvc.shutdown();

    // Wait until all threads are finish
    // Returns true if all tasks have completed following shut down.
    // Note that isTerminated is never true unless either shutdown or shutdownNow was called first.
    while (!execSvc.isTerminated()) {
      // empty body
    }

    LOGGER.info("export done!");
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
          addTypeOwlClassStatement(table, tableTTL);
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

    // TODO change output format here
    String rdfFileName = props.getWorkDir() + ONTO + File.separator + ONTO + ".ttl";

    LOGGER.info("exporting final RDF file to " + rdfFileName);
    try {

      Model baseModel = ModelFactory.createDefaultModel();

      for (Model m : editedGraphs) {
        baseModel.add(m);
      }
      baseModel.setNsPrefix("omv", "http://omv.ontoware.org/2005/05/ontology#");
      baseModel.setNsPrefix("gfbio", "http://terminologies.gfbio.org/terms/");
      baseModel.setNsPrefix("ts-schema", "http://terminologies.gfbio.org/terms/ts-schema/");
      baseModel.setNsPrefix("itis", "http://terminologies.gfbio.org/ITIS/");

      removeTypeClassStatement("taxonomic_units", baseModel);

      baseModel.write(new FileOutputStream(rdfFileName), "N-TRIPLES");

      baseModel.close();

      // close models after final export
      editedGraphs.forEach(m -> m.close());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Add the following statement to the graph:
   * <p>
   * http://terminologies.gfbio.org/ITIS/Kingdom http://www.w3.org/1999/02/22-rdf-syntax-ns#type
   * http://www.w3.org/2002/07/owl#Class
   * 
   * @param tableName
   * @param model
   */
  private void addTypeOwlClassStatement(String tableName, Model model) {

    LOGGER.info("addTypeOwlClassStatement \n");

    if (!tableToClass.get(tableName).equalsIgnoreCase("")) {

      Statement statement =
          model.createStatement(model.createResource(baseUri + "/" + tableToClass.get(tableName)),
              model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
              model.createResource("http://www.w3.org/2002/07/owl#Class"));

      model.add(statement);

    }
  }

  /**
   * Remove subject with type of its own class, e.g.,
   * <p>
   * http://terminologies.gfbio.org/ITIS/Taxa_16648 http://www.w3.org/1999/02/22-rdf-syntax-ns#type
   * http://terminologies.gfbio.org/ITIS#Taxa
   */
  private void removeTypeClassStatement(String tableName, Model model) {

    LOGGER.info("removeTypeClassStatement \n");

    List<Statement> toRemove = new ArrayList<Statement>();

    if (!tableToClass.get(tableName).equalsIgnoreCase("")) {

      // iterate through list of all the statements in the graph
      // ResIterator res = model.listResourcesWithProperty(RDF.type);

      for (final ResIterator res = model.listResourcesWithProperty(RDF.type); res.hasNext();) {

        // extract subjects
        Resource r = res.next();

        // (1) remove subject with type of its own class
        Statement statement = model.createStatement(r,
            model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
            model.createResource(baseUri + "#" + tableToClass.get(tableName)));

        toRemove.add(statement);

      }

    }

    model.remove(toRemove);

  }

  @Override
  public short preprocess() {
    // This does nothing

    return 0;
  }

  @Override
  /**
   * Delete .json and .ttl files which were created during processing.
   */
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
