package nl.naturalis.nda.elasticsearch.dao.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.naturalis.nda.domain.DefaultClassification;
import nl.naturalis.nda.domain.Person;
import nl.naturalis.nda.domain.ScientificName;
import nl.naturalis.nda.domain.SpecimenIdentification;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESGatheringEvent;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESGatheringSiteCoordinates;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESSpecimen;
import org.elasticsearch.common.joda.time.DateTime;
import org.junit.Before;

import static java.util.Arrays.asList;

/**
 * @author Quinten Krijger
 */
public class AbstractBioportalSpecimenDaoTest extends DaoIntegrationTest {
    protected static final String SPECIMEN_TYPE = "Specimen";
    protected static final String TAXON_TYPE = "Taxon";
    protected BioportalSpecimenDao dao;
    protected TestDocumentCreator documentCreator;
    protected ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        dao = new BioportalSpecimenDao(client(), INDEX_NAME, new BioportalTaxonDao(client(), INDEX_NAME));
        documentCreator = new TestDocumentCreator();
    }

    //=================================================== Helpers ======================================================

    protected ESSpecimen createSpecimen() {
        ESSpecimen esSpecimen = new ESSpecimen();

        esSpecimen.setUnitID("L  0191413");
        esSpecimen.setSourceSystemId("L  0191413");

        ESGatheringEvent gatheringEvent = new ESGatheringEvent();
        gatheringEvent.setGatheringPersons(asList(new Person("Meijer, W.")));
        gatheringEvent.setSiteCoordinates(asList(new ESGatheringSiteCoordinates(9.6373151, 55.7958149)));
        gatheringEvent.setDateTimeBegin(new DateTime().withMillis(-299725200000L).toDate());
        gatheringEvent.setDateTimeEnd(new DateTime().withMillis(-299725200000L).toDate());
        esSpecimen.setGatheringEvent(gatheringEvent);

        SpecimenIdentification specimenIdentification = new SpecimenIdentification();
        ScientificName scientificName = new ScientificName();
        scientificName.setGenusOrMonomial("Xylopia");
        scientificName.setSpecificEpithet("ferruginea");

        DefaultClassification defaultClassification = new DefaultClassification();
        defaultClassification.setKingdom("Plantae");

        specimenIdentification.setScientificName(scientificName);
        specimenIdentification.setDefaultClassification(defaultClassification);
        esSpecimen.setIdentifications(asList(specimenIdentification));

        return esSpecimen;
    }}
