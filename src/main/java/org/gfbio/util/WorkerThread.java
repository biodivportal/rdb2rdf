package org.gfbio.util;

import java.io.File;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import org.gfbio.db.MySQLAdapter;
import org.gfbio.rdb2rdf.ITISMapper;
import me.tongfei.progressbar.ProgressBar;

public class WorkerThread extends Thread {

  private static final Logger LOGGER = Logger.getLogger(ITISMapper.class);

  private static AppProperties props = AppProperties.getInstance();

  private String acronym;

  private MySQLAdapter mysqlIf;

  private MappingResource resource;

  public WorkerThread(MappingResource resource, MySQLAdapter mysqlIf, String acronym) {
    this.resource = resource;
    this.mysqlIf = mysqlIf;
    this.acronym = acronym;
  }

  @Override
  public void run() {

    LOGGER.info("running WorkerThread for resource=" + resource.toString());

    writeJSON();
  }

  private void writeJSON() {

    try {

      ResultSet set = null;

      LOGGER.info("exporting table " + resource.getTable() + " to JSON");

      int rows = mysqlIf.countRows(resource.getTable());

      ProgressBar pb = new ProgressBar("writing", rows);

      if (rows > Integer.valueOf(props.getQueryLimit())) {

        double batches = Math.ceil(rows / Integer.valueOf(props.getQueryLimit())) + 1;

        LOGGER.info("preparing " + batches + " batches");

        int offset = 0;

        for (short i = 1; i <= batches; i++) {

          StringBuilder filename =
              new StringBuilder().append(props.getWorkDir()).append(acronym).append(File.separator)
                  .append(resource.getTable()).append("-part-" + i + "-").append(".json");

          set = mysqlIf.select(resource.getTable(), acronym, String.valueOf(offset));

          mysqlIf.exportToJSON(set, resource.getTable(), filename.toString(), acronym, false);

          set.close();

          offset = i * Integer.valueOf(props.getQueryLimit());

          LOGGER.info("\n");

          pb.stepBy(Integer.valueOf(props.getQueryLimit()));
        }

      } else {

        // if table contains <10000 rows
        // write everything into one JSON file

        set = mysqlIf.select(resource.getTable(), acronym);

        if (set != null) {

          StringBuilder filename = new StringBuilder().append(props.getWorkDir()).append(acronym)
              .append(File.separator).append(resource.getTable()).append(".json");

          mysqlIf.exportToJSON(set, resource.getTable(), filename.toString(), acronym, false);

        } else {
          LOGGER.warn("ResultSet is null");
        }

        pb.stepTo(rows);

        set.close();

      }

      pb.stop();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
