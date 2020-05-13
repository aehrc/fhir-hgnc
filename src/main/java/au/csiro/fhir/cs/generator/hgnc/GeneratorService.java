/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com). All rights reserved. Use 
 * is subject to license terms and conditions.
 */
package au.csiro.fhir.cs.generator.hgnc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemHierarchyMeaning;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.CodeSystem.FilterOperator;
import org.hl7.fhir.r4.model.CodeSystem.PropertyComponent;
import org.hl7.fhir.r4.model.CodeSystem.PropertyType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

/**
 * Main generator service for HGNC code systems.
 * 
 * @author Alejandro Metke
 *
 */
@Component
public class GeneratorService {
  
  /**
   * Generates the two code systems for HGNC: the gene ids code system and the gene groups code
   * system.
   * 
   * @param completeHgnc
   * @param hgncGenesWithGroups
   * @param hierachyClosure
   * @return
   * @throws IOException 
   * @throws FileNotFoundException 
   * @throws CsvException 
   */
  public List<CodeSystem> generateCodeSystems(File completeHgnc, File hgncGenesWithGroups, 
      File hierachyClosure) throws FileNotFoundException, IOException, CsvException {
    final List<CodeSystem> res = new ArrayList<>();
    CodeSystem hgncGeneIdCs = generateGeneIdCodeSystem(completeHgnc);
    res.add(hgncGeneIdCs);
    res.add(generateGeneGroupCodeSystem(hgncGenesWithGroups, hierachyClosure, 
        hgncGeneIdCs.getVersion()));
    return res;
  }
  
  /**
   * Generates the FHIR code system <url>http://www.genenames.org/geneId</url>.
   * 
   * @param completeHgnc The complete HGNC dataset in JSON format.
   * @return
   * @throws IOException 
   * @throws FileNotFoundException 
   */
  public CodeSystem generateGeneIdCodeSystem(File completeHgnc) 
      throws FileNotFoundException, IOException {
    try (FileReader reader = new FileReader(completeHgnc)) {
      JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
      
      // Need to get the "response" attribute
      JsonElement je = jsonObject.get("response");
      if (je == null) {
        throw new RuntimeException("Malformed complete HGNC data set. Expected an attribute "
            + "\"response\".");
      }
      
      JsonObject resp = (JsonObject) je;
      je = resp.get("docs");
      if (je == null) {
        throw new RuntimeException("Malformed complete HGNC data set. Expected an attribute \""
            + "response.docs\".");
      }
      
      String version = "1970-01-01";
      
      CodeSystem res = new CodeSystem();
      res.setUrl("http://www.genenames.org/geneId");
      res.setName("HGNCGeneIDs");
      res.setTitle("HGNC Gene IDs");
      res.setStatus(PublicationStatus.ACTIVE);
      res.setExperimental(false);
      res.setPublisher("CSIRO");
      res.setContent(CodeSystemContentMode.COMPLETE);
      res.setDescription("Code system for gene IDs from HGNC.");
      res.setCaseSensitive(true);
      
      JsonArray docs = (JsonArray) je;
      for (JsonElement e : docs) {
        JsonObject doc = (JsonObject) e;
        String hgncId = doc.get("hgnc_id").getAsString();
        String symbol = doc.get("symbol").getAsString();
        String dateModified = "1970-01-01";
        JsonElement mod = doc.get("date_modified");
        if (mod != null) {
          dateModified = mod.getAsString();
        }
        if (version.compareTo(dateModified) < 0) {
          // version precedes modified date so we need to update the version
          version = dateModified;
        }
        res.addConcept().setCode(hgncId).setDisplay(symbol);
      }
      
      res.setVersion(version);
      return res;
    }
  }
  
  /**
   * 
   * @param hgncGenesWithGroups
   * @param hierachyClosure
   * @return
   * @throws IOException 
   * @throws FileNotFoundException 
   * @throws CsvException 
   */
  public CodeSystem generateGeneGroupCodeSystem(File family, File hierachyClosure, 
      String version) 
      throws FileNotFoundException, IOException, CsvException {
    
    // Create map with direct parents
    final Map<String, Set<String>> parents = new HashMap<>();
    try (FileReader reader = new FileReader(hierachyClosure)) {
      try (CSVReader csvReader = new CSVReader(reader)) {
        List<String[]> list = csvReader.readAll();
        
        for (String[] row : list) {
          String parent = row[0];
          String child = row[1];
          String distance = row[2];
          
          if ("1".equals(distance)) {
            Set<String> s = parents.get(child);
            if (s == null) {
              s = new HashSet<>();
              parents.put(child, s);
            }
            s.add(parent);
          }
        }
        
      }
    }
    
    try (FileReader reader = new FileReader(family)) {
      CodeSystem res = new CodeSystem();
      res.setUrl("http://www.genenames.org/genegroup");
      res.setName("HGNCGeneGroups");
      res.setTitle("HGNC Gene Groups");
      res.setStatus(PublicationStatus.ACTIVE);
      res.setExperimental(false);
      res.setPublisher("CSIRO");
      res.setContent(CodeSystemContentMode.COMPLETE);
      res.setDescription("Code system for gene groups from HGNC.");
      res.setCaseSensitive(true);
      res.setHierarchyMeaning(CodeSystemHierarchyMeaning.ISA);
      res.addFilter().setCode("root").addOperator(FilterOperator.EQUAL).setValue("True or false.");
      PropertyComponent p = res.addProperty();
      p.setCode("parent").setDescription("Parent codes.").setType(PropertyType.CODE);
      PropertyComponent r = res.addProperty();
      r.setCode("root").setDescription("Indicates if this concept is a root concept.")
        .setType(PropertyType.BOOLEAN);
      
      
      final Set<String> ids = new HashSet<>();
      try (CSVReader csvReader = new CSVReader(reader)) {
        List<String[]> list = csvReader.readAll();
        
        for (String[] row : list) {
          String groupId = row[0];
          String groupName = row[2];
          
          res.addConcept().setCode(groupId).setDisplay(groupName);
          ids.add(groupId);
        }
      }
      
      for (ConceptDefinitionComponent cdc : res.getConcept()) {
        String id = cdc.getCode();
        Set<String> pars = parents.get(id);
        if (pars != null && ! pars.isEmpty()) {
          for (String parent : pars) {
            if (!ids.contains(parent)) {
              System.err.println("Parent " + parent + " does not exist");
              continue;
            }
            cdc.addProperty().setCode("parent").setValue(new CodeType(parent));
          }
          cdc.addProperty().setCode("root").setValue(new BooleanType(false));
        } else {
          cdc.addProperty().setCode("root").setValue(new BooleanType(true));
        }
      }
      
      return res;
    }
  }

}
