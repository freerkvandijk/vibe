package org.molgenis.vibe.rdf_processing.query_string_creation;

import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.molgenis.vibe.TestFilesDir;
import org.molgenis.vibe.io.ModelReader;
import org.molgenis.vibe.io.TripleStoreDbReader;
import org.molgenis.vibe.rdf_processing.QueryTester;
import org.molgenis.vibe.rdf_processing.querying.QueryRunner;
import org.molgenis.vibe.rdf_processing.querying.QueryRunnerRewindable;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the {@link QueryStringGenerator} (based on Apache Jena for RDF file reading/querying).
 *
 * Note that these tests use data from DisGeNET for validation. These files are not provided (though a bash download
 * script is present in the GitHub repository). For validation purposes some data (such as gene-disease association IDs)
 * are present within this test class. However, this was kept as minimal as possible while still being able to actually
 * test the functioning of the code and only reflects what is EXPECTED to be found within the DisGeNET dataset when using
 * the query (on a technical basis). The DisGeNET RDF dataset can be downloaded from http://rdf.disgenet.org/download/
 * and the license can be found on http://www.disgenet.org/ds/DisGeNET/html/legal.html .
 */
public class QueryStringGeneratorTester extends QueryTester {
    private ModelReader reader;
    private QueryRunnerRewindable runner;
    private final String prefixes = DisgenetQueryStringGenerator.getPrefixes();

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

    @Test(expectedExceptions = QueryParseException.class)
    public void testInvalidQuery() {
        new QueryRunner(reader.getModel(), new QueryString(prefixes + "SELECT ?id \n" +
                "WHERE { brandNetelKaasMetEenDruppeltjeMunt?! }"));
    }

    @Test
    public void testEmptyResults() {
        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?id \n" +
                "WHERE { <http://rdf.disgenet.org/resource/gda/0> dcterms:identifier ?id . }"));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        Assert.assertEquals(runner.hasNext(), false);
    }

    @Test
    public void testSingleGdaId() {
        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?id \n" +
                "WHERE { <http://rdf.disgenet.org/resource/gda/DGNa4eb0beb985996e1956b22097c0ad0de> dcterms:identifier ?id }"));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "id", Arrays.asList("disgenet:DGNa4eb0beb985996e1956b22097c0ad0de"));
    }

    @Test
    public void testSingleGdaIdReferences() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://identifiers.org/ncbigene/1289");
        expectedOutput.add("http://linkedlifedata.com/resource/umls/id/C0039516");
        List<String> actualOutput = new ArrayList<>();

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?value \n" +
                "WHERE { <http://rdf.disgenet.org/resource/gda/DGNa4eb0beb985996e1956b22097c0ad0de> sio:SIO_000628 ?value }"));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "value", expectedOutput);
    }

    @Test
    public void testLimit() {
        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes +  "SELECT ?gda \n" +
                "WHERE { ?gda rdf:type sio:SIO_001121 } \n" +
                "LIMIT 3"));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertQueryResultCountWithExpectedResult(runner, 3); // test file contains 9
    }

    @Test
    public void testGdaGeneDiseaseQuery() {
        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?id ?gene ?disease \n" +
                "WHERE { <http://rdf.disgenet.org/resource/gda/DGNa4eb0beb985996e1956b22097c0ad0de> dcterms:identifier ?id ; \n" +
                "sio:SIO_000628 ?gene , ?disease . \n" +
                "?gene rdf:type ncit:C16612 . \n" +
                "?disease rdf:type ncit:C7057 . \n" +
                "}"));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();

        Assert.assertEquals(runner.hasNext(), true, "no match found");
        QuerySolution result = runner.next();
        Assert.assertEquals(runner.hasNext(), false, "more than 1 match found");

        Assert.assertEquals(result.get("id").asLiteral().getString(), "disgenet:DGNa4eb0beb985996e1956b22097c0ad0de");
        Assert.assertEquals(result.get("gene").toString(), "http://identifiers.org/ncbigene/1289");
        Assert.assertEquals(result.get("disease").toString(), "http://linkedlifedata.com/resource/umls/id/C0039516");
    }

    @Test
    public void testHpoSubClassOfInclusive() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0009811");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002967");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002996");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0001377");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0005060");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?hpo \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf* <http://purl.obolibrary.org/obo/HP_0009811> . \n" +
                "}"));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }

    @Test
    public void testHpoSubClassOfExclusive() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002967");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002996");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0001377");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0005060");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?hpo \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf+ <http://purl.obolibrary.org/obo/HP_0009811> . \n" +
                "}"));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }

    @Test
    public void testHpoSubClassOfNoGrandChilds() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0009811");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002967");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002996");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?hpo \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf? <http://purl.obolibrary.org/obo/HP_0009811> . \n" +
                "}"));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }

    /**
     * @see #testHpoSubClassOfAllStartingFromSelf()
     */
    @Test(groups = {"dependencyBug"})
    public void testHpoSubClassOfOnlyGrandChildrenWithoutDistinct() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0001377");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0005060");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?hpo \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf{2,} <http://purl.obolibrary.org/obo/HP_0009811> . \n" +
                "}", Syntax.syntaxARQ));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }

    /**
     * @see #testHpoSubClassOfAllStartingFromSelf()
     */
    @Test(groups = {"dependencyBug"})
    public void testHpoSubClassOfOnlyGrandChildrenWithDistinct() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0001377");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0005060");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT DISTINCT ?hpo \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf{2,} <http://purl.obolibrary.org/obo/HP_0009811> . \n" +
                "}", Syntax.syntaxARQ));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }

    /**
     * @see #testHpoSubClassOfAllStartingFromSelf()
     */
    @Test(groups = {"dependencyBug"})
    public void testHpoSubClassOfOnlyGrandChildrenWithIdRetrieval() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0001377");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0005060");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?hpo ?hpoId \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf{2,} <http://purl.obolibrary.org/obo/HP_0009811> ; \n" +
                "dcterms:identifier ?hpoId . \n" +
                "}", Syntax.syntaxARQ));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }

    @Test
    public void testHpoSubClassOfOnlyChildren2Deep() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0009811");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002967");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002996");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0001377");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?hpo \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf{,2} <http://purl.obolibrary.org/obo/HP_0009811> . \n" +
                "}", Syntax.syntaxARQ));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }

    /**
     * {0,} acts as {1,} instead of * (HP_0009811 is not included in the output).
     *
     * If no DISTINCT is added, HP_0005060 is returned twice.
     * Note that it is a child from HP_0002996 through both HP_0001377 and HP_0006376.
     * Nevertheless, when using * instead of a custom range, it is only returned once without the need for DISTINCT.
     *
     * @see #testHpoSubClassOfInclusive()
     */
    @Test(groups = {"dependencyBug"})
    public void testHpoSubClassOfAllStartingFromSelf() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0009811");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002967");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002996");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0001377");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0005060");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?hpo \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf{0,} <http://purl.obolibrary.org/obo/HP_0009811> . \n" +
                "}", Syntax.syntaxARQ));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }

    /**
     * @see #testHpoSubClassOfAllStartingFromSelf()
     */
    @Test(groups = {"dependencyBug"})
    public void testHpoSubClassOfAllStartingFromChild() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002967");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002996");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0001377");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0005060");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?hpo \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf{1,} <http://purl.obolibrary.org/obo/HP_0009811> . \n" +
                "}", Syntax.syntaxARQ));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }

    @Test
    public void testHpoSubClassOfOnlyChildren2DeepExcplicitStart() {
        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0009811");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002967");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0002996");
        expectedOutput.add("http://purl.obolibrary.org/obo/HP_0001377");

        runner = new QueryRunnerRewindable(reader.getModel(), new QueryString(prefixes + "SELECT ?hpo \n" +
                "WHERE { ?hpo rdf:type sio:SIO_010056 ; \n" +
                "rdfs:subClassOf{0,2} <http://purl.obolibrary.org/obo/HP_0009811> . \n" +
                "}", Syntax.syntaxARQ));
        ResultSetFormatter.out(System.out, runner.getResultSet());
        runner.reset();
        assertSingleFieldFromRunnerOutput(runner, "hpo", expectedOutput);
    }
}