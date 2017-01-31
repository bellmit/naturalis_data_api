package nl.naturalis.nba.dao.translate.query;

import static nl.naturalis.nba.common.es.map.ESDataType.DATE;
import static nl.naturalis.nba.dao.translate.query.TranslatorUtil.convertValuesForDateField;
import static nl.naturalis.nba.dao.translate.query.TranslatorUtil.ensureValueIsNotNull;
import static nl.naturalis.nba.dao.translate.query.TranslatorUtil.getESField;
import static nl.naturalis.nba.dao.translate.query.TranslatorUtil.getNestedPath;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

import java.util.List;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;

import nl.naturalis.nba.api.InvalidConditionException;
import nl.naturalis.nba.api.QueryCondition;
import nl.naturalis.nba.common.es.map.ESField;
import nl.naturalis.nba.common.es.map.MappingInfo;

/**
 * Translates conditions with an IN or NOT_IN operator when used with non-Geo
 * data types.
 * 
 * @author Ayco Holleman
 *
 */
class InConditionTranslator extends ConditionTranslator {

	InConditionTranslator(QueryCondition condition, MappingInfo<?> inspector)
	{
		super(condition, inspector);
	}

	@Override
	QueryBuilder translateCondition() throws InvalidConditionException
	{
		InValuesBuilder ivb = new InValuesBuilder(condition.getValue());
		QueryBuilder query;
		if (ivb.containsNull()) {
			if (ivb.getValues().size() == 0) {
				query = isNull();
			}
			else {
				query = isNullOrOneOf(ivb.getValues());
			}
		}
		else {
			query = isOneOf(ivb.getValues());
		}
		String nestedPath = getNestedPath(condition, mappingInfo);
		if (nestedPath == null || forSortField) {
			return query;
		}
		return nestedQuery(nestedPath, query, ScoreMode.None);
	}

	@Override
	void checkCondition() throws InvalidConditionException
	{
		ensureValueIsNotNull(condition);
		ESField field = getESField(condition, mappingInfo);
		if (field.getType() == DATE) {
			convertValuesForDateField(condition);
		}
	}

	private QueryBuilder isNullOrOneOf(List<?> values)
	{
		BoolQueryBuilder boolQuery = boolQuery();
		boolQuery.should(isOneOf(values));
		boolQuery.should(isNull());
		return boolQuery;
	}

	private TermsQueryBuilder isOneOf(List<?> values)
	{
		return termsQuery(condition.getField(), values);
	}

	private BoolQueryBuilder isNull()
	{
		String field = condition.getField();
		return boolQuery().mustNot(existsQuery(field));
	}

}