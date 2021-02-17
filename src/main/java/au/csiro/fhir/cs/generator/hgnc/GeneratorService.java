/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com). All rights reserved. Use 
 * is subject to license terms and conditions.
 */
package au.csiro.fhir.cs.generator.hgnc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.CodeSystem.PropertyComponent;
import org.hl7.fhir.r4.model.CodeSystem.PropertyType;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Main generator service for HGNC code systems.
 * 
 * @author Alejandro Metke
 *
 */

@Component
public class GeneratorService {
  
  /**
   * 
   * @param geneGroups
   * @param completeHgnc
   * @return
   * @throws IOException 
   * @throws FileNotFoundException 
   */
  public CodeSystem[] generateHgncCodeSystems(File geneGroups, File completeHgnc) 
      throws FileNotFoundException, IOException {
    
    final CodeSystem[] res = new CodeSystem[2];
    
    // Parse gene groups
    Map<String, String> geneGroupIdNameMap = new HashMap<>();
    Map<String, Set<String>> geneIdGroupMap = new HashMap<>();
    try (FileReader reader = new FileReader(geneGroups)) {
      JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
      for (JsonElement o : arr) {
        JsonObject obj = o.getAsJsonObject();
        String groupId = Integer.toString(obj.get("groupID").getAsInt());
        String groupName = obj.get("groupName").getAsString();
        String geneId = obj.get("hgncID").getAsString();
        geneGroupIdNameMap.put(groupId, groupName);
        
        Set<String> groupIds = geneIdGroupMap.get(geneId);
        if (groupIds == null) {
          groupIds = new HashSet<>();
          geneIdGroupMap.put(geneId, groupIds);
        }
        groupIds.add(groupId);
      }
    }
      
      
    // Parse gene names
    String version = "1970-01-01";
    final Map<String, String> geneIdSymbolMap = new HashMap<>();
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
        geneIdSymbolMap.put(hgncId, symbol);
        
      }
    }
    
    // Create gene groups code system
    CodeSystem geneGroupsCs = new CodeSystem();
    geneGroupsCs.setUrl("http://www.genenames.org/genegroup");
    geneGroupsCs.setName("HGNCGeneGroups");
    geneGroupsCs.setTitle("HGNC Gene Groups");
    geneGroupsCs.setStatus(PublicationStatus.ACTIVE);
    geneGroupsCs.setExperimental(false);
    geneGroupsCs.setPublisher("CSIRO");
    geneGroupsCs.setContent(CodeSystemContentMode.COMPLETE);
    geneGroupsCs.setDescription("Code system for gene groups from HGNC.");
    geneGroupsCs.setCaseSensitive(true);
    geneGroupsCs.setVersion(version);
    
    for (String key : geneGroupIdNameMap.keySet()) {
      geneGroupsCs.addConcept().setCode(key).setDisplay(geneGroupIdNameMap.get(key));
    }
    
    res[0] = geneGroupsCs;
    
    // Create gene ids code system
    CodeSystem geneIdsCs = new CodeSystem();
    geneIdsCs.setUrl("http://www.genenames.org/geneId");
    geneIdsCs.setName("HGNCGeneIDs");
    geneIdsCs.setTitle("HGNC Gene IDs");
    geneIdsCs.setStatus(PublicationStatus.ACTIVE);
    geneIdsCs.setExperimental(false);
    geneIdsCs.setPublisher("CSIRO");
    geneIdsCs.setContent(CodeSystemContentMode.COMPLETE);
    geneIdsCs.setDescription("Code system for gene IDs from HGNC.");
    geneIdsCs.setCaseSensitive(true);
    geneIdsCs.setVersion(version);
    PropertyComponent p = geneIdsCs.addProperty();
    //p.setCode("groupId").setDescription("Gene group ids.").setType(PropertyType.CODING);
    // Setting to string for now because coding type properties are not supported in Ontoserver
    p.setCode("groupId").setDescription("Gene group ids.").setType(PropertyType.STRING);
    
    for (String key : geneIdSymbolMap.keySet()) {
      ConceptDefinitionComponent cdc = geneIdsCs.addConcept().setCode(key)
          .setDisplay(geneIdSymbolMap.get(key));
      
      // Add the gene groups as properties
      if(geneIdGroupMap.containsKey(key)) {
        for(String groupId : geneIdGroupMap.get(key)) {
          cdc.addProperty().setCode("groupId").setValue(
              //new Coding("http://www.genenames.org/genegroup", groupId, 
              //    geneGroupIdNameMap.get(groupId))
              new StringType(groupId)
          );
        }
      }
      
    }
    
    res[1] = geneIdsCs;
    
    return res;
  }

}