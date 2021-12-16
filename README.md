# HGNC to FHIR Transformer

This Spring Boot CLI application transforms a collection of HGNC source files into FHIR code systems. It produces two FHIR code systems, one with the gene groups and another one with the gene ids.

## Building from source

You will need Maven installed in your computer. You can build the jar file using Maven.

```
mvn package
```

## Running

You need a JVM to run the application.

```
java -jar fhir-hgnc-0.1.0-SNAPSHOT.jar -igg [file] -igi [file] -ogg [file] -ogi [file]
```

All the parameters are mandatory and the following table shows their descriptions:

| Parameter          | Type        | Mandatory      |Description   |
| :----------------- | :---------- |:------------- |:------------- |
| -igg               | string      | No            |  The gene groups input file, download-all.json, available from [this page](https://www.genenames.org/download/statistics-and-files/).|
| -igi               | string      | Yes            | The gene ids input file, hgnc_complete_set.json, available from [this page](https://www.genenames.org/download/statistics-and-files/).|
| -ogg               | string      | No            | Gene groups FHIR code system target file. |
| -ogi               | string      | Yes            | Gene ids FHIR code system target file. |
