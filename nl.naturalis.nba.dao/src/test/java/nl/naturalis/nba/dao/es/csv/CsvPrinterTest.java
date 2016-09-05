package nl.naturalis.nba.dao.es.csv;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class CsvPrinterTest {

	private static Map<String, Object> specimen;

	@BeforeClass
	public static void init() throws ParseException
	{
		specimen = new LinkedHashMap<>();
		specimen.put("unitID", "RMNH.MAM.123456");
		specimen.put("sex", "male");
		List<Map<String, Object>> identifications = new ArrayList<>();
		specimen.put("identifications", identifications);
		Map<String, Object> identification = new LinkedHashMap<>();
		identifications.add(identification);
		identification.put("taxonRank", "species");
		Map<String, Object> defaultClassification = new LinkedHashMap<>();
		identification.put("defaultClassification", defaultClassification);
		defaultClassification.put("genus", "Larus");
		defaultClassification.put("specificEpithet", "fuscus");
		Map<String, Object> gatheringEvent = new LinkedHashMap<>();
		specimen.put("gatheringEvent", gatheringEvent);
		gatheringEvent.put("dateTimeBegin", "2012/08/13T00:00:00-200");
	}

	@Test
	public void testPrintHeader() throws UnsupportedEncodingException
	{
//		ITypedFieldFactory fieldFactory = new CsvFieldFactory();
//		FieldConfigurator fc = new FieldConfigurator(SPECIMEN, fieldFactory);
//		InputStream is = getClass().getResourceAsStream("CsvPrinterTest_fields.config");
//		IField[] fields = fc.getFields(is, "dummy");
//		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
//		CsvPrinter printer = new CsvPrinter(fields, out);
//		printer.printHeader();
//		// System.out.println(out);
//		String expected = "id,sex,nomenclaturalCode,verbatimEventDate,taxonRank,genus,specificEpithet";
//		assertEquals("01", expected, out.toString("UTF-8").trim());
	}

	@Test
	public void testPrintRecord() throws UnsupportedEncodingException
	{
//		ITypedFieldFactory fieldFactory = new CsvFieldFactory();
//		FieldConfigurator fc = new FieldConfigurator(SPECIMEN, fieldFactory);
//		InputStream is = getClass().getResourceAsStream("CsvPrinterTest_fields.config");
//		IField[] fields = fc.getFields(is, "dummy");
//		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
//		CsvPrinter printer = new CsvPrinter(fields, out);
//		printer.printRecord(specimen);
//		//System.out.println(out);
//		String expected = "RMNH.MAM.123456,male,ICZN,2012/08/13,species,Larus,fuscus";
//		assertEquals("01", expected, out.toString("UTF-8").trim());
	}

}
