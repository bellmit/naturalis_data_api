package nl.naturalis.nba.dao.es.transfer;

import nl.naturalis.nba.api.model.GatheringSiteCoordinates;
import nl.naturalis.nba.dao.es.types.ESGatheringSiteCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Transfer class used for transferring {@link ESGatheringSiteCoordinates} to
 * {@link GatheringSiteCoordinates}
 *
 * @author Byron Voorbach
 */
public class GatheringSiteCoordinatesTransfer {

	/**
	 * Static method to transfer the {@link ESGatheringSiteCoordinates} to a
	 * {@link GatheringSiteCoordinates} object
	 *
	 * @param in
	 *            the object to transfer
	 * @return the newly created {@link GatheringSiteCoordinates}
	 */
	public static List<GatheringSiteCoordinates> load(List<ESGatheringSiteCoordinates> in)
	{
		if (in == null) {
			return null;
		}
		List<GatheringSiteCoordinates> out = new ArrayList<>(in.size());
		for (ESGatheringSiteCoordinates esCoords : in) {
			GatheringSiteCoordinates coords = new GatheringSiteCoordinates();
			coords.setGridCellCode(esCoords.getGridCellCode());
			coords.setGridCellSystem(esCoords.getGridCellSystem());
			coords.setGridLatitudeDecimal(esCoords.getGridLatitudeDecimal());
			coords.setGridLongitudeDecimal(esCoords.getGridLongitudeDecimal());
			coords.setGridQualifier(esCoords.getGridQualifier());
			coords.setLatitudeDecimal(esCoords.getLatitudeDecimal());
			coords.setLongitudeDecimal(esCoords.getLongitudeDecimal());
			out.add(coords);
		}
		return out;
	}

	/**
	 * Static method to transfer the {@link GatheringSiteCoordinates} to an
	 * {@link ESGatheringSiteCoordinates} object
	 * 
	 * @param in
	 * @return
	 */
	public static List<ESGatheringSiteCoordinates> save(List<GatheringSiteCoordinates> in)
	{
		if (in == null) {
			return null;
		}
		List<ESGatheringSiteCoordinates> out = new ArrayList<>(in.size());
		for (GatheringSiteCoordinates coords : in) {
			ESGatheringSiteCoordinates esCoords = new ESGatheringSiteCoordinates();
			esCoords.setGridCellCode(coords.getGridCellCode());
			esCoords.setGridCellSystem(coords.getGridCellSystem());
			esCoords.setGridLatitudeDecimal(coords.getGridLatitudeDecimal());
			esCoords.setGridLongitudeDecimal(coords.getGridLongitudeDecimal());
			esCoords.setGridQualifier(coords.getGridQualifier());
			esCoords.setLatitudeDecimal(coords.getLatitudeDecimal());
			esCoords.setLongitudeDecimal(coords.getLongitudeDecimal());
			out.add(esCoords);
		}
		return out;
	}
}
