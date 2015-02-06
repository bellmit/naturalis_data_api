package nl.naturalis.nda.elasticsearch.dao.dao;

import nl.naturalis.nda.domain.DefaultClassification;
import nl.naturalis.nda.domain.Specimen;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESGatheringEvent;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESGatheringSiteCoordinates;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESSpecimen;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESTaxon;
import nl.naturalis.nda.elasticsearch.dao.transfer.SpecimenTransfer;
import nl.naturalis.nda.search.*;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static nl.naturalis.nda.elasticsearch.dao.dao.BioportalTaxonDaoTest.createTestTaxon;
import static nl.naturalis.nda.elasticsearch.dao.util.ESConstants.Fields.UNIT_ID;
import static nl.naturalis.nda.elasticsearch.dao.util.ESConstants.*;
import static org.hamcrest.Matchers.is;

public class BioportalSpecimenDaoTest extends AbstractBioportalSpecimenDaoTest {

    @Test
    public void testNewSearch() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE).setSource(getMapping("test-specimen-mapping.json")).execute().actionGet();
        ESSpecimen esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen)).setRefresh(true).execute().actionGet();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen)).setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("unitID", "L  0191413");
        params.add("localityText", "Leiden");
        params.add("gatheringAgent", "Van der Meijer Tussennaam W.");
        params.add("_andOr", "AND");

        SearchResultSet<Specimen> result = dao.specimenSearch(params);
        assertEquals(2, result.getTotalSize());
    }

    @Test
    public void testNewSearch2() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE).setSource(getMapping("test-specimen-mapping.json")).execute().actionGet();
        client().admin().indices().preparePutMapping(INDEX_NAME).setType(TAXON_TYPE).setSource(getMapping("test-taxon-mapping.json")).execute().actionGet();
        ESSpecimen esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen)).setRefresh(true).execute().actionGet();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen)).setRefresh(true).execute().actionGet();

        ESTaxon esTaxon = createTestTaxon();
        DefaultClassification defaultClassification = new DefaultClassification();
        defaultClassification.setKingdom("Plantae");
        esTaxon.setDefaultClassification(defaultClassification);
        esTaxon.setAcceptedName(esSpecimen.getIdentifications().get(0).getScientificName());
        esTaxon.getSynonyms().get(0).setGenusOrMonomial("geslacht");
        esTaxon.getSynonyms().get(0).setSpecificEpithet("specifiek");
        esTaxon.getSynonyms().get(0).setInfraspecificEpithet("infra");
        client().prepareIndex(INDEX_NAME, TAXON_TYPE, "1").setSource(objectMapper.writeValueAsString(esTaxon)).setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("genus", "Xylopia");
        params.add("unitID", "L  0191413");
        params.add("specificEpithet", "ferruginea ");
        params.add("kingdom", "Plantae");
        params.add("_andOr", "AND");

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);
        assertEquals(2, result.getResultGroups().get(0).getTotalSize());
    }

    @Test
    public void textNewNameSearch() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE).setSource(getMapping("test-specimen-mapping.json")).execute().actionGet();
        ESSpecimen esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen)).setRefresh(true).execute().actionGet();

        DefaultClassification classification = esSpecimen.getIdentifications().get(0).getDefaultClassification();
        esSpecimen.setUnitID("L  01914100");
        classification.setKingdom("fake");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen)).setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("kingdom", "Plantae");
        params.add("identifications.scientificName.genusOrMonomial", "Xylopia");
        params.add("_andOr", "OR");

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(2l));

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);

        assertEquals(2, result.getResultGroups().get(0).getTotalSize());
    }

    @Test
    public void testSpecimenSearch() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("gatheringEvent.gatheringPersons.fullName", "Meijer, W.");

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(2l));

        SearchResultSet<Specimen> resultSet = dao.specimenSearch(params);

        SearchResult result1 = resultSet.getSearchResults().get(0);
        List<StringMatchInfo> matchInfo = result1.getMatchInfo();
        assertThat(matchInfo.size(), is(1));
        assertThat(matchInfo.get(0).getValueHighlighted(),
                is("Van der <span class=\"search_hit\">Meijer</span> Tussennaam <span class=\"search_hit\">W</span>."));

        assertEquals(2, resultSet.getTotalSize());
    }

    @Test
    public void testSpecimenSearch_noResultsOnFalseValue() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("gatheringEvent.gatheringPersons.fullName", "fsad_Meijer");

        SearchResultSet<Specimen> resultSet = dao.specimenSearch(params);

        assertEquals(0, resultSet.getTotalSize());
    }

    @Test
    public void testSpecimenSearch_ngram_localityText() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        ESGatheringEvent gatheringEvent = new ESGatheringEvent();
        gatheringEvent.setLocalityText("Hallo lieve vrienden");
        esSpecimen.setGatheringEvent(gatheringEvent);

        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();
        esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("gatheringEvent.localityText", "Hal");

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(2l));

        SearchResultSet<Specimen> resultSet = dao.specimenSearch(params);

        assertEquals(1, resultSet.getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nested_AND_query() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();

        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        DefaultClassification classification = esSpecimen.getIdentifications().get(0).getDefaultClassification();
        esSpecimen.getIdentifications().get(0).getScientificName().setFullScientificName("test");
        classification.setKingdom("fake");
        esSpecimen.setUnitID("L  01914100");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("kingdom", "Plantae");
        params.add("identifications.scientificName.genusOrMonomial", "Xylopia");
        params.add("_andOr", "AND");

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(2l));

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);

        assertEquals(1, result.getResultGroups().get(0).getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nested_OR_query() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE).setSource(getMapping("test-specimen-mapping.json")).execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen)).setRefresh(true).execute().actionGet();

        DefaultClassification classification = esSpecimen.getIdentifications().get(0).getDefaultClassification();
        esSpecimen.setUnitID("L  01914100");
        classification.setKingdom("fake");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen)).setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("kingdom", "Plantae");
        params.add("identifications.scientificName.genusOrMonomial", "Xylopia");
        params.add("_andOr", "OR");

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(2l));

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);

        assertEquals(2, result.getResultGroups().get(0).getTotalSize());
    }

    @Test
    public void testExtendedNameSearch_nonNested_query() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();
        esSpecimen.setUnitID("L  01914100");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("gatheringEvent.dateTimeBegin", "-299725200000");

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(2l));

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);

        assertEquals(2, result.getResultGroups().get(0).getTotalSize());
        assertEquals(1, result.getResultGroups().get(0).getSearchResults().get(0).getLinks().size());
        assertEquals(1, result.getResultGroups().get(0).getSearchResults().get(1).getLinks().size());
    }

    @Test
    public void testExtendedNameSearch_combined_query() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();

        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();
        DefaultClassification classification = esSpecimen.getIdentifications().get(0).getDefaultClassification();
        esSpecimen.setUnitID("L  01914100");
        classification.setKingdom("fake");
        esSpecimen.getIdentifications().get(0).getScientificName().setFullScientificName("test");

        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("gatheringEvent.dateTimeBegin", "-299725200000");
        params.add("kingdom", "Plantae");
        params.add("_andOr", "AND");

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(2l));

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);

        assertEquals(1, result.getResultGroups().get(0).getTotalSize());
    }

    @Test
    public void testGetOtherSpecimensWithSameAssemblageId() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        Specimen specimen = SpecimenTransfer.transfer(esSpecimen);

        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();
        esSpecimen.setUnitID("2");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();
        esSpecimen.setUnitID("3");
        esSpecimen.setAssemblageID("other");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "3").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(3l));

        List<Specimen> otherSpecimensWithSameAssemblageId = dao.getOtherSpecimensWithSameAssemblageId(specimen, null);

        assertEquals(1, otherSpecimensWithSameAssemblageId.size());
        assertEquals("2", otherSpecimensWithSameAssemblageId.get(0).getUnitID());
    }

    @Test
    public void testGeoShapeMultiPolygonQuery() throws Exception {
        createIndex(INDEX_NAME);
        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute()
                .actionGet();

        String denmark = "[[[[11.256033,54.954576],[11.303477,54.93244],[11.321056,54.92772],[11.338878,54.919135],[11.368907,54.88109],[11.392751,54.872504],[11.416759,54.86933],[11.594737,54.811103],[11.585134,54.828518],[11.573578,54.839342],[11.572765,54.847968],[11.594737,54.858873],[11.622081,54.858873],[11.646332,54.865383],[11.656261,54.866278],[11.647227,54.877143],[11.644786,54.889635],[11.646332,54.902289],[11.649425,54.91352],[11.725759,54.873358],[11.731944,54.858873],[11.755626,54.841132],[11.770681,54.834296],[11.787283,54.831529],[11.777192,54.825995],[11.77296,54.824693],[11.787608,54.806871],[11.837738,54.782782],[11.848643,54.773179],[11.851329,54.756822],[11.855479,54.749457],[11.855479,54.743598],[11.845225,54.73192],[11.833832,54.724555],[11.823578,54.722113],[11.814708,54.718695],[11.806977,54.707994],[11.846446,54.697821],[11.842621,54.676215],[11.81072,54.655097],[11.766124,54.646552],[11.610525,54.666409],[11.560069,54.659613],[11.512218,54.646552],[11.474376,54.626125],[11.451834,54.628363],[11.358735,54.660549],[11.297699,54.691555],[11.187755,54.732245],[11.138194,54.738593],[11.084646,54.753119],[11.030528,54.760647],[11.009288,54.771633],[10.999197,54.78677],[11.005707,54.803656],[11.030772,54.812161],[11.06129,54.809312],[11.089692,54.810614],[11.108165,54.831529],[11.09197,54.835028],[11.07838,54.841132],[11.025238,54.879462],[11.018809,54.886176],[11.018809,54.903388],[11.0324,54.919257],[11.073985,54.948188],[11.088145,54.944159],[11.2317,54.961249],[11.256033,54.954576]]],[[[10.34962,54.896715],[10.364268,54.893012],[10.379405,54.893052],[10.391368,54.895453],[10.395844,54.903469],[10.388845,54.920315],[10.403087,54.908393],[10.415212,54.878485],[10.426036,54.872504],[10.446788,54.872382],[10.460948,54.874498],[10.463634,54.882473],[10.450206,54.899807],[10.466645,54.89057],[10.485688,54.883694],[10.499197,54.874579],[10.498709,54.858873],[10.508149,54.854926],[10.518565,54.851996],[10.440115,54.841498],[10.41863,54.827826],[10.406261,54.824693],[10.383474,54.830756],[10.211436,54.939358],[10.19516,54.961859],[10.18922,54.977484],[10.203868,54.968085],[10.288748,54.940741],[10.313731,54.92772],[10.33725,54.905911],[10.34962,54.896715]]],[[[12.537608,55.002387],[12.553722,54.968085],[12.551768,54.95718],[12.542654,54.951158],[12.526215,54.948676],[12.502778,54.948188],[12.384613,54.96308],[12.348399,54.960028],[12.313975,54.950873],[12.28004,54.934556],[12.226817,54.892646],[12.207774,54.886176],[12.178722,54.888983],[12.115001,54.907294],[12.115001,54.91352],[12.129161,54.91352],[12.124685,54.923733],[12.128917,54.931627],[12.138194,54.937242],[12.149099,54.940823],[12.144054,54.943793],[12.134532,54.951606],[12.129161,54.954413],[12.151378,54.968899],[12.164399,54.972642],[12.177013,54.968085],[12.184337,54.975531],[12.177013,54.989203],[12.193126,54.988349],[12.217296,54.978664],[12.232188,54.975531],[12.242198,54.97663],[12.27475,54.984442],[12.286876,54.989203],[12.281505,54.999091],[12.281261,55.007148],[12.285655,55.013129],[12.2942,55.016547],[12.2942,55.023383],[12.261567,55.036811],[12.252696,55.04385],[12.247569,55.05386],[12.250987,55.060736],[12.261974,55.064276],[12.28004,55.064276],[12.301768,55.04206],[12.335297,55.033271],[12.417166,55.030219],[12.504731,55.019761],[12.537608,55.002387]]],[[[9.793468,55.080756],[9.853038,55.040107],[9.884776,55.030219],[9.929047,55.02619],[9.960948,55.015041],[9.986583,54.997748],[10.011404,54.975531],[10.02589,54.959784],[10.065115,54.890815],[10.064464,54.881008],[10.050466,54.878852],[10.021983,54.87934],[10.007091,54.874823],[9.987559,54.865912],[9.968028,54.859524],[9.953624,54.862616],[9.937185,54.870917],[9.891124,54.879299],[9.874278,54.886176],[9.874278,54.893012],[9.90089,54.900702],[9.932302,54.892808],[9.963878,54.87995],[9.991547,54.872504],[9.991547,54.87934],[9.943696,54.903632],[9.898285,54.912991],[9.88795,54.91352],[9.87794,54.911322],[9.863536,54.903062],[9.85377,54.899807],[9.814708,54.901068],[9.780935,54.916815],[9.761241,54.943061],[9.764415,54.975531],[9.758149,54.975531],[9.758149,54.981757],[9.780284,54.972846],[9.804047,54.95718],[9.827403,54.945461],[9.847504,54.948188],[9.843272,54.951728],[9.836681,54.958564],[9.832774,54.965155],[9.836681,54.968085],[9.842133,54.970282],[9.835216,54.975165],[9.793712,54.992174],[9.786794,55.000149],[9.799083,55.009711],[9.799083,55.016547],[9.720551,55.012274],[9.706879,55.013129],[9.696951,55.017524],[9.684825,55.018541],[9.674164,55.021064],[9.668793,55.030219],[9.672374,55.034654],[9.691254,55.042792],[9.696625,55.050686],[9.678233,55.050198],[9.661632,55.046536],[9.645518,55.044867],[9.628429,55.050686],[9.628429,55.058051],[9.716645,55.082221],[9.751964,55.085395],[9.793468,55.080756]]],[[[10.837738,54.937242],[10.741059,54.752387],[10.731293,54.740058],[10.71754,54.735785],[10.693858,54.735297],[10.681895,54.744818],[10.656423,54.803656],[10.645518,54.814602],[10.632091,54.823432],[10.601085,54.838324],[10.625336,54.839301],[10.64088,54.843207],[10.653494,54.85163],[10.669281,54.866278],[10.678559,54.883043],[10.684418,54.887844],[10.693858,54.882758],[10.698904,54.881171],[10.724457,54.886176],[10.722179,54.896064],[10.717784,54.907294],[10.708995,54.902981],[10.700369,54.901557],[10.691905,54.902981],[10.682953,54.907294],[10.697032,54.931342],[10.718272,54.949164],[10.774099,54.986558],[10.798188,54.996568],[10.854991,55.04385],[10.86199,55.072089],[10.895518,55.121527],[10.934255,55.159654],[10.956716,55.153713],[10.908946,55.030219],[10.889903,54.997382],[10.837738,54.937242]]],[[[14.889008,55.233222],[14.926799,55.215799],[14.954354,55.219254],[14.973399,55.216132],[14.989757,55.196031],[15.000499,55.188422],[15.012869,55.183824],[15.084543,55.154579],[15.131602,55.145453],[15.151378,55.133205],[15.146658,55.128241],[15.144542,55.124823],[15.142345,55.122138],[15.137706,55.119534],[15.14503,55.104885],[15.147716,55.095608],[15.144705,55.087104],[15.134288,55.074856],[15.112804,55.054633],[15.106619,55.044664],[15.110362,55.036363],[15.110362,55.030219],[15.113242,55.018295],[15.097353,55.007823],[15.086686,54.999213],[15.072666,54.991638],[15.023664,54.999339],[14.98287,55.001013],[14.927265,55.013692],[14.858607,55.038226],[14.780833,55.051128],[14.699555,55.089179],[14.684173,55.101347],[14.697045,55.122654],[14.701427,55.1647],[14.70574,55.225653],[14.709239,55.235826],[14.71754,55.246772],[14.737071,55.266669],[14.745128,55.281562],[14.752126,55.298774],[14.760916,55.309272],[14.774587,55.3039],[14.814952,55.270413],[14.828949,55.261705],[14.851422,55.2482],[14.889008,55.233222]]],[[[8.411957,55.438707],[8.428559,55.433539],[8.448904,55.435533],[8.46339,55.427436],[8.463064,55.415717],[8.450206,55.393378],[8.452891,55.383368],[8.46046,55.372382],[8.467459,55.353705],[8.47641,55.345445],[8.451671,55.339748],[8.425304,55.355536],[8.402354,55.380032],[8.370942,55.427436],[8.364268,55.449042],[8.37436,55.463284],[8.408214,55.468411],[8.411957,55.438707]]],[[[10.33839,55.610093],[10.4074,55.582953],[10.423025,55.572008],[10.435395,55.559719],[10.444184,55.553168],[10.453624,55.550279],[10.471934,55.549018],[10.488536,55.545478],[10.518809,55.533433],[10.502452,55.537177],[10.486827,55.537177],[10.475271,55.531968],[10.470714,55.520209],[10.474376,55.515815],[10.489106,55.509101],[10.491222,55.503119],[10.486013,55.494778],[10.477794,55.492499],[10.467784,55.492133],[10.457042,55.489447],[10.419607,55.458808],[10.436046,55.442125],[10.471528,55.443061],[10.491222,55.465277],[10.502452,55.463568],[10.566905,55.482652],[10.604747,55.489447],[10.609141,55.49258],[10.607188,55.499416],[10.602306,55.506252],[10.597667,55.509345],[10.588715,55.510688],[10.579112,55.514106],[10.571056,55.518704],[10.566905,55.523627],[10.569835,55.532416],[10.582286,55.52912],[10.595551,55.521389],[10.601085,55.516791],[10.61378,55.527899],[10.610525,55.543647],[10.600841,55.561225],[10.594249,55.578274],[10.599132,55.573676],[10.614594,55.564602],[10.615001,55.581448],[10.608409,55.606269],[10.608572,55.612982],[10.627615,55.613593],[10.656912,55.594875],[10.704112,55.550279],[10.706065,55.543891],[10.707042,55.53498],[10.709239,55.527045],[10.714366,55.523627],[10.716807,55.52143],[10.738129,55.509345],[10.744477,55.495551],[10.742361,55.487494],[10.733084,55.481879],[10.703949,55.471137],[10.657481,55.459703],[10.58839,55.463365],[10.5713,55.461249],[10.559418,55.455308],[10.554047,55.442572],[10.563324,55.436225],[10.579438,55.435858],[10.594249,55.441067],[10.58961,55.446234],[10.588552,55.449408],[10.595958,55.459662],[10.615733,55.456855],[10.682953,55.447903],[10.697927,55.440253],[10.796641,55.358547],[10.803071,55.349758],[10.82838,55.306586],[10.834321,55.290269],[10.826915,55.290269],[10.815115,55.300849],[10.796886,55.307685],[10.780284,55.304918],[10.77296,55.287095],[10.778656,55.274848],[10.805349,55.250434],[10.813324,55.235582],[10.815196,55.214057],[10.812673,55.194281],[10.806,55.177395],[10.790782,55.156928],[10.787852,55.149482],[10.786632,55.129462],[10.783376,55.124213],[10.7588,55.105292],[10.74936,55.093085],[10.742686,55.081692],[10.733653,55.071845],[10.717784,55.064276],[10.705577,55.062567],[10.66627,55.064276],[10.62143,55.064276],[10.604747,55.062567],[10.586111,55.057522],[10.568858,55.049872],[10.556407,55.040107],[10.538259,55.029731],[10.516856,55.031317],[10.47755,55.04385],[10.410655,55.046942],[10.388845,55.050686],[10.379161,55.054511],[10.371349,55.059027],[10.362559,55.06273],[10.350922,55.064276],[10.325938,55.062893],[10.312755,55.063666],[10.234223,55.092231],[10.205251,55.093695],[10.197602,55.086086],[10.197032,55.064276],[10.181895,55.070868],[10.158376,55.086656],[10.141775,55.092231],[10.121267,55.092475],[10.083507,55.086982],[10.065929,55.092231],[10.142589,55.1258],[10.156016,55.140082],[10.138682,55.154283],[10.12615,55.17064],[10.112966,55.183783],[10.094005,55.188422],[10.078949,55.18594],[10.067556,55.182278],[10.055837,55.179755],[10.039317,55.180976],[10.016612,55.194281],[10.003591,55.197333],[9.991547,55.188422],[9.994477,55.172797],[10.029145,55.142401],[10.025645,55.125718],[10.020681,55.124457],[9.985525,55.127753],[9.982921,55.130601],[9.984386,55.13467],[9.984141,55.140082],[9.98699,55.13935],[9.989757,55.143378],[9.98878,55.151842],[9.974294,55.174018],[9.972016,55.184719],[9.974294,55.195258],[9.980724,55.204901],[9.979503,55.212836],[9.964122,55.218166],[9.924001,55.223944],[9.911388,55.228502],[9.900157,55.234117],[9.895356,55.239],[9.89324,55.244452],[9.883962,55.253485],[9.881684,55.259508],[9.883962,55.265611],[9.89324,55.274644],[9.895356,55.28026],[9.89324,55.291246],[9.883962,55.308783],[9.881684,55.314439],[9.884776,55.348863],[9.874522,55.353339],[9.858897,55.354071],[9.829845,55.352281],[9.81658,55.355211],[9.802419,55.362372],[9.778087,55.380276],[9.794932,55.383857],[9.81365,55.385159],[9.830089,55.388902],[9.840099,55.400092],[9.785492,55.413764],[9.793712,55.412787],[9.819591,55.413764],[9.809255,55.425482],[9.791026,55.43183],[9.751313,55.434882],[9.737315,55.438178],[9.703461,55.462144],[9.703461,55.468411],[9.730235,55.461493],[9.758474,55.447496],[9.786306,55.437201],[9.812185,55.441067],[9.71339,55.491034],[9.67628,55.495673],[9.68393,55.506049],[9.693533,55.510199],[9.704763,55.512397],[9.717784,55.516791],[9.755138,55.544135],[9.809093,55.550279],[9.827403,55.548407],[9.830821,55.543118],[9.831309,55.534735],[9.840099,55.523627],[9.888194,55.508694],[9.940278,55.519232],[10.039317,55.557766],[10.16863,55.582261],[10.212657,55.600653],[10.237315,55.605536],[10.264171,55.606635],[10.272146,55.605536],[10.272797,55.602525],[10.279063,55.588324],[10.279552,55.58511],[10.29363,55.58808],[10.29713,55.595649],[10.293793,55.616116],[10.307628,55.618232],[10.33839,55.610093]]],[[[12.577973,55.554023],[12.556326,55.550482],[12.534841,55.569322],[12.520518,55.596747],[12.520193,55.619208],[12.531016,55.631903],[12.543956,55.640367],[12.556814,55.646959],[12.567393,55.653998],[12.578136,55.666449],[12.595063,55.691352],[12.608409,55.701117],[12.612559,55.695136],[12.614106,55.69245],[12.615733,55.688137],[12.673839,55.605536],[12.674571,55.601264],[12.639415,55.578274],[12.624197,55.575629],[12.605724,55.569281],[12.588878,55.561347],[12.577973,55.554023]]],[[[10.62143,55.879869],[10.638682,55.866116],[10.650645,55.870103],[10.65919,55.880764],[10.66627,55.886705],[10.669281,55.880032],[10.65504,55.865383],[10.628266,55.845771],[10.623302,55.827704],[10.62436,55.794338],[10.614594,55.776842],[10.582205,55.759752],[10.54713,55.765204],[10.520274,55.78856],[10.511567,55.825263],[10.517263,55.845526],[10.52768,55.853909],[10.542166,55.85814],[10.559418,55.866278],[10.57309,55.878852],[10.581065,55.894517],[10.580251,55.911526],[10.566905,55.92829],[10.548595,55.935736],[10.528331,55.940619],[10.515391,55.951239],[10.518565,55.976142],[10.528819,55.989203],[10.543956,55.997992],[10.556814,55.995795],[10.56422,55.953843],[10.580251,55.93651],[10.602794,55.925238],[10.628266,55.920844],[10.619395,55.910956],[10.615408,55.900336],[10.616384,55.889797],[10.62143,55.879869]]],[[[12.328461,56.126899],[12.414887,56.099026],[12.486095,56.099026],[12.505382,56.093166],[12.540375,56.075385],[12.570079,56.068264],[12.621918,56.043769],[12.608409,56.028022],[12.567393,55.996568],[12.552745,55.979682],[12.525564,55.939887],[12.512706,55.92829],[12.512706,55.920844],[12.540294,55.898261],[12.554454,55.88288],[12.560557,55.86994],[12.565115,55.851142],[12.57602,55.829169],[12.589692,55.809475],[12.602224,55.797349],[12.597992,55.787584],[12.595876,55.774237],[12.594737,55.752997],[12.589041,55.735745],[12.587901,55.729071],[12.589203,55.72427],[12.592784,55.719143],[12.602224,55.708564],[12.581228,55.693061],[12.541026,55.648871],[12.503266,55.634019],[12.496755,55.621772],[12.487153,55.609849],[12.465017,55.605536],[12.448578,55.608873],[12.436778,55.614407],[12.423676,55.618842],[12.403575,55.619208],[12.331309,55.595404],[12.312022,55.58397],[12.248383,55.546251],[12.24822,55.546129],[12.19809,55.487494],[12.224864,55.434882],[12.273448,55.410224],[12.290538,55.406928],[12.352061,55.404446],[12.368826,55.400092],[12.41212,55.379136],[12.434744,55.363674],[12.444591,55.348863],[12.4463,55.331692],[12.450857,55.317206],[12.465017,55.290269],[12.450206,55.27969],[12.444591,55.276557],[12.392426,55.255316],[12.182791,55.2272],[12.167003,55.217353],[12.141368,55.207994],[12.121104,55.191067],[12.10906,55.170722],[12.115001,55.153713],[12.107432,55.145087],[12.098969,55.141425],[12.090099,55.142239],[12.08074,55.146877],[12.088145,55.168362],[12.076671,55.181952],[12.056407,55.185696],[12.036794,55.177558],[12.027843,55.172675],[12.02003,55.170966],[12.014659,55.167467],[12.012462,55.157131],[12.017914,55.153022],[12.046641,55.140082],[12.130544,55.135891],[12.16863,55.128485],[12.177013,55.105292],[12.171397,55.095445],[12.16505,55.091295],[12.157725,55.089179],[12.149099,55.085395],[12.128429,55.078843],[12.1185,55.073554],[12.125743,55.071112],[12.163341,55.023383],[12.162283,55.004381],[12.148936,54.994127],[12.128429,54.989976],[12.104991,54.989203],[12.089529,54.985663],[12.055919,54.970771],[12.03297,54.968085],[11.998057,54.976508],[11.943858,55.00609],[11.910167,55.009711],[11.909516,55.00495],[11.911143,55.003811],[11.913829,55.003852],[11.91684,55.002875],[11.911388,55.000922],[11.908376,54.998603],[11.904633,54.996568],[11.897146,54.995429],[11.901541,54.976386],[11.894379,54.964993],[11.88502,54.955146],[11.882823,54.940823],[11.890147,54.929633],[11.903331,54.923],[11.918468,54.921942],[11.931163,54.92772],[11.928559,54.931383],[11.923676,54.940823],[11.932384,54.941474],[11.957856,54.948188],[11.965994,54.942613],[11.980235,54.924262],[11.982188,54.920315],[11.991466,54.916327],[12.023936,54.898017],[12.040375,54.893012],[12.05893,54.894355],[12.078298,54.897854],[12.096446,54.898098],[12.111827,54.889594],[12.139903,54.864936],[12.170177,54.84455],[12.170177,54.838324],[12.081716,54.796454],[12.067149,54.786811],[12.028005,54.743557],[12.022716,54.735297],[12.012706,54.731106],[11.98585,54.712307],[11.975271,54.707994],[11.960704,54.694078],[11.960216,54.66234],[11.971446,54.605618],[11.969005,54.56977],[11.949067,54.56859],[11.928233,54.58397],[11.923676,54.598131],[11.909353,54.609524],[11.876638,54.646959],[11.869151,54.659613],[11.870616,54.680365],[11.88266,54.696438],[11.910167,54.721666],[11.889903,54.724189],[11.875824,54.729722],[11.872325,54.739936],[11.882823,54.756415],[11.87143,54.760321],[11.865245,54.765448],[11.855479,54.776923],[11.809093,54.804918],[11.805431,54.809516],[11.800955,54.817857],[11.798839,54.825873],[11.800059,54.831204],[11.799815,54.836575],[11.793468,54.84455],[11.776052,54.856147],[11.76059,54.863227],[11.749685,54.873114],[11.745616,54.893012],[11.745372,54.911322],[11.742035,54.925442],[11.731944,54.935289],[11.711436,54.940823],[11.711436,54.948188],[11.749766,54.965725],[11.759288,54.968085],[11.773611,54.965033],[11.792979,54.951321],[11.803966,54.948188],[11.814789,54.952094],[11.837576,54.966132],[11.841807,54.964667],[11.848318,54.959133],[11.862478,54.96369],[11.882823,54.975531],[11.882823,54.981757],[11.872325,54.989569],[11.853526,55.020087],[11.841807,55.030219],[11.774669,55.04857],[11.755626,55.050686],[11.750255,55.052802],[11.73699,55.062201],[11.72877,55.064276],[11.665538,55.066392],[11.646658,55.072333],[11.630382,55.081122],[11.615896,55.092231],[11.669607,55.085639],[11.775238,55.05744],[11.827485,55.050686],[11.805349,55.073879],[11.743419,55.100531],[11.717621,55.125718],[11.803966,55.134833],[11.809581,55.138861],[11.806977,55.146877],[11.785411,55.157213],[11.728526,55.154771],[11.711436,55.167914],[11.726248,55.176947],[11.734711,55.188666],[11.733084,55.200141],[11.717621,55.208319],[11.695079,55.208075],[11.678233,55.199368],[11.663829,55.1883],[11.649425,55.180976],[11.636241,55.181627],[11.599376,55.191311],[11.524181,55.202094],[11.506033,55.208319],[11.492361,55.204169],[11.423188,55.22191],[11.41033,55.22012],[11.382986,55.211168],[11.368907,55.208319],[11.299978,55.208319],[11.299978,55.201483],[11.320486,55.194648],[11.285899,55.19538],[11.260997,55.200588],[11.245372,55.214789],[11.238536,55.242418],[11.253103,55.236274],[11.267833,55.235907],[11.28004,55.242336],[11.286306,55.25609],[11.276052,55.251614],[11.265391,55.249457],[11.241954,55.248684],[11.23878,55.254136],[11.241547,55.27851],[11.235118,55.284003],[11.217133,55.290229],[11.16212,55.319159],[11.152843,55.328437],[11.161957,55.330878],[11.176443,55.331855],[11.188243,55.335273],[11.190115,55.345445],[11.182302,55.350409],[11.167979,55.352769],[11.152599,55.353217],[11.142345,55.352281],[11.135427,55.346869],[11.13087,55.338446],[11.123546,55.331855],[11.108165,55.331855],[11.101899,55.33633],[11.087738,55.358547],[11.087738,55.365953],[11.114594,55.364407],[11.128917,55.36518],[11.138927,55.369371],[11.147716,55.375922],[11.15919,55.381293],[11.171723,55.385077],[11.183279,55.38642],[11.204356,55.391832],[11.214203,55.403795],[11.213145,55.415758],[11.200938,55.42121],[11.204845,55.431342],[11.196462,55.453843],[11.176443,55.489447],[11.16391,55.501899],[11.151134,55.510321],[11.13559,55.515123],[11.115001,55.516791],[11.097179,55.509508],[11.085785,55.507554],[11.080903,55.513088],[11.082774,55.526557],[11.088552,55.53384],[11.098155,55.536689],[11.111583,55.537299],[11.13795,55.542914],[11.148448,55.557318],[11.145681,55.576117],[11.132091,55.595282],[11.087901,55.626939],[11.039236,55.643459],[10.929942,55.660142],[10.929942,55.667629],[11.087738,55.660142],[11.071056,55.676947],[11.008637,55.692288],[10.98463,55.701117],[10.974294,55.710842],[10.968272,55.718411],[10.960216,55.724351],[10.943614,55.729071],[10.891449,55.731269],[10.875499,55.736518],[10.875499,55.742743],[11.033051,55.729071],[11.065684,55.732245],[11.128184,55.746324],[11.162771,55.749498],[11.162771,55.742743],[11.151703,55.743638],[11.141124,55.743232],[11.13087,55.741034],[11.121837,55.736518],[11.137543,55.719062],[11.163422,55.70539],[11.192638,55.700344],[11.218028,55.708564],[11.176443,55.729071],[11.190115,55.736518],[11.199474,55.731106],[11.216156,55.728746],[11.252208,55.729071],[11.268403,55.732733],[11.302419,55.746527],[11.336681,55.750922],[11.352224,55.755805],[11.364024,55.765123],[11.368907,55.779975],[11.369965,55.787787],[11.374685,55.801581],[11.375662,55.807929],[11.372081,55.815863],[11.354666,55.830227],[11.348399,55.838324],[11.373871,55.828762],[11.398448,55.826402],[11.421723,55.831977],[11.44337,55.845771],[11.47755,55.841539],[11.505219,55.86872],[11.510509,55.908271],[11.477387,55.941352],[11.448416,55.948188],[11.384288,55.950832],[11.304535,55.967841],[11.281912,55.978949],[11.272634,55.996568],[11.400564,55.961982],[11.426931,55.965522],[11.581065,55.95185],[11.599376,55.942084],[11.64088,55.943427],[11.711436,55.955634],[11.747813,55.968248],[11.768321,55.970526],[11.779796,55.961859],[11.775401,55.95185],[11.747325,55.932807],[11.73878,55.920844],[11.750499,55.910386],[11.743988,55.903754],[11.727875,55.902289],[11.711436,55.907172],[11.713552,55.910834],[11.717621,55.920844],[11.685802,55.919867],[11.672862,55.91592],[11.663097,55.907172],[11.712657,55.8581],[11.731619,55.830715],[11.717621,55.810981],[11.702403,55.809475],[11.671235,55.814276],[11.656261,55.810981],[11.63795,55.796088],[11.626231,55.790595],[11.607758,55.790473],[11.607758,55.783026],[11.624278,55.780015],[11.673595,55.783026],[11.687022,55.786444],[11.70281,55.793402],[11.720388,55.798651],[11.73878,55.797349],[11.745453,55.789374],[11.75587,55.772935],[11.763682,55.756822],[11.762706,55.749498],[11.682302,55.741278],[11.664561,55.737128],[11.657237,55.730862],[11.669932,55.722846],[11.684337,55.721625],[11.729259,55.723578],[11.73878,55.725979],[11.746349,55.731024],[11.763031,55.721381],[11.787283,55.701117],[11.794444,55.687934],[11.793793,55.68301],[11.787934,55.676947],[11.779796,55.660142],[11.797618,55.669623],[11.810557,55.679104],[11.852306,55.730902],[11.854421,55.732164],[11.855479,55.732815],[11.853282,55.739976],[11.844005,55.752672],[11.841807,55.756415],[11.844981,55.76789],[11.852712,55.782294],[11.869151,55.804755],[11.884044,55.813666],[11.919444,55.819403],[11.937348,55.825263],[11.953624,55.853502],[11.933116,55.886135],[11.909516,55.915473],[11.91684,55.934516],[11.934744,55.933661],[11.969981,55.912584],[11.988617,55.907172],[11.99822,55.902818],[11.997895,55.892768],[11.993826,55.881537],[11.992035,55.873725],[11.996349,55.86107],[12.000173,55.856513],[12.004893,55.853461],[12.012462,55.845771],[12.031261,55.816596],[12.049001,55.775336],[12.051931,55.73664],[12.026134,55.715399],[11.990001,55.722805],[11.972667,55.721422],[11.965343,55.704901],[11.958181,55.693427],[11.923025,55.67414],[11.910167,55.660142],[11.930431,55.65762],[11.959158,55.665107],[11.982188,55.6789],[11.985199,55.694892],[12.00115,55.698676],[12.017589,55.695705],[12.031912,55.687201],[12.040375,55.674465],[12.009288,55.680487],[12.005138,55.678168],[12.011404,55.669013],[12.026622,55.662055],[12.060313,55.653998],[12.069102,55.674954],[12.092052,55.706732],[12.095063,55.729071],[12.053477,55.838324],[12.053477,55.873725],[12.049815,55.888007],[12.026134,55.941352],[12.01059,55.955268],[11.992442,55.960598],[11.970714,55.959866],[11.894298,55.942288],[11.865489,55.939358],[11.848643,55.948188],[11.865571,55.972724],[11.917328,55.997626],[12.012462,56.030707],[12.165375,56.103705],[12.245128,56.12873],[12.328461,56.126899]]],[[[11.581065,56.68122],[11.56422,56.677436],[11.543712,56.678656],[11.524587,56.684719],[11.512218,56.695461],[11.509532,56.71015],[11.520844,56.715399],[11.60613,56.720404],[11.649425,56.729641],[11.649425,56.722154],[11.645193,56.721991],[11.641938,56.720649],[11.635753,56.715969],[11.618012,56.71133],[11.604259,56.701158],[11.59254,56.689846],[11.581065,56.68122]]],[[[8.911388,56.956122],[8.908458,56.948717],[8.910411,56.944403],[8.920421,56.930487],[8.922699,56.922024],[8.920421,56.919908],[8.908458,56.887274],[8.900401,56.874823],[8.895274,56.870307],[8.88795,56.866848],[8.887706,56.87873],[8.88266,56.886379],[8.875011,56.891099],[8.867442,56.89411],[8.843761,56.88345],[8.83961,56.879828],[8.836436,56.869289],[8.839041,56.86286],[8.843598,56.858222],[8.847016,56.853095],[8.855154,56.81977],[8.863943,56.809516],[8.881114,56.805365],[8.874766,56.80036],[8.868419,56.798082],[8.861339,56.797553],[8.853282,56.797838],[8.860688,56.794908],[8.867442,56.791083],[8.854177,56.779934],[8.839122,56.75727],[8.825857,56.743842],[8.812673,56.734565],[8.771251,56.715969],[8.766124,56.698717],[8.686371,56.686591],[8.658376,56.678127],[8.648448,56.681057],[8.621755,56.713935],[8.61085,56.722154],[8.586274,56.736477],[8.562266,56.743232],[8.555919,56.743842],[8.524913,56.737291],[8.514659,56.738227],[8.518077,56.750067],[8.522797,56.755072],[8.530284,56.759345],[8.539236,56.762519],[8.548676,56.763739],[8.553233,56.768134],[8.551606,56.777777],[8.548025,56.787258],[8.545258,56.791083],[8.573009,56.802558],[8.643809,56.808051],[8.668793,56.825832],[8.656261,56.825141],[8.643809,56.825873],[8.632009,56.82807],[8.621104,56.832017],[8.62672,56.841539],[8.646739,56.85635],[8.655121,56.866848],[8.651866,56.866034],[8.64975,56.874254],[8.648448,56.890692],[8.651622,56.894192],[8.659353,56.894477],[8.668224,56.893785],[8.675059,56.89411],[8.703787,56.900051],[8.762462,56.905422],[8.791759,56.914537],[8.81365,56.910346],[8.833751,56.92475],[8.867442,56.962958],[8.890391,56.95482],[8.90504,56.96601],[8.91627,56.979071],[8.928884,56.97663],[8.923106,56.968573],[8.911388,56.956122]]],[[[11.190115,57.325507],[11.197276,57.313381],[11.196502,57.302821],[11.189113,57.294283],[11.166876,57.296775],[11.143231,57.301351],[11.06658,57.291327],[11.082693,57.283881],[11.094165,57.268996],[11.091101,57.252784],[11.067166,57.238362],[11.053517,57.226202],[11.045138,57.212715],[11.028835,57.201914],[10.997905,57.19791],[10.977962,57.206733],[10.951815,57.23373],[10.916537,57.237369],[10.888799,57.245344],[10.871104,57.254625],[10.854991,57.264635],[10.854991,57.270901],[10.888425,57.274865],[10.921263,57.29228],[10.946601,57.304643],[10.98687,57.305993],[11.010084,57.309873],[11.027723,57.320691],[11.101927,57.320417],[11.142345,57.332343],[11.169932,57.332709],[11.181488,57.330308],[11.190115,57.325507]]],[[[10.46046,57.630561],[10.430431,57.570543],[10.458344,57.525458],[10.508962,57.484036],[10.54656,57.435289],[10.517426,57.391303],[10.517914,57.339179],[10.539073,57.236721],[10.521983,57.221747],[10.470876,57.201158],[10.446788,57.184882],[10.411469,57.144436],[10.395518,57.117255],[10.384044,57.074205],[10.359223,57.026109],[10.347179,57.010199],[10.338715,57.003241],[10.330577,56.999335],[10.319672,56.997504],[10.272472,56.99787],[10.257986,56.995836],[10.239757,56.992825],[10.156016,57.02383],[10.094005,57.060492],[10.052745,57.069729],[10.019379,57.088446],[9.99464,57.092719],[9.974457,57.086493],[9.946137,57.061184],[9.926931,57.05858],[9.945486,57.057318],[9.966563,57.072211],[9.984141,57.079047],[10.001964,57.083889],[10.022634,57.079088],[10.121267,57.022121],[10.153982,57.013251],[10.192393,56.994045],[10.214366,56.989691],[10.295095,56.988593],[10.313731,56.982856],[10.285655,56.96369],[10.279552,56.955512],[10.27475,56.942288],[10.269298,56.915025],[10.26588,56.907701],[10.273123,56.891343],[10.284028,56.840318],[10.288341,56.800116],[10.295421,56.778876],[10.319998,56.736477],[10.328461,56.729804],[10.338227,56.723944],[10.340587,56.717353],[10.326834,56.708564],[10.318696,56.708238],[10.295177,56.714301],[10.282563,56.715969],[10.237559,56.715969],[10.221853,56.712104],[10.203868,56.702338],[10.187022,56.712226],[10.168142,56.720038],[10.146495,56.722357],[10.121267,56.715969],[10.098969,56.706122],[10.08961,56.704047],[10.073497,56.702338],[10.05836,56.698228],[10.027517,56.684272],[9.990896,56.676947],[9.945567,56.658149],[9.805919,56.64704],[9.805919,56.640204],[9.851329,56.635728],[10.001475,56.661322],[10.034923,56.672838],[10.049571,56.674994],[10.052908,56.677151],[10.053477,56.68183],[10.055431,56.68651],[10.062836,56.688666],[10.070974,56.687486],[10.084321,56.682359],[10.090587,56.68122],[10.101329,56.682685],[10.113617,56.686713],[10.12379,56.692328],[10.128103,56.698879],[10.134776,56.712388],[10.150564,56.714423],[10.18336,56.708564],[10.180837,56.704901],[10.175792,56.695461],[10.193207,56.69359],[10.241954,56.688381],[10.283539,56.689765],[10.310313,56.698879],[10.334239,56.703843],[10.352794,56.68183],[10.357677,56.648749],[10.340343,56.620347],[10.27296,56.591702],[10.223318,56.560614],[10.214366,56.558295],[10.207286,56.525784],[10.203868,56.516669],[10.211436,56.505316],[10.206798,56.490953],[10.195486,56.477688],[10.18336,56.46955],[10.214122,56.470445],[10.225597,56.475735],[10.224376,56.513617],[10.22755,56.541083],[10.237641,56.554389],[10.279552,56.571967],[10.312673,56.596625],[10.330333,56.603217],[10.350922,56.595893],[10.354503,56.589545],[10.359874,56.568101],[10.36085,56.561713],[10.365245,56.558417],[10.394867,56.544664],[10.4699,56.520901],[10.551524,56.51557],[10.789073,56.536363],[10.822276,56.534247],[10.854991,56.524156],[10.869884,56.514716],[10.90211,56.483222],[10.964122,56.448432],[10.964122,56.442206],[10.940929,56.429511],[10.928477,56.40648],[10.923513,56.379869],[10.922618,56.356269],[10.911632,56.334052],[10.819591,56.260932],[10.77296,56.242987],[10.755382,56.229315],[10.750011,56.216051],[10.752615,56.184963],[10.738617,56.154527],[10.707042,56.150865],[10.672211,56.165188],[10.648936,56.188381],[10.674653,56.190823],[10.685232,56.196234],[10.69044,56.208889],[10.687673,56.222235],[10.677582,56.226304],[10.664561,56.227607],[10.652599,56.232733],[10.628591,56.237535],[10.60377,56.221015],[10.581228,56.199205],[10.549164,56.178209],[10.556163,56.154934],[10.569347,56.129462],[10.573741,56.112616],[10.563975,56.105618],[10.548025,56.101264],[10.531016,56.100165],[10.518565,56.102729],[10.508474,56.111029],[10.498546,56.130194],[10.491222,56.13996],[10.499197,56.144721],[10.516124,56.151597],[10.525401,56.153632],[10.51824,56.164781],[10.509451,56.170111],[10.46811,56.177069],[10.458995,56.176093],[10.436697,56.167914],[10.408702,56.164862],[10.391449,56.172675],[10.376231,56.186672],[10.354666,56.202053],[10.376964,56.203315],[10.410411,56.218492],[10.42921,56.22248],[10.44337,56.21662],[10.460704,56.206204],[10.477794,56.202338],[10.491222,56.216295],[10.474294,56.232245],[10.475434,56.244696],[10.504731,56.277777],[10.491222,56.277655],[10.476817,56.280911],[10.463634,56.286851],[10.453624,56.294501],[10.442638,56.298651],[10.416759,56.291897],[10.403005,56.298245],[10.399262,56.284857],[10.393809,56.276516],[10.38502,56.272162],[10.371104,56.270941],[10.36378,56.26557],[10.336925,56.232733],[10.312511,56.215155],[10.258474,56.190253],[10.234223,56.171332],[10.220225,56.147895],[10.226899,56.133246],[10.243419,56.119574],[10.259044,56.099026],[10.265636,56.073717],[10.261485,56.054429],[10.249197,56.036933],[10.23113,56.017035],[10.255138,56.023627],[10.272716,56.024848],[10.277517,56.017768],[10.252452,55.986233],[10.250011,55.973456],[10.251638,55.948188],[10.250743,55.930732],[10.249278,55.923041],[10.245372,55.914618],[10.221365,55.896918],[10.21754,55.889838],[10.212657,55.889879],[10.201834,55.885891],[10.190196,55.879706],[10.18336,55.873725],[10.188324,55.871161],[10.195567,55.861314],[10.199392,55.850735],[10.190603,55.843492],[10.187348,55.838609],[10.18214,55.833686],[10.172699,55.831488],[10.160411,55.833319],[10.151052,55.838121],[10.135509,55.851996],[10.131033,55.858466],[10.128103,55.865871],[10.12379,55.873277],[10.114513,55.879869],[10.101817,55.882392],[10.004649,55.879869],[9.99171,55.877346],[9.983653,55.871405],[9.977387,55.864569],[9.97047,55.859442],[9.944509,55.853583],[9.891775,55.855373],[9.867442,55.851996],[9.892589,55.836249],[10.035655,55.818427],[10.045746,55.813951],[10.042166,55.803412],[10.025645,55.783026],[10.017345,55.76557],[10.019786,55.75788],[10.033702,55.756049],[10.059825,55.756415],[10.059825,55.749498],[10.018809,55.736274],[10.006847,55.725043],[10.01824,55.708564],[10.014496,55.706855],[10.005219,55.701117],[9.9817,55.709418],[9.871837,55.69123],[9.821788,55.675971],[9.792247,55.674465],[9.730968,55.688137],[9.695649,55.705797],[9.686534,55.708564],[9.583018,55.708889],[9.559581,55.715399],[9.556407,55.711575],[9.554535,55.708808],[9.553477,55.705797],[9.552745,55.701117],[9.573578,55.695461],[9.645518,55.694892],[9.658539,55.690375],[9.710297,55.660142],[9.720958,55.648261],[9.728201,55.637519],[9.738292,55.629706],[9.758149,55.626654],[9.85377,55.626654],[9.837413,55.61522],[9.785492,55.591946],[9.75766,55.571601],[9.744151,55.566352],[9.720876,55.564602],[9.712413,55.560289],[9.710623,55.55093],[9.710297,55.541571],[9.706879,55.537299],[9.643891,55.528306],[9.621104,55.518948],[9.60727,55.516791],[9.595225,55.518378],[9.572765,55.523912],[9.559581,55.523627],[9.562185,55.519517],[9.566417,55.516791],[9.551931,55.505113],[9.514903,55.498196],[9.498057,55.489447],[9.515636,55.48314],[9.534434,55.483832],[9.583669,55.491156],[9.592784,55.494289],[9.602387,55.495795],[9.660167,55.477973],[9.664236,55.466702],[9.659923,55.45303],[9.648936,55.441067],[9.635265,55.435736],[9.60141,55.43183],[9.586762,55.427436],[9.592052,55.416327],[9.60434,55.40119],[9.607921,55.393256],[9.605154,55.382025],[9.599294,55.373358],[9.600841,55.365912],[9.621104,55.358547],[9.633474,55.351996],[9.645274,55.342719],[9.648285,55.332913],[9.634613,55.325019],[9.634613,55.317572],[9.644867,55.31094],[9.681895,55.274644],[9.694184,55.271633],[9.706065,55.265082],[9.710297,55.248684],[9.706554,55.237006],[9.699474,55.229804],[9.692882,55.221015],[9.689789,55.204901],[9.684418,55.197577],[9.67156,55.19245],[9.655772,55.189439],[9.6421,55.188422],[9.583344,55.194648],[9.569835,55.192532],[9.561534,55.187201],[9.554942,55.180569],[9.545909,55.17414],[9.521821,55.165432],[9.507172,55.161933],[9.494395,55.160549],[9.486339,55.156236],[9.486339,55.147406],[9.49464,55.140082],[9.511892,55.140082],[9.511892,55.133205],[9.482432,55.133775],[9.468761,55.131781],[9.456554,55.125718],[9.466645,55.121405],[9.479177,55.119696],[9.508067,55.119534],[9.521332,55.115871],[9.535818,55.106879],[9.559581,55.085395],[9.533702,55.065823],[9.516612,55.056545],[9.498057,55.050686],[9.452403,55.045356],[9.434337,55.037543],[9.442882,55.023383],[9.455251,55.023179],[9.511892,55.036363],[9.537364,55.035386],[9.545909,55.036363],[9.553233,55.039618],[9.560232,55.04442],[9.568533,55.048774],[9.579926,55.050686],[9.601573,55.045396],[9.641856,55.019721],[9.662608,55.009711],[9.71754,55.001166],[9.730968,54.995429],[9.738617,54.980862],[9.760753,54.904975],[9.763194,54.900458],[9.761567,54.897406],[9.751964,54.893012],[9.737071,54.892768],[9.722992,54.89765],[9.711436,54.898586],[9.703461,54.886176],[9.715994,54.883775],[9.727875,54.879462],[9.737804,54.873521],[9.744477,54.866278],[9.748302,54.857856],[9.750255,54.846259],[9.745942,54.836005],[9.731212,54.831529],[9.719574,54.835842],[9.705333,54.85456],[9.696625,54.858873],[9.65504,54.857082],[9.6421,54.858873],[9.623302,54.865302],[9.624685,54.869289],[9.632823,54.873114],[9.634613,54.87934],[9.616954,54.894232],[9.610362,54.903225],[9.617524,54.907294],[9.644216,54.913275],[9.638845,54.92475],[9.615001,54.932685],[9.586762,54.92772],[9.581228,54.92178],[9.568044,54.901435],[9.556814,54.890326],[9.550548,54.882717],[9.545909,54.87934],[9.538341,54.877916],[9.522146,54.879299],[9.515147,54.875922],[9.498057,54.864325],[9.456391,54.843817],[9.442882,54.831529],[9.43629,54.810452],[9.437503,54.810411],[9.436922,54.810144],[9.422143,54.80725],[9.405193,54.808387],[9.385039,54.819394],[9.366952,54.817017],[9.355997,54.811332],[9.341734,54.809059],[9.332019,54.803219],[9.317033,54.801617],[9.244066,54.801772],[9.226392,54.805906],[9.219054,54.817792],[9.216057,54.831745],[9.21151,54.841822],[9.194766,54.8504],[8.982686,54.879339],[8.904138,54.897942],[8.824246,54.9059],[8.800889,54.903833],[8.732779,54.889054],[8.695882,54.889984],[8.660816,54.896304],[8.660818,54.896308],[8.668793,54.91352],[8.661388,54.920315],[8.674815,54.947943],[8.666352,54.973334],[8.650401,54.997992],[8.640961,55.023383],[8.644786,55.055732],[8.656505,55.082506],[8.670909,55.107123],[8.681895,55.133205],[8.575531,55.14411],[8.56365,55.143378],[8.559906,55.135077],[8.557384,55.096381],[8.551117,55.091376],[8.541352,55.090277],[8.528575,55.081977],[8.514822,55.070787],[8.49936,55.065579],[8.483897,55.067613],[8.470225,55.078599],[8.458832,55.103461],[8.460216,55.127387],[8.466482,55.151597],[8.470225,55.177558],[8.488943,55.19717],[8.530528,55.200751],[8.572602,55.193671],[8.593761,55.180976],[8.580333,55.178941],[8.569509,55.173774],[8.552094,55.160549],[8.555186,55.149359],[8.614757,55.144355],[8.669281,55.136908],[8.689464,55.141588],[8.687185,55.16059],[8.668793,55.194648],[8.655935,55.237494],[8.648448,55.28026],[8.650401,55.291734],[8.659353,55.305813],[8.661388,55.314439],[8.659679,55.327216],[8.648448,55.352281],[8.639415,55.397406],[8.632091,55.418769],[8.617686,55.437934],[8.59254,55.449286],[8.556651,55.454291],[8.490001,55.455308],[8.441905,55.463935],[8.401134,55.485663],[8.310395,55.56269],[8.311209,55.56977],[8.332367,55.572008],[8.332367,55.578274],[8.313162,55.582709],[8.290212,55.583808],[8.267833,55.580512],[8.250499,55.572008],[8.239024,55.557929],[8.242035,55.548529],[8.271739,55.530463],[8.262462,55.529202],[8.256358,55.526801],[8.243663,55.516791],[8.266775,55.51439],[8.293956,55.50434],[8.316742,55.489407],[8.326182,55.472113],[8.313324,55.469306],[8.193614,55.525946],[8.1692,55.534084],[8.110606,55.540229],[8.095225,55.549058],[8.094005,55.56391],[8.150238,55.649237],[8.169119,55.687934],[8.181651,55.729071],[8.18336,55.769599],[8.13559,55.96601],[8.130707,55.976142],[8.127778,55.978217],[8.126801,55.982978],[8.127778,55.987616],[8.130707,55.989732],[8.136729,55.988593],[8.139415,55.986029],[8.141124,55.983466],[8.144054,55.982327],[8.150564,55.977037],[8.175466,55.900336],[8.19337,55.873847],[8.195974,55.862779],[8.193614,55.853217],[8.183849,55.839504],[8.181651,55.828355],[8.185069,55.814643],[8.193614,55.811835],[8.238536,55.826077],[8.286306,55.84748],[8.308849,55.851996],[8.34783,55.86933],[8.387706,55.89289],[8.393321,55.909166],[8.386892,55.9265],[8.372895,55.943427],[8.356944,55.958686],[8.319509,55.986029],[8.319509,56.017035],[8.308116,56.052924],[8.2824,56.075873],[8.247732,56.08983],[8.170584,56.109565],[8.149669,56.111151],[8.140636,56.102729],[8.141612,56.060004],[8.139903,56.03974],[8.1338,56.017035],[8.137462,56.000393],[8.134776,55.993964],[8.120128,55.996568],[8.115977,56.001858],[8.111664,56.012274],[8.107921,56.024319],[8.103363,56.096869],[8.106456,56.119452],[8.130626,56.181952],[8.1338,56.205471],[8.122406,56.551663],[8.126638,56.568427],[8.1338,56.585639],[8.178722,56.672065],[8.198985,56.698879],[8.213878,56.711819],[8.219981,56.709174],[8.223969,56.700426],[8.233165,56.695461],[8.232432,56.688381],[8.209972,56.655951],[8.202159,56.64704],[8.202159,56.640204],[8.208344,56.636786],[8.215099,56.634955],[8.233165,56.633979],[8.236583,56.629706],[8.240489,56.620063],[8.245779,56.610419],[8.253917,56.606106],[8.294932,56.601996],[8.305919,56.597805],[8.291515,56.593004],[8.295177,56.583808],[8.292491,56.567694],[8.29835,56.558295],[8.307384,56.554348],[8.315196,56.556871],[8.318044,56.563625],[8.312022,56.571967],[8.312022,56.578803],[8.343516,56.58454],[8.3921,56.585273],[8.436534,56.580227],[8.455903,56.568549],[8.46518,56.566881],[8.514659,56.544664],[8.528494,56.548163],[8.541352,56.55683],[8.552094,56.56802],[8.559581,56.578803],[8.565766,56.578803],[8.596039,56.531806],[8.596853,56.524156],[8.593028,56.519924],[8.594574,56.510484],[8.601085,56.501125],[8.617442,56.493801],[8.636974,56.479722],[8.648448,56.475735],[8.665538,56.475898],[8.726573,56.483222],[8.733165,56.485053],[8.738536,56.490058],[8.742035,56.497748],[8.743419,56.507066],[8.743663,56.521674],[8.744965,56.531562],[8.750824,56.552069],[8.755138,56.557807],[8.760427,56.560126],[8.762543,56.563381],[8.757579,56.571967],[8.726573,56.585639],[8.716563,56.586168],[8.708995,56.588568],[8.704438,56.593695],[8.702973,56.602362],[8.698497,56.611233],[8.681488,56.616278],[8.681895,56.626532],[8.69516,56.633246],[8.740896,56.634955],[8.805431,56.695461],[8.820486,56.700385],[8.861583,56.707465],[8.916759,56.708808],[8.918712,56.711005],[8.894786,56.715969],[8.851085,56.716783],[8.840831,56.722357],[8.850108,56.74018],[8.864757,56.755845],[8.876638,56.764309],[8.908458,56.777411],[8.95574,56.802965],[8.974294,56.805365],[8.985199,56.804145],[9.003673,56.799058],[9.014903,56.797838],[9.026622,56.799628],[9.032725,56.803778],[9.037608,56.808539],[9.045584,56.812201],[9.063162,56.812567],[9.086111,56.808417],[9.105968,56.800116],[9.114513,56.787991],[9.120453,56.764594],[9.134451,56.750718],[9.150564,56.740912],[9.162852,56.729641],[9.176036,56.708564],[9.148204,56.704739],[9.118826,56.678778],[9.093435,56.674994],[9.093435,56.667548],[9.097504,56.658433],[9.086681,56.649604],[9.059337,56.633979],[9.049978,56.619818],[9.048839,56.607164],[9.052989,56.575385],[9.060557,56.565741],[9.076996,56.567369],[9.093435,56.57807],[9.106781,56.610785],[9.121593,56.613023],[9.139659,56.611029],[9.155447,56.612942],[9.149181,56.622504],[9.141124,56.626044],[9.131521,56.624823],[9.121349,56.620347],[9.128917,56.631252],[9.136729,56.639065],[9.143728,56.647691],[9.148611,56.661322],[9.155447,56.661322],[9.149425,56.638861],[9.175548,56.635443],[9.211192,56.638861],[9.233653,56.637152],[9.247244,56.632025],[9.262462,56.633734],[9.276215,56.632961],[9.28533,56.620347],[9.2824,56.610541],[9.259451,56.591946],[9.250336,56.578803],[9.261241,56.576361],[9.263357,56.571967],[9.264008,56.565904],[9.270844,56.558295],[9.28061,56.552802],[9.305675,56.544664],[9.309744,56.540513],[9.312185,56.535102],[9.316417,56.530829],[9.326182,56.530341],[9.340587,56.547065],[9.34669,56.552069],[9.36085,56.544745],[9.373057,56.55272],[9.372732,56.565375],[9.350352,56.571967],[9.319347,56.561713],[9.317393,56.558661],[9.312836,56.557766],[9.302257,56.558295],[9.293712,56.561021],[9.293142,56.566799],[9.296153,56.571601],[9.298839,56.571967],[9.300629,56.609117],[9.317068,56.641099],[9.32602,56.66942],[9.325938,56.669623],[9.305675,56.695461],[9.295909,56.701565],[9.290701,56.703843],[9.270844,56.702338],[9.230642,56.692084],[9.220714,56.684272],[9.199067,56.676581],[9.177908,56.675849],[9.169119,56.688666],[9.180349,56.700751],[9.22641,56.716254],[9.236827,56.733059],[9.242524,56.747056],[9.240489,56.753974],[9.215505,56.758857],[9.205251,56.763821],[9.196544,56.770453],[9.189708,56.777411],[9.184418,56.784003],[9.178722,56.794135],[9.174978,56.806057],[9.176036,56.818427],[9.181163,56.821723],[9.204112,56.84162],[9.206716,56.84634],[9.211436,56.851508],[9.210623,56.863105],[9.205903,56.874579],[9.199555,56.879828],[9.187836,56.881171],[9.178966,56.884589],[9.162852,56.89411],[9.181488,56.919094],[9.200206,56.93891],[9.22047,56.955064],[9.251475,56.972561],[9.258474,56.97484],[9.265961,56.976223],[9.274669,56.97663],[9.279959,56.979804],[9.285492,56.993964],[9.288748,56.997138],[9.437022,57.0244],[9.470063,57.02383],[9.586762,56.997138],[9.586762,56.989691],[9.573985,56.985012],[9.57309,56.978827],[9.580821,56.972968],[9.594249,56.969184],[9.607921,56.969875],[9.617686,56.975043],[9.654145,57.006781],[9.670665,57.024848],[9.688975,57.03913],[9.728201,57.047065],[9.761241,57.05858],[9.809093,57.051744],[9.915782,57.05858],[9.856619,57.08869],[9.826671,57.094875],[9.806407,57.104234],[9.795665,57.106391],[9.787608,57.103949],[9.752452,57.077786],[9.733246,57.066799],[9.711925,57.061265],[9.693207,57.069159],[9.675141,57.079169],[9.656098,57.076361],[9.621104,57.05858],[9.580821,57.049302],[9.493988,57.04857],[9.265147,57.003404],[9.245372,57.002346],[9.22641,57.007025],[9.114513,57.05858],[9.12086,57.045396],[9.112804,57.038642],[9.079845,57.031236],[9.055431,57.021145],[9.042654,57.017727],[9.025727,57.017564],[9.031993,57.02383],[9.014008,57.030178],[8.996755,57.028062],[8.979666,57.020494],[8.963145,57.010199],[8.946951,57.011176],[8.926443,57.000149],[8.909028,56.988186],[8.901622,56.986274],[8.893077,56.999661],[8.87322,57.004828],[8.850434,57.003485],[8.833344,56.997138],[8.847992,56.988471],[8.811697,56.969875],[8.762055,56.955471],[8.72641,56.961249],[8.702973,56.958441],[8.679535,56.952541],[8.668793,56.945299],[8.663341,56.92593],[8.649913,56.91413],[8.634125,56.905178],[8.621104,56.89411],[8.615408,56.865627],[8.614268,56.863349],[8.612559,56.854682],[8.607921,56.849677],[8.601329,56.845526],[8.569591,56.818427],[8.547048,56.817328],[8.536306,56.814521],[8.531749,56.808783],[8.524262,56.804755],[8.507823,56.801093],[8.491384,56.794257],[8.483897,56.781155],[8.492442,56.74726],[8.491466,56.731391],[8.47641,56.722154],[8.47641,56.715969],[8.516368,56.718207],[8.525564,56.715969],[8.533051,56.703803],[8.527029,56.695299],[8.514008,56.690253],[8.500987,56.688666],[8.489268,56.690741],[8.465831,56.700141],[8.452891,56.702338],[8.440929,56.700385],[8.420909,56.691352],[8.408214,56.688666],[8.408214,56.68122],[8.455903,56.688666],[8.470225,56.686103],[8.491222,56.676988],[8.500987,56.674994],[8.571544,56.691962],[8.586274,56.688666],[8.589041,56.681342],[8.586274,56.662299],[8.586274,56.653876],[8.601329,56.640611],[8.608084,56.631049],[8.593272,56.61636],[8.583018,56.60932],[8.569591,56.606106],[8.555431,56.604885],[8.546641,56.601711],[8.547048,56.59748],[8.559581,56.593004],[8.539317,56.59276],[8.511485,56.607001],[8.48699,56.625312],[8.47641,56.637152],[8.467296,56.652045],[8.444835,56.664049],[8.416759,56.672065],[8.391124,56.674994],[8.373383,56.680243],[8.352224,56.692939],[8.33253,56.708401],[8.319509,56.722154],[8.30836,56.758205],[8.302013,56.763739],[8.288422,56.766343],[8.260265,56.776679],[8.243663,56.777411],[8.258067,56.740546],[8.263194,56.71894],[8.260753,56.70539],[8.24586,56.704006],[8.238292,56.725043],[8.236176,56.771186],[8.247895,56.812445],[8.270356,56.843248],[8.449718,57.003974],[8.503591,57.034654],[8.586274,57.106391],[8.618012,57.12287],[8.653819,57.122504],[8.737153,57.106391],[8.783946,57.104804],[8.87436,57.116116],[8.914561,57.127427],[8.962576,57.156562],[8.980479,57.161038],[9.230642,57.141099],[9.321544,57.14704],[9.41212,57.165432],[9.497813,57.197008],[9.573253,57.242906],[9.788422,57.459296],[9.826427,57.489936],[9.895356,57.531562],[9.929861,57.56745],[9.943126,57.572496],[9.96697,57.591376],[10.015391,57.596625],[10.107677,57.592963],[10.197602,57.601223],[10.279552,57.620917],[10.350597,57.648993],[10.472504,57.716986],[10.539073,57.743842],[10.566091,57.749457],[10.596039,57.751166],[10.625011,57.747504],[10.648936,57.737006],[10.594412,57.725898],[10.542491,57.69892],[10.46046,57.630561]]]]";
        String geoShapeString = "{\"type\":\"MultiPolygon\",\"coordinates\":" + denmark + "} ";

        QueryParams params = new QueryParams();
        params.add("_geoShape", geoShapeString);

        assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(1l));

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);

        assertEquals(1, result.getTotalSize());
    }

    @Test
    public void testGeoShapePolygonQuery() throws Exception {
        createIndex(INDEX_NAME);
        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();


        ESSpecimen esSpecimen = createSpecimen();
        ESGatheringEvent gatheringEvent = new ESGatheringEvent();
        gatheringEvent.setSiteCoordinates(asList(new ESGatheringSiteCoordinates(14d, 12d)));
        esSpecimen.setGatheringEvent(gatheringEvent);

        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute()
                .actionGet();

        String geoShapeString = "{\"type\" : \"Polygon\",\"coordinates\" : [[[12,14], [13,14], [13,15], [12,15], [12,14]]]}";

        QueryParams params = new QueryParams();
        params.add("_geoShape", geoShapeString);

        //assertThat(client().prepareCount(INDEX_NAME).execute().actionGet().getCount(), is(1wl));

        ResultGroupSet<Specimen, String> result = dao.specimenNameSearch(params);
        assertEquals(1, result.getTotalSize());
    }

    @Test
    public void testPreviousAndNextLinks() {
        QueryParams params = new QueryParams();
        params.add(UNIT_ID, "2");

        ResultGroupSet<Specimen, String> specimenResultGroupSet = createSpecimenResultGroupSet();

        SearchResultSet<Specimen> specimenDetailSearchResultSet = dao.createSpecimenDetailSearchResultSet(params,
                specimenResultGroupSet);
        SearchResult<Specimen> specimenSearchResult = specimenDetailSearchResultSet.getSearchResults().get(0);
        assertEquals("2", specimenSearchResult.getResult().getUnitID());
        assertTrue(specimenSearchResult.getLinks().get(0).getHref().contains("1"));
        assertTrue(specimenSearchResult.getLinks().get(0).getRel().equals("_previous"));
        assertTrue(specimenSearchResult.getLinks().get(1).getHref().contains("3"));
        assertTrue(specimenSearchResult.getLinks().get(1).getRel().equals("_next"));
    }

    @Test
    public void testPreviousAndNextLinks_lastItemInSecondBucket() {
        QueryParams params = new QueryParams();
        params.add(UNIT_ID, "6");

        ResultGroupSet<Specimen, String> specimenResultGroupSet = createSpecimenResultGroupSet();

        SearchResultSet<Specimen> specimenDetailSearchResultSet = dao.createSpecimenDetailSearchResultSet(params,
                specimenResultGroupSet);
        SearchResult<Specimen> specimenSearchResult = specimenDetailSearchResultSet.getSearchResults().get(0);
        assertEquals("6", specimenSearchResult.getResult().getUnitID());
        assertTrue(specimenSearchResult.getLinks().get(0).getHref().contains("5"));
        assertTrue(specimenSearchResult.getLinks().get(0).getRel().equals("_previous"));
        assertTrue(specimenSearchResult.getLinks().get(1).getHref().contains("7"));
        assertTrue(specimenSearchResult.getLinks().get(1).getRel().equals("_next"));
    }

    @Test
    public void testPreviousAndNextLinks_firstItemInSecondBucket() {
        QueryParams params = new QueryParams();
        params.add(UNIT_ID, "4");

        ResultGroupSet<Specimen, String> specimenResultGroupSet = createSpecimenResultGroupSet();

        SearchResultSet<Specimen> specimenDetailSearchResultSet = dao.createSpecimenDetailSearchResultSet(params,
                specimenResultGroupSet);
        SearchResult<Specimen> specimenSearchResult = specimenDetailSearchResultSet.getSearchResults().get(0);
        assertEquals("4", specimenSearchResult.getResult().getUnitID());
        assertTrue(specimenSearchResult.getLinks().get(0).getHref().contains("3"));
        assertTrue(specimenSearchResult.getLinks().get(0).getRel().equals("_previous"));
        assertTrue(specimenSearchResult.getLinks().get(1).getHref().contains("5"));
        assertTrue(specimenSearchResult.getLinks().get(1).getRel().equals("_next"));
    }

    @Test
    public void testPreviousAndNextLinks_noPrevious() {
        QueryParams params = new QueryParams();
        params.add(UNIT_ID, "1");

        ResultGroupSet<Specimen, String> specimenResultGroupSet = createSpecimenResultGroupSet();

        SearchResultSet<Specimen> specimenDetailSearchResultSet = dao.createSpecimenDetailSearchResultSet(params,
                specimenResultGroupSet);
        SearchResult<Specimen> specimenSearchResult = specimenDetailSearchResultSet.getSearchResults().get(0);
        assertEquals("1", specimenSearchResult.getResult().getUnitID());
        assertEquals(1, specimenSearchResult.getLinks().size());
        assertTrue(specimenSearchResult.getLinks().get(0).getHref().contains("2"));
        assertTrue(specimenSearchResult.getLinks().get(0).getRel().equals("_next"));
    }

    @Test
    public void testPreviousAndNextLinks_noNext() {
        QueryParams params = new QueryParams();
        params.add(UNIT_ID, "9");

        ResultGroupSet<Specimen, String> specimenResultGroupSet = createSpecimenResultGroupSet();

        SearchResultSet<Specimen> specimenDetailSearchResultSet = dao.createSpecimenDetailSearchResultSet(params,
                specimenResultGroupSet);
        SearchResult<Specimen> specimenSearchResult = specimenDetailSearchResultSet.getSearchResults().get(0);
        assertEquals("9", specimenSearchResult.getResult().getUnitID());
        List<Link> links = specimenSearchResult.getLinks();
        assertEquals(1, links.size());
        assertTrue(links.get(0).getHref().contains("8"));
        assertTrue(links.get(0).getRel().equals("_previous"));
    }

    @Test
    public void testGetSpecimenDetailWithinSearchResult() throws IOException {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();
        esSpecimen.setUnitID(esSpecimen.getUnitID() + "_diff");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        QueryParams queryParams = new QueryParams();
        queryParams.add("_source", "SPECIMEN_SEARCH");
        queryParams.add("unitID", "L  0191413");
        queryParams.add("gatheringEvent.gatheringPersons.fullName", "Meijer, W.");

        SearchResultSet<Specimen> specimenStringResultGroupSet = dao.getSpecimenDetailWithinSearchResult(queryParams);
        System.out.println("temp");
    }

    @Test
    public void testSpecimenSearch_NDA_182() throws Exception {
        createIndex(INDEX_NAME);

        client().admin().indices().preparePutMapping(INDEX_NAME).setType(SPECIMEN_TYPE)
                .setSource(getMapping("test-specimen-mapping.json"))
                .execute().actionGet();

        ESSpecimen esSpecimen = createSpecimen();
        List<ESGatheringSiteCoordinates> siteCoordinates = new ArrayList<>();
        siteCoordinates.add(new ESGatheringSiteCoordinates(32.333333, 36.133333));
        esSpecimen.getGatheringEvent().setSiteCoordinates(siteCoordinates);
        esSpecimen.getGatheringEvent().setLocalityText("continent: Asia-Temperate; country: Jordan; province/state: Mafraq; 2 km W of Mafraq.");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "1").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        siteCoordinates = new ArrayList<>();
        siteCoordinates.add(new ESGatheringSiteCoordinates(32.3, 36.033333));
        esSpecimen.getGatheringEvent().setSiteCoordinates(siteCoordinates);
        esSpecimen.getGatheringEvent().setLocalityText("continent: Asia-Temperate; country: Jordan; province/state: Mafraq; Hamama, 3 km W of Rihab.");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "2").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        siteCoordinates = new ArrayList<>();
        siteCoordinates.add(new ESGatheringSiteCoordinates(31.716667, 35.8));
        esSpecimen.getGatheringEvent().setSiteCoordinates(siteCoordinates);
        esSpecimen.getGatheringEvent().setLocalityText("continent: Asia; country: Palestinian Territory; Mouth of Jordan in the Lake of Huie.");
        client().prepareIndex(INDEX_NAME, SPECIMEN_TYPE, "3").setSource(objectMapper.writeValueAsString(esSpecimen))
                .setRefresh(true).execute().actionGet();

        QueryParams params = new QueryParams();
        params.add("_sort", "_score");
        params.add("_sortDirection", "DESC");
        params.add("_maxResults", "20");
        params.add("_andOr", "AND");
        params.add("gatheringEvent.localityText", "Mafraq");

        String area = "[[[[39.046329,32.308494],[39.235775,32.352858],[39.256342,32.342678],[39.271122,32.311956],[39.291999,32.244519],[39.266471,32.212867],[39.146168,32.125844],[39.146375,32.118144],[39.136246,32.115353],[39.116816,32.102899],[38.998064,32.006936],[38.96344,31.994482],[38.96344,31.994327],[38.963337,31.994379],[38.849752,31.966319],[38.624856,31.91087],[38.400167,31.855369],[38.175375,31.799921],[37.986522,31.753358],[37.950479,31.744472],[37.761412,31.69612],[37.702742,31.681116],[37.455005,31.617709],[37.207269,31.554354],[36.997912,31.500814],[36.959532,31.490999],[37.089653,31.370076],[37.219774,31.249101],[37.221722,31.247292],[37.349895,31.12823],[37.480017,31.007256],[37.483117,31.004103],[37.486321,31.0009],[37.489422,30.997696],[37.492626,30.994492],[37.602283,30.883232],[37.712044,30.772025],[37.821908,30.660869],[37.931565,30.549661],[37.981071,30.499483],[37.981381,30.498811],[37.980968,30.498398],[37.980038,30.498088],[37.97332,30.494522],[37.900146,30.459072],[37.779327,30.400419],[37.670703,30.347606],[37.647552,30.330863],[37.634529,30.312776],[37.605177,30.250713],[37.56921,30.174955],[37.536137,30.105295],[37.491696,30.011193],[37.470198,29.994553],[37.352376,29.973314],[37.218534,29.949129],[37.075804,29.923291],[36.931936,29.897246],[36.84295,29.881226],[36.756237,29.865517],[36.728745,29.853528],[36.70487,29.831152],[36.649576,29.7494],[36.603584,29.681471],[36.541263,29.589409],[36.477081,29.494609],[36.399979,29.438902],[36.283707,29.35485],[36.177977,29.278369],[36.069457,29.200028],[36.043825,29.190881],[36.016437,29.189951],[35.912464,29.205686],[35.797225,29.223075],[35.740251,29.231731],[35.622042,29.249689],[35.473628,29.272065],[35.334618,29.293148],[35.179072,29.316661],[35.060319,29.334696],[34.949385,29.351686],[34.949392,29.351711],[34.962413,29.359768],[34.969005,29.450832],[34.976085,29.477037],[34.997813,29.517971],[34.996837,29.533881],[34.976085,29.552151],[34.962413,29.552151],[34.961599,29.555406],[34.961599,29.558092],[34.960216,29.559516],[34.95558,29.558987],[34.95986,29.586206],[34.966992,29.608116],[34.980324,29.627004],[34.989833,29.651964],[34.995104,29.708162],[35.002545,29.733096],[35.048951,29.842314],[35.053188,29.862623],[35.054118,29.923394],[35.061456,29.957346],[35.065384,29.965976],[35.070345,29.973727],[35.074065,29.982564],[35.074686,29.994604],[35.086261,30.034034],[35.129049,30.089741],[35.145276,30.123382],[35.145276,30.154905],[35.124812,30.21609],[35.125225,30.244667],[35.132356,30.261875],[35.141762,30.313965],[35.147756,30.32647],[35.154474,30.336754],[35.159952,30.347503],[35.162122,30.361404],[35.159332,30.375615],[35.144965,30.395872],[35.140005,30.406155],[35.140005,30.430185],[35.157368,30.470854],[35.162122,30.494677],[35.205324,30.617099],[35.263821,30.71978],[35.263882,30.719967],[35.271573,30.743706],[35.27612,30.768976],[35.279531,30.780241],[35.286145,30.792333],[35.293897,30.800188],[35.310847,30.813314],[35.316635,30.822823],[35.320045,30.84494],[35.319528,30.867316],[35.322216,30.88995],[35.334928,30.912585],[35.34711,30.92271],[35.374099,30.945141],[35.385261,30.963279],[35.385158,30.994647],[35.391565,31.023947],[35.438488,31.103736],[35.443242,31.132209],[35.436214,31.159546],[35.421331,31.184506],[35.410686,31.204608],[35.401177,31.230291],[35.3957,31.25768],[35.408205,31.282019],[35.422261,31.303],[35.423915,31.324601],[35.416473,31.331835],[35.435077,31.360619],[35.452854,31.400823],[35.456884,31.423509],[35.457128,31.433524],[35.458538,31.491619],[35.458125,31.491929],[35.458745,31.491567],[35.459158,31.491877],[35.459055,31.492808],[35.458745,31.494409],[35.464222,31.568565],[35.480139,31.641119],[35.502479,31.68536],[35.527578,31.735067],[35.55941,31.765349],[35.538326,31.819299],[35.538326,31.826741],[35.549075,31.839195],[35.524684,31.919241],[35.527474,31.927355],[35.533676,31.9303],[35.5406,31.932006],[35.545148,31.936605],[35.545871,31.944563],[35.53998,31.955622],[35.538326,31.963942],[35.537706,31.977584],[35.535536,31.988229],[35.524684,32.011691],[35.522824,32.057838],[35.528301,32.075098],[35.545148,32.086828],[35.534606,32.09923],[35.535226,32.110806],[35.551969,32.135197],[35.546698,32.141605],[35.546698,32.147031],[35.551246,32.151527],[35.55941,32.155093],[35.55941,32.162534],[35.555173,32.174394],[35.559562,32.190371],[35.572536,32.237594],[35.55941,32.237594],[35.561064,32.243149],[35.563751,32.246818],[35.567575,32.249506],[35.572536,32.251908],[35.564578,32.263587],[35.560857,32.28263],[35.561167,32.301699],[35.565612,32.313352],[35.556517,32.328209],[35.557343,32.358207],[35.551969,32.367948],[35.551969,32.374821],[35.55941,32.374821],[35.55941,32.367948],[35.565612,32.367948],[35.563958,32.377043],[35.560961,32.384717],[35.556827,32.390918],[35.551969,32.395285],[35.549592,32.39854],[35.545148,32.409573],[35.554863,32.411175],[35.5591,32.413966],[35.55817,32.417997],[35.551969,32.423242],[35.551969,32.429443],[35.55941,32.429443],[35.55786,32.43412],[35.556207,32.434791],[35.554139,32.434455],[35.551969,32.436212],[35.565612,32.443706],[35.55941,32.450527],[35.566232,32.453111],[35.572536,32.456728],[35.572536,32.464195],[35.568092,32.46497],[35.565405,32.466314],[35.563131,32.468174],[35.55941,32.470396],[35.561271,32.477089],[35.564991,32.48391],[35.570986,32.489207],[35.579978,32.49148],[35.579978,32.497733],[35.570573,32.506027],[35.568505,32.510265],[35.561374,32.519179],[35.55724,32.519334],[35.551969,32.518817],[35.551969,32.525587],[35.565612,32.525587],[35.562821,32.532021],[35.55941,32.55295],[35.565612,32.546102],[35.570159,32.556825],[35.572536,32.560391],[35.57388,32.556748],[35.574397,32.554396],[35.575844,32.554965],[35.579978,32.560391],[35.574397,32.572767],[35.571296,32.598554],[35.565612,32.607546],[35.565612,32.615013],[35.572536,32.615013],[35.572536,32.62124],[35.564061,32.625477],[35.560031,32.632686],[35.560547,32.640903],[35.562718,32.64421],[35.569849,32.646768],[35.578737,32.653434],[35.593827,32.670358],[35.612224,32.681546],[35.612334,32.681535],[35.635995,32.679143],[35.652015,32.686171],[35.685191,32.711234],[35.740175,32.740535],[35.757435,32.744282],[35.75759,32.744347],[35.763842,32.746969],[35.769734,32.748054],[35.774901,32.747279],[35.779139,32.744514],[35.779139,32.744462],[35.779035,32.744359],[35.779035,32.744282],[35.788234,32.734411],[35.895721,32.713276],[35.905229,32.708573],[35.922489,32.693768],[35.92745,32.692373],[35.940369,32.692502],[35.944193,32.690771],[35.945743,32.684104],[35.9444,32.677619],[35.941196,32.673536],[35.937475,32.674002],[35.94657,32.664441],[35.955355,32.657439],[35.965794,32.654365],[35.980263,32.656612],[36.003621,32.655088],[36.008272,32.643719],[36.005275,32.626692],[36.005998,32.607907],[36.015403,32.591164],[36.060465,32.533261],[36.066046,32.521608],[36.066253,32.517319],[36.06987,32.516595],[36.081906,32.516265],[36.096225,32.515872],[36.133226,32.520109],[36.13953,32.519541],[36.149865,32.51613],[36.15586,32.5152],[36.160821,32.517215],[36.17219,32.525923],[36.177357,32.527318],[36.188209,32.52228],[36.220765,32.494581],[36.285258,32.456935],[36.373108,32.386422],[36.387887,32.379317],[36.407627,32.374227],[36.463955,32.369395],[36.480181,32.360791],[36.516684,32.357014],[36.653504,32.342859],[36.689574,32.319656],[36.706937,32.328338],[36.728641,32.327795],[36.792513,32.313533],[36.806569,32.313042],[36.819385,32.316788],[36.980099,32.410038],[37.133165,32.494478],[37.133165,32.494529],[37.133371,32.494529],[37.133371,32.494581],[37.244062,32.554396],[37.415214,32.64713],[37.494606,32.690056],[37.586677,32.739837],[37.758036,32.832519],[37.929395,32.92533],[38.056726,32.994292],[38.056726,32.994344],[38.230875,33.086302],[38.315742,33.13118],[38.529565,33.244251],[38.774511,33.371685],[38.82102,33.229032],[38.862568,33.10072],[38.897191,32.994344],[38.94277,32.852337],[38.990002,32.705576],[39.057181,32.496596],[38.979977,32.476055],[38.978633,32.475693],[38.97822,32.47497],[38.978633,32.47373],[38.979977,32.472102],[39.028759,32.328338],[39.036201,32.313352],[39.046329,32.308494]]]]";
        String geoShapeString = "{\"type\":\"MultiPolygon\",\"coordinates\":" + area + "} ";
        params.add("_geoShape", geoShapeString);

        SearchResultSet<Specimen> resultSet = dao.specimenSearch(params);

        assertEquals(2, resultSet.getTotalSize());
    }

    private ResultGroupSet<Specimen, String> createSpecimenResultGroupSet() {
        ResultGroupSet<Specimen, String> specimenResultGroupSet = new ResultGroupSet<>();
        ResultGroup<Specimen, String> group1 = new ResultGroup<>();
        Specimen specimen1 = newSpecimentWithUnitId("1");
        Specimen specimen2 = newSpecimentWithUnitId("2");
        Specimen specimen3 = newSpecimentWithUnitId("3");
        group1.addSearchResult(specimen1);
        group1.addSearchResult(specimen2);
        group1.addSearchResult(specimen3);

        specimenResultGroupSet.addGroup(group1);

        ResultGroup<Specimen, String> group2 = new ResultGroup<>();
        Specimen specimen4 = newSpecimentWithUnitId("4");
        Specimen specimen5 = newSpecimentWithUnitId("5");
        Specimen specimen6 = newSpecimentWithUnitId("6");
        group2.addSearchResult(specimen4);
        group2.addSearchResult(specimen5);
        group2.addSearchResult(specimen6);

        specimenResultGroupSet.addGroup(group2);

        ResultGroup<Specimen, String> group3 = new ResultGroup<>();
        Specimen specimen7 = newSpecimentWithUnitId("7");
        Specimen specimen8 = newSpecimentWithUnitId("8");
        Specimen specimen9 = newSpecimentWithUnitId("9");
        group3.addSearchResult(specimen7);
        group3.addSearchResult(specimen8);
        group3.addSearchResult(specimen9);

        specimenResultGroupSet.addGroup(group3);
        return specimenResultGroupSet;
    }

    private Specimen newSpecimentWithUnitId(String unitId) {
        Specimen specimen = new Specimen();
        specimen.setUnitID(unitId);
        return specimen;
    }
}

