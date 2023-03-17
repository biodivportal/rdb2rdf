package org.gfbio.util;

public class MappingResource {

  private String modelName;

  private String modelFile;

  private String table;

  private boolean exportAsTTL;

  public MappingResource() {}

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getModelFile() {
    return modelFile;
  }

  public void setModelFile(String modelFile) {
    this.modelFile = modelFile;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public boolean isExportAsTTL() {
    return exportAsTTL;
  }

  public void setExportAsTTL(boolean exportAsTTL) {
    this.exportAsTTL = exportAsTTL;
  }

  public Boolean isEmpty() {
    return this.modelFile == null && this.modelName == null && this.table == null;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("MappingResource [modelName=");
    builder.append(modelName);
    builder.append(", modelFile=");
    builder.append(modelFile);
    builder.append(", table=");
    builder.append(table);
    builder.append(", exportAsTTL=");
    builder.append(exportAsTTL);
    builder.append("]");
    return builder.toString();
  }

}
