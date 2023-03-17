package org.gfbio.main;

import java.time.Duration;
import java.time.Instant;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.gfbio.rdb2rdf.GEONAMESMapper;
import org.gfbio.rdb2rdf.ITISMapper;
import org.gfbio.rdb2rdf.Mapper;

public class MapperMain {

  private static final Logger LOGGER = Logger.getLogger(MapperMain.class);

  public static void main(String[] args) {

    Mapper mapper = null;

    Options options = new Options();
    HelpFormatter formatter = new HelpFormatter();

    short rc = 0;

    try {

      // create commandline options
      options.addOption("m", true, "load mapper (currently supported: itis, geonames)");
      options.addOption("c", true, "if using 'geonames': country to map (e.g. 'DE')");
      options.addOption("p", "if using 'geonames': preprocess raw data to JSON");

      // create the parser
      CommandLineParser parser = new DefaultParser();

      // parse the command line arguments
      CommandLine line = parser.parse(options, args);

      // read in options
      if (line.hasOption("m")) {

        // select appropriate mapper class
        if (line.getOptionValue("m").equalsIgnoreCase("itis"))
          mapper = new ITISMapper();
        else if (line.getOptionValue("m").equalsIgnoreCase("geonames")) {
          if (line.hasOption("c"))
            mapper = new GEONAMESMapper(line.getOptionValue("c"));
          else {
            LOGGER.warn("missing argument '-c'");
            terminate((short) -1);
          }

        }

      } else {
        LOGGER.warn("missing argument");
        formatter.printHelp("java -jar mapper.jar", options, true);
        terminate((short) -1);
      }

      if (mapper != null) {

        Instant start = Instant.now();

        try {

          mapper.run();

          if (mapper.createOutDir() == true) {

            if (line.hasOption("p")) {
              rc = mapper.preprocess();
              System.out.println(rc);
              if (rc != 0)
                terminate(rc);
            }
            mapper.exportTablesToJSON();
            mapper.createTriples();
            mapper.generateRDFModel();
            mapper.exportFinalRDFModel();
            mapper.cleanTempFiles();

          } else
            LOGGER.warn("output directory could not be created");

        } catch (Exception e) {
          e.printStackTrace();
        }

        Instant end = Instant.now();

        LOGGER.info("Processing took " + Duration.between(start, end).toSeconds() + "s");
        terminate((short) 0);

      } else {
        LOGGER.warn("Mapper could not be instantiated! Maybe you missed an argument?");
        formatter.printHelp("java -jar mapper.jar", options, true);
        terminate((short) -1);
      }

    } catch (ParseException e) {

      LOGGER.error("Parsing failed.  Reason: " + e.getMessage());

      formatter.printHelp("mapper", options, true);

      terminate((short) -1);
    }

  }

  private static void terminate(short rc) {
    LOGGER.info("exiting with code=" + rc);
    System.exit(rc);
  }
}
