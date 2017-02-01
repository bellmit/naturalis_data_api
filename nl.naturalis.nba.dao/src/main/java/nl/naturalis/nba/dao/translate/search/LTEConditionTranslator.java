package nl.naturalis.nba.dao.translate.search;

import org.elasticsearch.index.query.RangeQueryBuilder;

import nl.naturalis.nba.api.SearchCondition;
import nl.naturalis.nba.common.es.map.MappingInfo;

class LTEConditionTranslator extends RangeConditionTranslator {

	LTEConditionTranslator(SearchCondition condition, MappingInfo<?> mappingInfo)
	{
		super(condition, mappingInfo);
	}

	@Override
	void setRange(RangeQueryBuilder query)
	{
		query.lte(condition.getValue());
	}

}
