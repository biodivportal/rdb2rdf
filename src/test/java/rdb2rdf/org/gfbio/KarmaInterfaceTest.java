package rdb2rdf.org.gfbio;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import org.gfbio.rdb2rdf.ITISMapper;
import org.gfbio.util.MappingParser;
import org.junit.Ignore;
import org.junit.Test;

public class KarmaInterfaceTest {

  @Test
  public void testJSONExport() {

  }

  @Test
  @Ignore
  public void testJSONToRDF() throws IOException, InterruptedException {
    // KarmaInterface.createTTLForTable("ITIS", "taxonomic_units", "json");
  }

  @Test
  @Ignore
  public void testMappingConfigParser() throws IOException {
    String json =
        new String(Files.readAllBytes(Paths.get("./src/test/resources/mappingConfig.json")));

    assertEquals(7, MappingParser.getKeys(json, "tables").size());
    assertEquals(2, MappingParser.getKeys(json, "vernaculars").size());
    assertEquals("String", MappingParser.getKeysValues(json, "kingdoms").get("kingdom_name"));
  }

  @Test
  @Ignore
  public void testMapperExecution() {
    Instant start = Instant.now();

    ITISMapper mapper = new ITISMapper();
    mapper.run();
    mapper.createOutDir();
    mapper.exportTablesToJSON();
    mapper.createTriples();
    mapper.generateRDFModel();
    mapper.exportFinalRDFModel();

    Instant end = Instant.now();

    System.out.println("Processing took " + Duration.between(start, end).toSeconds() + "s");
  }
}
