package nl.naturalis.nba.dao.query;

import static nl.naturalis.nba.dao.query.TranslatorUtil.getNestedPath;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.QueryBuilder;

import nl.naturalis.nba.api.InvalidConditionException;
import nl.naturalis.nba.api.QueryCondition;
import nl.naturalis.nba.common.es.map.MappingInfo;

/**
 * This ConditionTranslator is called when Condition.operator is NOT_EQUALS and
 * Condition.value is null. By not having this situation handled by the
 * {@link EqualsConditionTranslator}, we keep our code and the generated
 * Elasticsearch query easier to read.
 * 
 * @author Ayco Holleman
 *
 */
class IsNotNullConditionTranslator extends ConditionTranslator {

	IsNotNullConditionTranslator(QueryCondition condition, MappingInfo<?> inspector)
	{
		super(condition, inspector);
	}

	@Override
	QueryBuilder translateCondition() throws InvalidConditionException
	{
		String field = condition.getField();
		String nestedPath = getNestedPath(condition, mappingInfo);
		if (nestedPath == null || forSortField) {
			return existsQuery(field);
		}
		return nestedQuery(nestedPath, existsQuery(field), ScoreMode.None);
	}

	@Override
	void checkCondition() throws InvalidConditionException
	{
	}
}
