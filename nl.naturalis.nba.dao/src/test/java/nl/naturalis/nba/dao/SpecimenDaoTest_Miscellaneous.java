package nl.naturalis.nba.dao;

import static nl.naturalis.nba.api.ComparisonOperator.CONTAINS;
import static nl.naturalis.nba.api.ComparisonOperator.EQUALS;
import static nl.naturalis.nba.api.ComparisonOperator.EQUALS_IC;
import static nl.naturalis.nba.api.ComparisonOperator.NOT_EQUALS;
import static nl.naturalis.nba.api.ComparisonOperator.NOT_EQUALS_IC;
import static nl.naturalis.nba.api.UnaryBooleanOperator.NOT;
import static nl.naturalis.nba.dao.util.es.ESUtil.createIndex;
import static nl.naturalis.nba.dao.util.es.ESUtil.deleteIndex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import nl.naturalis.nba.api.InvalidQueryException;
import nl.naturalis.nba.api.QueryCondition;
import nl.naturalis.nba.api.QueryResult;
import nl.naturalis.nba.api.QuerySpec;
import nl.naturalis.nba.api.model.Specimen;
import nl.naturalis.nba.dao.mock.SpecimenMock;

public class SpecimenDaoTest_Miscellaneous {

	private static final Logger logger = DaoRegistry.getInstance()
			.getLogger(SpecimenDaoTest_Miscellaneous.class);

	static Specimen pMajor;
	static Specimen lFuscus1;
	static Specimen lFuscus2;
	static Specimen tRex;
	static Specimen mSylvestris;

	@BeforeClass
	public static void before()
	{
		logger.info("Starting tests");
		deleteIndex(DocumentType.SPECIMEN);
		createIndex(DocumentType.SPECIMEN);
		/*
		 * Insert 5 test specimens.
		 */
		pMajor = SpecimenMock.parusMajorSpecimen01();
		lFuscus1 = SpecimenMock.larusFuscusSpecimen01();
		lFuscus2 = SpecimenMock.larusFuscusSpecimen02();
		tRex = SpecimenMock.tRexSpecimen01();
		mSylvestris = SpecimenMock.malusSylvestrisSpecimen01();
		DaoTestUtil.saveSpecimens(pMajor, lFuscus1, lFuscus2, tRex, mSylvestris);
	}

	@After
	public void after()
	{
	}

	/*
	 * Test lookup using internal Elasticsearch ID.
	 */
	@Test
	public void test_find_01()
	{
		/*
		 * Internal Elasticsearch system IDs are generated by concatening UnitID
		 * and source system code, like so:
		 */
		String id = pMajor.getUnitID() + "@" + pMajor.getSourceSystem().getCode();
		SpecimenDao dao = new SpecimenDao();
		Specimen specimen = dao.find(id);
		assertNotNull("01", specimen);
	}

	@Test
	public void test_find_02()
	{
		/*
		 * Internal Elasticsearch system IDs are generated by concatening UnitID
		 * and source system code, like so:
		 */
		String id0 = pMajor.getUnitID() + "@" + pMajor.getSourceSystem().getCode();
		String id1 = lFuscus1.getUnitID() + "@" + lFuscus1.getSourceSystem().getCode();
		SpecimenDao dao = new SpecimenDao();
		Specimen[] specimens = dao.findByIds(new String[] { id0, id1 });
		assertEquals("01", 2, specimens.length);
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
		SpecimenDao dao = new SpecimenDao();
		Specimen[] specimens = dao.findByIds(ids);
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
		SpecimenDao dao = new SpecimenDao();
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
		SpecimenDao dao = new SpecimenDao();
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
		SpecimenDao dao = new SpecimenDao();
		Specimen[] result = dao.findByUnitID("BLA DI BLA");
		assertNotNull("01", result);
		assertEquals("02", 0, result.length);
	}

	/*
	 * Tests query method with a single EQUALS query condition.
	 */
	@Test
	public void testQuery__SearchSpec__01() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		String expected = pMajor.getUnitID();
		QueryCondition condition = new QueryCondition("unitID", EQUALS, expected);
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 1, result.size());
		String actual = result.get(0).getItem().getUnitID();
		assertEquals("02", expected, actual);
	}

	/*
	 * Tests query method with two EQUALS query conditions combined using AND.
	 */
	@Test
	public void testQuery__SearchSpec__02() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		QueryCondition condition = new QueryCondition(genus, EQUALS, "Parus");
		condition.and(species, EQUALS, "major");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 1, result.size());
	}

	/*
	 * Tests query method with using two ANDed conditions, one known to be
	 * false.
	 */
	@Test
	public void testQuery__SearchSpec__03() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		QueryCondition condition = new QueryCondition(genus, EQUALS, "Parus");
		condition.and(species, EQUALS, "BLA DI BLA");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 0, result.size());
	}

	/*
	 * Tests query method with two EQUALS query conditions combined using OR.
	 */
	@Test
	public void testQuery__SearchSpec__04() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		QueryCondition condition = new QueryCondition(genus, EQUALS, "Parus");
		condition.or(species, EQUALS, "major");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 1, result.size());
	}

	/*
	 * Tests query method with using two ORed conditions, one known to be false.
	 */
	@Test
	public void testQuery__SearchSpec__05() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		QueryCondition condition = new QueryCondition(genus, EQUALS, "Parus");
		condition.or(species, EQUALS, "BLA DI BLA");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 1, result.size());
	}

	/*
	 * Tests query method with using two ORed conditions, both known to be
	 * false.
	 */
	@Test
	public void testQuery__SearchSpec__06() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		String genus = "identifications.defaultClassification.genus";
		String species = "identifications.defaultClassification.specificEpithet";
		QueryCondition condition = new QueryCondition(genus, EQUALS, "BLA DI BLA");
		condition.or(species, EQUALS, "BLA DI BLA");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 0, result.size());
	}

	/*
	 * Tests query method with using NOT
	 */
	@Test
	public void testQuery__SearchSpec__08() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		String genus = "identifications.defaultClassification.genus";
		// This excludes pMajorSpecimen01 (so 4 remaining)
		QueryCondition condition = new QueryCondition(NOT, genus, EQUALS, "Parus");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 4, result.size());
	}

	/*
	 * Tests query method using negated conditions
	 */
	@Test
	public void testQuery__SearchSpec__10() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		String genus = "identifications.defaultClassification.genus";
		String sourceSystem = "sourceSystem.code";
		// This excludes larusFuscusSpecimen01 and larusFuscusSpecimen02
		QueryCondition condition = new QueryCondition(genus, NOT_EQUALS, "Larus");
		// This excludes parusMajorSpecimen01 and tRexSpecimen01
		condition.and(new QueryCondition(NOT, sourceSystem, EQUALS, "CRS"));
		// This excludes (again) larusFuscusSpecimen02
		condition.and(new QueryCondition(NOT, sourceSystem, EQUALS, "NDFF"));
		// Remains: malusSylvestrisSpecimen01
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 1, result.size());
	}

	/*
	 * Tests query method with operator EQUALS_IC
	 */
	@Test
	public void testQuery__SearchSpec__11() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		QueryCondition condition = new QueryCondition("recordBasis", EQUALS_IC,
				"PrEsErVeD sPeCiMeN");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 2, result.size());
	}

	/*
	 * Tests query method with operator NOT_EQUALS_IC
	 */
	@Test
	public void testQuery__SearchSpec__12() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		// This excludes pMajorSpecimen01, tRexSpecimen01 and
		// larusFuscusSpecimen01 (2 remaining).
		QueryCondition condition = new QueryCondition("recordBasis", NOT_EQUALS_IC,
				"PrEsErVeD sPeCiMeN");
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 3, result.size());
	}

	/*
	 * Tests query method with a combination of deeply nested AND and OR
	 * conditions.
	 */
	@Test
	public void testQuery__SearchSpec__13() throws InvalidQueryException
	{
		// TODO: move to more specific test class.
		String system = "sourceSystem.code";
		String country = "gatheringEvent.country";
		String locality = "gatheringEvent.localityText";
		QueryCondition condition1, condition2, condition3, condition4;
		// This leaves 2 specimens (tRexSpecimen01 and
		// malusSylvestrisSpecimen01)
		condition1 = new QueryCondition("sex", EQUALS, "female");
		// This still leaves tRexSpecimen01 and malusSylvestrisSpecimen01
		condition2 = new QueryCondition(system, EQUALS, "BRAHMS").or(system, EQUALS, "CRS");
		// This exludes malusSylvestrisSpecimen01 (collected in United Kingdom)
		condition3 = new QueryCondition(country, EQUALS, "United States");
		condition3.and(locality, CONTAINS, "Montana");
		// This exludes tRexSpecimen01 (collected in United States)
		condition4 = new QueryCondition(country, EQUALS, "United Kingdom");
		condition4.and(locality, CONTAINS, "Dorchester");
		// But this will include them both again: condition3.or(condition4);
		condition1.and(condition2).and(condition3.or(condition4));
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition1);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 2, result.size());
	}

	/*
	 * Tests getIdsInCollection for the one and only document belonging to the
	 * "Strange Plants" theme.
	 */
	@Test
	public void testGetIdsInCollection__String__01()
	{
		String theme = "Strange Plants";
		SpecimenDao dao = new SpecimenDao();
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
		SpecimenDao dao = new SpecimenDao();
		String[] ids = dao.getIdsInCollection(theme);
		assertEquals("01", 3, ids.length);
	}

	/*
	 * Test count with and without QuerySpec
	 */
	@Test
	public void testCount() throws InvalidQueryException
	{
	  SpecimenDao dao = new SpecimenDao();

	  assertEquals("01", 5L, dao.count(null));
	  QuerySpec querySpec = new QuerySpec();
	  QueryCondition condition = new QueryCondition("gatheringEvent.country", EQUALS, "Netherlands");
	  querySpec.addCondition(condition);
	  assertEquals("02", 3L, dao.count(querySpec));
	}
	
	/*
	 * Test countDistinctValues
	 */
	@Test
	public void testCountDistinctValues() throws InvalidQueryException
	{
	  SpecimenDao dao = new SpecimenDao();
	  
	  // Test without nested path
	  String field = "phaseOrStage";
	  assertEquals("01", 3L, dao.countDistinctValues(field, null));
    QuerySpec querySpec = new QuerySpec();
    QueryCondition condition = new QueryCondition("sourceSystem.code", EQUALS, "BRAHMS");
    querySpec.addCondition(condition);
    assertEquals("02", 1L, dao.countDistinctValues(field, querySpec));
	  
    // Test with nested path
    field = "identifications.scientificName.genusOrMonomial";
    assertEquals("03", 4L, dao.countDistinctValues(field, null));
    querySpec = new QuerySpec();
    condition = new QueryCondition("sourceSystem.code", EQUALS, "CRS");
    querySpec.addCondition(condition);
    assertEquals("04", 3L, dao.countDistinctValues(field, querySpec));
	}
	
	/*
	 * Test getDistinctValues with simple field an no QuerySpec
	 */
	@Test
	@Ignore
	public void testGetDistinctValues_01() throws InvalidQueryException
	{
		SpecimenDao dao = new SpecimenDao();
		
		Map<String, Long> result = dao.getDistinctValues("recordBasis", null);
		assertEquals("01", 3, result.size());
		Iterator<Map.Entry<String, Long>> entries = result.entrySet().iterator();
		Map.Entry<String, Long> entry = entries.next();
		assertEquals("02", "Preserved specimen", entry.getKey());
		assertEquals("03", Long.valueOf(2), entry.getValue());
		entry = entries.next();
		assertEquals("04", "Herbarium sheet", entry.getKey());
		assertEquals("05", Long.valueOf(1), entry.getValue());
    entry = entries.next();
    assertEquals("06", "FossileSpecimen", entry.getKey());
    assertEquals("07", Long.valueOf(1), entry.getValue());
	}

	/*
	 * Test getDistinctValues with nested field an no QuerySpec
	 */
	@Test
	@Ignore
	public void testGetDistinctValues_02() throws InvalidQueryException
	{
		SpecimenDao dao = new SpecimenDao();
		String field = "identifications.defaultClassification.genus";
		Map<String, Long> result = dao.getDistinctValues(field, null);
		// Genus Larus occurs twice
		assertEquals("01", 4, result.size());
	}

  @Test
  @Ignore
  public void testGetDistinctValuesPerGroup() throws InvalidQueryException
  {
    SpecimenDao dao = new SpecimenDao();
    String field = "collectionType";
    String group = "sourceSystem.code";
    List<Map<String, Object>> result = dao.getDistinctValuesPerGroup(field, group, null);
    logger.info(result);
    // assertEquals("01", 4, result.size());
  }

	
}
