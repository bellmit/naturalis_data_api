package nl.naturalis.nba.dao.es;

import static nl.naturalis.nba.api.query.Operator.*;
import static nl.naturalis.nba.dao.es.ESTestUtils.createIndex;
import static nl.naturalis.nba.dao.es.ESTestUtils.createType;
import static nl.naturalis.nba.dao.es.ESTestUtils.dropIndex;
import static nl.naturalis.nba.dao.es.ESTestUtils.refreshIndex;
import static nl.naturalis.nba.dao.es.ESTestUtils.saveObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import nl.naturalis.nba.api.model.Agent;
import nl.naturalis.nba.api.model.DefaultClassification;
import nl.naturalis.nba.api.model.Monomial;
import nl.naturalis.nba.api.model.Person;
import nl.naturalis.nba.api.model.Specimen;
import nl.naturalis.nba.api.model.SpecimenIdentification;
import nl.naturalis.nba.api.query.Condition;
import nl.naturalis.nba.api.query.InvalidQueryException;
import nl.naturalis.nba.api.query.QuerySpec;
import nl.naturalis.nba.dao.es.types.ESGatheringEvent;
import nl.naturalis.nba.dao.es.types.ESSpecimen;

public class SpecimenDAOTest {

	static final ESSpecimen specimen01 = new ESSpecimen();
	static final ESSpecimen specimen02 = new ESSpecimen();

	@BeforeClass
	public static void setup()
	{
		
		/* ****************************** */
		/* ******** 1st SPECIMEN ******** */
		/* ****************************** */

		Person person = new Person("Wallich, N");
		ESGatheringEvent gathering = new ESGatheringEvent();
		gathering.setGatheringPersons(Arrays.asList(person));

		DefaultClassification defaultClassification = new DefaultClassification();
		defaultClassification.setGenus("Parus");
		defaultClassification.setSpecificEpithet("major");
		
		List<Monomial> systemClassification = new ArrayList<>();
		systemClassification.add(new Monomial("kingdom", "Animalia"));
		systemClassification.add(new Monomial("phylum", "Chordata"));
		systemClassification.add(new Monomial("genus", "Parus"));

		SpecimenIdentification identification = new SpecimenIdentification();
		identification.setDefaultClassification(defaultClassification);
		identification.setSystemClassification(systemClassification);

		specimen01.setUnitID("ZMA.MAM.12345");
		specimen01.setGatheringEvent(gathering);
		specimen01.setIdentifications(Arrays.asList(identification));

		
		/* ****************************** */
		/* ******** 2nd SPECIMEN ******** */
		/* ****************************** */

		specimen02.setUnitID("L  0000123");
	}

	@Before
	public void before()
	{
		dropIndex(ESSpecimen.class);
		createIndex(ESSpecimen.class);
		createType(ESSpecimen.class);
	}

	@After
	public void after()
	{
		//dropIndex(ESSpecimen.class);
	}

	//@Test
	public void testFindById_1()
	{
		String id = "ZMA.MAM.12345@CRS";
		saveObject(id, specimen01);
		refreshIndex(ESSpecimen.class);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen out = dao.find(id);
		assertNotNull("01", out);
	}

	@Test
	public void testFindByUnitID_1()
	{
		String unitID = specimen01.getUnitID();
		saveObject(specimen01);
		saveObject(specimen01);
		refreshIndex(ESSpecimen.class);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.findByUnitID(unitID);
		assertNotNull("01", result);
		assertEquals("02", 2, result.length);
		assertEquals("03", unitID, result[0].getUnitID());
		assertEquals("04", unitID, result[1].getUnitID());
		// Make sure no weird analysis stuff is going on
		result = dao.findByUnitID("ZMA");
		assertNotNull("05", result);
		assertEquals("06", 0, result.length);
		result = dao.findByUnitID("12345");
		assertNotNull("07", result);
		assertEquals("08", 0, result.length);
	}

	@Test
	public void testFindByUnitID_2()
	{
		String unitID0 = specimen02.getUnitID();
		saveObject(specimen02);
		refreshIndex(ESSpecimen.class);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.findByUnitID(unitID0);
		assertNotNull("01", result);
		assertEquals("02", 1, result.length);
		assertEquals("03", unitID0, result[0].getUnitID());
		// Make sure no weird analysis stuff is going on
		result = dao.findByUnitID("L");
		assertNotNull("04", result);
		assertEquals("05", 0, result.length);
	}

	@Test
	public void testFindByUnitID_3()
	{
		ESSpecimen specimen = new ESSpecimen();
		specimen.setUnitID("A");
		saveObject(specimen);
		refreshIndex(ESSpecimen.class);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.findByUnitID("NOT A");
		assertNotNull("01", result);
		assertEquals("02", 0, result.length);
	}

	@Test
	public void testFindByCollector_1()
	{
		saveObject("ZMA.MAM.12345@CRS", specimen01);
		refreshIndex(ESSpecimen.class);
		SpecimenDAO dao = new SpecimenDAO();
		Person person = specimen01.getGatheringEvent().getGatheringPersons().get(0);
		String collector = person.getFullName();
		Specimen[] result = dao.findByCollector(collector);
		assertNotNull("01", result);
		assertNotNull("02", result[0]);
		assertNotNull("03", result[0].getGatheringEvent());
		assertNotNull("04", result[0].getGatheringEvent().getGatheringPersons());
		Agent agent = result[0].getGatheringEvent().getGatheringPersons().get(0);
		assertNotNull("05", agent);
		assertEquals("06", Person.class, agent.getClass());
		Person personOut = (Person) agent;
		assertEquals("07", person, personOut);
	}

	@Test
	public void testQuery__QuerySpec__01() throws InvalidQueryException
	{
		saveObject(specimen01);
		refreshIndex(ESSpecimen.class);
		String unitID = specimen01.getUnitID();
		Condition condition = new Condition("unitID", EQUALS, unitID);
		QuerySpec qs = new QuerySpec();
		qs.setCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
		assertEquals("02", specimen01.getUnitID(), result[0].getUnitID());
	}

	@Test
	public void testQuery__QuerySpec__02() throws InvalidQueryException
	{
		saveObject(specimen01);
		refreshIndex(ESSpecimen.class);
		String genus = "identifications.defaultClassification.genus";
		String specificEpithet = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, EQUALS, "Parus");
		condition.and(specificEpithet, EQUALS, "major");
		QuerySpec qs = new QuerySpec();
		qs.setCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
	}

	@Test
	public void testQuery__QuerySpec__03() throws InvalidQueryException
	{
		saveObject(specimen01);
		refreshIndex(ESSpecimen.class);
		String genus = "identifications.defaultClassification.genus";
		String specificEpithet = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, EQUALS, "Parus");
		condition.and(specificEpithet, EQUALS, "bla");
		QuerySpec qs = new QuerySpec();
		qs.setCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 0, result.length);
	}

	@Test
	public void testQuery__QuerySpec__04() throws InvalidQueryException
	{
		saveObject(specimen01);
		refreshIndex(ESSpecimen.class);
		String genus = "identifications.defaultClassification.genus";
		String specificEpithet = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, EQUALS, "Parus");
		condition.or(specificEpithet, EQUALS, "major");
		QuerySpec qs = new QuerySpec();
		qs.setCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
	}

	@Test
	public void testQuery__QuerySpec__05() throws InvalidQueryException
	{
		saveObject(specimen01);
		refreshIndex(ESSpecimen.class);
		String genus = "identifications.defaultClassification.genus";
		String specificEpithet = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, EQUALS, "Parus");
		condition.or(specificEpithet, EQUALS, "bla");
		QuerySpec qs = new QuerySpec();
		qs.setCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
	}

	@Test
	public void testQuery__QuerySpec__06() throws InvalidQueryException
	{
		saveObject(specimen01);
		refreshIndex(ESSpecimen.class);
		String genus = "identifications.defaultClassification.genus";
		String specificEpithet = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, EQUALS, "bla");
		condition.or(specificEpithet, EQUALS, "bla");
		QuerySpec qs = new QuerySpec();
		qs.setCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 0, result.length);
	}
	
	/*
	 *****************************************************
	 * Test query method with LIKE and NOT_LIKE operator *
	 *****************************************************
	 */

	@Test
	public void testQuery__QuerySpec__07() throws InvalidQueryException
	{
		saveObject(specimen01);
		refreshIndex(ESSpecimen.class);
		String collector = "gatheringEvent.gatheringPersons.fullName";
		Condition condition = new Condition(collector, LIKE, "allich");
		QuerySpec qs = new QuerySpec();
		qs.setCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
	}
}
