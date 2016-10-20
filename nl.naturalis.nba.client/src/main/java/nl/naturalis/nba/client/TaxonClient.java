package nl.naturalis.nba.client;

import static nl.naturalis.nba.client.ClientUtil.getObject;
import static nl.naturalis.nba.client.ServerException.newServerException;
import static nl.naturalis.nba.common.json.JsonUtil.toJson;
import static org.domainobject.util.http.SimpleHttpRequest.HTTP_OK;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.domainobject.util.IOUtil;
import org.domainobject.util.http.SimpleHttpGet;

import nl.naturalis.nba.api.ISpecimenAccess;
import nl.naturalis.nba.api.ITaxonAccess;
import nl.naturalis.nba.api.NoSuchDataSetException;
import nl.naturalis.nba.api.model.Taxon;
import nl.naturalis.nba.api.query.InvalidQueryException;
import nl.naturalis.nba.api.query.QuerySpec;
import nl.naturalis.nba.common.json.JsonUtil;

/**
 * Client-side implementation of the {@link ITaxonAccess taxon API}.
 * 
 * @author Ayco Holleman
 *
 */
public class TaxonClient extends AbstractClient implements ITaxonAccess {

	private static final Logger logger = LogManager.getLogger(TaxonClient.class);

	public TaxonClient(ClientConfig config)
	{
		super(config);
	}

	@Override
	public Taxon[] query(QuerySpec querySpec) throws InvalidQueryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[][] queryValues(QuerySpec querySpec) throws InvalidQueryException
	{
		SimpleHttpGet request = newJsonGetRequest();
		request.setPath("specimen/query");
		request.addParam("querySpec", toJson(querySpec));
		sendRequest(request);
		int status = request.getStatus();
		if (status != HTTP_OK) {
			throw newServerException(status, request.getResponseBody());
		}
		return getObject(request.getResponseBody(), Object[][].class);
	}

	@Override
	public void queryValues(QuerySpec spec, OutputStream out) throws InvalidQueryException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void dwcaQuery(QuerySpec querySpec, OutputStream out) throws InvalidQueryException
	{
		String json = JsonUtil.toJson(querySpec);
		SimpleHttpGet request = new SimpleHttpGet();
		request.setBaseUrl(config.getBaseUrl());
		request.setPath("taxon/dwca/query/" + json);
		sendRequest(request);
		int status = request.getStatus();
		if (status != HTTP_OK) {
			throw newServerException(status, request.getResponseBody());
		}
		InputStream in = null;
		try {
			logger.info("Downloading DarwinCore archive");
			in = request.getResponseBodyAsStream();
			IOUtil.pipe(in, out, 4096);
			logger.info("DarwinCore archive download complete");
		}
		finally {
			IOUtil.close(in);
		}
	}

	@Override
	public void dwcaGetDataSet(String name, OutputStream out) throws NoSuchDataSetException
	{
		SimpleHttpGet request = new SimpleHttpGet();
		request.setBaseUrl(config.getBaseUrl());
		request.setPath("taxon/dwca/dataset/" + name);
		sendRequest(request);
		int status = request.getStatus();
		if (status != HTTP_OK) {
			throw newServerException(status, request.getResponseBody());
		}
		InputStream in = null;
		try {
			logger.info("Downloading DarwinCore archive");
			in = request.getResponseBodyAsStream();
			IOUtil.pipe(in, out, 4096);
			logger.info("DarwinCore archive download complete");
		}
		finally {
			IOUtil.close(in);
		}
	}

	@Override
	public String[] dwcaGetDataSetNames()
	{
		SimpleHttpGet request = getJson("taxon/dwca/getDataSetNames");
		int status = request.getStatus();
		if (status != HTTP_OK) {
			throw newServerException(status, request.getResponseBody());
		}
		return getObject(request.getResponseBody(), String[].class);
	}

}
