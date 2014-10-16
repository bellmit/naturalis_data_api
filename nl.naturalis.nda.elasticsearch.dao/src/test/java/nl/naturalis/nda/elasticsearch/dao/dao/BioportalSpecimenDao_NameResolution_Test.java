package nl.naturalis.nda.elasticsearch.dao.dao;

import nl.naturalis.nda.domain.ScientificName;
import nl.naturalis.nda.domain.Specimen;
import nl.naturalis.nda.domain.SpecimenIdentification;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESSpecimen;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESTaxon;
import nl.naturalis.nda.elasticsearch.dao.util.QueryParams;
import nl.naturalis.nda.search.ResultGroupSet;
import org.junit.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static nl.naturalis.nda.elasticsearch.dao.dao.BioportalTaxonDaoTest.createTestTaxon;
import static org.hamcrest.Matchers.is;

public class BioportalSpecimenDao_NameResolution_Test extends AbstractBioportalSpecimenDaoTest {

    @Test
    public void testExtendedNameSearch_nameResolution_simpleSearch_vernacularName() throws Exception {
        setupNameResolutionTest();

        QueryParams params = new QueryParams();
        params.add("_search", "henkie");

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);
        assertEquals(1, result.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_simpleSearch_synonymGenusOrMonomial() throws Exception {
        setupNameResolutionTest();

        QueryParams params = new QueryParams();
        params.add("_search", "geslacht");

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);
        assertEquals(1, result.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_simpleSearch_synonymsSpecificEpithet() throws Exception {
        setupNameResolutionTest();

        QueryParams params = new QueryParams();
        params.add("_search", "specifiek");

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);
        assertEquals(1, result.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_simpleSearch_synonymsInfraspecificEpithet() throws Exception {
        setupNameResolutionTest();

        QueryParams params = new QueryParams();
        params.add("_search", "infra");

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);
        assertEquals(1, result.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_vernacularName() throws Exception {
        setupNameResolutionTest();

        QueryParams params = new QueryParams();
        params.add("vernacularName", "henkie");

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);
        assertEquals(1, result.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_vernacularName_OR() throws Exception {
        setupNameResolutionTest();

        QueryParams params = new QueryParams();
        params.add("_andOr", "OR");
        params.add("kingdom", "wrong value"); // has no results
        params.add("vernacularName", "henkie");

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);
        assertEquals(1, result.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_vernacularName_AND() throws Exception {
        setupNameResolutionTest();

        QueryParams params = new QueryParams();
        params.add("_andOr", "AND");
        params.add("kingdom", "wrong value"); // leads to no results
        params.add("vernacularName", "henkie");

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);
        assertEquals(0, result.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_kingdom() throws Exception {

        // GIVEN
        setupNameResolutionTest();

        QueryParams paramsWithoutNameResolution = new QueryParams();
        paramsWithoutNameResolution.add("kingdom", "wrong value");

        ResultGroupSet<Specimen, String> preResult = dao.specimenNameSearch(paramsWithoutNameResolution);
        assertEquals(0, preResult.getTotalSize());

        // WHEN
        QueryParams paramsWithNameResolution = new QueryParams();
        paramsWithNameResolution.add("kingdom", "Bacteria");
        ResultGroupSet<Specimen, String> resultWithName = dao.specimenNameSearch(paramsWithNameResolution);

        // THEN
        assertEquals(1, resultWithName.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_phylum() throws Exception {

        // GIVEN
        setupNameResolutionTest();

        QueryParams paramsWithoutNameResolution = new QueryParams();
        paramsWithoutNameResolution.add("phylum", "wrong value");

        ResultGroupSet<Specimen, String> preResult = dao.specimenNameSearch(paramsWithoutNameResolution);
        assertEquals(0, preResult.getTotalSize());

        // WHEN
        QueryParams paramsWithNameResolution = new QueryParams();
        paramsWithNameResolution.add("phylum", "Proteobacteria");
        ResultGroupSet<Specimen, String> resultWithName = dao.specimenNameSearch(paramsWithNameResolution);

        // THEN
        assertEquals(1, resultWithName.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_class() throws Exception {

        // GIVEN
        setupNameResolutionTest();

        QueryParams paramsWithoutNameResolution = new QueryParams();
        paramsWithoutNameResolution.add("class", "wrong value");

        ResultGroupSet<Specimen, String> preResult = dao.specimenNameSearch(paramsWithoutNameResolution);
        assertEquals(0, preResult.getTotalSize());

        // WHEN
        QueryParams paramsWithNameResolution = new QueryParams();
        paramsWithNameResolution.add("class", "Alphaproteobacteria");
        ResultGroupSet<Specimen, String> resultWithName = dao.specimenNameSearch(paramsWithNameResolution);

        // THEN
        assertEquals(1, resultWithName.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_order() throws Exception {

        // GIVEN
        setupNameResolutionTest();

        QueryParams paramsWithoutNameResolution = new QueryParams();
        paramsWithoutNameResolution.add("order", "wrong value");

        ResultGroupSet<Specimen, String> preResult = dao.specimenNameSearch(paramsWithoutNameResolution);
        assertEquals(0, preResult.getTotalSize());

        // WHEN
        QueryParams paramsWithNameResolution = new QueryParams();
        paramsWithNameResolution.add("order", "Rhodobacterales");
        ResultGroupSet<Specimen, String> resultWithName = dao.specimenNameSearch(paramsWithNameResolution);

        // THEN
        assertEquals(1, resultWithName.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nameResolution_family() throws Exception {

        // GIVEN
        setupNameResolutionTest();

        QueryParams paramsWithoutNameResolution = new QueryParams();
        paramsWithoutNameResolution.add("family", "wrong value");

        ResultGroupSet<Specimen, String> preResult = dao.specimenNameSearch(paramsWithoutNameResolution);
        assertEquals(0, preResult.getTotalSize());

        // WHEN
        QueryParams paramsWithNameResolution = new QueryParams();
        paramsWithNameResolution.add("family", "Rhodobacteraceae");
        ResultGroupSet<Specimen, String> resultWithName = dao.specimenNameSearch(paramsWithNameResolution);

        // THEN
        assertEquals(1, resultWithName.getTotalSize());
    }


    private void setupNameResolutionTest() throws IOException {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_INDEX_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();
        client().admin().indices().preparePutMapping(INDEX_NAME).setType(TAXON_INDEX_TYPE)
                .setSource(getMapping("test-taxon-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        SpecimenIdentification specimenIdentification = new SpecimenIdentification();

        ScientificName scientificName = new ScientificName();
        scientificName.setGenusOrMonomial("geslacht");
        scientificName.setSpecificEpithet("specifiek");
        scientificName.setInfraspecificEpithet("infra");

        // no default classification to avoid direct hit in name resolution via taxons
        specimenIdentification.setScientificName(scientificName);
        esSpecimen.setIdentifications(asList(specimenIdentification));

        client().prepareIndex(INDEX_NAME, SPECIMEN_INDEX_TYPE, "1")
                .setSource(objectMapper.writeValueAsString(esSpecimen)).setRefresh(true).execute().actionGet();

        ESTaxon esTaxon = createTestTaxon();
        esTaxon.setAcceptedName(scientificName);
        esTaxon.getSynonyms().get(0).setGenusOrMonomial("geslacht");
        esTaxon.getSynonyms().get(0).setSpecificEpithet("specifiek");
        esTaxon.getSynonyms().get(0).setInfraspecificEpithet("infra");
        client().prepareIndex(INDEX_NAME, TAXON_INDEX_TYPE, "1")
                .setSource(objectMapper.writeValueAsString(esTaxon)).setRefresh(true).execute().actionGet();

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(2l));
    }

}

