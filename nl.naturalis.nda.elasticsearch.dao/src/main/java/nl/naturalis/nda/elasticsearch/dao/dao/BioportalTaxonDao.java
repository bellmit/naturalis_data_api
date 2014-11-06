package nl.naturalis.nda.elasticsearch.dao.dao;

import nl.naturalis.nda.domain.ScientificName;
import nl.naturalis.nda.domain.Taxon;
import nl.naturalis.nda.elasticsearch.dao.util.FieldMapping;
import nl.naturalis.nda.search.*;
import org.elasticsearch.client.Client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nl.naturalis.nda.elasticsearch.dao.util.ESConstants.Fields.TaxonFields.*;

public class BioportalTaxonDao extends AbstractTaxonDao {

    private static final Set<String> allowedFieldNamesForSearch = new HashSet<>(Arrays.asList(
            IDENTIFYING_EPITHETS,
            ACCEPTEDNAME_GENUS_OR_MONOMIAL,
            ACCEPTEDNAME_SUBGENUS,
            ACCEPTEDNAME_SPECIFIC_EPITHET,
            ACCEPTEDNAME_INFRASPECIFIC_EPITHET,
            ACCEPTEDNAME_EXPERTS_FULLNAME,
            ACCEPTEDNAME_EXPERTS_ORGANIZATION_NAME,
            ACCEPTEDNAME_TAXONOMIC_STATUS,
            VERNACULARNAMES_NAME,
            VERNACULARNAMES_EXPERTS_FULLNAME,
            VERNACULARNAMES_EXPERTS_ORGANIZATION_NAME,
            SYNONYMS_GENUSORMONOMIAL,
            SYNONYMS_SUBGENUS,
            SYNONYMS_SPECIFIC_EPITHET,
            SYNONYMS_INFRASPECIFIC_EPITHET,
            SYNONYMS_EXPERT_FULLNAME,
            SYNONYMS_EXPERT_ORGANIZATION_NAME,
            SYNONYMS_TAXONOMIC_STATUS,
            DEFAULT_CLASSIFICATION_KINGDOM,
            DEFAULT_CLASSIFICATION_PHYLUM,
            DEFAULT_CLASSIFICATION_CLASS_NAME,
            DEFAULT_CLASSIFICATION_ORDER,
            DEFAULT_CLASSIFICATION_FAMILY,
            DEFAULT_CLASSIFICATION_GENUS,
            DEFAULT_CLASSIFICATION_SUBGENUS,
            DEFAULT_CLASSIFICATION_SPECIFIC_EPITHET,
            DEFAULT_CLASSIFICATION_INFRASPECIFIC_EPITHET,
            SYSTEM_CLASSIFICATION_NAME,
            EXPERTS_FULLNAME
    ));

    private static final Set<String> allowedFieldNamesForSearch_simpleSearchExceptions = Collections.emptySet();

    public BioportalTaxonDao(Client esClient, String ndaIndexName) {
        super(esClient, ndaIndexName);
    }

    /**
     * Retrieves taxa matching a variable number of criteria.
     *
     * @param params A {@link QueryParams} object containing:
     *               1. fields ... . A variable number of filters for fields. For example, the
     *               QueryParams object may contain a key “defaultClassification.genus” with a
     *               value of “Homo” and a key “defaultClassification.specificEpithet” with a
     *               value of “sapiens”. Fields must be mapped according to the mapping
     *               mechanism described above. Thus, if the QueryParams object contains a
     *               key “genus”, that key must be mapped to the “defaultClassification.genus”
     *               field.
     *               2. _andOr. An enumerated value with “AND” and “OR” as valid values. “AND”
     *               means all fields must match. “OR” means some fields must match. This is
     *               an optional parameter. By default only some fields must match. Will only
     *               be set if _source equals “Taxon_EXTENDED_NAME_SEARCH”. This value
     *               represents the DAO method whose query logic to re-execute.
     *               3. _sort. The field to sort on. Fields must be mapped according to the
     *               mapping mechanism described above. Special sort value: “_score” (sort by
     *               relevance). In practice sorting is only allowed on _score and on
     *               identifications.scientificName.fullScientificName. This is an optional
     *               parameter. By default sorting is done on _score.
     * @return search results
     */
    public ResultGroupSet<Taxon, String> taxonSearch(QueryParams params) {
        return search(params, allowedFieldNamesForSearch, allowedFieldNamesForSearch_simpleSearchExceptions, true);
    }

    /**
     * Retrieves Taxon documents by scientific name. Since the Taxon document type is populated from two source systems
     * (CoL and NSR), a search by scientific name may result in 0, 1 or at most 2 search results.
     * <p/>
     * A taxon retrieved through this method is always retrieved through a REST link in  the response from either
     * taxonSearch. This method is aware of the result set generated by those methods and is therefore capable of
     * generating REST links to the previous and next taxon in the result set. All parameters passed to taxonSearch
     * will also be passed to this method. Basically, this method has to re-execute the query executed by taxonSearch,
     * pick out the taxa with the specified accepted name, and generate REST links to the previous and next taxa
     * in the result set.
     *
     * @param params A {@link QueryParams} object
     * @return search result with previous and next link
     */
    public SearchResultSet<Taxon> getTaxonDetailWithinResultSet(QueryParams params) {
        ResultGroupSet<Taxon, String> searchResultSet = taxonSearch(params);

        return createTaxonDetailSearchResultSet(params, searchResultSet);
    }

    protected SearchResultSet<Taxon> createTaxonDetailSearchResultSet(QueryParams params,
                                                                      ResultGroupSet<Taxon, String> searchResultSet) {
        List<FieldMapping> fields = getSearchParamFieldMapping().getTaxonMappingForFields(params);
        List<FieldMapping> allowedFields = filterAllowedFieldMappings(fields, allowedFieldNamesForSearch);

        SearchResultSet<Taxon> detailResultSet = new SearchResultSet<>();

        SearchResult<Taxon> previousTaxon = null;
        SearchResult<Taxon> nextTaxon = null;
        SearchResult<Taxon> foundTaxonForAcceptedName = null;

        String genusOrMonomial = null;
        String specificEpithet = null;
        String infraspecificEpithet = null;

        for (FieldMapping allowedField : allowedFields) {
            if (allowedField.getFieldName().equals(ACCEPTEDNAME_GENUS_OR_MONOMIAL)){
                genusOrMonomial = allowedField.getValue();
            }
            if (allowedField.getFieldName().equals(ACCEPTEDNAME_SPECIFIC_EPITHET)){
                specificEpithet = allowedField.getValue();
            }
            if (allowedField.getFieldName().equals(ACCEPTEDNAME_INFRASPECIFIC_EPITHET)) {
                infraspecificEpithet = allowedField.getValue();
            }
        }

        List<ResultGroup<Taxon, String>> allBuckets = searchResultSet.getResultGroups();

        for (int currentBucket = 0; currentBucket < allBuckets.size(); currentBucket++) {
            ResultGroup<Taxon, String> bucket = allBuckets.get(currentBucket);

            List<SearchResult<Taxon>> resultsInBucket = bucket.getSearchResults();
            for (int indexInCurrentBucket = 0; indexInCurrentBucket < resultsInBucket.size(); indexInCurrentBucket++) {
                SearchResult<Taxon> searchResult = resultsInBucket.get(indexInCurrentBucket);
                ScientificName acceptedName = searchResult.getResult().getAcceptedName();
                if (acceptedName != null && acceptedName.isSameScientificName(createScientificName(genusOrMonomial, specificEpithet, infraspecificEpithet))) {

                    foundTaxonForAcceptedName = searchResult;
                    if (indexInCurrentBucket == 0) {
                        if (currentBucket != 0) {
                            List<SearchResult<Taxon>> previousBucket = allBuckets.get(currentBucket - 1)
                                    .getSearchResults();
                            previousTaxon = previousBucket.get(previousBucket.size() - 1);
                        }
                    } else {
                        previousTaxon = bucket.getSearchResults().get(indexInCurrentBucket - 1);
                    }

                    if (indexInCurrentBucket == resultsInBucket.size() - 1) {
                        if (currentBucket != allBuckets.size() - 1) {
                            List<SearchResult<Taxon>> nextBucket = allBuckets.get(currentBucket + 1)
                                    .getSearchResults();
                            nextTaxon = nextBucket.get(0);
                        }
                    } else {
                        nextTaxon = bucket.getSearchResults().get(indexInCurrentBucket + 1);
                    }
                    break;
                }
            }

        }
        if (previousTaxon != null) {
            //TODO NDA-66 taxon link must be to detail base url in result set
            foundTaxonForAcceptedName.addLink(new Link("_previous", TAXON_DETAIL_BASE_URL + createAcceptedNameParams(previousTaxon.getResult().getAcceptedName())));
        }
        if (nextTaxon != null) {
            //TODO NDA-66 taxon link must be to detail base url in result set
            foundTaxonForAcceptedName.addLink(new Link("_next", TAXON_DETAIL_BASE_URL + createAcceptedNameParams(nextTaxon.getResult().getAcceptedName())));
        }

        detailResultSet.addSearchResult(foundTaxonForAcceptedName);
        detailResultSet.setQueryParameters(params.copyWithoutGeoShape());
        detailResultSet.setTotalSize(searchResultSet.getTotalSize());

        return detailResultSet;
    }

    private ScientificName createScientificName(String genusOrMonomial, String specificEpithet,
                                                String infraspecificEpithet) {
        ScientificName scientificName = new ScientificName();
        scientificName.setGenusOrMonomial(genusOrMonomial);
        scientificName.setSpecificEpithet(specificEpithet);
        scientificName.setInfraspecificEpithet(infraspecificEpithet);
        return scientificName;
    }
}
