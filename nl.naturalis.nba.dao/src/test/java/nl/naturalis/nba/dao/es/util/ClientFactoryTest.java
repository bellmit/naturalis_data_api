package nl.naturalis.nba.dao.es.util;

import static org.junit.Assert.assertNotNull;

import org.elasticsearch.client.Client;
import org.junit.Test;

import nl.naturalis.nba.dao.es.Registry;

public class ClientFactoryTest {

	@Test
	public void testGetClient()
	{
		Registry registry = Registry.getInstance();
		Client client = registry.getESClientManager().getClient();
		assertNotNull("01", client);
	}

}
