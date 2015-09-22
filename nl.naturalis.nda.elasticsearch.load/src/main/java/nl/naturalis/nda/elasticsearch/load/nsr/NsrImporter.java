package nl.naturalis.nda.elasticsearch.load.nsr;

import static nl.naturalis.nda.domain.SourceSystem.NSR;
import static nl.naturalis.nda.elasticsearch.load.NBAImportAll.LUCENE_TYPE_MULTIMEDIA_OBJECT;
import static nl.naturalis.nda.elasticsearch.load.NBAImportAll.LUCENE_TYPE_TAXON;
import static nl.naturalis.nda.elasticsearch.load.nsr.NsrImportUtil.backupXmlFile;
import static nl.naturalis.nda.elasticsearch.load.nsr.NsrImportUtil.getXmlFiles;

import java.io.File;
import java.util.List;

import nl.naturalis.nda.elasticsearch.dao.estypes.ESMultiMediaObject;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESTaxon;
import nl.naturalis.nda.elasticsearch.load.ETLStatistics;
import nl.naturalis.nda.elasticsearch.load.LoadConstants;
import nl.naturalis.nda.elasticsearch.load.LoadUtil;
import nl.naturalis.nda.elasticsearch.load.Registry;
import nl.naturalis.nda.elasticsearch.load.XMLRecordInfo;

import org.domainobject.util.ConfigObject;
import org.domainobject.util.IOUtil;
import org.slf4j.Logger;

public class NsrImporter {

	public static void main(String[] args)
	{
		if (args.length == 0)
			new NsrImporter().importAll();
		else if (args[0].equalsIgnoreCase("taxa"))
			new NsrImporter().importTaxa();
		else if (args[0].equalsIgnoreCase("multimedia"))
			new NsrImporter().importMultiMedia();
		else
			logger.error("Invalid argument: " + args[0]);
	}

	private static final Logger logger;

	static {
		logger = Registry.getInstance().getLogger(NsrImporter.class);
	}

	private final boolean suppressErrors;
	private final int esBulkRequestSize;

	public NsrImporter()
	{
		suppressErrors = ConfigObject.isEnabled("nsr.suppress-errors");
		String key = LoadConstants.SYSPROP_ES_BULK_REQUEST_SIZE;
		String val = System.getProperty(key, "1000");
		esBulkRequestSize = Integer.parseInt(val);
	}

	public void importAll()
	{
		long start = System.currentTimeMillis();
		File[] xmlFiles = getXmlFiles();
		if (xmlFiles.length == 0) {
			logger.info("No XML files to process");
			return;
		}
		LoadUtil.truncate(LUCENE_TYPE_TAXON, NSR);
		LoadUtil.truncate(LUCENE_TYPE_MULTIMEDIA_OBJECT, NSR);
		ETLStatistics taxonStats = new ETLStatistics();
		ETLStatistics mediaStats = new ETLStatistics();
		mediaStats.setUseObjectsAccepted(true);
		NsrTaxonTransformer tTransformer = new NsrTaxonTransformer(taxonStats);
		tTransformer.setSuppressErrors(suppressErrors);
		NsrMultiMediaTransformer mTransformer = new NsrMultiMediaTransformer(mediaStats);
		mTransformer.setSuppressErrors(suppressErrors);
		NsrTaxonLoader taxonLoader = null;
		NsrMultiMediaLoader mediaLoader = null;
		try {
			taxonLoader = new NsrTaxonLoader(esBulkRequestSize, taxonStats);
			mediaLoader = new NsrMultiMediaLoader(esBulkRequestSize, mediaStats);
			for (File f : xmlFiles) {
				logger.info("Processing file " + f.getAbsolutePath());
				int i = 0;
				for (XMLRecordInfo extracted : new NsrExtractor(f, taxonStats)) {
					List<ESTaxon> taxa = tTransformer.transform(extracted);
					taxonLoader.load(taxa);
					mTransformer.setTaxon(taxa == null ? null : taxa.get(0));
					List<ESMultiMediaObject> multimedia = mTransformer.transform(extracted);
					mediaLoader.load(multimedia);
					if (++i % 5000 == 0)
						logger.info("Records processed: " + i);
				}
				backupXmlFile(f);
			}
		}
		finally {
			IOUtil.close(taxonLoader, mediaLoader);
		}
		taxonStats.logStatistics(logger, "taxa");
		mediaStats.badInput = taxonStats.badInput;
		mediaStats.logStatistics(logger, "multimedia");
		LoadUtil.logDuration(logger, getClass(), start);
	}

	public void importTaxa()
	{
		long start = System.currentTimeMillis();
		File[] xmlFiles = getXmlFiles();
		if (xmlFiles.length == 0) {
			logger.info("No XML files to process");
			return;
		}
		LoadUtil.truncate(LUCENE_TYPE_TAXON, NSR);
		ETLStatistics stats = new ETLStatistics();
		NsrTaxonTransformer transformer = new NsrTaxonTransformer(stats);
		transformer.setSuppressErrors(suppressErrors);
		NsrTaxonLoader loader = null;
		try {
			loader = new NsrTaxonLoader(esBulkRequestSize, stats);
			for (File f : xmlFiles) {
				logger.info("Processing file " + f.getAbsolutePath());
				int i = 0;
				for (XMLRecordInfo extracted : new NsrExtractor(f, stats)) {
					List<ESTaxon> transformed = transformer.transform(extracted);
					loader.load(transformed);
					if (++i % 5000 == 0)
						logger.info("Records processed: " + i);
				}
			}
		}
		finally {
			IOUtil.close(loader);
		}
		stats.logStatistics(logger, "taxa");
		LoadUtil.logDuration(logger, getClass(), start);
	}

	public void importMultiMedia()
	{
		long start = System.currentTimeMillis();
		File[] xmlFiles = getXmlFiles();
		if (xmlFiles.length == 0) {
			logger.info("No XML files to process");
			return;
		}
		LoadUtil.truncate(LUCENE_TYPE_MULTIMEDIA_OBJECT, NSR);
		ETLStatistics stats = new ETLStatistics();
		stats.setUseObjectsAccepted(true);
		NsrTaxonTransformer tTransformer = new NsrTaxonTransformer(new ETLStatistics());
		tTransformer.setSuppressErrors(suppressErrors);
		NsrMultiMediaTransformer mTransformer = new NsrMultiMediaTransformer(stats);
		mTransformer.setSuppressErrors(suppressErrors);
		NsrMultiMediaLoader loader = null;
		try {
			loader = new NsrMultiMediaLoader(esBulkRequestSize, stats);
			for (File f : xmlFiles) {
				logger.info("Processing file " + f.getAbsolutePath());
				int i = 0;
				for (XMLRecordInfo extracted : new NsrExtractor(f, stats)) {
					List<ESTaxon> taxa = tTransformer.transform(extracted);
					mTransformer.setTaxon(taxa == null ? null : taxa.get(0));
					List<ESMultiMediaObject> multimedia = mTransformer.transform(extracted);
					loader.load(multimedia);
					if (++i % 5000 == 0)
						logger.info("Records processed: " + i);
				}
				backupXmlFile(f);
			}
		}
		finally {
			IOUtil.close(loader);
		}
		stats.logStatistics(logger, "multimedia");
		LoadUtil.logDuration(logger, getClass(), start);
	}

}