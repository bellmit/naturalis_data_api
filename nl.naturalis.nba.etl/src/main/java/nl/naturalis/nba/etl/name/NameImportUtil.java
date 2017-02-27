package nl.naturalis.nba.etl.name;

import static nl.naturalis.nba.dao.DocumentType.NAME_GROUP;
import static nl.naturalis.nba.dao.DocumentType.TAXON;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.naturalis.nba.api.model.GatheringEvent;
import nl.naturalis.nba.api.model.GatheringSiteCoordinates;
import nl.naturalis.nba.api.model.NameGroup;
import nl.naturalis.nba.api.model.Person;
import nl.naturalis.nba.api.model.ScientificName;
import nl.naturalis.nba.api.model.SourceSystem;
import nl.naturalis.nba.api.model.Specimen;
import nl.naturalis.nba.api.model.SpecimenIdentification;
import nl.naturalis.nba.api.model.SummaryGatheringEvent;
import nl.naturalis.nba.api.model.SummaryGatheringSiteCoordinates;
import nl.naturalis.nba.api.model.SummaryPerson;
import nl.naturalis.nba.api.model.SummaryScientificName;
import nl.naturalis.nba.api.model.SummarySourceSystem;
import nl.naturalis.nba.api.model.SummarySpecimen;
import nl.naturalis.nba.api.model.SummarySpecimenIdentification;
import nl.naturalis.nba.api.model.Taxon;
import nl.naturalis.nba.dao.DocumentType;
import nl.naturalis.nba.dao.util.es.ESUtil;

class NameImportUtil {

	static long longHashCode(String s)
	{
		long h = 0;
		for (int i = 0; i < s.length(); i++) {
			h = 31 * h + s.charAt(i);
		}
		return h;
	}

	static SummarySpecimen copySpecimen(Specimen specimen)
	{
		SummarySpecimen summary = new SummarySpecimen();
		summary.setId(specimen.getId());
		summary.setIdentifications(copyIdentifications(specimen.getIdentifications()));
		summary.setCollectorsFieldNumber(specimen.getCollectorsFieldNumber());
		summary.setPhaseOrStage(specimen.getPhaseOrStage());
		summary.setSex(specimen.getSex());
		summary.setSourceSystem(copySourceSystem(specimen.getSourceSystem()));
		summary.setTypeStatus(specimen.getTypeStatus());
		summary.setGatheringEvent(copyGatheringEvent(specimen.getGatheringEvent()));
		summary.setUnitID(specimen.getUnitID());
		return summary;
	}

	private static SummaryGatheringEvent copyGatheringEvent(GatheringEvent ge)
	{
		if (ge == null) {
			return null;
		}
		SummaryGatheringEvent summary = new SummaryGatheringEvent();
		summary.setDateTimeBegin(ge.getDateTimeBegin());
		summary.setDateTimeEnd(ge.getDateTimeEnd());
		summary.setGatheringOrganizations(ge.getGatheringOrganizations());
		summary.setGatheringPersons(copyGatheringPersons(ge.getGatheringPersons()));
		summary.setLocalityText(ge.getLocalityText());
		summary.setSiteCoordinates(copySiteCoordinates(ge.getSiteCoordinates()));
		return summary;
	}

	private static List<SummaryGatheringSiteCoordinates> copySiteCoordinates(
			List<GatheringSiteCoordinates> coords)
	{
		if (coords == null) {
			return null;
		}
		List<SummaryGatheringSiteCoordinates> summaries = new ArrayList<>(coords.size());
		SummaryGatheringSiteCoordinates summary;
		for (GatheringSiteCoordinates coord : coords) {
			Double lat = coord.getLatitudeDecimal();
			Double lon = coord.getLongitudeDecimal();
			summary = new SummaryGatheringSiteCoordinates(lat, lon);
			summaries.add(summary);
		}
		return summaries;
	}

	private static List<SummarySpecimenIdentification> copyIdentifications(
			List<SpecimenIdentification> identifications)
	{
		if (identifications == null) {
			return null;
		}
		List<SummarySpecimenIdentification> summaries = new ArrayList<>(identifications.size());
		for (SpecimenIdentification si : identifications) {
			summaries.add(copyIdentification(si));
		}
		return summaries;
	}

	private static SummarySpecimenIdentification copyIdentification(SpecimenIdentification si)
	{
		SummarySpecimenIdentification ssi = new SummarySpecimenIdentification();
		ssi.setDefaultClassification(si.getDefaultClassification());
		ssi.setScientificName(copyScientificName(si.getScientificName()));
		return ssi;
	}

	private static SummaryScientificName copyScientificName(ScientificName sn)
	{
		SummaryScientificName ssn = new SummaryScientificName();
		ssn.setAuthorshipVerbatim(sn.getAuthorshipVerbatim());
		ssn.setFullScientificName(sn.getFullScientificName());
		ssn.setGenusOrMonomial(sn.getGenusOrMonomial());
		ssn.setInfraspecificEpithet(sn.getInfraspecificEpithet());
		ssn.setSpecificEpithet(sn.getSpecificEpithet());
		ssn.setSubgenus(sn.getSubgenus());
		ssn.setTaxonomicStatus(sn.getTaxonomicStatus());
		return ssn;
	}

	private static List<SummaryPerson> copyGatheringPersons(List<Person> persons)
	{
		if (persons == null) {
			return null;
		}
		List<SummaryPerson> summaries = new ArrayList<>(persons.size());
		for (Person p : persons) {
			SummaryPerson sp = new SummaryPerson();
			sp.setFullName(p.getFullName());
			sp.setOrganization(p.getOrganization());
			summaries.add(sp);
		}
		return summaries;
	}

	private static SummarySourceSystem copySourceSystem(SourceSystem ss)
	{
		return new SummarySourceSystem(ss.getCode());
	}

	static String createName(SpecimenIdentification si)
	{
		String genus = si.getScientificName().getGenusOrMonomial();
		if (genus == null) {
			genus = si.getDefaultClassification().getGenus();
			if (genus == null) {
				genus = "?";
			}
		}
		String species = si.getScientificName().getSpecificEpithet();
		if (species == null) {
			species = si.getDefaultClassification().getSpecificEpithet();
			if (species == null) {
				species = "?";
			}
		}
		String subspecies = si.getScientificName().getInfraspecificEpithet();
		if (subspecies == null) {
			subspecies = si.getDefaultClassification().getInfraspecificEpithet();
		}
		if (subspecies == null) {
			return genus + " " + species;
		}
		return genus + " " + species + " " + subspecies;
	}

	static List<NameGroup> loadNameGroups(Collection<String> names)
	{
		DocumentType<NameGroup> dt = NAME_GROUP;
		SearchRequestBuilder request = ESUtil.newSearchRequest(dt);
		IdsQueryBuilder query = QueryBuilders.idsQuery(dt.getName());
		query.addIds(names.toArray(new String[names.size()]));
		request.setQuery(query);
		SearchResponse response = ESUtil.executeSearchRequest(request);
		SearchHit[] hits = response.getHits().getHits();
		if (hits.length == 0) {
			return Collections.emptyList();
		}
		List<NameGroup> result = new ArrayList<>(hits.length);
		ObjectMapper om = dt.getObjectMapper();
		for (SearchHit hit : hits) {
			NameGroup sns = om.convertValue(hit.getSource(), dt.getJavaType());
			result.add(sns);
		}
		return result;
	}

	static List<Taxon> loadTaxa(Collection<String> names)
	{
		DocumentType<Taxon> dt = TAXON;
		SearchRequestBuilder request = ESUtil.newSearchRequest(dt);
		TermsQueryBuilder query = termsQuery("acceptedName.fullScientificName", names);
		request.setQuery(query);
		SearchResponse response = ESUtil.executeSearchRequest(request);
		SearchHit[] hits = response.getHits().getHits();
		if (hits.length == 0)
			return Collections.emptyList();
		List<Taxon> result = new ArrayList<>(hits.length);
		ObjectMapper om = dt.getObjectMapper();
		for (SearchHit hit : hits) {
			Taxon taxon = om.convertValue(hit.getSource(), dt.getJavaType());
			taxon.setId(hit.getId());
			result.add(taxon);
		}
		return result;
	}

	private NameImportUtil()
	{
	}

}
