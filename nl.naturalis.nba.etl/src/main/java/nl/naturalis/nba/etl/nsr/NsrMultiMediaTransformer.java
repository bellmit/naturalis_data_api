package nl.naturalis.nba.etl.nsr;

import static nl.naturalis.nba.api.model.ServiceAccessPoint.Variant.MEDIUM_QUALITY;
import static nl.naturalis.nba.api.model.SourceSystem.NSR;
import static nl.naturalis.nba.dao.util.es.ESUtil.getElasticsearchId;
import static nl.naturalis.nba.etl.ETLUtil.createScientificNameGroup;
import static nl.naturalis.nba.etl.LoadConstants.LICENCE;
import static nl.naturalis.nba.etl.LoadConstants.LICENCE_TYPE;
import static nl.naturalis.nba.etl.LoadConstants.SOURCE_INSTITUTION_ID;
import static nl.naturalis.nba.etl.TransformUtil.equalizeNameComponents;
import static nl.naturalis.nba.etl.TransformUtil.guessMimeType;
import static nl.naturalis.nba.etl.TransformUtil.parseDate;
import static nl.naturalis.nba.etl.nsr.NsrImportUtil.val;
import static nl.naturalis.nba.utils.DOMUtil.getDescendants;
import static nl.naturalis.nba.utils.DOMUtil.getValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import nl.naturalis.nba.api.model.MultiMediaContentIdentification;
import nl.naturalis.nba.api.model.MultiMediaGatheringEvent;
import nl.naturalis.nba.api.model.MultiMediaObject;
import nl.naturalis.nba.api.model.ServiceAccessPoint;
import nl.naturalis.nba.api.model.Taxon;
import nl.naturalis.nba.etl.AbstractXMLTransformer;
import nl.naturalis.nba.etl.ETLStatistics;
import nl.naturalis.nba.etl.NameMismatchException;

/**
 * Transforms and validates NSR source data.
 * 
 * @author Ayco Holleman
 *
 */
class NsrMultiMediaTransformer extends AbstractXMLTransformer<MultiMediaObject> {

	private Taxon taxon;

	public NsrMultiMediaTransformer(ETLStatistics stats)
	{
		super(stats);
	}

	/**
	 * Set the taxon object associated with this multimedia object. The taxon
	 * object is extracted from the same XML record by the
	 * {@link NsrTaxonTransformer}.
	 * 
	 * @param taxon
	 */
	public void setTaxon(Taxon taxon)
	{
		this.taxon = taxon;
	}

	@Override
	protected String getObjectID()
	{
		return val(input.getRecord(), "nsr_id");
	}

	/**
	 * Transforms an XML record into one ore more {@code MultiMediaObject}s. The
	 * multimedia transformer does not keep track of record-level statistics.
	 * The assumption is that if the taxon transformer was able to extract a
	 * taxon from the XML record, then the record was OK at the record level.
	 */
	@Override
	protected List<MultiMediaObject> doTransform()
	{
		if (taxon == null) {
			stats.recordsSkipped++;
			if (logger.isDebugEnabled())
				debug("Ignoring images for skipped or invalid taxon");
			return null;
		}
		List<Element> imageElems = getDescendants(input.getRecord(), "image");
		if (imageElems == null) {
			if (logger.isDebugEnabled())
				debug("Skipping taxon without images");
			stats.recordsSkipped++;
			return null;
		}
		stats.recordsAccepted++;
		List<MultiMediaObject> mmos = new ArrayList<>(imageElems.size());
		for (Element imageElement : imageElems) {
			MultiMediaObject mmo = transformOne(imageElement);
			if (mmo != null)
				mmos.add(mmo);
		}
		return mmos.size() == 0 ? null : mmos;
	}

	private MultiMediaObject transformOne(Element e)
	{
		stats.objectsProcessed++;
		try {
			URI uri = getUri(e);
			if (uri == null)
				return null;
			MultiMediaObject mmo = newMediaObject();
			String uriHash = String.valueOf(uri.hashCode()).replace('-', '0');
			mmo.setSourceSystemId(objectID + '_' + uriHash);
			mmo.setUnitID(mmo.getSourceSystemId());
			String format = getValue(e, "mime_type");
			if (format == null || format.length() == 0) {
				if (!suppressErrors) {
					String fmt = "Missing mime type for image \"%s\" (taxon \"%s\").";
					warn(fmt, uri, taxon.getAcceptedName().getFullScientificName());
				}
				format = guessMimeType(uri.toString());
			}
			mmo.addServiceAccessPoint(new ServiceAccessPoint(uri, format, MEDIUM_QUALITY));
			mmo.setCreator(val(e, "photographer_name"));
			mmo.setCopyrightText(val(e, "copyright"));
			if (mmo.getCopyrightText() == null) {
				mmo.setLicenseType(LICENCE_TYPE);
				mmo.setLicense(LICENCE);
			}
			mmo.setDescription(val(e, "short_description"));
			mmo.setCaption(mmo.getDescription());
			String date = val(e, "date_taken");
			if (date != null && date.equalsIgnoreCase("in prep")) {
				date = null;
				if (logger.isDebugEnabled()) {
					logger.debug("Invalid date: \"{}\"", date);
				}
			}
			String locality = val(e, "geography");
			if (locality != null || date != null) {
				MultiMediaGatheringEvent ge = new MultiMediaGatheringEvent();
				mmo.setGatheringEvents(Arrays.asList(ge));
				ge.setLocalityText(locality);
				ge.setDateTimeBegin(parseDate(date));
				ge.setDateTimeEnd(ge.getDateTimeBegin());
			}
			stats.objectsAccepted++;
			return mmo;
		}
		catch (Throwable t) {
			handleError(t);
			return null;
		}
	}

	private MultiMediaObject newMediaObject() throws NameMismatchException
	{
		MultiMediaObject mmo = new MultiMediaObject();
		mmo.setSourceSystem(NSR);
		mmo.setSourceInstitutionID(SOURCE_INSTITUTION_ID);
		mmo.setOwner(SOURCE_INSTITUTION_ID);
		mmo.setSourceID("LNG NSR");
		mmo.setCollectionType("Nederlandse soorten en exoten");
		String taxonId = getElasticsearchId(NSR, taxon.getSourceSystemId());
		mmo.setAssociatedTaxonReference(taxonId);
		mmo.setIdentifications(Arrays.asList(getIdentification()));
		equalizeNameComponents(mmo);
		return mmo;
	}

	private MultiMediaContentIdentification getIdentification()
	{
		Taxon t = taxon;
		MultiMediaContentIdentification mmci = new MultiMediaContentIdentification();
		mmci.setTaxonRank(t.getTaxonRank());
		mmci.setScientificName(t.getAcceptedName());
		mmci.setDefaultClassification(t.getDefaultClassification());
		mmci.setSystemClassification(t.getSystemClassification());
		mmci.setVernacularNames(t.getVernacularNames());
		String nameGroup = createScientificNameGroup(mmci);
		mmci.setScientificNameGroup(nameGroup);
		return mmci;
	}

	private URI getUri(Element elem)
	{
		String url = val(elem, "url");
		if (url == null) {
			stats.objectsRejected++;
			if (!suppressErrors) {
				String sn = taxon.getAcceptedName().getFullScientificName();
				error("Empty <url> element for \"%s\"", sn);
			}
			return null;
		}
		try {
			return new URI(url.trim());
		}
		catch (URISyntaxException e) {
			stats.objectsRejected++;
			if (!suppressErrors)
				error("Invalid image URL: \"%s\"", url);
			return null;
		}
	}

}
