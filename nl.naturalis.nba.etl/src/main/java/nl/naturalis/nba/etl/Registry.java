package nl.naturalis.nba.etl;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.domainobject.util.ConfigObject;
import org.domainobject.util.FileUtil;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequest;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import nl.naturalis.nba.etl.elasticsearch.IndexManagerNative;

/**
 * Class providing centralized access to core services such as logging and
 * elasticsearch. If anything goes wrong while configuring those services an
 * {@link InitializationException} is thrown and it probably doesn't make much
 * sense to let the program continue. Therefore one of the first things an
 * import program should do is retrieve an instance of the {@code Registry}
 * class.
 * 
 * @author Ayco Holleman
 *
 */
public class Registry {

	/*
	 * System property telling us where the directory containing all sort of
	 * configuration files is. This directory contains nba.etl.properties,
	 * log4j2.xml, es-settings.json, etc.
	 */
	private static final String SYSPROP_CONFIG_DIR = "nba.v2.etl.conf.dir";
	/*
	 * System property that we are going to set and that tells log4j how to name
	 * the log file.
	 */
	private static final String SYSPROP_ETL_LOGFILE = "nba.v2.etl.logfile";
	/*
	 * Name of the main configuration file.
	 */
	private static final String CONFIG_FILE_NAME = "nba.etl.properties";

	private static Registry instance;

	private File confDir;
	private ConfigObject config;
	private Client esClient;

	private Logger logger;

	/**
	 * Instantiates and initializes a {@code Registry} instance.
	 */
	public static void initialize()
	{
		if (instance == null) {
			instance = new Registry();
		}
	}

	/**
	 * Return a {@code Registry} instance. Will call {@link #initialize()}
	 * first.
	 * 
	 * @return A {@code Registry} instance.
	 */
	public static Registry getInstance()
	{
		initialize();
		return instance;
	}

	private Registry()
	{
		setConfDir();
		loadConfig();
		setupLogging();
	}

	/**
	 * Get a {@code ConfigObject} for the main configuration file
	 * (purl.properties).
	 * 
	 * @return
	 */
	public ConfigObject getConfig()
	{
		return config;
	}

	/**
	 * Get the directory designated to contain the application's configuration
	 * files. This directory will contain at least purl.properties, but may
	 * contain additional files that the application expects to be there.
	 * 
	 * @return
	 */
	public File getConfDir()
	{
		return confDir;
	}

	/**
	 * Returns a file within the application's configuration directory or one of
	 * its subdirectories.
	 * 
	 * @param relativePath
	 *            The path of the file relative to the configuration directory.
	 * @return
	 */
	public File getFile(String relativePath)
	{
		return FileUtil.newFile(confDir, relativePath);
	}

	/**
	 * Get a logger for the specified class.
	 * 
	 * @param cls
	 * @return
	 */
	public Logger getLogger(Class<?> cls)
	{
		return LogManager.getLogger(cls);
	}

	/**
	 * Get a native Java ElasticSearch {@code Client}.
	 * 
	 * @return
	 */
	public Client getESClient()
	{
		if (esClient == null) {
			logger.info("Connecting to Elasticsearch cluster");
			InetAddress[] hosts = getHosts();
			int[] ports = getPorts(hosts.length);
			esClient = createClient();
			for (int i = 0; i < hosts.length; ++i) {
				InetAddress host = hosts[i];
				int port = ports[i];
				logger.info("Adding transport address \"{}:{}\"", host.getHostAddress(), port);
				InetSocketTransportAddress addr;
				addr = new InetSocketTransportAddress(host, port);
				((TransportClient) esClient).addTransportAddress(addr);
			}
			ping();
		}
		return esClient;
	}

	/**
	 * Releases the connection to ElasticSearch. Must always be called once
	 * you're done interacting with ElasticSearch. If the connection was not
	 * open, this method does nothing.
	 */
	public void closeESClient()
	{
		if (esClient != null)
			esClient.close();
	}

	/**
	 * Get an index manager for the NBA index.
	 * 
	 * @return
	 */
	public IndexManagerNative getNbaIndexManager()
	{
		return new IndexManagerNative(getESClient(),
				getConfig().required("elasticsearch.index.name"));
	}

	private void setConfDir()
	{
		String path = System.getProperty(SYSPROP_CONFIG_DIR);
		if (path == null) {
			String msg = String.format("Missing system property \"%s\"", SYSPROP_CONFIG_DIR);
			throw new InitializationException(msg);
		}
		File dir = new File(path);
		if (!dir.isDirectory()) {
			String msg = String.format(
					"Invalid value for system property \"%s\": \"%s\" (no such directory)",
					SYSPROP_CONFIG_DIR, path);
			throw new InitializationException(msg);
		}
		try {
			confDir = dir.getCanonicalFile();
			System.out.println("Configuration directory: " + dir.getAbsolutePath());
		}
		catch (IOException e) {
			throw new InitializationException(e);
		}
	}

	private Client createClient()
	{
		Builder builder = Settings.settingsBuilder();
		String cluster = config.required("elasticsearch.cluster.name");
		builder.put("cluster.name", cluster);
		Settings settings = builder.build();
		return TransportClient.builder().settings(settings).build();
	}

	private InetAddress[] getHosts()
	{
		String s = config.required("elasticsearch.transportaddress.host");
		String names[] = s.trim().split(",");
		InetAddress[] addresses = new InetAddress[names.length];
		for (int i = 0; i < names.length; ++i) {
			String name = names[i].trim();
			try {
				addresses[i] = InetAddress.getByName(name);
			}
			catch (UnknownHostException e) {
				String msg = "Unknown host: \"" + name + "\"";
				throw new ConnectionFailureException(msg);
			}
		}
		return addresses;
	}

	private int[] getPorts(int numHosts)
	{
		int[] ports = new int[numHosts];
		String s = config.get("elasticsearch.transportaddress.port");
		if (s == null) {
			Arrays.fill(ports, 9300);
			return ports;
		}
		String[] chunks = s.trim().split(",");
		if (chunks.length == 1) {
			int port = Integer.parseInt(chunks[0].trim());
			Arrays.fill(ports, port);
			return ports;
		}
		if (chunks.length == numHosts) {
			for (int i = 0; i < numHosts; ++i) {
				int port = Integer.parseInt(chunks[i].trim());
				ports[i] = port;
			}
			return ports;
		}
		String msg = "Number of ports must be either one or match number of hosts";
		throw new ConnectionFailureException(msg);
	}

	private void ping()
	{
		ClusterStatsRequest request = new ClusterStatsRequest();
		ClusterStatsResponse response = null;
		try {
			response = esClient.admin().cluster().clusterStats(request).actionGet();
			if (logger.isDebugEnabled()) {
				logger.debug("Cluster stats: " + response.toString());
			}
		}
		catch (NoNodeAvailableException e) {
			String cluster = config.required("elasticsearch.cluster.name");
			String hosts = config.required("elasticsearch.transportaddress.host");
			String ports = config.get("elasticsearch.transportaddress.port");
			String msg = "Ping resulted in NoNodeAvailableException\n" + "* Check configuration:\n"
					+ "  > elasticsearch.cluster.name={}\n"
					+ "  > elasticsearch.transportaddress.host={}\n"
					+ "  > elasticsearch.transportaddress.port={}\n"
					+ "* Make sure Elasticsearch is running\n"
					+ "* Make sure client version matches server version";
			logger.error(msg, cluster, hosts, ports);
			throw new ConnectionFailureException(e);
		}
		if (response.getStatus().equals(ClusterHealthStatus.RED)) {
			throw new ConnectionFailureException("ElasticSearch cluster in bad health");
		}
	}

	private void loadConfig()
	{
		File file = FileUtil.newFile(confDir, CONFIG_FILE_NAME);
		if (!file.isFile()) {
			String msg = String.format("Configuration file missing: %s", file.getPath());
			throw new InitializationException(msg);
		}
		this.config = new ConfigObject(file);
	}

	private void setupLogging()
	{
		System.setProperty(SYSPROP_ETL_LOGFILE, getLogFileName());
		if (System.getProperty("log4j.configurationFile") == null) {
			File f = FileUtil.newFile(confDir, "log4j2.xml");
			if (f.exists()) {
				System.setProperty("log4j.configurationFile", f.getAbsolutePath());
			}
			else {
				String fmt = "Log4j config file not in default location "
						+ "(%s) and no system property \"log4j.configurationFile\"";
				String msg = String.format(fmt, f.getAbsolutePath());
				throw new InitializationException(msg);
			}
		}
		logger = LogManager.getLogger(getClass());
	}

	private String getLogFileName()
	{
		File logDir = FileUtil.newFile(confDir.getParentFile(), "log");
		String now = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
		String command = System.getProperty("sun.java.command");
		String[] chunks = command.split("\\.");
		String mainClass = chunks[chunks.length - 1].split(" ")[0];
		String logFileName = now + "." + mainClass + ".log";
		File logFile = FileUtil.newFile(logDir, logFileName);
		System.out.println("Log file: " + logFile.getAbsolutePath());
		return logFile.getAbsolutePath();
	}

}
