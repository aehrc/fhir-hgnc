package au.csiro.fhir.cs.generator.hgnc;

import java.io.File;
import java.io.FileWriter;

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
  
  @Override
  public void run(String... args) throws Exception {
    File geneGroups = new File(args[0]);
    File completeHgnc = new File(args[1]);
    File geneGroupsCodeSystemTargetFile = new File(args[2]);
    File genesCodeSystemTargetFile = new File(args[3]);
    
    System.out.println("Generating HGNC code systems");
    CodeSystem[] cs = service.generateHgncCodeSystems(geneGroups, completeHgnc);
    
    FhirContext ctx = FhirContext.forR4();
    
    System.out.println("Saving HGNC gene groups code system to " 
        + geneGroupsCodeSystemTargetFile.getAbsolutePath());
    try (FileWriter fw = new FileWriter(geneGroupsCodeSystemTargetFile)) {
      ctx.newJsonParser().encodeResourceToWriter(cs[0], fw);
    }
    
    System.out.println("Saving HGNC gene IDs code system to " 
        + genesCodeSystemTargetFile.getAbsolutePath());
    try (FileWriter fw = new FileWriter(genesCodeSystemTargetFile)) {
      ctx.newJsonParser().encodeResourceToWriter(cs[1], fw);
    }
    
    
    
  }

}
