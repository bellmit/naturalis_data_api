package nl.naturalis.nba.dao.aggregation;

import static nl.naturalis.nba.dao.util.es.ESUtil.newSearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import nl.naturalis.nba.api.InvalidQueryException;
import nl.naturalis.nba.api.NoSuchFieldException;
import nl.naturalis.nba.api.QuerySpec;
import nl.naturalis.nba.api.SortField;
import nl.naturalis.nba.api.model.IDocumentObject;
import nl.naturalis.nba.common.es.map.MappingInfo;
import nl.naturalis.nba.dao.DocumentType;
import nl.naturalis.nba.dao.translate.QuerySpecTranslator;

public abstract class AggregationQuery<T extends IDocumentObject, U> {
  
  QuerySpec querySpec;
  DocumentType<T> dt;

  AggregationQuery(DocumentType<T> dt, QuerySpec querySpec) {
    this.dt = dt;
    this.querySpec = querySpec;
  }
  
  public abstract SearchResponse executeQuery() throws InvalidQueryException;

  public abstract U getResult() throws InvalidQueryException; 

  /**
   * Takes a QuerySpec and returns a SearchRequestBuilder that can be used for an
   * aggregation query.
   * 
   * @param querySpec
   * @return 
   * @throws InvalidQueryException
   */
  SearchRequestBuilder createSearchRequest(QuerySpec querySpec) throws InvalidQueryException {
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
  
  /**
   * Aggregation Size: the value of the size parameter from the queryspec is used to set 
   * the value of the aggregation size.
   * 
   * @param querySpec
   * @return
   */
  public static int getAggregationSize(QuerySpec querySpec) {
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
  String getNestedPath(String field) throws InvalidQueryException {
    MappingInfo<?> mappingInfo = new MappingInfo<>(dt.getMapping());
    String nestedPath = null;
    try {
      nestedPath = mappingInfo.getNestedPath(field);
    } catch (NoSuchFieldException e) {
      throw new InvalidQueryException(e.getMessage());
    }
    return nestedPath;    
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
  static Order setOrdering(String fieldName, QuerySpec querySpec) {
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