package org.gfbio.rdb2rdf;

public interface Mapper {

  public short preprocess() throws RuntimeException;

  public void createTriples() throws RuntimeException;

  public void generateRDFModel() throws RuntimeException;

  public void exportFinalRDFModel() throws RuntimeException;

  public void exportTablesToJSON() throws RuntimeException;

  public void run();

  public boolean createOutDir();

  public void cleanTempFiles() throws RuntimeException;
}
