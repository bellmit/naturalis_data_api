package nl.naturalis.nba.dao.es.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import nl.naturalis.nba.api.annotations.MappedProperty;
import nl.naturalis.nba.api.model.GeoPoint;
import nl.naturalis.nba.api.model.NBADomainObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ESGatheringSiteCoordinates extends NBADomainObject {

	private Double longitudeDecimal;
	private Double latitudeDecimal;

	private String gridCellSystem;
	private Double gridLatitudeDecimal;
	private Double gridLongitudeDecimal;
	private String gridCellCode;
	private String gridQualifier;

	public ESGatheringSiteCoordinates()
	{
	}

	public ESGatheringSiteCoordinates(Double latitude, Double longitude)
	{
		this.longitudeDecimal = longitude;
		this.latitudeDecimal = latitude;
	}

	@MappedProperty
	public GeoPoint getPoint()
	{
		if (longitudeDecimal == null || latitudeDecimal == null) {
			return null;
		}
		return new GeoPoint(longitudeDecimal, latitudeDecimal);
	}

	public Double getLongitudeDecimal()
	{
		return longitudeDecimal;
	}

	public void setLongitudeDecimal(Double longitudeDecimal)
	{
		this.longitudeDecimal = longitudeDecimal;
	}

	public Double getLatitudeDecimal()
	{
		return latitudeDecimal;
	}

	public void setLatitudeDecimal(Double latitudeDecimal)
	{
		this.latitudeDecimal = latitudeDecimal;
	}

	public String getGridCellSystem()
	{
		return gridCellSystem;
	}

	public void setGridCellSystem(String gridCellSystem)
	{
		this.gridCellSystem = gridCellSystem;
	}

	public Double getGridLatitudeDecimal()
	{
		return gridLatitudeDecimal;
	}

	public void setGridLatitudeDecimal(Double gridLatitudeDecimal)
	{
		this.gridLatitudeDecimal = gridLatitudeDecimal;
	}

	public Double getGridLongitudeDecimal()
	{
		return gridLongitudeDecimal;
	}

	public void setGridLongitudeDecimal(Double gridLongitudeDecimal)
	{
		this.gridLongitudeDecimal = gridLongitudeDecimal;
	}

	public String getGridCellCode()
	{
		return gridCellCode;
	}

	public void setGridCellCode(String gridCellCode)
	{
		this.gridCellCode = gridCellCode;
	}

	public String getGridQualifier()
	{
		return gridQualifier;
	}

	public void setGridQualifier(String gridQualifier)
	{
		this.gridQualifier = gridQualifier;
	}
}