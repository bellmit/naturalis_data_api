package nl.naturalis.nda.elasticsearch.dao.estypes;

import java.util.ArrayList;
import java.util.List;

import nl.naturalis.nda.domain.GatheringEvent;
import nl.naturalis.nda.domain.NdaTraceableObject;
import nl.naturalis.nda.domain.Specimen;
import nl.naturalis.nda.domain.SpecimenIdentification;

public class ESSpecimen extends NdaTraceableObject {

	private String unitID;
	private String unitGUID;
	private String setID;
	private String sourceInstitutionID;
	private String recordBasis;
	private String kindOfUnit;
	private String collectionType;
	private String typeStatus;
	private String sex;
	private String phaseOrStage;
	private String accessionSpecimenNumbers;
	private String title;
	private String notes;
	private boolean objectPublic;
	private boolean multiMediaPublic;

	private GatheringEvent gatheringEvent;
	private List<SpecimenIdentification> identifications;


	public void addIndentification(SpecimenIdentification identification)
	{
		if (identifications == null) {
			identifications = new ArrayList<SpecimenIdentification>();
		}
		identifications.add(identification);
	}


	public String getUnitID()
	{
		return unitID;
	}


	public void setUnitID(String unitID)
	{
		this.unitID = unitID;
	}


	public String getUnitGUID()
	{
		return unitGUID;
	}


	public void setUnitGUID(String unitGUID)
	{
		this.unitGUID = unitGUID;
	}


	public String getSetID()
	{
		return setID;
	}


	public void setSetID(String setID)
	{
		this.setID = setID;
	}


	public String getSourceInstitutionID()
	{
		return sourceInstitutionID;
	}


	public void setSourceInstitutionID(String sourceInstitutionID)
	{
		this.sourceInstitutionID = sourceInstitutionID;
	}


	public String getRecordBasis()
	{
		return recordBasis;
	}


	public void setRecordBasis(String recordBasis)
	{
		this.recordBasis = recordBasis;
	}


	public String getKindOfUnit()
	{
		return kindOfUnit;
	}


	public void setKindOfUnit(String kindOfUnit)
	{
		this.kindOfUnit = kindOfUnit;
	}


	public String getTypeStatus()
	{
		return typeStatus;
	}


	public void setTypeStatus(String typeStatus)
	{
		this.typeStatus = typeStatus;
	}


	public String getCollectionType()
	{
		return collectionType;
	}


	public void setCollectionType(String collectionType)
	{
		this.collectionType = collectionType;
	}


	public String getSex()
	{
		return sex;
	}


	public void setSex(String sex)
	{
		this.sex = sex;
	}


	public String getPhaseOrStage()
	{
		return phaseOrStage;
	}


	public void setPhaseOrStage(String phaseOrStage)
	{
		this.phaseOrStage = phaseOrStage;
	}


	public String getAccessionSpecimenNumbers()
	{
		return accessionSpecimenNumbers;
	}


	public void setAccessionSpecimenNumbers(String accessionSpecimenNumbers)
	{
		this.accessionSpecimenNumbers = accessionSpecimenNumbers;
	}


	public String getTitle()
	{
		return title;
	}


	public void setTitle(String title)
	{
		this.title = title;
	}


	public String getNotes()
	{
		return notes;
	}


	public void setNotes(String notes)
	{
		this.notes = notes;
	}


	public boolean isObjectPublic()
	{
		return objectPublic;
	}


	public void setObjectPublic(boolean objectPublic)
	{
		this.objectPublic = objectPublic;
	}


	public boolean isMultiMediaPublic()
	{
		return multiMediaPublic;
	}


	public void setMultiMediaPublic(boolean multiMediaPublic)
	{
		this.multiMediaPublic = multiMediaPublic;
	}


	public GatheringEvent getGatheringEvent()
	{
		return gatheringEvent;
	}


	public void setGatheringEvent(GatheringEvent gatheringEvent)
	{
		this.gatheringEvent = gatheringEvent;
	}


	public List<SpecimenIdentification> getIdentifications()
	{
		return identifications;
	}


	public void setIdentifications(List<SpecimenIdentification> identifications)
	{
		this.identifications = identifications;
	}



}
