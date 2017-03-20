package nl.naturalis.nba.etl.col;

import static nl.naturalis.nba.dao.DocumentType.TAXON;
import static nl.naturalis.nba.etl.ETLUtil.getLogger;
import static nl.naturalis.nba.etl.ETLUtil.logDuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.Logger;

import nl.naturalis.nba.api.model.Taxon;
import nl.naturalis.nba.dao.DaoRegistry;
import nl.naturalis.nba.dao.ESClientManager;
import nl.naturalis.nba.dao.util.es.ESUtil;
import nl.naturalis.nba.etl.BulkIndexer;
import nl.naturalis.nba.etl.CSVExtractor;
import nl.naturalis.nba.etl.CSVRecordInfo;
import nl.naturalis.nba.etl.ETLRuntimeException;
import nl.naturalis.nba.etl.ETLStatistics;

/**
 * Enriches Taxon documents with literature references sourced from the
 * reference.txt file in a CoL DwC archive.
 * 
 * @author Ayco Holleman
 *
 */
public class CoLReferenceBatchImporter {

	public static void main(String[] args) throws Exception
	{
		String prop = System.getProperty("batchSize", "1000");
		int batchSize = 0;
		try {
			batchSize = Integer.parseInt(prop);
		}
		catch (NumberFormatException e) {
			System.err.println("Invalid batch size: " + prop);
			System.exit(1);
		}
		try {
			CoLReferenceBatchImporter importer = new CoLReferenceBatchImporter();
			importer.setBatchSize(batchSize);
			String dwcaDir = DaoRegistry.getInstance().getConfiguration().required("col.data.dir");
			importer.importCsv(dwcaDir + "/reference.txt");
		}
		finally {
			ESUtil.refreshIndex(TAXON);
			ESClientManager.getInstance().closeClient();
		}
	}

	private static final Logger logger = getLogger(CoLReferenceBatchImporter.class);

	public CoLReferenceBatchImporter()
	{
	}

	private int batchSize;

	/**
	 * Processes the reference.txt file
	 * 
	 * @param path
	 */
	public void importCsv(String path)
	{
		long start = System.currentTimeMillis();
		ETLStatistics stats;
		CSVExtractor<CoLReferenceCsvField> extractor;
		CoLReferenceBatchTransformer transformer = null;
		BulkIndexer<Taxon> updater;
		ArrayList<CSVRecordInfo<CoLReferenceCsvField>> records;
		Collection<Taxon> taxa;
		int processed = 0;
		try {
			File f = new File(path);
			if (!f.exists()) {
				throw new ETLRuntimeException("No such file: " + path);
			}
			stats = new ETLStatistics();
			extractor = createExtractor(stats, f);
			transformer = new CoLReferenceBatchTransformer();
			updater = new BulkIndexer<>(TAXON);
			records = new ArrayList<>(batchSize);
			logger.info("Processing file {}", f.getAbsolutePath());
			for (CSVRecordInfo<CoLReferenceCsvField> rec : extractor) {
				if (rec == null) {
					continue;
				}
				records.add(rec);
				if (records.size() == batchSize) {
					taxa = transformer.transform(records);
					updater.index(taxa);
					ESUtil.refreshIndex(TAXON);
					records.clear();
				}
				if (++processed % 100000 == 0) {
					logger.info("Records processed: {}", processed);
					logger.info("References created: {}", transformer.getNumCreated());
					logger.info("Taxa updated: {}", transformer.getNumUpdated());
					logger.info("Duplicate references: {}", transformer.getNumDuplicates());
				}
			}
			if (records.size() != 0) {
				taxa = transformer.transform(records);
				updater.index(taxa);
			}
		}
		catch (Throwable t) {
			logger.error(getClass().getSimpleName() + " terminated unexpectedly!", t);
		}
		logger.info("Records processed: {}", processed);
		logger.info("References created: {}", transformer.getNumCreated());
		logger.info("Taxa updated: {}", transformer.getNumUpdated());
		logger.info("Duplicate references: {}", transformer.getNumDuplicates());
		logDuration(logger, getClass(), start);
	}

	public int getBatchSize()
	{
		return batchSize;
	}

	public void setBatchSize(int batchSize)
	{
		this.batchSize = batchSize;
	}

	private static CSVExtractor<CoLReferenceCsvField> createExtractor(ETLStatistics stats, File f)
	{
		CSVExtractor<CoLReferenceCsvField> extractor;
		extractor = new CSVExtractor<>(f, stats);
		extractor.setSkipHeader(true);
		extractor.setDelimiter('\t');
		return extractor;
	}

}