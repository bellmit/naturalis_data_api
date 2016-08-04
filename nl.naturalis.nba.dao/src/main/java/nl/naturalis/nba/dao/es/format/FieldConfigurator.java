package nl.naturalis.nba.dao.es.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.domainobject.util.IOUtil;

import nl.naturalis.nba.dao.es.exception.DaoException;
import nl.naturalis.nba.dao.es.format.calc.ICalculator;
import nl.naturalis.nba.dao.es.map.DocumentField;
import nl.naturalis.nba.dao.es.map.ESField;
import nl.naturalis.nba.dao.es.map.Mapping;
import nl.naturalis.nba.dao.es.map.MappingInfo;
import nl.naturalis.nba.dao.es.map.NoSuchFieldException;

/**
 * A {@code FieldConfigurator} determines which {@link IDataSetField fields} to
 * include in a data set. It does so by reading a configuration file (or other
 * type of resource) that is parsed as follows:
 * <ul>
 * <li>Lines starting with the hash character (#) are ignored.
 * <li>Empty lines are ignored.
 * <li>Other lines display key-value pairs with the equals sign (=) separating
 * key and value. For example:<br>
 * {@code lifeStage = phaseOrStage}.
 * <li>Both key and value are whitespace-trimmed before being processed.
 * <li>The key is the name of the field to be included in the data set.
 * <li>If the value starts with an asterisk (*), it specifies a constant (a.k.a.
 * default) value. Everything <i>following</i> the asterisk is used as the
 * default value for the CSV field.
 * <li>If the value is the percentage sign (%), it means the field has a
 * calculated value. The word following the percentage sign specifies the simple
 * class name of an {@link ICalculator} implementation.
 * <li>If the value does not start with an asterisk or percentage sign, it
 * specifies the full path of the Elasticsearch field containing the value to be
 * written to the CSV file. Array access can be achieved by adding the array
 * index after the name of the field that represents the array. For example:<br>
 * {@code kingdom = identifications.0.defaultClassification.kingdom}
 * <li>
 * </ul>
 * 
 * @see IDataSetField
 * @see IDataSetFieldFactory
 * 
 * @author Ayco Holleman
 */
public class FieldConfigurator {

	/**
	 * Constructs the full path of an Elasticsearch field from the specified
	 * path elements. Array indices in the specified string array are skipped.
	 * For example, if you pass {"identifications", "0",
	 * "defaultClassification", "kingdom"}, then
	 * "identifications.defaultClassification.kingdom" is returned.
	 * 
	 * @param pathElements
	 * @return
	 */
	public static String getPath(String[] pathElements)
	{
		StringBuilder sb = new StringBuilder(50);
		for (String element : pathElements) {
			try {
				Integer.parseInt(element);
			}
			catch (NumberFormatException e) {
				if (sb.length() != 0)
					sb.append('.');
				sb.append(element);
			}
		}
		return sb.toString();
	}

	private static class ConfigurationException extends Exception {

		ConfigurationException(String message)
		{
			super(message);
		}
	}

	private static final String CALC_PACKAGE = ICalculator.class.getPackage().getName();

	private IDataSetFieldFactory factory;
	private DataSetCollection dsc;

	public FieldConfigurator(DataSetCollection dsc, IDataSetFieldFactory fieldFactory)
	{
		this.dsc = dsc;
		this.factory = fieldFactory;
	}

	/**
	 * Returns the fields to be included in a data set as configured in the
	 * specified configuration file.
	 * 
	 * @param confFile
	 * @return
	 */
	public IDataSetField[] getFields(File confFile)
	{
		try {
			FileReader fr = new FileReader(confFile);
			LineNumberReader lnr = new LineNumberReader(fr);
			return getFields(lnr, confFile.getAbsolutePath());
		}
		catch (FileNotFoundException e) {
			throw new DaoException("File not found: " + confFile.getAbsolutePath());
		}
	}

	/**
	 * Returns the fields to be included in a data set as configured in the
	 * specified resource. The {@code source} argument should specify the name
	 * of the resource and is only used for reporting purposes.
	 * 
	 * @param is
	 * @param source
	 * @return
	 */
	public IDataSetField[] getFields(InputStream is, String source)
	{
		try {
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			LineNumberReader lnr = new LineNumberReader(isr);
			return getFields(lnr, source);
		}
		catch (UnsupportedEncodingException e) {
			throw new DaoException(e);
		}
	}

	/**
	 * Returns the fields to be included in a data set as configured in the
	 * specified resource. The {@code source} argument should specify the name
	 * of the resource and is only used for reporting purposes.
	 * 
	 * @param is
	 * @param source
	 * @return
	 */
	public IDataSetField[] getFields(LineNumberReader lnr, String source)
	{
		ArrayList<IDataSetField> fields = new ArrayList<>(60);
		try {
			for (String line = lnr.readLine(); line != null; line = lnr.readLine()) {
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#') {
					continue;
				}
				String[] chunks = line.split("=");
				if (chunks.length != 2) {
					throw new ConfigurationException("Missing delimiter");
				}
				String fieldName = chunks[0].trim();
				String fieldDef = chunks[1].trim();
				IDataSetField field;
				if (fieldDef.charAt(0) == '*') {
					String constant = fieldDef.substring(1);
					field = factory.createConstantField(dsc, fieldName, constant);
				}
				else if (fieldDef.charAt(0) == '%') {
					String className = fieldDef.substring(1).trim();
					ICalculator calculator = getCalculator(className);
					field = factory.createdCalculatedField(dsc, fieldName, calculator);
				}
				else {
					field = createDataField(fieldName, fieldDef);
				}
				fields.add(field);
			}
		}
		catch (ConfigurationException e) {
			throw error(source, lnr, e);
		}
		catch (IOException e) {
			throw new DaoException(e);
		}
		finally {
			IOUtil.close(lnr);
		}
		return fields.toArray(new IDataSetField[fields.size()]);
	}

	private IDataSetField createDataField(String fieldName, String path)
			throws ConfigurationException
	{
		String[] pathElements = path.split("\\.");
		checkPath(pathElements);
		checkArrayIndices(pathElements);
		return factory.createDataField(dsc, fieldName, pathElements);
	}

	private void checkPath(String[] elements) throws ConfigurationException
	{
		Mapping mapping = dsc.getDocumentType().getMapping();
		MappingInfo mi = new MappingInfo(mapping);
		String path = getPath(elements);
		try {
			ESField esField = mi.getField(path);
			if (!(esField instanceof DocumentField)) {
				String fmt = "Invalid field (type is object/nested): %s";
				String msg = String.format(fmt, path);
				throw new ConfigurationException(msg);
			}
		}
		catch (NoSuchFieldException e) {
			throw new ConfigurationException(e.getMessage());
		}
	}

	private void checkArrayIndices(String[] path) throws ConfigurationException
	{
		Mapping mapping = dsc.getDocumentType().getMapping();
		MappingInfo mi = new MappingInfo(mapping);
		StringBuilder sb = new StringBuilder(50);
		for (String element : path) {
			try {
				int index = Integer.parseInt(element);
				ESField esField = mi.getField(sb.toString());
				if (!esField.isMultiValued()) {
					String fmt = "Illegal array index (%s) following single-valued field: %s";
					String msg = String.format(fmt, index, sb.toString());
					throw new ConfigurationException(msg);
				}
			}
			catch (NumberFormatException e) {
				if (sb.length() != 0)
					sb.append('.');
				sb.append(element);
			}
		}
	}

	private static ICalculator getCalculator(String name) throws ConfigurationException
	{
		String className = CALC_PACKAGE + '.' + name;
		try {
			Class<?> cls = Class.forName(className);
			return (ICalculator) cls.newInstance();
		}
		catch (ClassNotFoundException e) {
			throw new ConfigurationException("No such calculator: \"" + name + "\"");
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new DaoException(e);
		}
	}

	private static DaoException error(String source, LineNumberReader lnr, ConfigurationException e)
	{
		int line = lnr.getLineNumber();
		String fmt = "%s (%s, line %s)";
		String msg = String.format(fmt, e.getMessage(), source, line);
		return new DaoException(msg);
	}

}
