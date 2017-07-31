package nl.naturalis.nba.dao.format.csv;

import static nl.naturalis.nba.dao.format.FormatUtil.EMPTY_STRING;
import static nl.naturalis.nba.dao.format.FormatUtil.formatDate;

import java.net.URI;
import java.util.Date;

import nl.naturalis.nba.api.Path;
import nl.naturalis.nba.common.PathValueReader;
import nl.naturalis.nba.dao.format.AbstractField;
import nl.naturalis.nba.dao.format.EntityObject;

class EntityDateField extends AbstractField {

	private PathValueReader pvr;

	EntityDateField(String name, URI term, Path path)
	{
		super(name, term);
		this.pvr = new PathValueReader(path);
	}

	@Override
	public String getValue(EntityObject entity)
	{
		Date value = (Date) pvr.read(entity.getEntity());
		return value == null ? EMPTY_STRING : formatDate(value);
	}

}
