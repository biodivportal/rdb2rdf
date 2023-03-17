package org.gfbio.util;


import java.util.List;


public class MappingBasic {

  private String baseInstanceUri;

  private List<MappingResource> resources;

  public MappingBasic() {}

  public MappingBasic(String baseInstanceUri, List<MappingResource> resources) {
    this.baseInstanceUri = baseInstanceUri;
    this.resources = resources;
  }

  public String getBaseInstanceUri() {
    return baseInstanceUri;
  }

  public void setBaseInstanceUri(String baseInstanceUri) {
    this.baseInstanceUri = baseInstanceUri;
  }

  public List<MappingResource> getResources() {
    return resources;
  }

  public void setResources(List<MappingResource> resources) {
    this.resources = resources;
  }


  public MappingResource getResource(String modelName) {
    MappingResource resource = new MappingResource();
    for (MappingResource r : resources) {
      if (r.getModelName().equals(modelName)) {
        return r;
      }
    }
    return resource;

  }

}
