package au.csiro.fhir.cs.generator.hgnc;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.commons.cli.*;
import org.hl7.fhir.r4.model.CodeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ca.uhn.fhir.context.FhirContext;

import static java.lang.System.exit;

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
    Options options = new Options();
    options.addOption(
      Option.builder("igg")
        .required(false)
        .hasArg(true)
        .longOpt("input-gene-groups")
        .desc("The input HGNC gene groups file.")
        .build()
    );
    options.addOption(
      Option.builder("igi")
        .required(true)
        .hasArg(true)
        .longOpt("input-gene-ids")
        .desc("The input HGNC gene ids file.")
        .build()
    );
    options.addOption(
      Option.builder("ogg")
        .required(false)
        .hasArg(true)
        .longOpt("output-gene-groups")
        .desc("The output HGNC gene groups FHIR code system file.")
        .build()
    );
    options.addOption(
      Option.builder("ogi")
        .required(true)
        .hasArg(true)
        .longOpt("output-gene-ids")
        .desc("The output HGNC gene ids FHIR code system file.")
        .build()
    );

    CommandLineParser parser = new DefaultParser();
    try {
      File geneGroups = null;
      File completeHgnc = null;
      File geneGroupsCodeSystemTargetFile = null;
      File genesCodeSystemTargetFile = null;

      // parse the command line arguments
      CommandLine line = parser.parse(options, args);
      String val = line.getOptionValue("igg");
      if (val != null) {
        geneGroups = new File(val);
      }

      val = line.getOptionValue("igi");
      if (val != null) {
        completeHgnc = new File(val);
      } else  {
        System.out.println("The -igi argument is required.");
        System.exit(0);
      }

      val = line.getOptionValue("ogg");
      if (val != null) {
        geneGroupsCodeSystemTargetFile = new File(val);
      }

      val = line.getOptionValue("ogi");
      if (val != null) {
        genesCodeSystemTargetFile = new File(val);
      } else  {
        System.out.println("The -ogi argument is required.");
        System.exit(0);
      }

      System.out.println("Generating HGNC code systems");
      FhirContext ctx = FhirContext.forR4();
      if (geneGroups == null) {
        System.out.println("Gene groups file is not available");
        CodeSystem cs = service.generateHgncCodeSystem(completeHgnc);
        System.out.println("Saving HGNC gene IDs code system to "
          + genesCodeSystemTargetFile.getAbsolutePath());
        try (FileWriter fw = new FileWriter(genesCodeSystemTargetFile)) {
          ctx.newJsonParser().setPrettyPrint(true).encodeResourceToWriter(cs, fw);
        }
      } else {
        CodeSystem[] cs = service.generateHgncCodeSystems(geneGroups, completeHgnc);

        if (geneGroupsCodeSystemTargetFile == null) {
          System.out.println("The -ogg argument is required.");
          System.exit(0);
        }

        System.out.println("Saving HGNC gene groups code system to "
          + geneGroupsCodeSystemTargetFile.getAbsolutePath());
        try (FileWriter fw = new FileWriter(geneGroupsCodeSystemTargetFile)) {
          ctx.newJsonParser().setPrettyPrint(true).encodeResourceToWriter(cs[0], fw);
        }

        System.out.println("Saving HGNC gene IDs code system to "
          + genesCodeSystemTargetFile.getAbsolutePath());
        try (FileWriter fw = new FileWriter(genesCodeSystemTargetFile)) {
          ctx.newJsonParser().setPrettyPrint(true).encodeResourceToWriter(cs[1], fw);
        }
      }

    } catch (ParseException exp) {
      // oops, something went wrong
      System.out.println(exp.getMessage());
      printUsage(options);
    }

    exit(0);
  }

  private static void printUsage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    final PrintWriter writer = new PrintWriter(System.out);
    formatter.printUsage(writer, 80, "FHIR HGNC", options);
    writer.flush();
  }

}
