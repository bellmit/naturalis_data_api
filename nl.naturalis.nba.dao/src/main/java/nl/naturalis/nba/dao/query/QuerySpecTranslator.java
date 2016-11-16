package nl.naturalis.nba.dao.query;

import static nl.naturalis.nba.api.query.LogicalOperator.AND;
import static nl.naturalis.nba.common.json.JsonUtil.toPrettyJson;
import static nl.naturalis.nba.dao.DaoUtil.getLogger;
import static nl.naturalis.nba.dao.query.ConditionTranslatorFactory.getTranslator;
import static nl.naturalis.nba.dao.util.ESUtil.newSearchRequest;
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;

import nl.naturalis.nba.api.query.Condition;
import nl.naturalis.nba.api.query.InvalidConditionException;
import nl.naturalis.nba.api.query.InvalidQueryException;
import nl.naturalis.nba.api.query.QuerySpec;
import nl.naturalis.nba.api.query.SortField;
import nl.naturalis.nba.common.es.map.MappingInfo;
import nl.naturalis.nba.common.es.map.NoSuchFieldException;
import nl.naturalis.nba.dao.DocumentType;

/**
 * A {@code QuerySpecTranslator} is responsible for translating an NBA
 * {@link QuerySpec} object into an Elasticsearch {@link SearchRequestBuilder}
 * object.
 * 
 * @author Ayco Holleman
 *
 */
public class QuerySpecTranslator {

	private static final Logger logger = getLogger(QuerySpecTranslator.class);

	private QuerySpec spec;
	private DocumentType<?> dt;

	public QuerySpecTranslator(QuerySpec querySpec, DocumentType<?> documentType)
	{
		this.spec = querySpec;
		this.dt = documentType;
	}

	public SearchRequestBuilder translate() throws InvalidQueryException
	{
		if (logger.isDebugEnabled()) {
			logger.debug("Translating QuerySpec:\n{}", toPrettyJson(spec));
		}
		QueryBuilder query = translateConditions();
		ConstantScoreQueryBuilder csq = constantScoreQuery(query);
		SearchRequestBuilder request = newSearchRequest(dt);
		if (spec.getFields() != null) {
			addFields(request);
		}
		request.setQuery(csq);
		request.setFrom(spec.getFrom());
		request.setSize(spec.getSize());
		if (spec.getSortFields() != null) {
			List<SortField> fields = spec.getSortFields();
			SortFieldsTranslator sft = new SortFieldsTranslator(fields, dt);
			for (SortBuilder sb : sft.translate()) {
				request.addSort(sb);
			}
		}
		return request;
	}

	private void addFields(SearchRequestBuilder request) throws InvalidQueryException
	{
		MappingInfo<?> mappingInfo = new MappingInfo<>(dt.getMapping());
		List<String> fields = spec.getFields();
		for (String field : fields) {
			if (field.equals("id")) {
				/*
				 * This is a special field that can be used to retrieve the
				 * Elasticsearch document ID, which is not part of the document
				 * itself.
				 */
				continue;
			}
			try {
				mappingInfo.getField(field);
			}
			catch (NoSuchFieldException e) {
				throw new InvalidQueryException(e.getMessage());
			}
		}
		String[] include = fields.toArray(new String[fields.size()]);
		request.setFetchSource(include, null);
	}

	private QueryBuilder translateConditions() throws InvalidConditionException
	{
		List<Condition> conditions = spec.getConditions();
		if (conditions == null || conditions.size() == 0) {
			return QueryBuilders.matchAllQuery();
		}
		if (conditions.size() == 1) {
			Condition c = conditions.iterator().next();
			return getTranslator(c, dt).translate();
		}
		BoolQueryBuilder result = QueryBuilders.boolQuery();
		if (spec.getLogicalOperator() == AND) {
			for (Condition c : conditions) {
				result.must(getTranslator(c, dt).translate());
			}
		}
		else {
			for (Condition c : conditions) {
				result.should(getTranslator(c, dt).translate());
			}
		}
		return result;
	}

}