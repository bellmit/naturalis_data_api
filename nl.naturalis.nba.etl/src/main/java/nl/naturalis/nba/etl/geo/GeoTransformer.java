package nl.naturalis.nba.etl.geo;

import static nl.naturalis.nba.etl.geo.GeoCsvField.country_nl;
import static nl.naturalis.nba.etl.geo.GeoCsvField.geojson;
import static nl.naturalis.nba.etl.geo.GeoCsvField.gid;
import static nl.naturalis.nba.etl.geo.GeoCsvField.iso;
import static nl.naturalis.nba.etl.geo.GeoCsvField.locality;
import static nl.naturalis.nba.etl.geo.GeoCsvField.source;
import static nl.naturalis.nba.etl.geo.GeoCsvField.type;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.geojson.GeoJsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.naturalis.nba.api.model.GeoArea;
import nl.naturalis.nba.api.model.SourceSystem;
import nl.naturalis.nba.etl.AbstractCSVTransformer;
import nl.naturalis.nba.etl.ETLStatistics;

class GeoTransformer extends AbstractCSVTransformer<GeoCsvField, GeoArea> {

	private ObjectMapper mapper = new ObjectMapper();

	GeoTransformer(ETLStatistics stats)
	{
		super(stats);
	}

	@Override
	protected String getObjectID()
	{
		return input.get(gid);
	}

	@Override
	protected List<GeoArea> doTransform()
	{
		stats.objectsProcessed++;
		String geoJson = input.get(geojson);
		if(geoJson == null) {
			error("Missing value for field geojson");
			stats.recordsRejected++;
			return null;
		}
		stats.recordsAccepted++;
		GeoArea area = new GeoArea();
		area.setSourceSystem(SourceSystem.GEO);
		area.setSourceSystemId(input.get(gid));
		area.setAreaType(input.get(type));
		area.setCountryNL(input.get(country_nl));
		GeoJsonObject obj;
		try {
			obj = mapper.readValue(geoJson, GeoJsonObject.class);
		}
		catch (IOException e) {
			logger.error(e.getMessage());
			return null;
		}
		area.setShape(obj);
		area.setIsoCode(input.get(iso));
		area.setLocality(input.get(locality));
		area.setSource(input.get(source));
		return Arrays.asList(area);
	}

}
