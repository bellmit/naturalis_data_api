package nl.naturalis.nba.dao.es.format;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.naturalis.nba.dao.es.format.config.DataSetXmlConfig;
import nl.naturalis.nba.dao.es.format.config.EntityXmlConfig;
import nl.naturalis.nba.dao.es.format.config.FieldXmlConfig;

/*
 * N.B. All builder classes strongly rely on XML values being whitespace-trimmed
 * and yielding null if nothing remains (rather than an empty string). This is
 * guaranteed by the use of StringTrimXmlAdapter class in the config package and
 * by the &#64;XmlJavaTypeAdapter annotation in the package-info.java file of
 * the config package. So be careful when regenerating the JAXB classes, which
 * also end up in that package !!! Use the xjc-dataset-config.sh script.
 */
public class DataSetBuilder {

	private static String ERR_NO_CONFIG = "Missing configuration file: %s";
	private static String ERR_BAD_ENTITY = "Entity %s: %s";
	private static String ERR_NO_FIELD_FACTORY = "Entity %s: configuration requires a default or dedicated instance of IFieldFactory";
	private static String ERR_BAD_FIELD = "Entity %s, field %s: %s";

	@SuppressWarnings("unused")
	private static Logger logger = LogManager.getLogger(DataSetBuilder.class);

	private InputStream config;

	private IFieldFactory defaultFieldFactory;
	private HashMap<String, IFieldFactory> entityFieldFactories;

	public DataSetBuilder(File configFile) throws DataSetConfigurationException
	{
		try {
			config = new FileInputStream(configFile);
		}
		catch (FileNotFoundException e) {
			String msg = format(ERR_NO_CONFIG, configFile.getAbsolutePath());
			throw new DataSetConfigurationException(msg);
		}
	}

	public DataSetBuilder(String configFile, boolean isResource)
			throws DataSetConfigurationException
	{
		if (isResource) {
			config = getClass().getResourceAsStream(configFile);
		}
		else {
			try {
				config = new FileInputStream(configFile);
			}
			catch (FileNotFoundException e) {
				String msg = format(ERR_NO_CONFIG, configFile);
				throw new DataSetConfigurationException(msg);
			}
		}
	}

	public DataSetBuilder setDefaultFieldFactory(IFieldFactory fieldFactory)
	{
		this.defaultFieldFactory = fieldFactory;
		return this;
	}

	public DataSetBuilder setFieldFactoryForEntity(String entityName, IFieldFactory fieldFactory)
	{
		if (entityFieldFactories == null)
			entityFieldFactories = new HashMap<>(8);
		entityFieldFactories.put(entityName, fieldFactory);
		return this;
	}

	public DataSet build() throws DataSetConfigurationException
	{
		DataSetXmlConfig dataSetConfig = parseConfigFile();
		DataSet dataSet = new DataSet();
		for (EntityXmlConfig entityConfig : dataSetConfig.getEntity()) {
			Entity entity = new Entity();
			dataSet.addEntity(entity);
			entity.setName(entityConfig.getName());
			DataSourceBuilder dsb = new DataSourceBuilder(entityConfig.getDataSource());
			DataSource dataSource = dsb.build();
			entity.setDataSource(dataSource);
			IFieldFactory fieldFactory = getFieldFactory(entityConfig.getName());
			FieldBuilder fieldBuilder = new FieldBuilder(fieldFactory, dataSource);
			for (FieldXmlConfig field : entityConfig.getFields().getField()) {
				try {
					entity.addField(fieldBuilder.build(field));
				}
				catch (FieldConfigurationException e0) {
					e0.printStackTrace();
					String msg = format(ERR_BAD_FIELD, entity.getName(), e0.getField(),
							e0.getMessage());
					throw new DataSetConfigurationException(msg);
				}
				catch (DataSetConfigurationException e1) {
					String msg = format(ERR_BAD_ENTITY, e1.getMessage());
					throw new DataSetConfigurationException(msg);
				}
			}
		}
		return dataSet;
	}

	private IFieldFactory getFieldFactory(String entity) throws DataSetConfigurationException
	{
		IFieldFactory ff;
		if (entityFieldFactories == null || ((ff = entityFieldFactories.get(entity)) == null))
			ff = defaultFieldFactory;
		if (ff == null) {
			String msg = format(ERR_NO_FIELD_FACTORY, entity);
			throw new DataSetConfigurationException(msg);
		}
		return ff;
	}

	private DataSetXmlConfig parseConfigFile() throws DataSetConfigurationException
	{
		try {
			JAXBContext ctx = JAXBContext.newInstance(DataSetXmlConfig.class);
			Unmarshaller unmarshaller = ctx.createUnmarshaller();
			DataSetXmlConfig root;
			root = (DataSetXmlConfig) unmarshaller.unmarshal(config);
			return root;
		}
		catch (JAXBException e) {
			throw new DataSetConfigurationException(e);
		}
	}

}
