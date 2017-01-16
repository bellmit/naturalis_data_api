package nl.naturalis.nba.dao;

import static nl.naturalis.nba.api.ComparisonOperator.BETWEEN;
import static nl.naturalis.nba.api.ComparisonOperator.NOT_BETWEEN;
import static nl.naturalis.nba.dao.util.es.ESUtil.createIndex;
import static nl.naturalis.nba.dao.util.es.ESUtil.createType;
import static nl.naturalis.nba.dao.util.es.ESUtil.deleteIndex;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import nl.naturalis.nba.api.InvalidQueryException;
import nl.naturalis.nba.api.QueryCondition;
import nl.naturalis.nba.api.QueryResult;
import nl.naturalis.nba.api.QuerySpec;
import nl.naturalis.nba.api.model.Specimen;

@SuppressWarnings("static-method")
public class SpecimenDao_QueriesWithBetweenOperatorTest {

	static Specimen pMajor;
	static Specimen lFuscus1;
	static Specimen lFuscus2;
	static Specimen tRex;
	static Specimen mSylvestris;

	@BeforeClass
	public static void before()
	{
		deleteIndex(DocumentType.SPECIMEN);
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
		// dropIndex(Specimen.class);
	}

	@Test
	public void testQuery__QuerySpec__01() throws InvalidQueryException
	{
		Date gatheringDate = pMajor.getGatheringEvent().getDateTimeBegin();
		Instant instant = gatheringDate.toInstant();
		ZoneId dfault = ZoneId.systemDefault();
		OffsetDateTime two = OffsetDateTime.ofInstant(instant, dfault).minusDays(7L);
		OffsetDateTime t = OffsetDateTime.ofInstant(instant, dfault).plusDays(7L);
		OffsetDateTime[] fromTo = new OffsetDateTime[] { two, t };
		QueryCondition condition = new QueryCondition("gatheringEvent.dateTimeBegin", BETWEEN, fromTo);
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		// Each test specimen has a gatheringEvent.dateTimeBegin that lies one
		// year after the next test specimen, so we can have only one query
		// result (pMajor).
		assertEquals("01", 1, result.size());
	}

	@Test
	public void testQuery__QuerySpec__02() throws InvalidQueryException
	{
		Date gatheringDate = pMajor.getGatheringEvent().getDateTimeBegin();
		Instant instant = gatheringDate.toInstant();
		ZoneId dfault = ZoneId.systemDefault();
		OffsetDateTime from = OffsetDateTime.ofInstant(instant, dfault).minusDays(7L);
		OffsetDateTime to = OffsetDateTime.ofInstant(instant, dfault).plusDays(7L);
		OffsetDateTime[] fromTo = new OffsetDateTime[] { from, to };
		QueryCondition condition = new QueryCondition("gatheringEvent.dateTimeBegin", NOT_BETWEEN, fromTo);
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		// Since we use NOT_BETWEEN, all specimens except pMajor should come
		// back.
		assertEquals("01", 4, result.size());
	}

	/*
	 * Test with plain "string dates"
	 */
	@Test
	public void testQuery__QuerySpec__03() throws InvalidQueryException
	{
		String from = "2007-04-01";
		String to = "2007-05-01";
		String[] fromTo = new String[] { from, to };
		QueryCondition condition = new QueryCondition("gatheringEvent.dateTimeBegin", BETWEEN, fromTo);
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 1, result.size());
	}

	/*
	 * Test with plain "string dates"
	 */
	@Test
	public void testQuery__QuerySpec__04() throws InvalidQueryException
	{
		String from = "2007-04-01T00:00:00Z";
		String to = "2007-05-01T23:50:00Z";
		String[] fromTo = new String[] { from, to };
		QueryCondition condition = new QueryCondition("gatheringEvent.dateTimeBegin", BETWEEN, fromTo);
		QuerySpec qs = new QuerySpec();
		qs.addCondition(condition);
		SpecimenDao dao = new SpecimenDao();
		QueryResult<Specimen> result = dao.query(qs);
		assertEquals("01", 1, result.size());
	}
}
