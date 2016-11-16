package nl.naturalis.nba.api;

import java.util.List;

import org.geojson.GeoJsonObject;

import nl.naturalis.nba.api.model.GeoArea;
import nl.naturalis.nba.api.model.GeoPoint;
import nl.naturalis.nba.api.model.IDocumentObject;
import nl.naturalis.nba.api.query.ComparisonOperator;
import nl.naturalis.nba.api.query.Condition;

/**
 * <p>
 * Specifies methods for accessing geographical areas and their coordinates.
 * This interface is mainly intended for lookups of GeoArea document IDs. When
 * using operator {@link ComparisonOperator#IN IN} to retrieve specimens found
 * within a certain area, you can either provide the coordinates yourself or you
 * can provide the ID of the GeoArea document specifying the coordinates. The
 * {@link Condition query condition} would then look like this:
 * </p>
 * <p>
 * <code>
 * // Find specimens found in the Netherlands:<br>
 * Condition condition = new Condition("gatheringEvent.siteCoordinates.geoShape", IN, "1004050@GEO");
 * </code>
 * </p>
 * <p>
 * This lookup will only work for fields of type {@link GeoJsonObject}. It will
 * not work for fields of type {@link GeoPoint} (e.g.
 * {@code gatheringEvent.siteCoordinates.geoPoint}). You should not rely on the
 * document ID being stable. You should look it up using, for example,
 * {@link #getIdForLocality(String) getIdForLocality} or
 * {@link #getIdForIsoCode(String) getIdForIsoCode}.
 * </p>
 * 
 * @see IDocumentObject
 * 
 * @author Ayco Holleman
 *
 */
public interface IGeoAreaAccess extends INbaAccess<GeoArea> {

	/**
	 * Returns the {@link IDocumentObject#getId() Elasticsearch ID} of the
	 * {@link GeoArea} with the specified locality. You can use this ID when
	 * performing geo queries. See operator {@link ComparisonOperator#IN IN}).
	 * 
	 * @see IDocumentObject
	 * @see ComparisonOperator#IN
	 * 
	 * @return
	 */
	String getIdForLocality(String locality);

	/**
	 * Returns the {@link IDocumentObject#getId() Elasticsearch ID} of the
	 * {@link GeoArea} with the specified ISO code. Note that not all areas in
	 * the Naturalis area index have an ISO code. You can use this ID when
	 * performing geo queries. See operator {@link ComparisonOperator#IN IN}.
	 * 
	 * @see IDocumentObject
	 * @see ComparisonOperator#IN
	 * 
	 * @return
	 */
	String getIdForIsoCode(String isoCode);

	/**
	 * Returns the coordinates of the area with the specified area ID.
	 * 
	 * @see IDocumentObject
	 * 
	 * @return
	 */
	String getGeoJsonForId(String id);

	/**
	 * Returns all areas as a list of key-value pairs where the key is a
	 * {@link GeoArea#getLocality() locality} and the value is the corresponding
	 * Elasticsearch document ID. In other words this method allows you to look
	 * up a document ID for a locality.
	 * 
	 * @see IDocumentObject
	 * 
	 * @return
	 */
	List<KeyValuePair<String, String>> getLocalities();

	/**
	 * Returns all areas as a list of key-value pairs where the key is an
	 * {@link GeoArea#getIsoCode() ISO code} and the value is the corresponding
	 * Elasticsearch document ID. In other words this method allows you to look
	 * up a document ID for the ISO code for a geographical area. Note that not
	 * all areas have an ISO code.
	 * 
	 * @see IDocumentObject
	 * 
	 * @return
	 */
	List<KeyValuePair<String, String>> getIsoCodes();

}