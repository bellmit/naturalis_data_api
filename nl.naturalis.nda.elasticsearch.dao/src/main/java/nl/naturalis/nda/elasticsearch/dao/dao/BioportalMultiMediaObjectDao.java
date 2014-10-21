package nl.naturalis.nda.elasticsearch.dao.dao;

import nl.naturalis.nda.domain.MultiMediaObject;
import nl.naturalis.nda.domain.Taxon;
import nl.naturalis.nda.domain.TaxonMultiMediaObject;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESMultiMediaObject;
import nl.naturalis.nda.elasticsearch.dao.transfer.MultiMediaObjectTransfer;
import nl.naturalis.nda.elasticsearch.dao.util.FieldMapping;
import nl.naturalis.nda.search.QueryParams;
import nl.naturalis.nda.search.Link;
import nl.naturalis.nda.search.SearchResult;
import nl.naturalis.nda.search.SearchResultSet;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import java.util.*;

public class BioportalMultiMediaObjectDao extends AbstractDao {

    private static final Set<String> multiMediaSearchFields = new HashSet<>(Arrays.asList(
            "unitID",
            "sexes",
            "specimenTypeStatus",
            "phasesOrStages",
            "identifications.vernacularNames.name",
            "identifications.defaultClassification.kingdom",
            "identifications.defaultClassification.phylum",
            "identifications.defaultClassification.className",
            "identifications.defaultClassification.order",
            "identifications.defaultClassification.family",
            "identifications.defaultClassification.genusOrMonomial",
            "identifications.defaultClassification.subgenus",
            "identifications.defaultClassification.specificEpithet",
            "identifications.defaultClassification.infraspecificEpithet",
            "identifications.scientificName.genusOrMonomial",
            "identifications.scientificName.subgenus",
            "identifications.scientificName.specificEpithet",
            "identifications.scientificName.infraspecificEpithet",
            "gatheringEvents.siteCoordinates.point"
    ));

    private final BioportalTaxonDao bioportalTaxonDao;
    private TaxonDao taxonDao;

    public BioportalMultiMediaObjectDao(Client esClient, String ndaIndexName, BioportalTaxonDao bioportalTaxonDao, TaxonDao taxonDao) {
        super(esClient, ndaIndexName);
        this.bioportalTaxonDao = bioportalTaxonDao;
        this.taxonDao = taxonDao;
    }

    /**
     * Retrieves multimedia matching a variable number of criteria. Rather than having
     * one search term and a fixed set of fields to match the search term against, the
     * fields to query and the values to look for are specified as parameters to this
     * method. Nevertheless, the fields will always belong to the list:
     * <ol>
     * <li>unitID</li>
     * <li>sexes</li>
     * <li>specimenTypeStatus</li>
     * <li>phasesOrStages</li>
     * <li>identifications.vernacularNames.name</li>
     * <li>identifications.defaultClassification.kingdom</li>
     * <li>identifications.defaultClassification.phylum</li>
     * <li>identifications.defaultClassification.className</li>
     * <li>identifications.defaultClassification.order</li>
     * <li>identifications.defaultClassification.family</li>
     * <li>identifications.defaultClassification.genusOrMonomial</li>
     * <li>identifications.defaultClassification.subgenus</li>
     * <li>identifications.defaultClassification.specificEpithet</li>
     * <li>identifications.defaultClassification.infraspecificEpithet</li>
     * <li>identifications.scientificName.genusOrMonomial</li>
     * <li>identifications.scientificName.subgenus</li>
     * <li>identifications.scientificName.specificEpithet</li>
     * <li>identifications.scientificName.infraspecificEpithet</li>
     * <li>gatheringEvents.siteCoordinates.point (= geo search)</li>
     * </ol>
     * Name resolution is used to find additional MultiMediaObject documents.
     *
     * @param params
     * @return
     */
    public SearchResultSet<MultiMediaObject> multiMediaObjectSearch(QueryParams params) {
        return search(params, multiMediaSearchFields);
    }

    public SearchResultSet<MultiMediaObject> getTaxonMultiMediaObjectDetailWithinResultSet(QueryParams params) {
        SearchResultSet<MultiMediaObject> multiMediaObjectSearchResultSet = multiMediaObjectSearch(params);

        return createMultiMediaObjectDetailSearchResultSet(params, multiMediaObjectSearchResultSet);
    }

    /**
     * Method as generic as possible for internal use
     *
     * @param params            search parameters
     * @param allowedFieldNames may be null if you don't want filtering
     * @return search results
     */
    SearchResultSet<MultiMediaObject> search(QueryParams params, Set<String> allowedFieldNames) {
        List<FieldMapping> fields = getSearchParamFieldMapping().getMultimediaMappingForFields(params);
        List<FieldMapping> allowedFields = (allowedFieldNames == null)
                ? fields
                : filterAllowedFieldMappings(fields, allowedFieldNames);

        SearchResponse searchResponse = executeExtendedSearch(params, allowedFields, MULTI_MEDIA_OBJECT_TYPE, true,
                buildNameResolutionQuery(fields, params.getParam("_search"), bioportalTaxonDao),
                Arrays.asList(IDENTIFICATIONS_SCIENTIFIC_NAME_GENUS_OR_MONOMIAL,
                        IDENTIFICATIONS_SCIENTIFIC_NAME_SPECIFIC_EPITHET,
                        IDENTIFICATIONS_SCIENTIFIC_NAME_INFRASPECIFIC_EPITHET));

        return responseToMultiMediaObjectSearchResultSet(searchResponse, params);
    }

    protected SearchResultSet<MultiMediaObject> createMultiMediaObjectDetailSearchResultSet(QueryParams params, SearchResultSet<MultiMediaObject> searchResultSet) {
        SearchResultSet<MultiMediaObject> detailResultSet = new SearchResultSet<>();

        SearchResult<MultiMediaObject> previousMultiMediaObject = null;
        SearchResult<MultiMediaObject> nextMultiMediaObject = null;

        String unitID = params.getParam("unitID");

        List<SearchResult<MultiMediaObject>> searchResults = searchResultSet.getSearchResults();
        for (SearchResult<MultiMediaObject> searchResult : searchResults) {
            List<Link> links = new ArrayList<>();

            MultiMediaObject multiMediaObject = searchResult.getResult();
            if (multiMediaObject.getUnitID().equals(unitID)) {
                int indexFoundMultiMediaObject = searchResults.indexOf(searchResult);
                int searchResultSize = searchResults.size();
                if (searchResultSize > 1) {
                    if (indexFoundMultiMediaObject == 0) {
                        // first item, no previous
                        nextMultiMediaObject = searchResults.get(1);
                    } else if (indexFoundMultiMediaObject == (searchResultSize - 1)) {
                        // last item, no next
                        previousMultiMediaObject = searchResults.get(indexFoundMultiMediaObject - 1);
                    } else {
                        nextMultiMediaObject = searchResults.get(indexFoundMultiMediaObject + 1);
                        previousMultiMediaObject = searchResults.get(indexFoundMultiMediaObject - 1);
                    }
                }

                //TODO Change links to correct url and href
                if (previousMultiMediaObject != null) {
                    links.add(new Link("http://test.nl?unitID=" + previousMultiMediaObject.getResult().getUnitID(), "_previous"));
                }
                if (nextMultiMediaObject != null) {
                    links.add(new Link("http://test.nl?unitID=" + nextMultiMediaObject.getResult().getUnitID(), "_next"));
                }

                QueryParams taxonParams = new QueryParams();
                taxonParams.add("sourceSystemId", multiMediaObject.getAssociatedTaxonReference());

                SearchResultSet<Taxon> taxonDetail = taxonDao.getTaxonDetail(taxonParams);
                if (taxonDetail.getSearchResults() != null && taxonDetail.getSearchResults().get(0) != null) {
                    multiMediaObject.setAssociatedTaxon(taxonDetail.getSearchResults().get(0).getResult());
                    links.add(new Link("http://test.nl?taxon=" + taxonDetail.getSearchResults().get(0).getResult().getSourceSystemId(), "_next"));
                }

                detailResultSet.addSearchResult(multiMediaObject);
                detailResultSet.setLinks(links);
            }
        }

        detailResultSet.setQueryParameters(params.copyWithoutGeoShape());
        return detailResultSet;
    }


    private SearchResultSet<MultiMediaObject> responseToMultiMediaObjectSearchResultSet(SearchResponse searchResponse,
                                                                                        QueryParams params) {
        SearchResultSet<MultiMediaObject> searchResultSet = new SearchResultSet<>();
        for (SearchHit hit : searchResponse.getHits()) {
            ESMultiMediaObject esObject = getObjectMapper().convertValue(hit.getSource(), ESMultiMediaObject.class);
            MultiMediaObject multiMediaObject = MultiMediaObjectTransfer.transfer(esObject);

            searchResultSet.addSearchResult(multiMediaObject);
        }

        // TODO links
        searchResultSet.setTotalSize(searchResponse.getHits().getTotalHits());
        searchResultSet.setQueryParameters(params.copyWithoutGeoShape());

        return searchResultSet;
    }

}
