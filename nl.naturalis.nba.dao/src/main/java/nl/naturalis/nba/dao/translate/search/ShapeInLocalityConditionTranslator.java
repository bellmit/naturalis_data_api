package nl.naturalis.nba.dao.translate.search;

import static nl.naturalis.nba.api.ComparisonOperator.EQUALS;
import static nl.naturalis.nba.dao.DaoUtil.getLogger;
import static nl.naturalis.nba.dao.DocumentType.GEO_AREA;
import static nl.naturalis.nba.dao.translate.search.TranslatorUtil.ensureValueIsNotNull;
import static nl.naturalis.nba.dao.translate.search.TranslatorUtil.getNestedPath;
import static nl.naturalis.nba.dao.translate.search.TranslatorUtil.searchTermHasWrongType;
import static nl.naturalis.nba.dao.util.es.ESUtil.executeSearchRequest;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoShapeQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.geojson.GeoJsonObject;

import nl.naturalis.nba.api.InvalidConditionException;
import nl.naturalis.nba.api.InvalidQueryException;
import nl.naturalis.nba.api.Path;
import nl.naturalis.nba.api.QueryCondition;
import nl.naturalis.nba.api.QuerySpec;
import nl.naturalis.nba.api.SearchCondition;
import nl.naturalis.nba.common.es.map.MappingInfo;
import nl.naturalis.nba.dao.exception.DaoException;
import nl.naturalis.nba.dao.translate.query.QuerySpecTranslator;

/**
 * Translates conditions with an IN or NOT_IN operator when used with fields of
 * type {@link GeoJsonObject} and with a {@link SearchCondition#getValue()
 * search term of type {@link String}, supposedly specifying a geographical name
 * like "Amsterdam" or "France".
 * 
 * @author Ayco Holleman
 *
 */
class ShapeInLocalityConditionTranslator extends ConditionTranslator {

	private static final Logger logger = getLogger(ShapeInLocalityConditionTranslator.class);

	ShapeInLocalityConditionTranslator(SearchCondition condition, MappingInfo<?> mappingInfo)
	{
		super(condition, mappingInfo);
	}

	@Override
	QueryBuilder translateCondition() throws InvalidConditionException
	{
		QueryBuilder query;
		String[] localities = getLocality(condition.getValue());
		if (localities.length == 1) {
			query = createQueryForLocality(localities[0]);
		}
		else {
			query = boolQuery();
			for (String locality : localities) {
				QueryBuilder qb = createQueryForLocality(locality);
				((BoolQueryBuilder) query).should(qb);
			}
		}

		if (forSortField) {
			return query;
		}
		Path path = condition.getFields().iterator().next();
		String nestedPath = getNestedPath(path, mappingInfo);
		if (nestedPath != null) {
			query = nestedQuery(nestedPath, query, ScoreMode.None);
		}
		if (condition.isFilter().booleanValue()) {
			query = constantScoreQuery(query);
		}
		else if (condition.getBoost() != null) {
			query.boost(condition.getBoost());
		}
		return query;
	}

	@Override
	void checkCondition() throws InvalidConditionException
	{
		ensureValueIsNotNull(condition);
	}

	private String[] getLocality(Object value) throws InvalidConditionException
	{
		String[] localities;
		if (value instanceof CharSequence) {
			localities = new String[] { value.toString() };
		}
		else if (value.getClass().isArray()) {
			Object[] values = (Object[]) value;
			localities = new String[values.length];
			System.arraycopy(values, 0, localities, 0, values.length);
		}
		else if (value instanceof Collection) {
			Collection<?> values = (Collection<?>) value;
			localities = new String[values.size()];
			int i = 0;
			for (Object obj : values) {
				localities[i++] = obj.toString();
			}
		}
		else {
			throw searchTermHasWrongType(condition);
		}
		return localities;
	}

	private QueryBuilder createQueryForLocality(String locality) throws InvalidConditionException
	{
		String field = condition.getFields().iterator().next().toString();
		String id = getIdForLocality(locality);
		String index = GEO_AREA.getIndexInfo().getName();
		String type = GEO_AREA.getName();
		GeoShapeQueryBuilder query = geoShapeQuery(field, id, type);
		query.indexedShapeIndex(index);
		return query;
	}

	private static String getIdForLocality(String locality) throws InvalidConditionException
	{
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up document ID for locality \"{}\"", locality);
		}
		QuerySpec qs = new QuerySpec();
		qs.addCondition(new QueryCondition("locality", EQUALS, locality));
		QuerySpecTranslator translator = new QuerySpecTranslator(qs, GEO_AREA);
		SearchRequestBuilder request;
		try {
			request = translator.translate();
			request.setFetchSource(false);
		}
		catch (InvalidQueryException e) {
			// We made this one ourselves, so eh ...
			throw new DaoException(e);
		}
		SearchResponse response = executeSearchRequest(request);
		SearchHit[] hits = response.getHits().getHits();
		if (hits.length == 0) {
			String fmt = "No such locality: \"%s\"";
			String msg = String.format(fmt, locality);
			throw new InvalidConditionException(msg);
		}
		return hits[0].getId();
	}

}
