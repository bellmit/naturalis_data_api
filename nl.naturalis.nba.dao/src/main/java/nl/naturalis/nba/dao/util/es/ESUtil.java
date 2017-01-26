package nl.naturalis.nba.dao.util.es;

import static nl.naturalis.nba.dao.DaoUtil.getLogger;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.naturalis.nba.api.model.IDocumentObject;
import nl.naturalis.nba.api.model.SourceSystem;
import nl.naturalis.nba.common.es.map.MappingSerializer;
import nl.naturalis.nba.dao.DocumentType;
import nl.naturalis.nba.dao.ESClientManager;
import nl.naturalis.nba.dao.IndexInfo;
import nl.naturalis.nba.dao.exception.DaoException;

/**
 * Methods for interacting with Elasticsearch, mostly intended to be used for
 * unit testing and by the data import programs in {@code nl.naturalis.nba.etl}.
 * 
 * @author Ayco Holleman
 *
 */
public class ESUtil {

	private static final Logger logger = getLogger(ESUtil.class);

	private ESUtil()
	{
	}

	/**
	 * Returns an Elasticsearch {@code Client} object.
	 * 
	 * @return
	 */
	public static Client esClient()
	{
		return ESClientManager.getInstance().getClient();
	}

	/**
	 * Prepares a new search request for the specified document type.
	 * 
	 * @param dt
	 * @return
	 */
	public static SearchRequestBuilder newSearchRequest(DocumentType<?> dt)
	{
		String index = dt.getIndexInfo().getName();
		String type = dt.getName();
		if (logger.isDebugEnabled()) {
			String pattern = "New search request (index: {}; type: {})";
			logger.debug(pattern, index, type);
		}
		SearchRequestBuilder request = esClient().prepareSearch(index);
		request.setTypes(type);
		return request;
	}

	/**
	 * Executes the specified search request.
	 * 
	 * @param request
	 * @return
	 */
	public static SearchResponse executeSearchRequest(SearchRequestBuilder request)
	{
		if (logger.isDebugEnabled()) {
			if (request.request().source().toString().length() < 3000) {
				logger.debug("Executing search request:\n{}", request);
			}
			else {
				logger.debug("Executing search request (too large to be logged)");
			}
		}
		SearchResponse response = request.get();
		if (logger.isDebugEnabled()) {
			logger.debug("Documents found: {}", response.getHits().totalHits());
		}
		return response;
	}

	/**
	 * Generates the value of the Elasticsearch _id field based on the source
	 * system of the record and the id the record had in the source system.
	 * Values for the _id field are never auto-generated by Elasticsearch.
	 * Instead, the value is generated as follows:
	 * {@code sourceSystemId + '@' + sourceSystem.getCode()}.
	 * 
	 * @param sourceSystem
	 * @param sourceSystemId
	 * @return
	 */
	public static String getElasticsearchId(SourceSystem sourceSystem, String sourceSystemId)
	{
		return sourceSystemId + '@' + sourceSystem.getCode();
	}

	/**
	 * Generates the value of the Elasticsearch _id field based on the source
	 * system of the record and the id the record had in the source system.
	 * 
	 * @param sourceSystem
	 * @param sourceSystemId
	 * @return
	 */
	public static String getElasticsearchId(SourceSystem sourceSystem, int sourceSystemId)
	{
		return String.valueOf(sourceSystemId) + '@' + sourceSystem.getCode();
	}

	/**
	 * Returns all indices hosting the NBA {@link DocumentType document types}.
	 * Document types may share an index, but this method only returns unique
	 * indices.
	 * 
	 * @return
	 */
	public static Set<IndexInfo> getDistinctIndices()
	{
		Set<IndexInfo> result = new HashSet<>(8);
		result.add(DocumentType.SPECIMEN.getIndexInfo());
		result.add(DocumentType.TAXON.getIndexInfo());
		result.add(DocumentType.MULTI_MEDIA_OBJECT.getIndexInfo());
		result.add(DocumentType.GEO_AREA.getIndexInfo());
		result.add(DocumentType.SCIENTIFIC_NAME_SUMMARY.getIndexInfo());
		return result;
	}

	/**
	 * Returns the unique indices for the specified document types.
	 * 
	 * @param documentTypes
	 * @return
	 */
	public static Set<IndexInfo> getDistinctIndices(DocumentType<?>... documentTypes)
	{
		LinkedHashSet<IndexInfo> result = new LinkedHashSet<>(3);
		for (DocumentType<?> dt : documentTypes) {
			result.add(dt.getIndexInfo());
		}
		return result;
	}

	/**
	 * Deletes all indices used by the NBA. All NBA data will be lost. WATCH
	 * OUT!
	 */
	public static void deleteAllIndices()
	{
		for (IndexInfo index : getDistinctIndices()) {
			deleteIndex(index);
		}
	}

	/**
	 * Creates the Elasticsearch indices for the NBA {@link DocumentType
	 * document types}.
	 */
	public static void createAllIndices()
	{
		for (IndexInfo index : getDistinctIndices()) {
			createIndex(index);
		}
	}

	/**
	 * Deletes the Elasticsearch index hosting the specified {@link DocumentType
	 * document type}.
	 * 
	 * @param documentType
	 */
	public static void deleteIndex(DocumentType<?> documentType)
	{
		deleteIndex(documentType.getIndexInfo());
	}

	/**
	 * Deletes the specified Elasticsearch index.
	 * 
	 * @param indexInfo
	 */
	public static void deleteIndex(IndexInfo indexInfo)
	{
		String index = indexInfo.getName();
		logger.info("Deleting index {}", index);
		DeleteIndexRequestBuilder request = indices().prepareDelete(index);
		try {
			DeleteIndexResponse response = request.execute().actionGet();
			if (!response.isAcknowledged()) {
				throw new RuntimeException("Failed to delete index " + index);
			}
			logger.info("Index deleted");
		}
		catch (IndexNotFoundException e) {
			logger.info("No such index \"{}\" (nothing deleted)", index);
		}
	}

	/**
	 * Creates an Elasticsearch index for the specified {@link DocumentType
	 * document type}.
	 * 
	 * @param documentType
	 */
	public static void createIndex(DocumentType<?> documentType)
	{
		createIndex(documentType.getIndexInfo());
	}

	/**
	 * Creates the specified index plus all document types it is configured to
	 * host.
	 * 
	 * @param indexInfo
	 */
	public static void createIndex(IndexInfo indexInfo)
	{
		String index = indexInfo.getName();
		logger.info("Creating index {}", index);
		// First load non-user-configurable settings
		String resource = "/es-settings.json";
		InputStream is = ESUtil.class.getResourceAsStream(resource);
		Builder builder = Settings.builder();
		try {
			builder.loadFromStream(resource, is);
		}
		catch (IOException e) {
			throw new DaoException(e);
		}
		// Then add user-configurable settings
		builder.put("index.number_of_shards", indexInfo.getNumShards());
		builder.put("index.number_of_replicas", indexInfo.getNumReplicas());
		CreateIndexRequestBuilder request = indices().prepareCreate(index);
		request.setSettings(builder.build());
		CreateIndexResponse response = request.execute().actionGet();
		if (!response.isAcknowledged()) {
			throw new DaoException("Failed to create index " + index);
		}
		for (DocumentType<?> dt : indexInfo.getTypes()) {
			createType(dt);
		}
	}

	/**
	 * Refreshes the index hosting the specified {@link DocumentType document
	 * type} (forcing all imported data to become "visible").
	 * 
	 * @param documentType
	 */
	public static void refreshIndex(DocumentType<?> documentType)
	{
		refreshIndex(documentType.getIndexInfo());
	}

	/**
	 * Refreshed the specified index (forcing all imported data to become
	 * "visible").
	 * 
	 * @param indexInfo
	 */
	public static void refreshIndex(IndexInfo indexInfo)
	{
		logger.info("Refreshing index " + indexInfo.getName());
		String index = indexInfo.getName();
		RefreshRequestBuilder request = indices().prepareRefresh(index);
		request.execute().actionGet();
		RefreshResponse response = request.execute().actionGet();
		if (response.getFailedShards() != 0) {
			logger.error("Index refresh failed index " + indexInfo.getName());
		}
	}

	/**
	 * Returns the index refresh interval for the specified index.
	 * 
	 * @param indexInfo
	 * @return
	 */
	public static String getAutoRefreshInterval(IndexInfo indexInfo)
	{
		String index = indexInfo.getName();
		GetSettingsRequest request = new GetSettingsRequest();
		GetSettingsResponse response = indices().getSettings(request).actionGet();
		try {
			return response.getSetting(index, "index.refresh_interval");
		}
		/*
		 * Hack to work around a bug in Elasticsearch (2.3.3). You get a nasty
		 * NullPointerException if the index does not exist, or if no settings
		 * have been explicitly set for it.
		 */
		catch (NullPointerException e) {
			return null;
		}
	}

	/**
	 * Sets the index refresh interval for -1 for the specified index.
	 * 
	 * @param indexInfo
	 * @return The original refresh interval
	 */
	public static String disableAutoRefresh(IndexInfo indexInfo)
	{
		String index = indexInfo.getName();
		logger.info("Disabling auto-refresh for index " + index);
		String origValue = getAutoRefreshInterval(indexInfo);
		UpdateSettingsRequest request = new UpdateSettingsRequest(index);
		Builder builder = Settings.builder();
		builder.put("index.refresh_interval", -1);
		request.settings(builder.build());
		UpdateSettingsResponse response = indices().updateSettings(request).actionGet();
		if (!response.isAcknowledged()) {
			String msg = "Failed to disable auto-refresh for index " + index;
			throw new DaoException(msg);
		}
		return origValue;
	}

	/**
	 * Sets the index refresh interval for the specified index.
	 * 
	 * @param indexInfo
	 * @param interval
	 */
	public static void setAutoRefreshInterval(IndexInfo indexInfo, String interval)
	{
		if (interval == null) {
			logger.warn("Setting the index refresh interval to null has no effect");
			return;
		}
		String index = indexInfo.getName();
		logger.info("Updating index refresh interval for index " + index);
		UpdateSettingsRequest request = new UpdateSettingsRequest(index);
		Builder builder = Settings.builder();
		builder.put("index.refresh_interval", interval);
		request.settings(builder.build());
		UpdateSettingsResponse response = indices().updateSettings(request).actionGet();
		if (!response.isAcknowledged()) {
			String msg = "Failed to update index refresh interval for index " + index;
			throw new DaoException(msg);
		}
	}

	/**
	 * Creates a type mapping for the specified {@link DocumentType document
	 * type}.
	 * 
	 * @param dt
	 */
	public static <T extends IDocumentObject> void createType(DocumentType<T> dt)
	{
		String index = dt.getIndexInfo().getName();
		String type = dt.getName();
		logger.info("Creating type {} in index {}", type, index);
		PutMappingRequestBuilder request = indices().preparePutMapping(index);
		MappingSerializer<T> serializer = new MappingSerializer<>();
		String source = serializer.serialize(dt.getMapping());
		request.setSource(source);
		request.setType(type);
		try {
			PutMappingResponse response = request.execute().actionGet();
			if (!response.isAcknowledged()) {
				throw new DaoException("Failed to create type " + type);
			}
		}
		catch (Throwable t) {
			String fmt = "Failed to create type %s: %s";
			String msg = String.format(fmt, type, t.getMessage());
			if (logger.isDebugEnabled()) {
				logger.debug(t);
			}
			throw new DaoException(msg);
		}
	}

	/**
	 * Retrieves the document with the specified {@code _id} and converts it to
	 * an instance of the class corresponding to the specified
	 * {@link DocumentType document type}.
	 * 
	 * @param dt
	 * @param id
	 * @return
	 */
	public static <T extends IDocumentObject> T find(DocumentType<T> dt, String id)
	{
		String index = dt.getIndexInfo().getName();
		String type = dt.getName();
		Class<T> cls = dt.getJavaType();
		Client client = ESClientManager.getInstance().getClient();
		GetRequestBuilder grb = client.prepareGet(index, type, id);
		GetResponse response = grb.execute().actionGet();
		ObjectMapper om = dt.getObjectMapper();
		if (response.isExists()) {
			try {
				/*
				 * NB we don't put the id back on the IDocumentObject instance.
				 * The reason is we only use this method in the ETL module to
				 * load a document, enrich it with new data and then save it
				 * back to Elasticsearch (e.g. see CoLSynonymTransformer).
				 * Therefore the id field must be blank. It corresponds to the
				 * document ID (_id), which is not part of the document source.
				 * If the id field would be set, you would get an error when
				 * saving the IDocumentObject instance, because there is no id
				 * field in the document type mapping. Therefore this method is
				 * of limited use. TODO: think some more about this.
				 */
				return om.readValue(response.getSourceAsBytes(), cls);
			}
			catch (IOException e) {
				throw new DaoException(e);
			}
		}
		return null;
	}

	public static <T extends IDocumentObject> List<T> find(DocumentType<T> dt,
			Collection<String> ids)
	{
		SearchRequestBuilder request = newSearchRequest(dt);
		IdsQueryBuilder query = QueryBuilders.idsQuery(dt.getName());
		query.addIds(ids.toArray(new String[ids.size()]));
		request.setQuery(query);
		SearchResponse response = executeSearchRequest(request);
		SearchHit[] hits = response.getHits().getHits();
		if (hits.length == 0) {
			return Collections.emptyList();
		}
		List<T> objs = new ArrayList<>(hits.length);
		ObjectMapper om = dt.getObjectMapper();
		for (SearchHit hit : hits) {
			/*
			 * NB again, we don't put the id back on the IDocumentObject
			 * instance. See above.
			 */
			T obj = om.convertValue(hit.getSource(), dt.getJavaType());
			objs.add(obj);
		}
		return objs;
	}

	public static <T extends IDocumentObject> List<T> find(DocumentType<T> dt, String field,
			String value)
	{
		SearchRequestBuilder request = newSearchRequest(dt);
		request.setQuery(termQuery(field, value));
		SearchResponse response = executeSearchRequest(request);
		SearchHit[] hits = response.getHits().getHits();
		if (hits.length == 0) {
			return Collections.emptyList();
		}
		List<T> objs = new ArrayList<>(hits.length);
		ObjectMapper om = dt.getObjectMapper();
		for (SearchHit hit : hits) {
			/*
			 * NB now we do set the ID, because we use this method in the
			 * nl.naturalis.nba.etl.name.NameTransformer, which really needs the
			 * ID. In other words, this little family of methods is hugely
			 * inconsistent and (TODO) we need to tighten this up. Maybe not
			 * provide these methods at all via ESUtil, and let classes who need
			 * their functionality implement themselves it as they see fit.
			 */
			T obj = om.convertValue(hit.getSource(), dt.getJavaType());
			obj.setId(hit.getId());
			objs.add(obj);
		}
		return objs;
	}

	public static List<String> lookup(DocumentType<?> dt, String field, String value)
	{
		SearchRequestBuilder request = newSearchRequest(dt);
		request.setQuery(termQuery(field, value));
		request.setFetchSource(false);
		SearchResponse response = executeSearchRequest(request);
		SearchHit[] hits = response.getHits().getHits();
		List<String> ids = new ArrayList<>(hits.length);
		for (SearchHit hit : hits) {
			ids.add(hit.getId());
		}
		return ids;
	}

	private static IndicesAdminClient indices()
	{
		return ESClientManager.getInstance().getClient().admin().indices();
	}

}
