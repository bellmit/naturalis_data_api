package nl.naturalis.nba.dao.es;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.logging.log4j.Logger;
import org.domainobject.util.ConfigObject;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequest;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import nl.naturalis.nba.dao.es.exception.ConnectionFailureException;

/**
 * A factory for Elasticsearch {@link Client} instances. You can get hold of an
 * {@code ESClientFactory} via {@link DAORegistry#getESClientManager()}.
 * 
 * @author Ayco Holleman
 *
 */
public class ESClientManager {

	private static final Logger logger = DAORegistry.getInstance().getLogger(ESClientManager.class);

	private static ESClientManager instance;

	public static ESClientManager getInstance()
	{
		if (instance == null) {
			ConfigObject cfg = DAORegistry.getInstance().getConfiguration();
			instance = new ESClientManager(cfg);
		}
		return instance;
	}

	private final ConfigObject config;

	private Client client;

	private ESClientManager(ConfigObject config)
	{
		this.config = config;
	}

	/**
	 * Get a native Java Elasticsearch {@link Client}.
	 * 
	 * @return
	 */
	public Client getClient()
	{
		if (client == null) {
			logger.info("Connecting to Elasticsearch cluster");
			InetAddress[] hosts = getHosts();
			int[] ports = getPorts(hosts.length);
			client = createClient();
			for (int i = 0; i < hosts.length; ++i) {
				InetAddress host = hosts[i];
				int port = ports[i];
				logger.info("Adding transport address \"{}:{}\"", host.getHostAddress(), port);
				InetSocketTransportAddress addr;
				addr = new InetSocketTransportAddress(host, port);
				((TransportClient) client).addTransportAddress(addr);
			}
			ping();
		}
		return client;
	}

	public void closeClient()
	{
		if (client != null) {
			try {
				client.close();
			}
			finally {
				client = null;
			}
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
			response = client.admin().cluster().clusterStats(request).actionGet();
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
}