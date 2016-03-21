package nl.naturalis.nba.elasticsearch.map;

import java.util.LinkedHashMap;

public class Document extends ESField {

	private final LinkedHashMap<String, ESField> properties;

	public Document()
	{
		this(null);
	}

	public Document(ESDataType type)
	{
		super(type);
		properties = new LinkedHashMap<>();
	}

	public LinkedHashMap<String, ESField> getProperties()
	{
		return properties;
	}

	public void addField(String name, ESField f)
	{
		properties.put(name, f);
	}

}
