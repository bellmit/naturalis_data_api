package nl.naturalis.nba.dao.es;

import static nl.naturalis.nba.api.query.ComparisonOperator.EQUALS;
import static nl.naturalis.nba.api.query.ComparisonOperator.EQUALS_IC;
import static nl.naturalis.nba.api.query.ComparisonOperator.LIKE;
import static nl.naturalis.nba.api.query.ComparisonOperator.NOT_EQUALS;
import static nl.naturalis.nba.api.query.ComparisonOperator.NOT_EQUALS_IC;
import static nl.naturalis.nba.api.query.UnaryBooleanOperator.NOT;
import static nl.naturalis.nba.dao.es.ESTestUtils.createIndex;
import static nl.naturalis.nba.dao.es.ESTestUtils.createType;
import static nl.naturalis.nba.dao.es.ESTestUtils.dropIndex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.naturalis.nba.api.model.Agent;
import nl.naturalis.nba.api.model.Person;
import nl.naturalis.nba.api.model.Specimen;
import nl.naturalis.nba.api.query.Condition;
import nl.naturalis.nba.api.query.InvalidQueryException;
import nl.naturalis.nba.api.query.QuerySpec;
import nl.naturalis.nba.dao.es.transfer.SpecimenTransfer;
import nl.naturalis.nba.dao.es.types.ESSpecimen;
import nl.naturalis.nba.dao.es.util.DocumentType;

public class SpecimenDAOTest {

	static ESSpecimen pMajor;
	static ESSpecimen lFuscus1;
	static ESSpecimen lFuscus2;
	static ESSpecimen tRex;
	static ESSpecimen mSylvestris;

	@Before
	public void before()
	{
		dropIndex(DocumentType.SPECIMEN);
		createIndex(DocumentType.SPECIMEN);
		createType(DocumentType.SPECIMEN);
		/*
		 * Insert 5 test specimens.
		 */
		pMajor = TestSpecimens.parusMajorSpecimen01();
		lFuscus1 = TestSpecimens.larusFuscusSpecimen01();
		lFuscus2 = TestSpecimens.larusFuscusSpecimen02();
		tRex = TestSpecimens.tRexSpecimen01();
		mSylvestris = TestSpecimens.malusSylvestrisSpecimen01();
		ESTestUtils.saveSpecimens(pMajor, lFuscus1, lFuscus2, tRex, mSylvestris);
	}

	@After
	public void after()
	{
		//dropIndex(ESSpecimen.class);
	}

	/*
	 * Test lookup using internal Elasticsearch ID.
	 */
	@Test
	public void test_01()
	{
		/*
		 * Internal Elasticsearch system IDs are generated by concatening UnitID
		 * and source system code, like so:
		 */
		String id = pMajor.getUnitID() + "@" + pMajor.getSourceSystem().getCode();
		SpecimenDAO dao = new SpecimenDAO();
		Specimen specimen = dao.find(id);
		assertNotNull("01", specimen);
	}

	/*
	 * Test lookup using internal multiple Elasticsearch IDs.
	 */
	@Test
	public void testFindByIds_01()
	{
		String id1 = pMajor.getUnitID() + "@" + pMajor.getSourceSystem().getCode();
		String id2 = lFuscus1.getUnitID() + "@" + lFuscus1.getSourceSystem().getCode();
		String id3 = "BLA DI BLA";
		String[] ids = new String[] { id1, id2, id3 };
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] specimens = dao.find(ids);
		assertNotNull("01", specimens);
		assertEquals("02", 2, specimens.length);
	}

	/*
	 * Test lookup of Parus major specimen using UnitID.
	 */
	@Test
	public void testFindByUnitID_01()
	{
		String unitID = pMajor.getUnitID();
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.findByUnitID(unitID);
		assertNotNull("01", result);
		assertEquals("02", 1, result.length);
		/*
		 * Make sure no weird analysis stuff is going on
		 */
		result = dao.findByUnitID("ZMA");
		assertNotNull("05", result);
		assertEquals("06", 0, result.length);
		result = dao.findByUnitID("100");
		assertNotNull("07", result);
		assertEquals("08", 0, result.length);
	}

	/*
	 * Test lookup of Larus fuscus specimen using UnitID.
	 */
	@Test
	public void testFindByUnitID_02()
	{
		String unitID0 = lFuscus1.getUnitID();
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.findByUnitID(unitID0);
		assertNotNull("01", result);
		assertEquals("02", 1, result.length);
		assertEquals("03", unitID0, result[0].getUnitID());
		/*
		 * Make sure no weird analysis stuff is going on
		 */
		result = dao.findByUnitID("L");
		assertNotNull("04", result);
		assertEquals("05", 0, result.length);
	}

	/*
	 * Test lookup using non-existent UnitID.
	 */
	@Test
	public void testFindByUnitID_03()
	{
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.findByUnitID("BLA DI BLA");
		assertNotNull("01", result);
		assertEquals("02", 0, result.length);
	}

	/*
	 * Tests findByCollector using Parus major specimen.
	 */
	@Test
	public void testFindByCollector_01()
	{
		SpecimenDAO dao = new SpecimenDAO();
		Person person = pMajor.getGatheringEvent().getGatheringPersons().get(0);
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

	/*
	 * Tests query method with a single EQUALS query condition.
	 */
	@Test
	public void testQuery__QuerySpec__01() throws InvalidQueryException
	{
		String unitID = pMajor.getUnitID();
		Condition condition = new Condition("unitID", EQUALS, unitID);
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
		assertEquals("02", pMajor.getUnitID(), result[0].getUnitID());
	}

	/*
	 * Tests query method with two EQUALS query conditions combined using AND.
	 */
	@Test
	public void testQuery__QuerySpec__02() throws InvalidQueryException
	{
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, "=", "Parus");
		condition.and(species, EQUALS, "major");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
	}

	/*
	 * Tests query method with using two ANDed conditions, one known to be
	 * false.
	 */
	@Test
	public void testQuery__QuerySpec__03() throws InvalidQueryException
	{
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, EQUALS, "Parus");
		condition.and(species, EQUALS, "BLA DI BLA");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 0, result.length);
	}

	/*
	 * Tests query method with two EQUALS query conditions combined using OR.
	 */
	@Test
	public void testQuery__QuerySpec__04() throws InvalidQueryException
	{
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, EQUALS, "Parus");
		condition.or(species, EQUALS, "major");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
	}

	/*
	 * Tests query method with using two ORed conditions, one known to be false.
	 */
	@Test
	public void testQuery__QuerySpec__05() throws InvalidQueryException
	{
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, EQUALS, "Parus");
		condition.or(species, EQUALS, "BLA DI BLA");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
	}

	/*
	 * Tests query method with using two ORed conditions, both known to be
	 * false.
	 */
	@Test
	public void testQuery__QuerySpec__06() throws InvalidQueryException
	{
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		Condition condition = new Condition(genus, EQUALS, "BLA DI BLA");
		condition.or(species, EQUALS, "BLA DI BLA");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 0, result.length);
	}

	/*
	 * Test query method with LIKE operator.
	 */
	@Test
	public void testQuery__QuerySpec__07() throws InvalidQueryException
	{
		String collector = "gatheringEvent.gatheringPersons.fullName";
		Condition condition = new Condition(collector, LIKE, "altenbu");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 3, result.length);
	}

	/*
	 * Tests query method with using NOT
	 */
	@Test
	public void testQuery__QuerySpec__08() throws InvalidQueryException
	{
		String genus = "identifications.defaultClassification.genus";
		// This excludes pMajorSpecimen01 (so 4 remaining)
		Condition condition = new Condition(NOT, genus, EQUALS, "Parus");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 4, result.length);
	}

	/*
	 * Tests query method with using AND NOT
	 */
	@Test
	public void testQuery__QuerySpec__09() throws InvalidQueryException
	{
		String genus = "identifications.defaultClassification.genus";
		String collector = "gatheringEvent.gatheringPersons.fullName";
		String sourceSystem = "sourceSystem.code";
		Condition condition = new Condition(genus, EQUALS, "Larus");
		condition.and(collector, LIKE, "altenburg");
		condition.andNot(sourceSystem, EQUALS, "NDFF");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
	}

	/*
	 * Tests query method using negated conditions
	 */
	@Test
	public void testQuery__QuerySpec__10() throws InvalidQueryException
	{
		String genus = "identifications.defaultClassification.genus";
		String sourceSystem = "sourceSystem.code";
		// This excludes larusFuscusSpecimen01 and larusFuscusSpecimen02
		Condition condition = new Condition(genus, NOT_EQUALS, "Larus");
		// This excludes parusMajorSpecimen01 and tRexSpecimen01
		condition.andNot(sourceSystem, EQUALS, "CRS");
		// This excludes (again) larusFuscusSpecimen02
		condition.andNot(sourceSystem, EQUALS, "NDFF");
		// Remains: malusSylvestrisSpecimen01
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 1, result.length);
	}

	/*
	 * Tests query method with operator EQUALS_IC
	 */
	@Test
	public void testQuery__QuerySpec__11() throws InvalidQueryException
	{
		Condition condition = new Condition("recordBasis", EQUALS_IC, "preserved specimen");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 3, result.length);
	}

	/*
	 * Tests query method with operator NOT_EQUALS_IC
	 */
	@Test
	public void testQuery__QuerySpec__12() throws InvalidQueryException
	{
		// This excludes pMajorSpecimen01, tRexSpecimen01 and
		// larusFuscusSpecimen01 (2 remaining).
		Condition condition = new Condition("recordBasis", NOT_EQUALS_IC, "preserved specimen");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 2, result.length);
	}

	/*
	 * Tests query method with a combination of deeply nested AND and OR
	 * conditions.
	 */
	@Test
	public void testQuery__QuerySpec__13() throws InvalidQueryException
	{
		String system = "sourceSystem.code";
		String country = "gatheringEvent.country";
		String locality = "gatheringEvent.localityText";
		Condition condition1, condition2, condition3, condition4;
		// This leaves 2 specimens (tRexSpecimen01 and malusSylvestrisSpecimen01)
		condition1 = new Condition("sex", EQUALS, "female");
		// This still leaves tRexSpecimen01 and malusSylvestrisSpecimen01		
		condition2 = new Condition(system, "=", "BRAHMS").or(system, "=", "CRS");
		// This exludes malusSylvestrisSpecimen01 (collected in United Kingdom)	
		condition3 = new Condition(country, "=", "United States");
		condition3.and(locality, LIKE, "Montana");
		// This exludes tRexSpecimen01 (collected in United States)	
		condition4 = new Condition(country, "=", "United Kingdom");
		condition4.and(locality, LIKE, "Dorchester");
		// But this will include them both again: condition3.or(condition4);
		condition1.and(condition2).and(condition3.or(condition4));
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition1);
		SpecimenDAO dao = new SpecimenDAO();
		Specimen[] result = dao.query(qs);
		assertEquals("01", 2, result.length);
	}

	/*
	 * Tests getIdsInCollection for the one and only document belonging to the
	 * "Strange Plants" theme.
	 */
	@Test
	public void testGetIdsInCollection__String__01()
	{
		String theme = "Strange Plants";
		SpecimenDAO dao = new SpecimenDAO();
		String[] ids = dao.getIdsInCollection(theme);
		assertEquals("01", 1, ids.length);
	}

	/*
	 * Tests getIdsInCollection for the documents belonging to the
	 * "Living Dinos" theme.
	 */
	@Test
	public void testGetIdsInCollection__String__02()
	{
		String theme = "Living Dinos";
		SpecimenDAO dao = new SpecimenDAO();
		String[] ids = dao.getIdsInCollection(theme);
		assertEquals("01", 3, ids.length);
	}

	/*
	 * Tests save method.
	 */
	@Test
	public void testSave__Specimen__01()
	{
		Specimen toBeSaved = SpecimenTransfer.load(pMajor, null);
		SpecimenDAO dao = new SpecimenDAO();
		String id = dao.save(toBeSaved, true);
		assertNotNull("01", id);
		Specimen retrieved = dao.find(id);
		assertNotNull("02", retrieved);
		assertEquals("03", pMajor.getUnitID(), retrieved.getUnitID());
	}

}
