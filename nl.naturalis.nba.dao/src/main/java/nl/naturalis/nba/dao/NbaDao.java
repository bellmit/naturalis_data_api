package nl.naturalis.nba.dao;

import static nl.naturalis.nba.dao.DaoUtil.getLogger;
import static nl.naturalis.nba.dao.util.es.ESUtil.executeSearchRequest;
import static nl.naturalis.nba.dao.util.es.ESUtil.newSearchRequest;
import static nl.naturalis.nba.dao.util.es.ESUtil.toDocumentObject;
import static nl.naturalis.nba.utils.debug.DebugUtil.printCall;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.nested.InternalReverseNested;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality;
import nl.naturalis.nba.api.INbaAccess;
import nl.naturalis.nba.api.InvalidQueryException;
import nl.naturalis.nba.api.NoSuchFieldException;
import nl.naturalis.nba.api.QueryResult;
import nl.naturalis.nba.api.QueryResultItem;
import nl.naturalis.nba.api.QuerySpec;
import nl.naturalis.nba.api.SortField;
import nl.naturalis.nba.api.model.IDocumentObject;
import nl.naturalis.nba.common.es.map.MappingInfo;
import nl.naturalis.nba.common.json.JsonUtil;
import nl.naturalis.nba.dao.exception.DaoException;
import nl.naturalis.nba.dao.translate.QuerySpecTranslator;
import nl.naturalis.nba.dao.util.es.ESUtil;

public abstract class NbaDao<T extends IDocumentObject> implements INbaAccess<T> {

  private static final Logger logger = getLogger(NbaDao.class);

  private final DocumentType<T> dt;

  NbaDao(DocumentType<T> dt) {
    this.dt = dt;
  }

  @Override
  public T find(String id) {
    if (logger.isDebugEnabled()) {
      logger.debug(printCall("find", id));
    }
    GetRequestBuilder request = ESUtil.esClient().prepareGet();
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
    byte[] json = BytesReference.toBytes(response.getSourceAsBytesRef());
    T obj = JsonUtil.deserialize(dt.getObjectMapper(), json, dt.getJavaType());
    obj.setId(id);
    return obj;
  }

  @Override
  public T[] findByIds(String[] ids) {
    if (logger.isDebugEnabled()) {
      logger.debug(printCall("find", ids));
    }
    if (ids.length > 1024) {
      String fmt = "Number of ids to look up exceeds maximum of 1024: %s";
      String msg = String.format(fmt, ids.length);
      throw new DaoException(msg);
    }
    String type = dt.getName();
    SearchRequestBuilder request = newSearchRequest(dt);
    IdsQueryBuilder query = QueryBuilders.idsQuery(type);
    query.addIds(ids);
    request.setQuery(query);
    request.setSize(ids.length);
    return processSearchRequest(request);
  }

  @Override
  public QueryResult<T> query(QuerySpec querySpec) throws InvalidQueryException {
    if (logger.isDebugEnabled()) {
      logger.debug(printCall("query", querySpec));
    }
    QuerySpecTranslator translator = new QuerySpecTranslator(querySpec, dt);
    return createSearchResult(translator.translate());
  }

  @Override
  public long count(QuerySpec querySpec) throws InvalidQueryException {
    if (logger.isDebugEnabled()) {
      logger.debug(printCall("count", querySpec));
    }
    
    SearchRequestBuilder request = createSearchRequest(querySpec);
    SearchResponse response = executeSearchRequest(request);
    return response.getHits().totalHits();
  }

  public long countDistinctValues(String forField, QuerySpec querySpec) throws InvalidQueryException {
    if (logger.isDebugEnabled()) {
      logger.debug(printCall("countDistinctValues", forField, querySpec));
    }

    SearchRequestBuilder request = createSearchRequest(querySpec);
    String nestedPath = getNestedPath(forField);
    long result = 0L;
    
    if (nestedPath != null) {
      AggregationBuilder nested = AggregationBuilders.nested("NESTED", nestedPath);
      AggregationBuilder agg = AggregationBuilders.cardinality("CARDINALITY").field(forField);
      nested.subAggregation(agg);
      request.addAggregation(nested);
      
      SearchResponse response = executeSearchRequest(request);
      
      Nested nestedDocs = response.getAggregations().get("NESTED");
      Cardinality cardinality = nestedDocs.getAggregations().get("CARDINALITY");
      result = cardinality.getValue();
      
    } else {
      AggregationBuilder agg = AggregationBuilders.cardinality("CARDINALITY").field(forField);
      request.addAggregation(agg);
      
      SearchResponse response = executeSearchRequest(request);  
      
      Cardinality cardinality = response.getAggregations().get("CARDINALITY");
      result = cardinality.getValue();
    }
    return result;
  }

  public List<Map<String, Object>> countDistinctValuesPerGroup(String forField, String forGroup,
      QuerySpec querySpec) throws InvalidQueryException {
    if (logger.isDebugEnabled()) {
      logger.debug(printCall("countDistinctValuesPerGroup", forField, forGroup, querySpec));
    }
    
    SearchRequestBuilder request = createSearchRequest(querySpec);
    String pathToNestedField = getNestedPath(forField);
    String pathToNestedGroup = getNestedPath(forGroup);
    int aggSize = getAggregationSize(querySpec);
    Order groupOrder = setOrdering(forGroup, querySpec);
    
    List<Map<String, Object>> result = new LinkedList<>();

    // Based on the query mapping, use the correct aggregation builder
    if (pathToNestedField == null && pathToNestedGroup == null) {
      // http://localhost:8080/v2/specimen/countDistinctValuesPerGroup/collectionType/recordBasis
      CardinalityAggregationBuilder fieldAgg = AggregationBuilders.cardinality("DISTINCT_VALUES").field(forField);
      AggregationBuilder groupAgg = AggregationBuilders.terms("GROUP").field(forGroup).size(aggSize).order(groupOrder);
      groupAgg.subAggregation(fieldAgg);
      request.addAggregation(groupAgg);
      
      SearchResponse response = executeSearchRequest(request);

      Terms groupTerms = response.getAggregations().get("GROUP");
      List<Bucket> buckets = groupTerms.getBuckets();
      for (Bucket bucket : buckets) {
        InternalCardinality cardinality = bucket.getAggregations().get("DISTINCT_VALUES");
        Map<String, Object> hashMap = new LinkedHashMap<>(2);
        hashMap.put(forGroup, bucket.getKeyAsString());
        hashMap.put(forField, cardinality.getValue());
        result.add(hashMap);
      }
    } else if (pathToNestedField != null && pathToNestedGroup == null) {
      // http://localhost:8080/v2/specimen/countDistinctValuesPerGroup/collectionType/gatheringEvent.gatheringPersons.fullName
      AggregationBuilder fieldAgg = AggregationBuilders.nested("FIELD", pathToNestedField);
      CardinalityAggregationBuilder cardinalityField = AggregationBuilders.cardinality("DISTINCT_VALUES").field(forField);
      fieldAgg.subAggregation(cardinalityField);
      AggregationBuilder groupAgg = AggregationBuilders.terms("GROUP").field(forGroup).size(aggSize).order(groupOrder);
      groupAgg.subAggregation(fieldAgg);
      request.addAggregation(groupAgg);
      
      SearchResponse response = executeSearchRequest(request);

     Terms groupTerms = response.getAggregations().get("GROUP");
      List<Bucket> buckets = groupTerms.getBuckets();
      for (Bucket bucket : buckets) {
        InternalNested fields = bucket.getAggregations().get("FIELD");
        InternalCardinality cardinality = fields.getAggregations().get("DISTINCT_VALUES");
        Map<String, Object> hashMap = new LinkedHashMap<>(2);
        hashMap.put(forGroup, bucket.getKeyAsString());
        hashMap.put(forField, cardinality.getValue());
        result.add(hashMap);
      }

    } else if (pathToNestedField == null && pathToNestedGroup != null) {
      // http://localhost:8080/v2/specimen/countDistinctValuesPerGroup/identifications.defaultClassification.className/collectionType
      AggregationBuilder fieldAgg = AggregationBuilders.reverseNested("REVERSE_NESTED_FIELD");
      CardinalityAggregationBuilder cardinalityField = AggregationBuilders.cardinality("DISTINCT_VALUES").field(forField);
      fieldAgg.subAggregation(cardinalityField);
      AggregationBuilder groupAgg = AggregationBuilders.nested("NESTED_GROUP", pathToNestedGroup);
      AggregationBuilder groupTerm = AggregationBuilders.terms("GROUP").field(forGroup).size(aggSize).order(groupOrder);
      groupTerm.subAggregation(fieldAgg);
      groupAgg.subAggregation(groupTerm);
      request.addAggregation(groupAgg);
      
      SearchResponse response = executeSearchRequest(request);

      InternalNested nestedGroup = response.getAggregations().get("NESTED_GROUP");
      Terms groupTerms = nestedGroup.getAggregations().get("GROUP");
      List<Bucket> buckets = groupTerms.getBuckets();
      for (Bucket bucket : buckets) {
        InternalReverseNested fields = bucket.getAggregations().get("REVERSE_NESTED_FIELD");
        InternalCardinality cardinality = fields.getAggregations().get("DISTINCT_VALUES");
        Map<String, Object> hashMap = new LinkedHashMap<>(2);
        hashMap.put(forGroup, bucket.getKeyAsString());
        hashMap.put(forField, cardinality.getValue());
        result.add(hashMap);
      }
    } else {
      // http://localhost:8080/v2/specimen/countDistinctValuesPerGroup/identifications.defaultClassification.className/gatheringEvent.gatheringPersons.fullName
      AggregationBuilder fieldAgg = AggregationBuilders.reverseNested("REVERSE_NESTED_FIELD");
      AggregationBuilder fieldNested = AggregationBuilders.nested(forField, pathToNestedField);
      CardinalityAggregationBuilder cardinalityField = AggregationBuilders.cardinality("DISTINCT_VALUES").field(forField);
      fieldNested.subAggregation(cardinalityField);
      fieldAgg.subAggregation(fieldNested);
      AggregationBuilder groupAgg = AggregationBuilders.nested("NESTED_GROUP", pathToNestedGroup);
      AggregationBuilder groupTerm = AggregationBuilders.terms("GROUP").field(forGroup).size(aggSize).order(groupOrder);
      groupTerm.subAggregation(fieldAgg);
      groupAgg.subAggregation(groupTerm);
      request.addAggregation(groupAgg);
      
      SearchResponse response = executeSearchRequest(request);

      InternalNested nestedGroup = response.getAggregations().get("NESTED_GROUP");
      Terms groupTerms = nestedGroup.getAggregations().get("GROUP");
      List<Bucket> buckets = groupTerms.getBuckets();
      for (Bucket bucket : buckets) {
        InternalReverseNested fields = bucket.getAggregations().get("REVERSE_NESTED_FIELD");
        InternalNested nestedFields = fields.getAggregations().get(forField);
        InternalCardinality cardinality = nestedFields.getAggregations().get("DISTINCT_VALUES");
        Map<String, Object> hashMap = new LinkedHashMap<>(2);
        hashMap.put(forGroup, bucket.getKeyAsString());
        hashMap.put(forField, cardinality.getValue());
        result.add(hashMap);
      }
    }
    return result;
  }

  @Override
  public Map<String, Long> getDistinctValues(String forField, QuerySpec querySpec)
      throws InvalidQueryException {
    if (logger.isDebugEnabled()) {
      logger.debug(printCall("getDistinctValues", forField, querySpec));
    }
    SearchRequestBuilder request = createSearchRequest(querySpec);
    String nestedPath = getNestedPath(forField);
    int aggSize = getAggregationSize(querySpec);
    Order fieldOrder = setOrdering(forField, querySpec);
    
    TermsAggregationBuilder termsAggregation = terms("FIELD");
    termsAggregation.field(forField);
    termsAggregation.size(aggSize).order(fieldOrder);

    Terms terms;
    if (nestedPath == null) {
      request.addAggregation(termsAggregation);
      SearchResponse response = executeSearchRequest(request);
      terms = response.getAggregations().get("FIELD");
    } else {
      NestedAggregationBuilder nestedAggregation = nested("NESTED_FIELD", nestedPath);
      nestedAggregation.subAggregation(termsAggregation);
      request.addAggregation(nestedAggregation);
      SearchResponse response = executeSearchRequest(request);
      Nested nested = response.getAggregations().get("NESTED_FIELD");
      terms = nested.getAggregations().get("FIELD");
    }
    Map<String, Long> result = new LinkedHashMap<>(terms.getBuckets().size());
    for (Bucket bucket : terms.getBuckets()) {
      result.put(bucket.getKeyAsString(), bucket.getDocCount());
    }
    return result;
  }

  public List<Map<String, Object>> getDistinctValuesPerGroup(String forField, String forGroup,
      QuerySpec querySpec) throws InvalidQueryException {
    if (logger.isDebugEnabled()) {
      logger.debug(printCall("getDistinctValuesPerGroup", forField, forGroup, querySpec));
    }

    SearchRequestBuilder request = createSearchRequest(querySpec);
    String pathToNestedField = getNestedPath(forField);
    String pathToNestedGroup = getNestedPath(forGroup);
    int aggSize = getAggregationSize(querySpec);
    Order fieldOrder = setOrdering(forField, querySpec);
    Order groupOrder = setOrdering(forGroup, querySpec);

    List<Map<String, Object>> result = new LinkedList<>();
    Terms groupTerms;
    StringTerms fieldTerms;
    
    if (pathToNestedField == null && pathToNestedGroup == null) {
      // http://localhost:8080/v2/specimen/getDistinctValuesPerGroup/sourceSystem.code/collectionType
      AggregationBuilder fieldAgg = AggregationBuilders.terms("FIELD").field(forField).size(aggSize).order(fieldOrder);      
      AggregationBuilder groupAgg = AggregationBuilders.terms("GROUP").field(forGroup).size(aggSize).order(groupOrder);
      groupAgg.subAggregation(fieldAgg);

      request.addAggregation(groupAgg);
      SearchResponse response = executeSearchRequest(request);

      groupTerms = response.getAggregations().get("GROUP");
      List<Bucket> buckets = groupTerms.getBuckets();
      for (Bucket bucket : buckets) {
        fieldTerms = bucket.getAggregations().get("FIELD");
        List<StringTerms.Bucket> innerBuckets = fieldTerms.getBucketsInternal();
        List<Map<String, Object>> fieldTermsList = new LinkedList<>();
        for (Bucket innerBucket : innerBuckets) {
          Map<String, Object> aggregate = new LinkedHashMap<>(2);
          aggregate.put(forField, innerBucket.getKeyAsString());
          aggregate.put("count", innerBucket.getDocCount());
          if (innerBucket.getDocCount() > 0) {
            fieldTermsList.add(aggregate);
          }
        }
        Map<String, Object> hashMap = new LinkedHashMap<>(2);
        hashMap.put(forGroup, bucket.getKeyAsString());
        hashMap.put("count", bucket.getDocCount());
        if (fieldTermsList.size() > 0) {
          hashMap.put("values", fieldTermsList);
        }
        result.add(hashMap);
      }
    } else if (pathToNestedField != null && pathToNestedGroup == null) {
      // http://localhost:8080/v2/specimen/getDistinctValuesPerGroup/sourceSystem.code/identifications.taxonRank
      AggregationBuilder fieldAgg = AggregationBuilders.terms("FIELD").field(forField).size(aggSize).order(fieldOrder);
      AggregationBuilder nestedFieldAgg = AggregationBuilders.nested("NESTED_FIELD", pathToNestedField);
      nestedFieldAgg.subAggregation(fieldAgg);
      AggregationBuilder groupAgg = AggregationBuilders.terms("GROUP").field(forGroup).size(aggSize).order(groupOrder);
      groupAgg.subAggregation(nestedFieldAgg);

      request.addAggregation(groupAgg);
      SearchResponse response = executeSearchRequest(request);

      groupTerms = response.getAggregations().get("GROUP");
      List<Bucket> buckets = groupTerms.getBuckets();
      for (Bucket bucket : buckets) {
        Nested nestedField = bucket.getAggregations().get("NESTED_FIELD");
        fieldTerms = nestedField.getAggregations().get("FIELD");
        List<Bucket> innerBuckets = fieldTerms.getBuckets();
        List<Map<String, Object>> fieldTermsList = new LinkedList<>();
        for (Bucket innerBucket : innerBuckets) {
          Map<String, Object> aggregate = new LinkedHashMap<>(2);
          aggregate.put(forField, innerBucket.getKeyAsString());
          aggregate.put("count", innerBucket.getDocCount());
          if (innerBucket.getDocCount() > 0) {
            fieldTermsList.add(aggregate);
          }
        }
        Map<String, Object> hashMap = new LinkedHashMap<>(2);
        hashMap.put(forGroup, bucket.getKeyAsString());
        hashMap.put("count", bucket.getDocCount());
        if (fieldTermsList.size() > 0) {
          hashMap.put("values", fieldTermsList);
        }
        result.add(hashMap);
      }
    } else if (pathToNestedField == null && pathToNestedGroup != null) {
      // http://localhost:8080/v2/specimen/getDistinctValuesPerGroup/identifications.taxonRank/sourceSystem.code
      AggregationBuilder fieldAgg = AggregationBuilders.terms("FIELD").field(forField).size(aggSize).order(fieldOrder);
      ReverseNestedAggregationBuilder revNestedFieldAgg = AggregationBuilders.reverseNested("REVERSE_NESTED_FIELD");
      revNestedFieldAgg.subAggregation(fieldAgg);
      AggregationBuilder nestedGroupAgg = AggregationBuilders.nested("NESTED_GROUP", pathToNestedGroup);
      AggregationBuilder groupAgg = AggregationBuilders.terms("GROUP").field(forGroup).size(aggSize).order(groupOrder);
      groupAgg.subAggregation(revNestedFieldAgg);
      nestedGroupAgg.subAggregation(groupAgg);

      request.addAggregation(nestedGroupAgg);
      SearchResponse response = executeSearchRequest(request);

      Nested nestedGroup = response.getAggregations().get("NESTED_GROUP");
      groupTerms = nestedGroup.getAggregations().get("GROUP");
      List<Bucket> buckets = groupTerms.getBuckets();
      for (Bucket bucket : buckets) {
        InternalReverseNested nestedField = bucket.getAggregations().get("REVERSE_NESTED_FIELD");
        fieldTerms = nestedField.getAggregations().get("FIELD");
        List<Bucket> innerBuckets = fieldTerms.getBuckets();
        List<Map<String, Object>> fieldTermsList = new LinkedList<>();
        for (Bucket innerBucket : innerBuckets) {
          Map<String, Object> aggregate = new LinkedHashMap<>(2);
          aggregate.put(forField, innerBucket.getKeyAsString());
          aggregate.put("count", innerBucket.getDocCount());
          if (innerBucket.getDocCount() > 0) {
            fieldTermsList.add(aggregate);
          }
        }
        Map<String, Object> hashMap = new LinkedHashMap<>(2);
        hashMap.put(forGroup, bucket.getKeyAsString());
        hashMap.put("count", bucket.getDocCount());
        if (fieldTermsList.size() > 0) {
          hashMap.put("values", fieldTermsList);
        }
        result.add(hashMap);
      }
    } else {
      // http://localhost:8080/v2/specimen/getDistinctValuesPerGroup/identifications.taxonRank/gatheringEvent.gatheringPersons.fullName
      AggregationBuilder nestedFieldAgg = AggregationBuilders.nested("NESTED_FIELD", pathToNestedField);
      AggregationBuilder fieldAgg = AggregationBuilders.terms("FIELD").field(forField).size(aggSize).order(fieldOrder);
      nestedFieldAgg.subAggregation(fieldAgg);
      AggregationBuilder nestedGroupAgg = AggregationBuilders.nested("NESTED_GROUP", pathToNestedGroup);
      AggregationBuilder groupAgg = AggregationBuilders.terms("GROUP").field(forGroup).size(aggSize).order(groupOrder);
      ReverseNestedAggregationBuilder revNestedFieldAgg = AggregationBuilders.reverseNested("REVERSE_NESTED_FIELD");
      revNestedFieldAgg.subAggregation(nestedFieldAgg);
      groupAgg.subAggregation(revNestedFieldAgg);
      nestedGroupAgg.subAggregation(groupAgg);

      request.addAggregation(nestedGroupAgg);
      SearchResponse response = executeSearchRequest(request);

      Nested nestedGroup = response.getAggregations().get("NESTED_GROUP");
      groupTerms = nestedGroup.getAggregations().get("GROUP");
      List<Bucket> buckets = groupTerms.getBuckets();
      for (Bucket bucket : buckets) {
        InternalReverseNested reverseNestedField = bucket.getAggregations().get("REVERSE_NESTED_FIELD");
        Nested nestedField = reverseNestedField.getAggregations().get("NESTED_FIELD");
        fieldTerms = nestedField.getAggregations().get("FIELD");
        List<Bucket> innerBuckets = fieldTerms.getBuckets();
        List<Map<String, Object>> fieldTermsList = new LinkedList<>();
        for (Bucket innerBucket : innerBuckets) {
          Map<String, Object> aggregate = new LinkedHashMap<>(2);
          aggregate.put(forField, innerBucket.getKeyAsString());
          aggregate.put("count", innerBucket.getDocCount());
          if (innerBucket.getDocCount() > 0) {
            fieldTermsList.add(aggregate);
          }
        }
        Map<String, Object> hashMap = new LinkedHashMap<>(2);
        hashMap.put(forGroup, bucket.getKeyAsString());
        hashMap.put("count", bucket.getDocCount());
        if (fieldTermsList.size() > 0) {
          hashMap.put("values", fieldTermsList);
        }
        result.add(hashMap);
      }
    }
    return result;
  }

  public String save(T apiObject, boolean immediate) {
    String id = apiObject.getId();
    String index = dt.getIndexInfo().getName();
    String type = dt.getName();
    if (logger.isDebugEnabled()) {
      String pattern = "New save request (index={};type={};id={})";
      logger.debug(pattern, index, type, id);
    }
    IndexRequestBuilder request = ESUtil.esClient().prepareIndex(index, type, id);
    byte[] source = JsonUtil.serialize(apiObject);
    request.setSource(source);
    IndexResponse response = request.execute().actionGet();
    if (immediate) {
      IndicesAdminClient iac = ESUtil.esClient().admin().indices();
      RefreshRequestBuilder rrb = iac.prepareRefresh(index);
      rrb.execute().actionGet();
    }
    apiObject.setId(response.getId());
    return response.getId();
  }

  public boolean delete(String id, boolean immediate) {
    String index = dt.getIndexInfo().getName();
    String type = dt.getName();
    DeleteRequestBuilder request = ESUtil.esClient().prepareDelete(index, type, id);
    DeleteResponse response = request.execute().actionGet();
    if (immediate) {
      IndicesAdminClient iac = ESUtil.esClient().admin().indices();
      RefreshRequestBuilder rrb = iac.prepareRefresh(index);
      rrb.execute().actionGet();
    }
    return response.getResult() == Result.DELETED;
  }

  abstract T[] createDocumentObjectArray(int length);

  T[] processSearchRequest(SearchRequestBuilder request) {
    SearchResponse response = executeSearchRequest(request);
    return processQueryResponse(response);
  }

  private SearchRequestBuilder createSearchRequest(QuerySpec querySpec) throws InvalidQueryException {
    SearchRequestBuilder request;
    if (querySpec == null) {
      request = newSearchRequest(dt);
    } else {
      QuerySpecTranslator translator = new QuerySpecTranslator(querySpec, dt);
      request = translator.translate();
    }
    request.setSize(0);
    return request;
  }
  
  private List<QueryResultItem<T>> createItems(SearchResponse response) {
    if (logger.isDebugEnabled()) {
      String type = dt.getJavaType().getSimpleName();
      logger.debug("Converting search hits to {} instances", type);
    }
    SearchHit[] hits = response.getHits().getHits();
    List<QueryResultItem<T>> items = new ArrayList<>(hits.length);
    for (SearchHit hit : hits) {
      T obj = toDocumentObject(hit, dt);
      items.add(new QueryResultItem<>(obj, hit.getScore()));
    }
    return items;
  }

  private QueryResult<T> createSearchResult(SearchRequestBuilder request) {
    SearchResponse response = executeSearchRequest(request);
    QueryResult<T> result = new QueryResult<>();
    result.setTotalSize(response.getHits().totalHits());
    result.setResultSet(createItems(response));
    return result;
  }
  
  /**
   * Aggregation Size: the value of the size parameter from the queryspec is used to set 
   * the value of the aggregation size.
   * 
   * @param querySpec
   * @return
   */
  private static int getAggregationSize(QuerySpec querySpec) {
    int aggSize = 10000;
    if (querySpec != null && querySpec.getSize() != null && querySpec.getSize() > 0) {
      aggSize = querySpec.getSize();
    }
    return aggSize;
  }
  
  /**
   * Return the nested path needed for the Elasticsearch query.
   * 
   * @param field
   * @return nestedPath
   * @throws InvalidQueryException
   */
  private String getNestedPath(String field) throws InvalidQueryException {
    MappingInfo<T> mappingInfo = new MappingInfo<>(dt.getMapping());
    String nestedPath = null;
    try {
      nestedPath = mappingInfo.getNestedPath(field);
    } catch (NoSuchFieldException e) {
      throw new InvalidQueryException(e.getMessage());
    }
    return nestedPath;    
  }

  private T[] processQueryResponse(SearchResponse response) {
    SearchHit[] hits = response.getHits().getHits();
    T[] documentObjects = createDocumentObjectArray(hits.length);
    for (int i = 0; i < hits.length; ++i) {
      documentObjects[i] = toDocumentObject(hits[i], dt);
    }
    return documentObjects;
  }
  
  /**
   * Sorting: if the field (or group) is included as a sortField in the querySpec, 
   * then it is also used to order the aggregation result by the group terms. 
   * Otherwise, the aggregation will be ordered by descending document count.
   * 
   * @param fieldName
   * @param querySpec
   * @return order
   */
  private static Order setOrdering(String fieldName, QuerySpec querySpec) {
    Order order = Terms.Order.count(false);
    if (querySpec != null && querySpec.getSortFields() != null) {
      for (SortField sortField : querySpec.getSortFields()) {
        if (sortField.getPath().equals(new SortField(fieldName).getPath())) {
          if (sortField.isAscending())
            order = Terms.Order.term(true);
          else
            order = Terms.Order.term(false);
        }
      }
    }
    return order;
  }



}
