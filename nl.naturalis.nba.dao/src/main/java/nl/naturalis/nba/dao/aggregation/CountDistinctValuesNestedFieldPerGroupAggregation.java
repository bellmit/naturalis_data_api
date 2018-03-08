package nl.naturalis.nba.dao.aggregation;

import static nl.naturalis.nba.dao.util.es.ESUtil.executeSearchRequest;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality;
import nl.naturalis.nba.api.InvalidQueryException;
import nl.naturalis.nba.api.QuerySpec;
import nl.naturalis.nba.api.model.IDocumentObject;
import nl.naturalis.nba.dao.DocumentType;

public class CountDistinctValuesNestedFieldPerGroupAggregation<T extends IDocumentObject, U> extends CountDistinctValuesPerGroupAggregation<T, List<Map<String, Object>>> {

  CountDistinctValuesNestedFieldPerGroupAggregation(DocumentType<T> dt, String field, String group,
      QuerySpec querySpec) {
    super(dt, field, group, querySpec);
  }

  @Override
  public SearchResponse executeQuery() throws InvalidQueryException {

    SearchRequestBuilder request = createSearchRequest(querySpec);
    String pathToNestedField = getNestedPath(field);
    int aggSize = getAggregationSize(querySpec);
    Order groupOrder = setOrdering(group, querySpec);

    AggregationBuilder fieldAgg = AggregationBuilders.nested("FIELD", pathToNestedField);
    CardinalityAggregationBuilder cardinalityField = AggregationBuilders.cardinality("DISTINCT_VALUES").field(field);
    fieldAgg.subAggregation(cardinalityField);
    AggregationBuilder groupAgg = AggregationBuilders.terms("GROUP").field(group).size(aggSize).order(groupOrder);
    groupAgg.subAggregation(fieldAgg);
    request.addAggregation(groupAgg);

    return executeSearchRequest(request);
  }

  @Override
  public List<Map<String, Object>> getResult() throws InvalidQueryException {
    
    SearchResponse response = executeQuery();
    List<Map<String, Object>> result = new LinkedList<>();
    
    Terms groupTerms = response.getAggregations().get("GROUP");
    List<Bucket> buckets = groupTerms.getBuckets();
    for (Bucket bucket : buckets) {
      InternalNested fields = bucket.getAggregations().get("FIELD");
      InternalCardinality cardinality = fields.getAggregations().get("DISTINCT_VALUES");
      Map<String, Object> hashMap = new LinkedHashMap<>(2);
      hashMap.put(group, bucket.getKeyAsString());
      hashMap.put(field, cardinality.getValue());
      result.add(hashMap);
    }
    return result;
  }

}