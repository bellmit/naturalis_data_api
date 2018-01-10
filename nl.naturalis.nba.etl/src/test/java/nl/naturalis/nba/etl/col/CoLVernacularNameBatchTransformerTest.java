package nl.naturalis.nba.etl.col;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import nl.naturalis.nba.api.model.Taxon;
import nl.naturalis.nba.api.model.VernacularName;
import nl.naturalis.nba.etl.AllTests;
import nl.naturalis.nba.etl.CSVRecordInfo;
import nl.naturalis.nba.utils.reflect.ReflectionUtil;

/**
 * Test class for CoLVernacularNameBatchTransformer.java
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CSVRecordInfo.class)
@PowerMockIgnore("javax.management.*")
@SuppressWarnings({"static-method", "unchecked"})
public class CoLVernacularNameBatchTransformerTest {

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    String logFile = "log4j2.xml";
    URL logFileUrl = AllTests.class.getResource(logFile);
    String logFilePath = logFileUrl.getFile().toString();
    String dirPath = logFilePath.substring(0, logFilePath.lastIndexOf("/"));
    System.setProperty("nba.v2.conf.dir", dirPath);
    System.setProperty("brahms.data.dir", dirPath);
    System.setProperty("log4j.configurationFile", logFilePath);

    // First import a test data row into the ES store .
    CoLTaxonImporter cti = new CoLTaxonImporter();
    cti.importCsv(dirPath + "/taxa.txt");

    System.setProperty("nl.naturalis.nba.etl.testGenera",
        "malus,parus,larus,bombus,rhododendron,felix,tulipa,rosa,canis,passer,trientalis");

  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {}

  /**
   * Test method for
   * {@link nl.naturalis.nba.etl.col.CoLVernacularNameBatchTransformer#transform(java.util.ArrayList)}.
   * 
   * Test to verify the transform method returns the correct {Collection<@Taxon> } object Since this
   * method calls the ES so the texa data needs to be present in the ES store (which are loading
   * into the ES in the setUp())
   */
  @Test
  public void testTransform() {

    CSVRecordInfo<CoLVernacularNameCsvField> csvRecordInfo = PowerMockito.mock(CSVRecordInfo.class);
    when(csvRecordInfo.get(CoLVernacularNameCsvField.taxonID)).thenReturn("6931870");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.vernacularName)).thenReturn("pinhead spot");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.language)).thenReturn("English US");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.countryCode)).thenReturn("USA");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.locality)).thenReturn("");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.transliteration)).thenReturn("");

    List<CSVRecordInfo<CoLVernacularNameCsvField>> list = new ArrayList<>();
    list.add(csvRecordInfo);

    CoLVernacularNameBatchTransformer batchTransformer = new CoLVernacularNameBatchTransformer();

    Object returned = ReflectionUtil.call(batchTransformer, "transform",
        new Class[] {ArrayList.class}, new Object[] {list});
    Collection<Taxon> updates = (Collection<Taxon>) returned;
    Taxon actual = updates.iterator().next();

    String expectedId = "6931870@COL";
    String expectedRecordURI =
        "http://www.catalogueoflife.org/annual-checklist/2016/details/species/id/39ed89a52a61ef3a59eef66b9ce8ad7e";
    String expectedAuthorshipVerbatim = "Cresson, 1863";
    String expectedScientificNameGroup = "bombus affinis";
    String expectedFullScientificName = "Bombus affinis Cresson, 1863";
    String expectedSourceSystemName = "Species 2000 - Catalogue Of Life";
    String expectedSpcificEpithet = "affinis";
    String expectedVernecularName = "pinhead spot";
    String expectedLanguage = "English US";

    assertNotNull(actual);
    assertEquals(expectedId, actual.getId());
    assertEquals(expectedRecordURI, actual.getRecordURI().toString());
    assertEquals(expectedAuthorshipVerbatim, actual.getValidName().getAuthorshipVerbatim());
    assertEquals(expectedScientificNameGroup, actual.getValidName().getScientificNameGroup());
    assertEquals(expectedFullScientificName, actual.getValidName().getFullScientificName());
    assertEquals(expectedSourceSystemName, actual.getSourceSystem().getName());
    assertEquals(expectedSpcificEpithet, actual.getDefaultClassification().getSpecificEpithet());
    assertEquals(expectedVernecularName, actual.getVernacularNames().iterator().next().getName());
    assertEquals(expectedLanguage, actual.getVernacularNames().iterator().next().getLanguage());

  }

  /**
   * Test method for
   * {@link nl.naturalis.nba.etl.col.CoLVernacularNameBatchTransformer#testCreateLookupTable(CSVRecordInfo<CoLVernacularNameCsvField>
   * csvRecordInfo }.
   * 
   * Test to verify createLookupTable method returns a correct {HashMap<String, @Taxon>} object.
   * 
   * Since this method calls the ES so the data needs to be present in the ES store (which are
   * loaded into the ES in the setUp()
   */
  @Test
  public void testCreateLookupTable() {

    CSVRecordInfo<CoLVernacularNameCsvField> csvRecordInfo = PowerMockito.mock(CSVRecordInfo.class);
    when(csvRecordInfo.get(CoLVernacularNameCsvField.taxonID)).thenReturn("6931870");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.vernacularName)).thenReturn("pinhead spot");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.language)).thenReturn("English US");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.countryCode)).thenReturn("USA");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.locality)).thenReturn("");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.transliteration)).thenReturn("");

    List<CSVRecordInfo<CoLVernacularNameCsvField>> list = new ArrayList<>();
    list.add(csvRecordInfo);

    Object returned = ReflectionUtil.callStatic(CoLVernacularNameBatchTransformer.class,
        "createLookupTable", new Class[] {ArrayList.class}, new Object[] {list});
    HashMap<String, Taxon> actualResults = (HashMap<String, Taxon>) returned;

    String expectedKey = "6931870";
    Taxon actual = actualResults.get(expectedKey);
    String expectedId = "6931870@COL";
    String expectedRecordURI =
        "http://www.catalogueoflife.org/annual-checklist/2016/details/species/id/39ed89a52a61ef3a59eef66b9ce8ad7e";
    String expectedScientificNameGroup = "bombus affinis";
    String expectedFullScientificName = "Bombus affinis Cresson, 1863";
    String expectedSourceSystemName = "Species 2000 - Catalogue Of Life";
    String expectedSpcificEpithet = "affinis";

    assertEquals(expectedKey, actualResults.keySet().iterator().next());
    assertEquals(expectedId, actual.getId());
    assertEquals(expectedRecordURI, actual.getRecordURI().toString());
    assertEquals(expectedFullScientificName, actual.getAcceptedName().getFullScientificName());
    assertEquals(expectedScientificNameGroup, actual.getAcceptedName().getScientificNameGroup());
    assertEquals(expectedSpcificEpithet, actual.getDefaultClassification().getSpecificEpithet());
    assertEquals(expectedSourceSystemName, actual.getSourceSystem().getName());

  }

  /**
   * Test method for
   * {@link nl.naturalis.nba.etl.col.CoLVernacularNameBatchTransformer#createVernacularName(CSVRecordInfo<CoLVernacularNameCsvField>
   * csvRecordInfo }.
   * 
   * Test to verify createSynonym method returns a correct VernacularName object.
   * 
   */
  @Test
  public void testCreateVernacularName() {

    CSVRecordInfo<CoLVernacularNameCsvField> csvRecordInfo = PowerMockito.mock(CSVRecordInfo.class);
    when(csvRecordInfo.get(CoLVernacularNameCsvField.taxonID)).thenReturn("6931870");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.vernacularName)).thenReturn("pinhead spot");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.language)).thenReturn("English US");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.countryCode)).thenReturn("USA");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.locality)).thenReturn("");
    when(csvRecordInfo.get(CoLVernacularNameCsvField.transliteration)).thenReturn("");

    Object returned = ReflectionUtil.callStatic(CoLVernacularNameBatchTransformer.class,
        "createVernacularName", new Class[] {CSVRecordInfo.class}, new Object[] {csvRecordInfo});
    VernacularName actual = (VernacularName) returned;

    String expectedVenecularName = "pinhead spot";
    String expectedLangauge = "English US";

    assertNotNull(actual);
    assertEquals(expectedVenecularName, actual.getName());
    assertEquals(expectedLangauge, actual.getLanguage());

  }

}