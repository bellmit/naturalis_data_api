package nl.naturalis.nba.dao.query;

import static nl.naturalis.nba.common.es.map.MultiField.IGNORE_CASE_MULTIFIELD;
import static nl.naturalis.nba.dao.query.TranslatorUtil.ensureValueIsString;
import static nl.naturalis.nba.dao.query.TranslatorUtil.getNestedPath;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import org.elasticsearch.index.query.QueryBuilder;

import nl.naturalis.nba.api.InvalidConditionException;
import nl.naturalis.nba.api.QueryCondition;
import nl.naturalis.nba.common.es.map.MappingInfo;

class NotEqualsIgnoreCaseConditionTranslator extends ConditionTranslator {

	NotEqualsIgnoreCaseConditionTranslator(QueryCondition condition, MappingInfo<?> inspector)
	{
		super(condition, inspector);
	}

	@Override
	QueryBuilder translateCondition() throws InvalidConditionException
	{
		String field = condition.getField();
		String value = condition.getValue().toString().toLowerCase();
		String nestedPath = getNestedPath(condition, mappingInfo);
		String multiField = field + '.' + IGNORE_CASE_MULTIFIELD.getName();
		if (nestedPath == null || forSortField) {
			return boolQuery().mustNot(termQuery(multiField, value));
		}
		return boolQuery().mustNot(nestedQuery(nestedPath, termQuery(multiField, value)));
	}

	@Override
	void checkCondition() throws InvalidConditionException
	{
		ensureValueIsString(condition);
	}

}
