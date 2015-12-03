package nl.naturalis.nda.elasticsearch.load.brahms;

import static nl.naturalis.nda.domain.SourceSystem.BRAHMS;
import static nl.naturalis.nda.elasticsearch.load.CSVImportUtil.getFloat;
import static nl.naturalis.nda.elasticsearch.load.CSVImportUtil.val;
import static nl.naturalis.nda.elasticsearch.load.DocumentType.SPECIMEN;
import static nl.naturalis.nda.elasticsearch.load.LoadConstants.BRAHMS_ABCD_COLLECTION_TYPE;
import static nl.naturalis.nda.elasticsearch.load.LoadConstants.BRAHMS_ABCD_SOURCE_ID;
import static nl.naturalis.nda.elasticsearch.load.LoadConstants.ES_ID_PREFIX_BRAHMS;
import static nl.naturalis.nda.elasticsearch.load.LoadConstants.LICENCE;
import static nl.naturalis.nda.elasticsearch.load.LoadConstants.LICENCE_TYPE;
import static nl.naturalis.nda.elasticsearch.load.LoadConstants.SOURCE_INSTITUTION_ID;
import static nl.naturalis.nda.elasticsearch.load.LoadUtil.getSpecimenPurl;
import static nl.naturalis.nda.elasticsearch.load.brahms.BrahmsCsvField.BARCODE;
import static nl.naturalis.nda.elasticsearch.load.brahms.BrahmsCsvField.CATEGORY;
import static nl.naturalis.nda.elasticsearch.load.brahms.BrahmsCsvField.NOTONLINE;
import static nl.naturalis.nda.elasticsearch.load.brahms.BrahmsCsvField.PLANTDESC;
import static nl.naturalis.nda.elasticsearch.load.brahms.BrahmsCsvField.TYPE;
import static nl.naturalis.nda.elasticsearch.load.brahms.BrahmsImportUtil.getGatheringEvent;
import static nl.naturalis.nda.elasticsearch.load.brahms.BrahmsImportUtil.getSpecimenIdentification;

import java.util.Arrays;
import java.util.List;

import nl.naturalis.nda.domain.SourceSystem;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESSpecimen;
import nl.naturalis.nda.elasticsearch.load.AbstractCSVTransformer;
import nl.naturalis.nda.elasticsearch.load.ETLStatistics;
import nl.naturalis.nda.elasticsearch.load.ThemeCache;
import nl.naturalis.nda.elasticsearch.load.normalize.SpecimenTypeStatusNormalizer;

import org.apache.commons.csv.CSVRecord;
import org.domainobject.util.ConfigObject;

/**
 * The transformer component in the Brahms ETL cycle for specimens.
 * 
 * @author Ayco Holleman
 *
 */
class BrahmsSpecimenTransformer extends AbstractCSVTransformer<BrahmsCsvField, ESSpecimen> {

	private static final SpecimenTypeStatusNormalizer typeStatusNormalizer;
	private static final ThemeCache themeCache;

	static {
		typeStatusNormalizer = SpecimenTypeStatusNormalizer.getInstance();
		themeCache = ThemeCache.getInstance();
	}

	public BrahmsSpecimenTransformer(ETLStatistics stats)
	{
		super(stats);
		suppressErrors = ConfigObject.isEnabled("brahms.suppress-errors");
	}

	@Override
	protected String getObjectID()
	{
		return val(input.getRecord(), BARCODE);
	}

	@Override
	protected List<ESSpecimen> doTransform()
	{
		// No record-level validations, so:
		stats.recordsAccepted++;
		stats.objectsProcessed++;
		try {
			CSVRecord record = input.getRecord();
			ESSpecimen specimen = new ESSpecimen();
			specimen.setSourceSystemId(objectID);
			specimen.setUnitID(objectID);
			specimen.setUnitGUID(getSpecimenPurl(objectID));
			setConstants(specimen);
			List<String> themes = themeCache.lookup(objectID, SPECIMEN, BRAHMS);
			specimen.setTheme(themes);
			String s = val(record, CATEGORY);
			if (s == null)
				specimen.setRecordBasis("Preserved Specimen");
			else
				specimen.setRecordBasis(s);
			specimen.setAssemblageID(getAssemblageID(record));
			specimen.setNotes(val(record, PLANTDESC));
			specimen.setTypeStatus(getTypeStatus(record));
			s = val(record, NOTONLINE);
			if (s == null || s.equals("0"))
				specimen.setObjectPublic(true);
			else
				specimen.setObjectPublic(false);
			specimen.setGatheringEvent(getGatheringEvent(record));
			specimen.addIndentification(getSpecimenIdentification(record));
			stats.objectsAccepted++;
			return Arrays.asList(specimen);
		}
		catch (Throwable t) {
			stats.objectsRejected++;
			if (!suppressErrors) {
				error(t.getMessage());
				error(input.getLine());
			}
			return null;
		}
	}

	private static void setConstants(ESSpecimen specimen)
	{
		specimen.setSourceSystem(SourceSystem.BRAHMS);
		specimen.setSourceInstitutionID(SOURCE_INSTITUTION_ID);
		specimen.setOwner(SOURCE_INSTITUTION_ID);
		specimen.setSourceID(BRAHMS_ABCD_SOURCE_ID);
		specimen.setLicenceType(LICENCE_TYPE);
		specimen.setLicence(LICENCE);
		specimen.setCollectionType(BRAHMS_ABCD_COLLECTION_TYPE);
	}

	private static String getAssemblageID(CSVRecord record)
	{
		Float f = getFloat(record, BrahmsCsvField.BRAHMS);
		if (f == null)
			return null;
		return ES_ID_PREFIX_BRAHMS + f.intValue();
	}

	private static String getTypeStatus(CSVRecord record)
	{
		return typeStatusNormalizer.normalize(val(record, TYPE));
	}

}
