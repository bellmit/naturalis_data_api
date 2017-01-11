package nl.naturalis.nba.etl.brahms;

import static nl.naturalis.nba.dao.DocumentType.MULTI_MEDIA_OBJECT;
import static nl.naturalis.nba.dao.DocumentType.SPECIMEN;
import static nl.naturalis.nba.dao.util.es.ESUtil.getDistinctIndices;
import static nl.naturalis.nba.etl.ETLUtil.getDuration;
import static nl.naturalis.nba.etl.ETLUtil.logDuration;
import static nl.naturalis.nba.etl.LoadConstants.SYSPROP_LOADER_QUEUE_SIZE;
import static nl.naturalis.nba.etl.LoadConstants.SYSPROP_SUPPRESS_ERRORS;
import static nl.naturalis.nba.etl.brahms.BrahmsImportUtil.backup;
import static nl.naturalis.nba.etl.brahms.BrahmsImportUtil.getCsvFiles;
import static nl.naturalis.nba.etl.brahms.BrahmsImportUtil.removeBackupExtension;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Logger;

import nl.naturalis.nba.api.model.SourceSystem;
import nl.naturalis.nba.dao.DocumentType;
import nl.naturalis.nba.dao.ESClientManager;
import nl.naturalis.nba.dao.IndexInfo;
import nl.naturalis.nba.dao.util.es.ESUtil;
import nl.naturalis.nba.etl.CSVExtractor;
import nl.naturalis.nba.etl.CSVRecordInfo;
import nl.naturalis.nba.etl.ETLRegistry;
import nl.naturalis.nba.etl.ETLStatistics;
import nl.naturalis.nba.etl.ETLUtil;
import nl.naturalis.nba.etl.ThemeCache;
import nl.naturalis.nba.etl.normalize.SpecimenTypeStatusNormalizer;
import nl.naturalis.nba.utils.ConfigObject;
import nl.naturalis.nba.utils.IOUtil;

/**
 * Manages the import of Brahms specimens and multimedia. Since specimens and
 * multimedia are extracted from the same CSV record, this class allows you to
 * import either per file or per type (first all specimens, then all
 * multimedia). With the first option each CSV file is processed only once. With
 * the second option each CSV file is processed twice: once for the specimen
 * import and once for the multimedia import. Thus the first option should be
 * faster and is the default. To force a per-type import add
 * {@code -Dbrahms.parallel=false} to the java command line.
 * 
 * @author Ayco Holleman
 *
 */
public class BrahmsImportAll {

	public static void main(String[] args)
	{
		if (args.length == 0) {
			try {
				new BrahmsImportAll().importAll();
			}
			finally {
				for (IndexInfo ii : getDistinctIndices(SPECIMEN, MULTI_MEDIA_OBJECT)) {
					ESUtil.refreshIndex(ii);
				}
				ESClientManager.getInstance().closeClient();
			}
		}
		else if (args[0].equalsIgnoreCase("backup")) {
			new BrahmsImportAll().backupSourceFiles();
		}
		else if (args[0].equalsIgnoreCase("reset")) {
			new BrahmsImportAll().reset();
		}
		else {
			logger.error("Invalid argument: " + args[0]);
		}
	}

	private static final Logger logger = ETLRegistry.getInstance().getLogger(BrahmsImportAll.class);

	private final boolean backup;
	private final boolean parallel;

	private final int loaderQueueSize;
	private final boolean suppressErrors;

	public BrahmsImportAll()
	{
		backup = ConfigObject.isEnabled("brahms.backup", true);
		parallel = ConfigObject.isEnabled("brahms.parallel", true);
		suppressErrors = ConfigObject.isEnabled(SYSPROP_SUPPRESS_ERRORS);
		String val = System.getProperty(SYSPROP_LOADER_QUEUE_SIZE, "1000");
		loaderQueueSize = Integer.parseInt(val);
	}

	/**
	 * Import specimens and multimedia either in parallel fashion or in serial
	 * fashion, depending on the {@code brahms.parallel} system property.
	 */
	public void importAll()
	{
		if (parallel) {
			importPerFile();
		}
		else {
			importPerType();
		}
	}

	/**
	 * This method first imports all specimens, then all multimedia. Thus each
	 * CSV file is read twice.
	 * 
	 */
	public void importPerType()
	{
		BrahmsSpecimenImporter specimenImporter = new BrahmsSpecimenImporter();
		specimenImporter.importCsvFiles();
		BrahmsMultiMediaImporter multiMediaImporter = new BrahmsMultiMediaImporter();
		multiMediaImporter.importCsvFiles();
		if (backup) {
			backup();
		}
	}

	/**
	 * This method processes each CSV files only once, extracting and loading
	 * both specimens and multimedia at the same time.
	 * 
	 */
	public void importPerFile()
	{
		long start = System.currentTimeMillis();
		File[] csvFiles = getCsvFiles();
		if (csvFiles.length == 0) {
			logger.info("No CSV files to process");
			return;
		}
		SpecimenTypeStatusNormalizer.getInstance().resetStatistics();
		ThemeCache.getInstance().resetMatchCounters();
		/* Global statistics for specimen import (across all files) */
		ETLStatistics sStats = new ETLStatistics();
		/* Global statistics for multimedia import (across all files) */
		ETLStatistics mStats = new ETLStatistics();
		mStats.setOneToMany(true);
		try {
			ETLUtil.truncate(DocumentType.SPECIMEN, SourceSystem.BRAHMS);
			ETLUtil.truncate(DocumentType.MULTI_MEDIA_OBJECT, SourceSystem.BRAHMS);
			for (File f : csvFiles) {
				processFile(f, sStats, mStats);
			}
			if (backup) {
				backup();
			}
		}
		catch (Throwable t) {
			logger.error(getClass().getSimpleName() + " terminated unexpectedly!", t);
		}
		SpecimenTypeStatusNormalizer.getInstance().logStatistics();
		ThemeCache.getInstance().logMatchInfo();
		sStats.logStatistics(logger, "Specimens");
		mStats.logStatistics(logger, "Multimedia");
		logDuration(logger, getClass(), start);
	}

	/**
	 * Backs up the CSV files in the Brahms data directory by appending a
	 * "&#46;imported" extension to the file name.
	 */
	@SuppressWarnings("static-method")
	public void backupSourceFiles()
	{
		backup();
	}

	/**
	 * Removes the "&#46;imported" file name extension from the files in the
	 * Brahms data directory. Nice for repitive testing. Not meant for
	 * production purposes.
	 */
	@SuppressWarnings("static-method")
	public void reset()
	{
		removeBackupExtension();
	}

	private void processFile(File f, ETLStatistics sStats, ETLStatistics mStats)
	{
		long start = System.currentTimeMillis();
		logger.info("Processing file {}", f.getAbsolutePath());
		/* Statistics for specimen import (current file) */
		ETLStatistics specimenStats = new ETLStatistics();
		/* Statistics for multimedia import (current file) */
		ETLStatistics multimediaStats = new ETLStatistics();
		multimediaStats.setOneToMany(true);
		ETLStatistics extractionStats = new ETLStatistics();
		CSVExtractor<BrahmsCsvField> extractor = null;
		BrahmsSpecimenTransformer specimenTransformer = null;
		BrahmsMultiMediaTransformer multimediaTransformer = null;
		BrahmsSpecimenLoader specimenLoader = null;
		BrahmsMultiMediaLoader multimediaLoader = null;
		try {
			extractor = createExtractor(f, extractionStats);
			specimenTransformer = new BrahmsSpecimenTransformer(specimenStats);
			specimenLoader = new BrahmsSpecimenLoader(loaderQueueSize, specimenStats);
			specimenLoader.suppressErrors(suppressErrors);
			multimediaTransformer = new BrahmsMultiMediaTransformer(multimediaStats);
			multimediaLoader = new BrahmsMultiMediaLoader(loaderQueueSize, multimediaStats);
			multimediaLoader.suppressErrors(suppressErrors);
			for (CSVRecordInfo<BrahmsCsvField> rec : extractor) {
				if (rec == null)
					continue;
				specimenLoader.queue(specimenTransformer.transform(rec));
				multimediaLoader.queue(multimediaTransformer.transform(rec));
				if (specimenStats.recordsProcessed != 0
						&& specimenStats.recordsProcessed % 50000 == 0) {
					logger.info("Records processed: {}", specimenStats.recordsProcessed);
					logger.info("Specimen documents indexed: {}", specimenStats.documentsIndexed);
					logger.info("Multimedia documents indexed: {}",
							multimediaStats.documentsIndexed);
				}
			}
		}
		finally {
			IOUtil.close(specimenLoader, multimediaLoader);
		}
		specimenStats.add(extractionStats);
		multimediaStats.add(extractionStats);
		specimenStats.logStatistics(logger, "Specimens");
		multimediaStats.logStatistics(logger, "Multimedia");
		sStats.add(specimenStats);
		mStats.add(multimediaStats);
		logger.info("Importing {} took {}", f.getName(), getDuration(start));
		logger.info(" ");
		logger.info(" ");
	}

	private CSVExtractor<BrahmsCsvField> createExtractor(File f, ETLStatistics extractionStats)
	{
		CSVExtractor<BrahmsCsvField> extractor = new CSVExtractor<>(f, extractionStats);
		extractor.setSkipHeader(true);
		extractor.setDelimiter(',');
		extractor.setCharset(Charset.forName("Windows-1252"));
		extractor.setSuppressErrors(suppressErrors);
		return extractor;
	}
}
