package nl.naturalis.nba.dao;

import static nl.naturalis.nba.api.query.ComparisonOperator.EQUALS;
import static nl.naturalis.nba.dao.DaoUtil.getLogger;
import static nl.naturalis.nba.dao.DocumentType.GEO_AREA;
import static nl.naturalis.nba.dao.util.ESUtil.executeSearchRequest;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.geojson.GeoJsonObject;

import nl.naturalis.nba.api.IGeoAreaAccess;
import nl.naturalis.nba.api.KeyValuePair;
import nl.naturalis.nba.api.model.GeoArea;
import nl.naturalis.nba.api.query.Condition;
import nl.naturalis.nba.api.query.InvalidQueryException;
import nl.naturalis.nba.api.query.QuerySpec;
import nl.naturalis.nba.dao.exception.DaoException;
import nl.naturalis.nba.dao.query.QuerySpecTranslator;

public class GeoAreaDao extends NbaDao<GeoArea> implements IGeoAreaAccess {

	private static final Logger logger = getLogger(GeoAreaDao.class);

	private static List<KeyValuePair<String, String>> localities;
	private static List<KeyValuePair<String, String>> isoCodes;

	public GeoAreaDao()
	{
		super(GEO_AREA);
	}

	@Override
	public String getIdForLocality(String locality)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("getIdForLocality(\"{}\")", locality);
		}
		QuerySpec qs = new QuerySpec();
		qs.addCondition(new Condition("locality", EQUALS, locality));
		QuerySpecTranslator translator = new QuerySpecTranslator(qs, GEO_AREA);
		SearchRequestBuilder request;
		try {
			request = translator.translate();
			request.setNoFields();
		}
		catch (InvalidQueryException e) {
			// We made this one outselves, so eh ...
			throw new DaoException(e);
		}
		SearchResponse response = executeSearchRequest(request);
		SearchHit[] hits = response.getHits().getHits();
		if (hits.length == 0) {
			return null;
		}
		return hits[0].getId();
	}

	@Override
	public String getIdForIsoCode(String isoCode)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("getIdForIsoCode(\"{}\")", isoCode);
		}
		QuerySpec qs = new QuerySpec();
		qs.addCondition(new Condition("isoCode", EQUALS, isoCode));
		QuerySpecTranslator translator = new QuerySpecTranslator(qs, GEO_AREA);
		SearchRequestBuilder request;
		try {
			request = translator.translate();
			request.setNoFields();
		}
		catch (InvalidQueryException e) {
			// We made this one outselves, so eh ...
			throw new DaoException(e);
		}
		SearchResponse response = executeSearchRequest(request);
		SearchHit[] hits = response.getHits().getHits();
		if (hits.length == 0) {
			return null;
		}
		return hits[0].getId();
	}

	@Override
	public GeoJsonObject getGeoJsonForId(String id)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("getGeoJsonForId(\"{}\")", id);
		}
		GeoArea area = find(id);
		if (area == null) {
			return null;
		}
		return area.getShape();
	}

	@Override
	public List<KeyValuePair<String, String>> getLocalities()
	{
		if (logger.isDebugEnabled()) {
			logger.debug("getLocalities()");
		}
		if (localities == null) {
			QuerySpec qs = new QuerySpec();
			qs.addFields("locality");
			qs.sortBy("locality");
			qs.setSize(2500); // Something big
			QuerySpecTranslator translator = new QuerySpecTranslator(qs, GEO_AREA);
			SearchRequestBuilder request;
			try {
				request = translator.translate();
			}
			catch (InvalidQueryException e) {
				// We made this one outselves, so eh ...
				throw new DaoException(e);
			}
			SearchResponse response = executeSearchRequest(request);
			SearchHit[] hits = response.getHits().getHits();
			localities = new ArrayList<>(hits.length);
			for (SearchHit hit : hits) {
				Object value = hit.getSource().get("locality");
				if (value == null) {
					continue;
				}
				localities.add(new KeyValuePair<String, String>(value.toString(), hit.getId()));
			}
		}
		return localities;
	}

	@Override
	public List<KeyValuePair<String, String>> getIsoCodes()
	{
		if (logger.isDebugEnabled()) {
			logger.debug("getIsoCodes()");
		}
		if (isoCodes == null) {
			QuerySpec qs = new QuerySpec();
			qs.addFields("isoCode");
			qs.sortBy("isoCode");
			qs.setSize(2500); // Something big
			QuerySpecTranslator translator = new QuerySpecTranslator(qs, GEO_AREA);
			SearchRequestBuilder request;
			try {
				request = translator.translate();
			}
			catch (InvalidQueryException e) {
				// We made this one outselves, so eh ...
				throw new DaoException(e);
			}
			SearchResponse response = executeSearchRequest(request);
			SearchHit[] hits = response.getHits().getHits();
			isoCodes = new ArrayList<>(hits.length);
			for (SearchHit hit : hits) {
				Object value = hit.getSource().get("isoCode");
				if (value == null) {
					continue;
				}
				isoCodes.add(new KeyValuePair<String, String>(value.toString(), hit.getId()));
			}
		}
		return isoCodes;
	}

	@Override
	GeoArea[] createDocumentObjectArray(int length)
	{
		return new GeoArea[length];
	}

}
