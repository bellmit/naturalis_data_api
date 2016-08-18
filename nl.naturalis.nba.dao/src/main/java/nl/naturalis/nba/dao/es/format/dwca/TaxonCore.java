package nl.naturalis.nba.dao.es.format.dwca;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import nl.naturalis.nba.dao.es.format.DataSet;
import nl.naturalis.nba.dao.es.format.DataSetEntity;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "core")
class TaxonCore extends Core {

	private static final String ROW_TYPE = "http://rs.tdwg.org/dwc/terms/Taxon";
	private static final String LOCATION = "taxa.txt";

	TaxonCore()
	{
		super();
		this.rowType = ROW_TYPE;
		this.files = new Files(LOCATION);
	}

	TaxonCore forDataSet(DataSet ds)
	{
		DataSetEntity entity = ds.getDataSetCollection().getEntity("taxa");
		this.fields = DwcaUtil.getMetaXmlFieldElements(entity);
		return this;
	}

}
