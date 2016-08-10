package nl.naturalis.nba.dao.es.format;

import nl.naturalis.nba.dao.es.format.calc.ICalculator;

/**
 * A {@code IDataSetFieldFactory} produces format-specific versions of data set
 * fields. For each format (CSV, XML, etc.) a concrete implementation must be
 * provided that produces {@link IDataSetField} instances that format and escape
 * values as appropriate for that format.
 * 
 * @author Ayco Holleman
 *
 */
public interface IDataSetFieldFactory {

	/**
	 * Returns an {@link IDataSetField} instance that retrieves its value
	 * directly a field in an Elasticsearch document. The {@code name} argument
	 * specifies the name of the data set field. The {@code path} argument
	 * specifies the full path of an Elasticsearch field. The path must be
	 * specified as an array of path elements, with each element representing a
	 * successively deeper level in the Elasticsearch document. For example the
	 * Elasticsearch field {@code gatheringEvent.dateTimeBegin} would be passed
	 * to this method as<br>
	 * <code>
	 * new String[] {"gatheringEvent", "dateTimeBegin"}
	 * </code><br>
	 * Array access can be specified as in the following example:<br>
	 * <code>
	 * new String[] {"identifications", "0", "defaultClassification", "kingdom"}
	 * </code>
	 * 
	 * @see FieldConfigurator
	 * 
	 * @param dsc
	 *            The data set collection
	 * @param name
	 *            The name of the data set field
	 * @param path
	 *            The Elasticsearch field providing the value for the data set
	 *            field
	 * @return
	 */
	IDataSetField createDataField(DataSetCollection dsc, String name, String[] path);

	/**
	 * Returns an {@link IDataSetField} instance that provides a default value
	 * for the specified field.
	 * 
	 * @param dsc
	 * @param name
	 * @param constant
	 * @return
	 */
	IDataSetField createConstantField(DataSetCollection dsc, String name, String constant);

	/**
	 * Returns an {@link IDataSetField} instance that uses an
	 * {@link ICalculator} instance to calculate a value for the specified
	 * field.
	 * 
	 * @param dsc
	 * @param name
	 * @param calculator
	 * @return
	 */
	IDataSetField createdCalculatedField(DataSetCollection dsc, String name,
			ICalculator calculator);

}