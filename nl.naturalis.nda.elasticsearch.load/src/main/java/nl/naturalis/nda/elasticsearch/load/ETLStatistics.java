package nl.naturalis.nda.elasticsearch.load;

import static org.domainobject.util.StringUtil.pad;
import static org.domainobject.util.StringUtil.rpad;

import org.slf4j.Logger;

/**
 * A Java bean maintaining a set of running totals for an ETL program.
 * 
 * @author Ayco Holleman
 *
 */
public class ETLStatistics {

	/**
	 * The number of times that the source data could not be parsed into a
	 * record (usually during extraction phase). This is useful when processing
	 * CSV files, where a raw line may not be parsable into a {@code CSVRecord}.
	 * It is not very useful when processing XML files, because these are parsed
	 * as a whole into a DOM tree.
	 */
	public int badInput;

	/**
	 * The number of records processed by the import program.
	 */
	public int recordsProcessed;
	/**
	 * The number of records that contain data that are not meant to be imported
	 * by the import program. For example the taxa.txt file in CoL DwCA files
	 * containes both taxa and synonyms. The taxon importer only needs to
	 * process the taxa in that file.<br>
	 * {@code recordsSkipped + recordsRejected + recordsRejected = recordsProcessed}
	 */
	public int recordsSkipped;
	/**
	 * The number of records that failed some validation. Validation is done for
	 * the record as a whole, rather than for the object(s) extracted from it.
	 */
	public int recordsRejected;
	/**
	 * The number of records that made it to the transformation phase. Simply
	 * the number of records that were neither skipped nor rejected.<br>
	 * {@code recordsSkipped + recordsRejected + recordsAccepted = recordsProcessed}
	 */
	public int recordsAccepted;
	/**
	 * The number of objects processed by the import program. Note that one
	 * record may contain multiple objects. For example, one line in a Brahms
	 * CSV export may contain multiple images.<br>
	 * {@code objectsSkipped + objectsRejected + objectsIndexed = objectsProcessed}
	 */
	public int objectsProcessed;
	/**
	 * The number of objects that are not meant to be imported by the import
	 * program.
	 */
	public int objectsSkipped;
	/**
	 * The number of objects that failed some validation.
	 */
	public int objectsRejected;
	/**
	 * The number of objects that made it to ElasticSearch.
	 */
	public int objectsIndexed;
	/**
	 * The number of objects that passed validation
	 */
	public int objectsAccepted;

	private boolean objectsAcceptedNotObjectsIndexed;

	/**
	 * Ordinarily the following rule applies:
	 * {@code objectsSkipped + objectsRejected + objectsIndexed = objectsProcessed}
	 * . In this case the transformer provides the first two statistics while
	 * the loader provides the last statistic. ETL programs for which this rule
	 * applies don't need to keep track of the {@code objectsAccepted} counter.
	 * However, if a data source is only used to add children (nested objects)
	 * to an existing parent document, this rule no longer applies. The rule
	 * that applies then is:
	 * {@code objectsSkipped + objectsRejected + objectsAccepted = objectsProcessed}
	 * . In this case the transformer provides all three statistics and the
	 * number of objects indexed is more or less meaningless. If a document has
	 * no children, it is not updated. If it has 10 children, it may be updated
	 * only once or up to 10 times, depending on how far apart the CSV
	 * records containing the children were. In case of adjacent records they
	 * are added all at once to the parent document, resulting in just one index
	 * request for 10 CSV records.
	 * 
	 * @param b
	 */
	public void setObjectsAcceptedNotObjectsIndexed(boolean b)
	{
		this.objectsAcceptedNotObjectsIndexed = b;
	}

	public boolean isObjectsAcceptedNotObjectsIndexed()
	{
		return objectsAcceptedNotObjectsIndexed;
	}

	/**
	 * Reset all counters
	 */
	public void reset()
	{
		badInput = 0;
		recordsProcessed = 0;
		recordsSkipped = 0;
		recordsRejected = 0;
		objectsProcessed = 0;
		objectsSkipped = 0;
		objectsRejected = 0;
		objectsIndexed = 0;
		objectsAccepted = 0;
	}

	/**
	 * Add the counters from the specified statistics object to this statistics
	 * object.
	 * 
	 * @param other
	 */
	public void add(ETLStatistics other)
	{
		badInput += other.badInput;
		recordsProcessed += other.recordsProcessed;
		recordsSkipped += other.recordsSkipped;
		recordsRejected += other.recordsRejected;
		recordsAccepted += other.recordsAccepted;
		objectsProcessed += other.objectsProcessed;
		objectsSkipped += other.objectsSkipped;
		objectsRejected += other.objectsRejected;
		objectsIndexed += other.objectsIndexed;
		objectsAccepted += other.objectsAccepted;
	}

	/**
	 * Log statistic about the ETL cycle.
	 * 
	 * @param logger
	 */
	public void logStatistics(Logger logger)
	{
		logStatistics(logger, null);
	}

	/**
	 * Log statistic about the ETL cycle, using a user-friendly name for the
	 * type of objects being indexed.
	 * 
	 * @param logger
	 * @param niceName
	 */
	public void logStatistics(Logger logger, String niceName)
	{
		logger.info(" ");
		if (niceName != null) {
			String title = niceName.toUpperCase() + " IMPORT";
			logger.info(pad(title, 38));
		}
		else {
			niceName = "Objects";
		}
		logger.info("=====================================");
		logger.info(statistic("Extraction/parse failures", badInput));
		logger.info(" ");
		logger.info(statistic("Records skipped", recordsSkipped));
		logger.info(statistic("Records accepted", recordsAccepted));
		logger.info(statistic("Records rejected", recordsRejected));
		logger.info("------------------------------------- +");
		logger.info(statistic("Records processed", recordsProcessed));

		logger.info(" ");
		logger.info(statistic(niceName, "skipped", objectsSkipped));
		if (objectsAcceptedNotObjectsIndexed)
			logger.info(statistic(niceName, "accepted", objectsAccepted));
		else
			logger.info(statistic(niceName, "indexed", objectsIndexed));
		logger.info(statistic(niceName, "rejected", objectsRejected));
		logger.info("------------------------------------- +");
		logger.info(statistic(niceName, "processed", objectsProcessed));

		if (objectsAcceptedNotObjectsIndexed) {
			logger.info(" ");
			logger.info(statistic("ElasticSearch index requests", objectsIndexed));
		}

		logger.info("=====================================");
		logger.info(" ");
	}

	private static String statistic(String niceName, String statName, int stat)
	{
		return rpad(niceName + " " + statName, 28, ": ") + String.format("%7d", stat);
	}

	private static String statistic(String statName, int stat)
	{
		return rpad(statName, 28, ": ") + String.format("%7d", stat);
	}

}
