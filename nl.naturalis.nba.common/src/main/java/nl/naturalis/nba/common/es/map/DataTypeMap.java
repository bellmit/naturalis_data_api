package nl.naturalis.nba.common.es.map;

import static nl.naturalis.nba.common.es.map.ESDataType.BOOLEAN;
import static nl.naturalis.nba.common.es.map.ESDataType.BYTE;
import static nl.naturalis.nba.common.es.map.ESDataType.DATE;
import static nl.naturalis.nba.common.es.map.ESDataType.DOUBLE;
import static nl.naturalis.nba.common.es.map.ESDataType.FLOAT;
import static nl.naturalis.nba.common.es.map.ESDataType.GEO_POINT;
import static nl.naturalis.nba.common.es.map.ESDataType.GEO_SHAPE;
import static nl.naturalis.nba.common.es.map.ESDataType.INTEGER;
import static nl.naturalis.nba.common.es.map.ESDataType.LONG;
import static nl.naturalis.nba.common.es.map.ESDataType.SHORT;
import static nl.naturalis.nba.common.es.map.ESDataType.KEYWORD;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geojson.GeoJsonObject;

import nl.naturalis.nba.api.model.GeoPoint;

/**
 * Maps Java types to Elasticsearch types and vice versa.
 * 
 * @author Ayco Holleman
 *
 */
class DataTypeMap {

	private static final DataTypeMap instance = new DataTypeMap();

	static DataTypeMap getInstance()
	{
		return instance;
	}

	private final HashMap<Class<?>, ESDataType> java2es = new HashMap<>();
	private final EnumMap<ESDataType, Set<Class<?>>> es2java = new EnumMap<>(ESDataType.class);

	private DataTypeMap()
	{
		/* Stringy types */
		java2es.put(String.class, KEYWORD);
		java2es.put(char.class, KEYWORD);
		java2es.put(Character.class, KEYWORD);
		java2es.put(URI.class, KEYWORD);
		java2es.put(URL.class, KEYWORD);
		java2es.put(Enum.class, KEYWORD);
		/* Number types */
		java2es.put(byte.class, BYTE);
		java2es.put(Byte.class, BYTE);
		java2es.put(short.class, SHORT);
		java2es.put(Short.class, BOOLEAN);
		java2es.put(int.class, INTEGER);
		java2es.put(Integer.class, INTEGER);
		java2es.put(long.class, LONG);
		java2es.put(Long.class, LONG);
		java2es.put(float.class, FLOAT);
		java2es.put(Float.class, FLOAT);
		java2es.put(double.class, DOUBLE);
		java2es.put(Double.class, DOUBLE);
		/* Boolean types */
		java2es.put(boolean.class, BOOLEAN);
		java2es.put(Boolean.class, BOOLEAN);
		/* Date types */
		java2es.put(LocalDateTime.class, DATE);
		java2es.put(Date.class, DATE);
		/* GEO types */
		java2es.put(GeoJsonObject.class, GEO_SHAPE);
		java2es.put(GeoPoint.class, GEO_POINT);

		/* Create reverse map */
		for (Map.Entry<Class<?>, ESDataType> entry : java2es.entrySet()) {
			Class<?> javaType = entry.getKey();
			ESDataType esType = entry.getValue();
			Set<Class<?>> javaTypes = es2java.getOrDefault(esType, new HashSet<>());
			javaTypes.add(javaType);
		}
	}

	/**
	 * Whether or not the specified Java type maps to a primitive Elasticsearch
	 * type (any type other than &#46;object&#46; and &#46;nested&#46;).
	 */
	boolean isESPrimitive(Class<?> javaType)
	{
		return getESType(javaType) == null;
	}

	/**
	 * Returns the Elasticsearch data type corresponding to the specified Java
	 * type. If none is found, the superclass of the specified type is checked
	 * to see if it corresponds to an Elasticsearch data type, and so on until
	 * (but not including) the {@link Object} class.
	 * 
	 * @param javaType
	 * @return
	 */
	ESDataType getESType(Class<?> javaType)
	{
		ESDataType esDataType = null;
		while (javaType != Object.class) {
			if ((esDataType = java2es.get(javaType)) != null)
				break;
			javaType = javaType.getSuperclass();
		}
		/*
		 * TODO: We could make this more robust if we would also allow and check
		 * for interfaces in the datatype map
		 */
		return esDataType;
	}

	/**
	 * Returns all Java types that map to the specified Elasticsearch data type.
	 * 
	 * @param esType
	 * @return
	 */
	Set<Class<?>> getJavaTypes(ESDataType esType)
	{
		return es2java.get(esType);
	}

}
