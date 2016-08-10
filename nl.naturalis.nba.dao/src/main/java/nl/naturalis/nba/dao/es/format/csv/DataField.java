package nl.naturalis.nba.dao.es.format.csv;

import static nl.naturalis.nba.common.json.JsonUtil.MISSING_VALUE;
import static nl.naturalis.nba.common.json.JsonUtil.readField;
import static nl.naturalis.nba.dao.es.format.FormatUtil.EMPTY_STRING;
import static org.apache.commons.lang3.StringEscapeUtils.escapeCsv;

import java.util.Map;

class DataField extends AbstractCsvField {

	private final String[] path;

	DataField(String name, String[] path)
	{
		super(name);
		this.path = path;
	}

	@Override
	public String getValue(Map<String, Object> esDocumentAsMap)
	{
		Object value = readField(esDocumentAsMap, path);
		if (value == MISSING_VALUE)
			return EMPTY_STRING;
		return escapeCsv(value.toString());
	}

}