package au.csiro.fhir.cs.generator.hgnc;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.hl7.fhir.r4.model.CodeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ca.uhn.fhir.context.FhirContext;

@SpringBootApplication
public class HgncFhirCodeSystemGeneratorApplication implements CommandLineRunner {
  
  @Autowired
  private GeneratorService service;
  
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(HgncFhirCodeSystemGeneratorApplication.class);
    app.setBannerMode(Banner.Mode.OFF);
    app.run(args);
  }
  
  /**
   * src/main/resources/hgnc_complete_set.json
   * src/main/resources/download-all.json
   * src/main/resources/hierarchy_closure.csv
   */
  @Override
  public void run(String... args) throws Exception {
    File completeHgnc = new File(args[0]);
    File hgncGenesWithGroups = new File(args[1]);
    File hierachyClosure = new File(args[2]);
    File geneIdsCs = new File(args[3]);
    File geneGroupsCs = new File(args[4]);
    
    System.out.println("Generating HGNC code systems");
    List<CodeSystem> cs = service.generateCodeSystems(completeHgnc, hgncGenesWithGroups, 
        hierachyClosure);
    
    FhirContext ctx = FhirContext.forR4();
    
    System.out.println("Saving HGNC gene IDs code system to " + geneIdsCs.getAbsolutePath());
    try (FileWriter fw = new FileWriter(geneIdsCs)) {
      ctx.newJsonParser().encodeResourceToWriter(cs.get(0), fw);
    }
    
    System.out.println("Saving HGNC gene groups code system to " + geneGroupsCs.getAbsolutePath());
    try (FileWriter fw = new FileWriter(geneGroupsCs)) {
      ctx.newJsonParser().encodeResourceToWriter(cs.get(1), fw);
    }
    
  }

}
