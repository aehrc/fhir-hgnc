package au.csiro.fhir.cs.generator.hgnc;

import java.util.Map;
import java.util.Set;

public class GeneIdsAndVersion {
  final Map<String, String> geneIdSymbolMap;
  final Map<String, Set<String>> geneIdPreviousSymbolMap;
  final String version;

  public GeneIdsAndVersion(Map<String, String> geneIdSymbolMap, Map<String, Set<String>> geneIdPreviousSymbolMap,
                           String version) {
    this.geneIdSymbolMap = geneIdSymbolMap;
    this.geneIdPreviousSymbolMap = geneIdPreviousSymbolMap;
    this.version = version;
  }

  public Map<String, String> getGeneIdSymbolMap() {
    return geneIdSymbolMap;
  }

  public Map<String, Set<String>> getGeneIdPreviousSymbolMap() {
    return geneIdPreviousSymbolMap;
  }

  public String getVersion() {
    return version;
  }
}
