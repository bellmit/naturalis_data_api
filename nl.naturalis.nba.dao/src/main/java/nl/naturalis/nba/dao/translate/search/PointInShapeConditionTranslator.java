package nl.naturalis.nba.dao.translate.search;

import static org.elasticsearch.index.query.QueryBuilders.geoPolygonQuery;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.QueryBuilder;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;

import nl.naturalis.nba.api.InvalidConditionException;
import nl.naturalis.nba.api.Path;
import nl.naturalis.nba.api.SearchCondition;
import nl.naturalis.nba.common.es.map.MappingInfo;

@Deprecated
class PointInShapeConditionTranslator extends ConditionTranslator {

	PointInShapeConditionTranslator(SearchCondition condition, MappingInfo<?> mappingInfo)
	{
		super(condition, mappingInfo);
	}

	@Override
	QueryBuilder translateCondition() throws InvalidConditionException
	{
		Polygon polygon = (Polygon) condition.getValue();
		List<GeoPoint> points = new ArrayList<>(128);
		for (List<LngLatAlt> lngLatAlts : polygon.getCoordinates()) {
			for (LngLatAlt coord : lngLatAlts) {
				points.add(new GeoPoint(coord.getLatitude(), coord.getLongitude()));
			}
		}
		Path path = condition.getFields().iterator().next();
		return geoPolygonQuery(path.toString(),points);
	}

	@Override
	void checkCondition() throws InvalidConditionException
	{
		if (condition.getValue().getClass() != Polygon.class) {
			String msg = "Search term must be GeoJSON object of type \"polygon\"";
			throw new InvalidConditionException(msg);
		}
	}
}
