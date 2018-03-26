package org.molgenis.vibe.rdf_processing.query_string_creation;

import org.apache.jena.query.ResultSetFormatter;
import org.molgenis.vibe.TestFilesDir;
import org.molgenis.vibe.exceptions.InvalidStringFormatException;
import org.molgenis.vibe.formats.Disease;
import org.molgenis.vibe.formats.Phenotype;
import org.molgenis.vibe.io.ModelReader;
import org.molgenis.vibe.io.TripleStoreDbReader;
import org.molgenis.vibe.rdf_processing.QueryTester;
import org.molgenis.vibe.rdf_processing.querying.QueryRunnerRewindable;
import org.testng.Assert;
import org.testng.annotations.*;

import java.net.URI;
import java.util.*;

/**
 * Note that these tests use data from DisGeNET for validation. These files are not provided (though a bash download
 * script is present in the GitHub repository). For validation purposes some data (such as gene-disease association IDs)
 * are present within this test class. However, this was kept as minimal as possible while still being able to actually
 * test the functioning of the code and only reflects what is EXPECTED to be found within the DisGeNET dataset when using
 * the query (on a technical basis). The DisGeNET RDF dataset can be downloaded from http://rdf.disgenet.org/download/
 * and the license can be found on http://www.disgenet.org/ds/DisGeNET/html/legal.html .
 */
public class DisgenetQueryStringGeneratorTester extends QueryTester {
    private static final String delimiter = " - ";

    private ModelReader reader;

    private QueryRunnerRewindable runner;

    @BeforeClass
    public void beforeClass() {
        reader = new TripleStoreDbReader(TestFilesDir.TDB_MINI.getDir());
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        reader.close();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() {
        runner.close();
    }

    private void runQueryTest(QueryString queryString, String[] fieldOrder, List<List<String>> expectedOutput) {
        runQueryTest(queryString, fieldOrder, expectedOutput, true);
    }

    private void runQueryTest(QueryString queryString, String[] fieldOrder, List<List<String>> expectedOutput, boolean verbose) {
        if(verbose) {
            System.out.println(queryString.getQuery());
        }

        runner = new QueryRunnerRewindable(reader.getModel(), queryString);

        if(verbose) {
            ResultSetFormatter.out(System.out, runner.getResultSet());
            runner.reset();
        }

        assertRunnerOutput(runner, fieldOrder, expectedOutput);
    }

    @Test
    public void testSourcesUnique() {
        QueryString queryString = DisgenetQueryStringGenerator.getSources();
        runner = new QueryRunnerRewindable(reader.getModel(), queryString);

        Set<String> sources = new HashSet<>();

        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();

        while(runner.hasNext()) {
            String source = runner.next().get("source").asResource().getURI();
            if(sources.contains(source)) {
                Assert.fail("a source URI was found more than once");
            }
            sources.add(source);
        }

    }

    @Test
    public void testIriForHpo() throws InvalidStringFormatException {
        Set<Phenotype> phenotypes = new HashSet<>();
        phenotypes.add(new Phenotype("hp:0009811"));

        String[] fieldOrder = {"hpo"};

        List<List<String>> expectedOutput = Arrays.asList(
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0009811")
        );

        QueryString queryString = DisgenetQueryStringGenerator.getIriForHpo(phenotypes);
        runQueryTest(queryString, fieldOrder, expectedOutput);
    }

    @Test
    public void testIriForMultiHpo() throws InvalidStringFormatException {
        Set<Phenotype> phenotypes = new HashSet<>();
        phenotypes.add(new Phenotype("hp:0009811"));
        phenotypes.add(new Phenotype("hp:0002967"));
        phenotypes.add(new Phenotype("hp:0002996"));
        phenotypes.add(new Phenotype("hp:0001377"));

        String[] fieldOrder = {"hpo"};

        List<List<String>> expectedOutput = Arrays.asList(
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0009811"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0002967"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0002996"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0001377")
        );

        QueryString queryString = DisgenetQueryStringGenerator.getIriForHpo(phenotypes);
        runQueryTest(queryString, fieldOrder, expectedOutput);
    }

    @Test
    public void testAllHpoChildren() throws InvalidStringFormatException {
        Set<Phenotype> phenotypes = new HashSet<>();
        phenotypes.add(new Phenotype("hp:0009811", "Abnormality of the elbow", URI.create("http://purl.obolibrary.org/obo/HP_0009811")));

        String[] fieldOrder = {"hpo"};

        List<List<String>> expectedOutput = Arrays.asList(
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0009811"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0002967"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0002996"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0001377"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0005060")
        );

        QueryString queryString = DisgenetQueryStringGenerator.getHpoChildren(phenotypes,
                new QueryStringPathRange(0, true));
        runQueryTest(queryString, fieldOrder, expectedOutput);
    }

    @Test
    public void testAllHpoChildrenExcludingSelfUsingMultipleParents() throws InvalidStringFormatException {
        Set<Phenotype> phenotypes = new HashSet<>();
        phenotypes.add(new Phenotype("hp:0009811", "Abnormality of the elbow", URI.create("http://purl.obolibrary.org/obo/HP_0009811")));
        phenotypes.add(new Phenotype("hp:0001376", "Limitation of joint mobility", URI.create("http://purl.obolibrary.org/obo/HP_0001376")));

        String[] fieldOrder = {"hpoRoot", "hpo"};

        List<List<String>> expectedOutput = Arrays.asList(
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0009811", "http://purl.obolibrary.org/obo/HP_0002967"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0009811", "http://purl.obolibrary.org/obo/HP_0002996"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0009811", "http://purl.obolibrary.org/obo/HP_0001377"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0009811", "http://purl.obolibrary.org/obo/HP_0005060"),

                Arrays.asList("http://purl.obolibrary.org/obo/HP_0001376", "http://purl.obolibrary.org/obo/HP_0002996"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0001376", "http://purl.obolibrary.org/obo/HP_0001377"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0001376", "http://purl.obolibrary.org/obo/HP_0005060")
        );

        QueryString queryString = DisgenetQueryStringGenerator.getHpoChildren(phenotypes,
                new QueryStringPathRange(1, true));
        runQueryTest(queryString, fieldOrder, expectedOutput);
    }

    @Test
    public void testPdasSingleHpo() throws InvalidStringFormatException {
        Set<Phenotype> phenotypes = new HashSet<>();
        phenotypes.add(new Phenotype("hp:0009811", "Abnormality of the elbow", URI.create("http://purl.obolibrary.org/obo/HP_0009811")));

        String[] fieldOrder = {"disease", "diseaseTitle", "pdaSource"};

        List<List<String>> expectedOutput = Arrays.asList(
                Arrays.asList("http://linkedlifedata.com/resource/umls/id/C0039516", "Tennis Elbow", "http://rdf.disgenet.org/v5.0.0/void/GROZA-2015"),
                Arrays.asList("http://linkedlifedata.com/resource/umls/id/C0152084", "Jaccoud's syndrome", "http://rdf.disgenet.org/v5.0.0/void/HOEHNDORF-2015")
        );

        QueryString queryString = DisgenetQueryStringGenerator.getPdas(phenotypes);
        runQueryTest(queryString, fieldOrder, expectedOutput);
    }

    @Test
    public void testPdasMultipleHpo() throws InvalidStringFormatException {
        Set<Phenotype> phenotypes = new HashSet<>();
        phenotypes.add(new Phenotype("hp:0009811", "Abnormality of the elbow", URI.create("http://purl.obolibrary.org/obo/HP_0009811")));
        phenotypes.add(new Phenotype("hp:0002967", "Cubitus valgus", URI.create("http://purl.obolibrary.org/obo/HP_0002967")));
        phenotypes.add(new Phenotype("hp:0002966", "Limited elbow movement", URI.create("http://purl.obolibrary.org/obo/HP_0002996")));

        String[] fieldOrder = {"hpo", "disease"};

        List<List<String>> expectedOutput = Arrays.asList(
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0009811", "http://linkedlifedata.com/resource/umls/id/C0039516"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0009811", "http://linkedlifedata.com/resource/umls/id/C0152084"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0002967", "http://linkedlifedata.com/resource/umls/id/C0175704"),
                Arrays.asList("http://purl.obolibrary.org/obo/HP_0002996", "http://linkedlifedata.com/resource/umls/id/C1834674")
        );

        QueryString queryString = DisgenetQueryStringGenerator.getPdas(phenotypes);
        runQueryTest(queryString, fieldOrder, expectedOutput);
    }

    @Test
    public void geneDiseaseCombinationsForSingleDisease() {
        Set<Disease> inputDiseases = new HashSet<>();
        inputDiseases.add(new Disease("umls:C1853733", "HEMOCHROMATOSIS, TYPE 4", URI.create("http://linkedlifedata.com/resource/umls/id/C1853733")));

        String[] fieldOrder = {"gene", "geneId", "geneSymbolTitle", "gdaScoreNumber", "gdaSource", "evidence"};

        List<List<String>> expectedOutput = Arrays.asList(
                Arrays.asList("http://identifiers.org/ncbigene/30061", "ncbigene:30061", "SLC40A1", "0.682472541057918E0", "http://rdf.disgenet.org/v5.0.0/void/UNIPROT", "http://identifiers.org/pubmed/15466004"),
                Arrays.asList("http://identifiers.org/ncbigene/30061", "ncbigene:30061", "SLC40A1", "0.682472541057918E0", "http://rdf.disgenet.org/v5.0.0/void/CTD_human")
        );

        QueryString queryString = DisgenetQueryStringGenerator.getGdas(inputDiseases, DisgenetAssociationType.GENE_DISEASE);
        runQueryTest(queryString, fieldOrder, expectedOutput);
    }

    @Test
    public void geneDiseaseCombinationsForMultipleDiseases() {
        Set<Disease> inputDiseases = new HashSet<>();
        inputDiseases.add(new Disease("umls:C1853733", "HEMOCHROMATOSIS, TYPE 4", URI.create("http://linkedlifedata.com/resource/umls/id/C1853733")));
        inputDiseases.add(new Disease("umls:C0015773", "Felty Syndrome", URI.create("http://linkedlifedata.com/resource/umls/id/C0015773")));

        String[] fieldOrder = {"disease", "gene", "geneId", "geneSymbolTitle", "gdaScoreNumber", "gdaSource", "evidence"};

        List<List<String>> expectedOutput = Arrays.asList(
                Arrays.asList("http://linkedlifedata.com/resource/umls/id/C1853733", "http://identifiers.org/ncbigene/30061", "ncbigene:30061", "SLC40A1", "0.682472541057918E0", "http://rdf.disgenet.org/v5.0.0/void/UNIPROT", "http://identifiers.org/pubmed/15466004"),
                Arrays.asList("http://linkedlifedata.com/resource/umls/id/C1853733", "http://identifiers.org/ncbigene/30061", "ncbigene:30061", "SLC40A1", "0.682472541057918E0", "http://rdf.disgenet.org/v5.0.0/void/CTD_human"),
                Arrays.asList("http://linkedlifedata.com/resource/umls/id/C0015773", "http://identifiers.org/ncbigene/3105", "ncbigene:3105", "HLA-A", "2.747267842131E-4", "http://rdf.disgenet.org/v5.0.0/void/BEFREE", "http://identifiers.org/pubmed/10817772"),
                Arrays.asList("http://linkedlifedata.com/resource/umls/id/C0015773", "http://identifiers.org/ncbigene/27087", "ncbigene:27087", "B3GAT1", "2.747267842131E-4", "http://rdf.disgenet.org/v5.0.0/void/BEFREE", "http://identifiers.org/pubmed/3345230")
        );

        QueryString queryString = DisgenetQueryStringGenerator.getGdas(inputDiseases, DisgenetAssociationType.GENE_DISEASE);
        runQueryTest(queryString, fieldOrder, expectedOutput);
    }
}