package nl.naturalis.nba.dao.es.format.dwca;

import static nl.naturalis.nba.dao.es.DaoUtil.getLogger;
import static nl.naturalis.nba.dao.es.DocumentType.SPECIMEN;
import static org.domainobject.util.FileUtil.getSubdirectories;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.domainobject.util.FileUtil;

import nl.naturalis.nba.api.query.QuerySpec;
import nl.naturalis.nba.common.json.JsonDeserializationException;
import nl.naturalis.nba.common.json.JsonUtil;
import nl.naturalis.nba.dao.es.DaoRegistry;
import nl.naturalis.nba.dao.es.DocumentType;
import nl.naturalis.nba.dao.es.exception.DwcaCreationException;
import nl.naturalis.nba.dao.es.format.DataSetCollectionConfiguration;
import nl.naturalis.nba.dao.es.format.Entity;
import nl.naturalis.nba.dao.es.format.IDataSetField;

/**
 * Utility class for the DwCA generation process.
 * 
 * @author Ayco Holleman
 *
 */
public class DwcaUtil {

	@SuppressWarnings("unused")
	private static Logger logger = getLogger(DwcaUtil.class);

	private DwcaUtil()
	{
	}

	/**
	 * Returns the fields&#46;config file for the specified collection of data
	 * sets. This file must be named "fields.config" and it must reside in the
	 * top directory for the data set collection.
	 * 
	 * @see #getDataSetCollectionDirectory(DataSetCollectionConfiguration)
	 * 
	 * @param documentType
	 * @param setType
	 * @return
	 */
	public static File getFieldsConfigFile(DataSetCollectionConfiguration dsc)
	{
		File dir = getDataSetCollectionDirectory(dsc);
		File f = FileUtil.newFile(dir, "fields.config");
		if (!f.isFile())
			throw fileNotFound(f);
		return f;
	}

//	/**
//	 * Returns the object that will generate the meta&#46;.xml file for the DwC
//	 * archive.
//	 * 
//	 * @param dsc
//	 * @param fields
//	 * @return
//	 */
//	public static MetaXmlGenerator getMetaXmlGenerator(DataSetCollection dsc,
//			IDataSetField[] fields)
//	{
//		if (dsc.getDocumentType() == SPECIMEN)
//			return new OccurrenceMetaXmlGenerator(fields);
//		String fmt = "Cannot generate meta.xml for %s";
//		String msg = String.format(fmt, dsc.getDocumentType());
//		throw new DwcaCreationException(msg);
//	}

	/**
	 * Returns the name of the CSV file contained within the DwC archive. For
	 * specimens the name is "occurence.txt". For taxa it is "taxa.txt".
	 * 
	 * @param dsc
	 * @return
	 */
	public static String getCsvFileName(DataSetCollectionConfiguration dsc)
	{
		if (dsc.getDocumentType() == SPECIMEN)
			return "occurrence.txt";
		String fmt = "Cannot determine CSV file name for %s";
		String msg = String.format(fmt, dsc.getDocumentType());
		throw new DwcaCreationException(msg);
	}

	/**
	 * Returns the eml.xml file for the specified data set. This file must
	 * reside in the {@link #getDatasetDirectory(DataSetCollectionConfiguration, String)
	 * directory} for the specified data set.
	 * 
	 * @param documentType
	 * @param setType
	 * @param setName
	 * @return
	 */
	public static File getEmlFile(DataSetCollectionConfiguration dsc, String setName)
	{
		File dir = getDatasetDirectory(dsc, setName);
		File emlFile = FileUtil.newFile(dir, "eml.xml");
		if (!emlFile.isFile())
			throw fileNotFound(emlFile);
		return emlFile;
	}

	/**
	 * Returns a {@link QuerySpec} instance for the specified data set. This
	 * instance is created through the deserialization of the contents of a file
	 * name queryspec.json. This file must reside in the
	 * {@link #getDatasetDirectory(DataSetCollectionConfiguration, String) directory} for the
	 * specified data set.
	 * 
	 * @param documentType
	 * @param setType
	 * @param setName
	 * @return
	 */
	public static QuerySpec getQuerySpec(DataSetCollectionConfiguration dsc, String setName)
	{
		File dir = getDatasetDirectory(dsc, setName);
		File f = FileUtil.newFile(dir, "queryspec.json");
		if (!f.isFile())
			throw fileNotFound(f);
		byte[] data = FileUtil.getByteContents(f.getAbsolutePath());
		try {
			return JsonUtil.deserialize(data, QuerySpec.class);
		}
		catch (JsonDeserializationException e) {
			String msg = "Invalid JSON in file " + f.getPath();
			throw new DwcaCreationException(msg);
		}
	}

	/**
	 * Returns the directory containing configuration data for the specified
	 * data set. This directory ordinarily is a subdirectory of the directory
	 * for the specified data set collection. However for collections containing
	 * just one data set, it is allowed to use the collection directory as the
	 * data set directory. In other words, collection directory and data set
	 * directory are one and the same and will contain both collection-specific
	 * artefacts (e.g. fields.config) and dataset-specific artefacts (e.g.
	 * eml.xml). If you pass {@code null} for the {@code setName} argument, or
	 * if the {@code setName} argument is the same as the name of the provided
	 * data set collection, then it is assumed that you want the directory for
	 * such a one-of-a-kind data set.
	 * 
	 * 
	 * @param dsc
	 * @param setName
	 * @return
	 */
	public static File getDatasetDirectory(DataSetCollectionConfiguration dsc, String setName)
	{
		File dir = getDataSetCollectionDirectory(dsc);
		if (setName == null || setName.equals(dsc.getName()))
			return dir;
		File f = FileUtil.newFile(dir, setName);
		if (!f.isDirectory())
			throw directoryNotFound(f);
		return f;
	}

	/**
	 * Returns the top directory for the specified {@link DataSetCollectionConfiguration
	 * collection of data sets}. Besides subdirectories for the individual data
	 * sets, this directory must also contain the "fields.config" file that
	 * configures which fields to include in the data sets (see
	 * {@link FieldConfigurator}).
	 * 
	 * @param dsc
	 * @return
	 */
	public static File getDataSetCollectionDirectory(DataSetCollectionConfiguration dsc)
	{
		File dir = getDocumentTypeDirectory(dsc.getDocumentType());
		File f = FileUtil.newFile(dir, dsc.getName());
		if (!f.isDirectory())
			throw directoryNotFound(f);
		return f;
	}

	/**
	 * Returns the top directory for all data sets sourced from the specified
	 * Elasticsearch {@link DocumentType document type}.
	 * 
	 * @param dt
	 * @return
	 */
	public static File getDocumentTypeDirectory(DocumentType<?> dt)
	{
		File nbaConfDir = DaoRegistry.getInstance().getConfigurationDirectory();
		String docType = dt.toString().toLowerCase();
		Path path = Paths.get(nbaConfDir.getPath(), "dwca", docType);
		File f = path.toFile();
		if (!f.isDirectory())
			throw directoryNotFound(f);
		return f;
	}

	private static DwcaCreationException fileNotFound(File f)
	{
		return new DwcaCreationException("File not found: " + f.getPath());
	}

	private static DwcaCreationException directoryNotFound(File f)
	{
		return new DwcaCreationException("Directory not found: " + f.getPath());
	}

	static List<Field> getMetaXmlFieldElements(Entity entity)
	{
		IDataSetField[] fields = entity.getFields();
		String base = "http://rs.tdwg.org/dwc/terms/";
		List<Field> list = new ArrayList<>(fields.length);
		// First field in CSV record MUST be the id field. SKip it
		for (int i = 1; i < fields.length; i++) {
			Field field = new Field();
			field.setIndex(String.valueOf(i));
			field.setTerm(base + fields[i].getName());
			list.add(field);
		}
		return list;
	}

}
