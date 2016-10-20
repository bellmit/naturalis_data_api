package nl.naturalis.nba.dao;

import static nl.naturalis.nba.common.json.JsonUtil.MISSING_VALUE;
import static nl.naturalis.nba.common.json.JsonUtil.readField;
import static nl.naturalis.nba.common.json.JsonUtil.toJson;
import static nl.naturalis.nba.dao.DaoUtil.getLogger;

import java.io.OutputStream;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.naturalis.nba.api.model.IDocumentObject;
import nl.naturalis.nba.api.query.InvalidQueryException;
import nl.naturalis.nba.api.query.QuerySpec;
import nl.naturalis.nba.common.json.JsonUtil;
import nl.naturalis.nba.dao.query.QuerySpecTranslator;
import nl.naturalis.nba.dao.transfer.ITransferObject;
import nl.naturalis.nba.dao.types.ESType;

abstract class AbstractDao<API_OBJECT extends IDocumentObject, ES_OBJECT extends ESType> {

	private static Logger logger = getLogger(SpecimenDao.class);

	private DocumentType<ES_OBJECT> dt;
	private ITransferObject<API_OBJECT, ES_OBJECT> to;

	AbstractDao(DocumentType<ES_OBJECT> dt)
	{
		this.dt = dt;
		this.to = getTransferObject();
	}

	public API_OBJECT find(String id)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("find(\"{}\")", id);
		}
		GetRequestBuilder request = client().prepareGet();
		String index = dt.getIndexInfo().getName();
		String type = dt.getName();
		request.setIndex(index);
		request.setType(type);
		request.setId(id);
		GetResponse response = request.execute().actionGet();
		if (!response.isExists()) {
			if (logger.isDebugEnabled()) {
				logger.debug("{} with id \"{}\" not found", dt, id);
			}
			return null;
		}
		Map<String, Object> data = response.getSource();
		return createApiObject(id, data);
	}

	public API_OBJECT[] find(String[] ids)
	{
		if (logger.isDebugEnabled())
			logger.debug("find({})", toJson(ids));
		String type = dt.getName();
		SearchRequestBuilder request = newSearchRequest();
		IdsQueryBuilder query = QueryBuilders.idsQuery(type);
		query.ids(ids);
		request.setQuery(query);
		return processSearchRequest(request);
	}

	public API_OBJECT[] query(QuerySpec spec) throws InvalidQueryException
	{
		QuerySpecTranslator translator = new QuerySpecTranslator(spec, dt);
		return processSearchRequest(translator.translate());
	}

	public Object[][] queryValues(QuerySpec spec) throws InvalidQueryException
	{
		if (spec.getFields() == null || spec.getFields().size() == 0) {
			throw new InvalidQueryException("At least one field must be selected");
		}
		String[] fields = spec.getFields().toArray(new String[spec.getFields().size()]);
		/* Enable reference comparison for id field: */
		String idField = "id";
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].equals(idField)) {
				fields[i] = idField;
			}
		}
		QuerySpecTranslator translator = new QuerySpecTranslator(spec, dt);
		SearchRequestBuilder request = translator.translate();
		if (logger.isDebugEnabled()) {
			logger.debug("Executing query:\n{}", request);
		}
		SearchResponse response = request.execute().actionGet();
		SearchHit[] hits = response.getHits().getHits();
		if (logger.isDebugEnabled()) {
			logger.debug("Documents found: {}", response.getHits().totalHits());
		}
		Object[][] records = new Object[hits.length][];
		for (int i = 0; i < hits.length; ++i) {
			Map<String, Object> data = hits[i].getSource();
			Object[] record = new Object[fields.length];
			for (int j = 0; j < fields.length; ++j) {
				if (fields[j] == idField) {
					record[j] = hits[i].getId();
				}
				else {
					Object val = readField(data, fields[j]);
					record[j] = ((val == MISSING_VALUE) ? null : val);
				}
			}
			records[i] = record;
		}
		return records;
	}

	public void queryValues(QuerySpec spec, OutputStream out) throws InvalidQueryException
	{
		// TODO Auto-generated method stub

	}

	public String save(API_OBJECT apiObject, boolean immediate)
	{
		String id = apiObject.getId();
		String index = dt.getIndexInfo().getName();
		String type = dt.getName();
		if (logger.isDebugEnabled()) {
			String pattern = "New save request (index={};type={};id={})";
			logger.debug(pattern, index, type, id);
		}
		IndexRequestBuilder request = client().prepareIndex(index, type, id);
		ES_OBJECT esObject = to.getEsObject(apiObject);
		byte[] source = JsonUtil.serialize(esObject);
		request.setSource(source);
		IndexResponse response = request.execute().actionGet();
		if (immediate) {
			IndicesAdminClient iac = client().admin().indices();
			RefreshRequestBuilder rrb = iac.prepareRefresh(index);
			rrb.execute().actionGet();
		}
		apiObject.setId(response.getId());
		return response.getId();
	}

	@SuppressWarnings("unused")
	public boolean delete(String id, boolean immediate)
	{
		String index = dt.getIndexInfo().getName();
		String type = dt.getName();
		DeleteRequestBuilder request = client().prepareDelete(index, type, id);
		DeleteResponse response = request.execute().actionGet();
		return response.isFound();
	}

	abstract ITransferObject<API_OBJECT, ES_OBJECT> getTransferObject();

	abstract API_OBJECT[] createApiObjectArray(int length);

	SearchRequestBuilder newSearchRequest()
	{
		String index = dt.getIndexInfo().getName();
		String type = dt.getName();
		if (logger.isDebugEnabled()) {
			String pattern = "New search request (index={};type={})";
			logger.debug(pattern, index, type);
		}
		SearchRequestBuilder request = client().prepareSearch(index);
		request.setTypes(type);
		return request;
	}

	API_OBJECT[] processSearchRequest(SearchRequestBuilder request)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("Executing query:\n{}", request);
		}
		SearchResponse response = request.execute().actionGet();
		SearchHit[] hits = response.getHits().getHits();
		if (logger.isDebugEnabled()) {
			logger.debug("Documents found: {}", response.getHits().totalHits());
		}
		API_OBJECT[] apiObjects = createApiObjectArray(hits.length);
		for (int i = 0; i < hits.length; ++i) {
			String id = hits[i].getId();
			Map<String, Object> data = hits[i].getSource();
			API_OBJECT specimen = createApiObject(id, data);
			apiObjects[i] = specimen;
		}
		return apiObjects;
	}

	private API_OBJECT createApiObject(String id, Map<String, Object> data)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("Creating {} instance with id {}", dt, id);
		}
		ObjectMapper om = dt.getObjectMapper();
		ES_OBJECT esObject = om.convertValue(data, dt.getESType());
		return to.getApiObject(esObject, id);
	}

	private static Client client()
	{
		return ESClientManager.getInstance().getClient();
	}
}
