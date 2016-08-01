package nl.naturalis.nba.dao.es.format.csv;

import static org.apache.commons.lang3.StringEscapeUtils.escapeCsv;

import java.util.Map;

class ConstantField extends AbstractCsvField {

	private final String value;

	ConstantField(String name, String value)
	{
		super(name);
		this.value = escapeCsv(value);
	}

	@Override
	public String getValue(Map<String, Object> esDocumentAsMap)
	{
		return value;
	}

}
