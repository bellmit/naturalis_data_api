package nl.naturalis.nba.etl.col;

import static nl.naturalis.nba.etl.ETLUtil.getLogger;
import static nl.naturalis.nba.etl.ETLUtil.logDuration;
import static nl.naturalis.nba.etl.col.CoLReferenceCsvField.taxonID;
import static nl.naturalis.nba.etl.col.CoLReferenceCsvField.title;
import static nl.naturalis.nba.etl.col.CoLReferenceCsvField.creator;
import static nl.naturalis.nba.etl.col.CoLReferenceCsvField.date;
import static nl.naturalis.nba.etl.col.CoLReferenceCsvField.description;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import nl.naturalis.nba.api.model.Person;
import nl.naturalis.nba.api.model.Reference;
import nl.naturalis.nba.api.model.VernacularName;
import nl.naturalis.nba.common.es.ESDateInput;
import nl.naturalis.nba.common.json.JsonUtil;
import nl.naturalis.nba.etl.CSVExtractor;
import nl.naturalis.nba.etl.CSVRecordInfo;
import nl.naturalis.nba.etl.ETLRuntimeException;
import nl.naturalis.nba.etl.ETLStatistics;

/**
 * Load literature references into the H2 Database
 * 
 * @author Tom Gilissen
 *
 */
public class CoLReferenceLoader {
  private static final Logger logger = getLogger(CoLVernacularNameLoader.class);
  
  private Connection connection;
  private int batchSize = 1000;
  
  public CoLReferenceLoader(Connection connection)
  {
    this.connection = connection;
    createTable();
  }

  /**
   * Processes the reference.txt file
   * 
   * @param path
   */
  public void importCsv(String path)
  {
    File f = new File(path);
    if (!f.exists()) {
      throw new ETLRuntimeException("No such file: " + path);
    }
    long start = System.currentTimeMillis();
    ETLStatistics stats = new ETLStatistics();
    
    CSVExtractor<CoLReferenceCsvField> extractor = createExtractor(stats, f);
    ArrayList<CSVRecordInfo<CoLReferenceCsvField>> csvRecords;
    csvRecords = new ArrayList<>(batchSize);
    
    int processed = 0;
    int skipped = 0;
    logger.info("Processing file {}", f.getAbsolutePath());
    logger.info("Batch size: {}", batchSize);
    
    for (CSVRecordInfo<CoLReferenceCsvField> record : extractor) {
      if (++processed % 100000 == 0) {
        logger.info("Records processed: {}", processed);
      }
      if (record == null || record.get(taxonID) == null) {
        // Garbage
        skipped++;
        continue;
      }
      csvRecords.add(record);
      if (csvRecords.size() == batchSize) {
        saveRecords(csvRecords);
        csvRecords.clear();
        }
      }
    if (!csvRecords.isEmpty()) {
      saveRecords(csvRecords);
      csvRecords.clear();
    }
    logger.info("Records processed:        {}", processed);
    logger.info("Records skipped:          {}", skipped);
    logger.info("References created: {}", countReferences());
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
    extractor = new CSVExtractor<>(f, CoLReferenceCsvField.class, stats);
    extractor.setSkipHeader(true);
    extractor.setDelimiter('\t');
    extractor.setQuote('\u0000');
    return extractor;
  }
  
  private static Reference createReference(CSVRecordInfo<CoLReferenceCsvField> record)
  {
    Reference reference = new Reference();
    reference.setTitleCitation(record.get(title));
    reference.setCitationDetail(record.get(description));
    String s;
    if ((s = record.get(date)) != null) {
      OffsetDateTime odt = new ESDateInput(s).parseAsYear();
      if (odt == null) {
        logger.warn("Invalid date: {}", s);
      }
      else {
        reference.setPublicationDate(odt);
      }
    }
    if ((s = record.get(creator)) != null) {
      reference.setAuthor(new Person(s));
    }
    return reference;
  }
  
  private void createTable() {
    Statement stmt = null;
    try {
      connection.setAutoCommit(false);      
      stmt = connection.createStatement();
      stmt.execute("CREATE TABLE REFERENCES (id int primary key auto_increment, taxonId varchar(50) not null, document LONGTEXT)");
      stmt.close();
      connection.commit();

    } catch (SQLException e) {
      System.out.println("Exception Message " + e.getLocalizedMessage());
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
  
  private void saveRecords(ArrayList<CSVRecordInfo<CoLReferenceCsvField>> records) {
    Statement stmt = null;
    try {          
      connection.setAutoCommit(false);
      stmt = connection.createStatement();
      for (CSVRecordInfo<CoLReferenceCsvField> record : records) {
        Reference reference = createReference(record);
        String taxonId = record.get(taxonID);
        String document = JsonUtil.toJson(reference).replaceAll("'", "''");
        stmt.execute(String.format("INSERT INTO REFERENCES(taxonId, document) VALUES('%s', '%s')", taxonId, document));
      }
      stmt.close();
      connection.commit();
    } catch (SQLException e) {
        System.out.println("Exception Message " + e.getLocalizedMessage());
    } catch (Exception e) {
        e.printStackTrace();
    }
  }

  private long countReferences() {
    long n = 0L;
    Statement stmt = null;
    try {          
      connection.setAutoCommit(false);
      stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT COUNT(taxonId) FROM REFERENCES");
      rs.next();
      n = rs.getLong(1);
      stmt.close();
      connection.commit();
    } catch (SQLException e) {
        System.out.println("Exception Message " + e.getLocalizedMessage());
    } catch (Exception e) {
        e.printStackTrace();
    } 
    return n;
  }
  
}