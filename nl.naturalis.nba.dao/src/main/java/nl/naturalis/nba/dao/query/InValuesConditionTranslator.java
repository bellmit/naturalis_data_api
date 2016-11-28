package nl.naturalis.nba.dao.query;

import static nl.naturalis.nba.dao.query.TranslatorUtil.getNestedPath;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;

import nl.naturalis.nba.api.query.Condition;
import nl.naturalis.nba.api.query.InvalidConditionException;
import nl.naturalis.nba.common.es.map.MappingInfo;

/**
 * Translates conditions with an IN or NOT_IN operator when used with non-Geo
 * data types.
 * 
 * @author Ayco Holleman
 *
 */
class InValuesConditionTranslator extends ConditionTranslator {

	InValuesConditionTranslator(Condition condition, MappingInfo<?> inspector)
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
		if (nestedPath == null) {
			return query;
		}
		return nestedQuery(nestedPath, query);
	}

	@Override
	void checkOperatorValueCombi() throws InvalidConditionException
	{
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
