package nl.naturalis.nba.common.json;

import static nl.naturalis.nba.common.TestUtils.deserialize;
import static nl.naturalis.nba.common.TestUtils.stringEqualsFileContents;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geojson.GeoJsonObject;
import org.geojson.Polygon;
import org.junit.Test;

import nl.naturalis.nba.api.ComparisonOperator;
import nl.naturalis.nba.api.Path;
import nl.naturalis.nba.api.SearchCondition;
import nl.naturalis.nba.api.SearchSpec;
import nl.naturalis.nba.api.SortField;
import nl.naturalis.nba.api.SortOrder;

@SuppressWarnings("static-method")
public class JsonUtilTest {

	@Test
	public void testDeserialize()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			Map<String, Object> map = JsonUtil.deserialize(is);
			assertEquals("01", map.get("firstName"), "John");
			assertEquals("02", map.get("lastName"), "Smith");
			assertTrue("03", map.containsKey("hobbies"));
			assertNull("04", map.get("hobbies"));
			assertEquals("05", map.get("age"), 36);
			@SuppressWarnings("unchecked")
			Map<String, Object> address = (Map<String, Object>) map.get("address");
			assertEquals("06", address.get("number"), 1429);
			List<?> kids = (List<?>) map.get("kids");
			assertEquals("07", 3, kids.size());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_01()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			String s = (String) JsonUtil.readField(is, "address.country.name");
			assertEquals("01", "U.S.A", s);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_02()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			int i = (int) JsonUtil.readField(is, "address.number");
			assertEquals("01", 1429, i);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_03()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			List<?> kids = (List<?>) JsonUtil.readField(is, "kids");
			assertEquals("01", 3, kids.size());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_04()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			String kid = (String) JsonUtil.readField(is, "kids.0");
			assertEquals("01", "Mary", kid);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_05()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			String kid = (String) JsonUtil.readField(is, "kids.1");
			assertEquals("01", "Lisa", kid);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_06()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			String kid = (String) JsonUtil.readField(is, "kids.2");
			assertEquals("01", "Junior", kid);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_07()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			Object value = JsonUtil.readField(is, "kids.3");
			assertTrue("01", value == JsonUtil.MISSING_VALUE);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_08()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			Object value = JsonUtil.readField(is, "bla");
			assertTrue("01", value == JsonUtil.MISSING_VALUE);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_09()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			Object value = JsonUtil.readField(is, "bla.0");
			assertTrue("01", value == JsonUtil.MISSING_VALUE);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadField_10()
	{
		try (InputStream is = JsonUtilTest.class.getResourceAsStream("JsonUtilTest.json")) {
			Object value = JsonUtil.readField(is, "bla.bla");
			assertTrue("01", value == JsonUtil.MISSING_VALUE);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testDeserialize_02()
	{
		String input = "{\"type\":\"Polygon\",\"coordinates\":[[[4.713134765625,52.83595824834852],[5.1416015625,52.83595824834852],[5.1416015625,52.516220863930734],[4.713134765625,52.516220863930734],[4.713134765625,52.83595824834852]]]}";
		Object output = JsonUtil.deserialize(input, GeoJsonObject.class);
		assertEquals("01", Polygon.class, output.getClass());
	}

	@Test(expected = JsonDeserializationException.class)
	public void testDeserialize_03()
	{
		// Missing opening '{'
		String input = "\"type\":\"Polygon\",\"coordinates\":[[[4.713134765625,52.83595824834852],[5.1416015625,52.83595824834852],[5.1416015625,52.516220863930734],[4.713134765625,52.516220863930734],[4.713134765625,52.83595824834852]]]}";
		JsonUtil.deserialize(input, GeoJsonObject.class);
	}

	@Test(expected = JsonDeserializationException.class)
	public void testDeserialize_04()
	{
		// Missing opening '{' (we shouldn't even be able 
		// to cast this to a Map object)
		String input = "\"type\":\"Polygon\",\"coordinates\":[[[4.713134765625,52.83595824834852],[5.1416015625,52.83595824834852],[5.1416015625,52.516220863930734],[4.713134765625,52.516220863930734],[4.713134765625,52.83595824834852]]]}";
		JsonUtil.deserialize(input);
	}

	@Test(expected = JsonDeserializationException.class)
	public void testDeserialize_05()
	{
		// Valid JSON, but not convertible to GeoJsonObject
		String input = "false";
		JsonUtil.deserialize(input, GeoJsonObject.class);
	}

	@Test()
	public void testDeserialize_06()
	{
		// Just make sure there's nothing special going on here
		String input = "false";
		Object output = JsonUtil.deserialize(input, Boolean.class);
		assertEquals("01", Boolean.FALSE, output);
	}

	@Test(expected = JsonDeserializationException.class)
	public void testDeserialize_08()
	{
		// Just make absolutely sure we can't brew any GeoJsonObject
		// out of this
		String input = "Amsterdam";
		JsonUtil.deserialize(input, GeoJsonObject.class);
	}

	@Test(expected = JsonDeserializationException.class)
	public void testDeserialize_09()
	{
		// In fact, since it is not valid JSON in the first place,
		// even this should not work
		String input = "Amsterdam";
		JsonUtil.deserialize(input, String.class);
	}

	/*
	 * Make sure serialization of SortField object works as expected: notably
	 * the serialization of the path field. Serialization of Path objects should
	 * yield plain strings - nothing betraying the internals of the Path class.
	 * See the annotations in Path class.
	 */
	@Test
	public void testSerializeSortField_01()
	{
		SortField sf = new SortField("identifications.defaultClassification.genus", SortOrder.DESC);
		String serialized = JsonUtil.toPrettyJson(sf, false);
		String file = "json/JsonUtilTest__testSerializeSortField_01.json";
		//System.out.println(serialized);
		assertTrue("01", stringEqualsFileContents(serialized, file));
	}

	@Test
	public void testSerializeSortField_02()
	{
		SortField sf = new SortField("identifications.defaultClassification.genus");
		String serialized = JsonUtil.toPrettyJson(sf, false);
		String file = "json/JsonUtilTest__testSerializeSortField_02.json";
		//System.out.println(serialized);
		assertTrue("01", stringEqualsFileContents(serialized, file));
	}

	/*
	 * Make sure SortField deserialization works if only path is present (see
	 * json file).
	 */
	@Test
	public void testDeserializeSortField_01()
	{
		String file = "json/JsonUtilTest__testDeserializeSortField_01.json";
		SortField sf = deserialize(file, SortField.class);
		assertEquals("01", "unitID", sf.getPath().toString());
		assertNotNull("02", sf.getSortOrder());
		assertTrue("03", sf.isAscending());
	}

	/*
	 * Make sure SortField deserialization works if both path and sortOrder
	 * present (see json file).
	 */
	@Test
	public void testDeserializeSortField_02()
	{
		String file = "json/JsonUtilTest__testDeserializeSortField_02.json";
		SortField sf = deserialize(file, SortField.class);
		assertEquals("01", "unitID", sf.getPath().toString());
		assertEquals("02", SortOrder.DESC, sf.getSortOrder());
	}

	/*
	 * Test deserialization of SearchCondition.
	 */
	@Test
	public void testDeserializeSearchCondition_01()
	{
		String file = "json/JsonUtilTest__testDeserializeSearchCondition_01.json";
		SearchCondition condition = deserialize(file, SearchCondition.class);
		assertEquals("01", 1, condition.getFields().size());
		String field = condition.getFields().iterator().next().toString();
		assertEquals("02", "gatheringEvent.dateTimeBegin", field);
		assertEquals("03", ComparisonOperator.EQUALS, condition.getOperator());
		assertEquals("04", "2014-08-07", condition.getValue());
		assertTrue("05", 1.3f == condition.getBoost());
	}

	/*
	 * Test deserialization of SearchCondition.
	 */
	@Test
	public void testDeserializeSearchCondition_02()
	{
		String file = "json/JsonUtilTest__testDeserializeSearchCondition_02.json";
		SearchCondition condition = deserialize(file, SearchCondition.class);
		assertEquals("01", 2, condition.getFields().size());
	}

	/*
	 * Test deserialization of SearchSpec.
	 */
	@Test
	public void testDeserializeSearchSpec_01()
	{
		String file = "json/JsonUtilTest__testDeserializeSearchSpec_01.json";
		SearchSpec ss = deserialize(file, SearchSpec.class);
		assertEquals("01", 1, ss.getConditions().size());
		SearchCondition sc = ss.getConditions().iterator().next();
		String field = sc.getFields().iterator().next().toString();
		assertEquals("02", "gatheringEvent.dateTimeBegin", field);
		assertTrue("03", ss.isConstantScore());
	}

	/*
	 * Test deserialization of SearchSpec.
	 */
	@Test
	public void testDeserializeSearchSpec_02()
	{
		String file = "json/JsonUtilTest__testDeserializeSearchSpec_02.json";
		SearchSpec ss = deserialize(file, SearchSpec.class);
		assertEquals("01", 2, ss.getConditions().size());
		Iterator<SearchCondition> conditions = ss.getConditions().iterator();
		SearchCondition sc = conditions.next();
		assertEquals("02", 2, sc.getFields().size());
		Iterator<Path> fields = sc.getFields().iterator();
		Path path = fields.next();
		assertEquals("03", "unitID", path.toString());
		path = fields.next();
		assertEquals("04", "unitGUID", path.toString());
		sc = conditions.next();
		assertEquals("05", 1, sc.getFields().size());
		fields = sc.getFields().iterator();
		path = fields.next();
		assertEquals("06", "gatheringEvent.dateTimeBegin", path.toString());
	}

}
