package nl.naturalis.nba.api.model;

import static nl.naturalis.nba.api.annotations.Analyzer.CASE_INSENSITIVE;
import static nl.naturalis.nba.api.annotations.Analyzer.DEFAULT;
import static nl.naturalis.nba.api.annotations.Analyzer.LIKE;

import java.util.ArrayList;
import java.util.List;

import nl.naturalis.nba.api.annotations.Analyzers;
import nl.naturalis.nba.api.annotations.NotIndexed;
import nl.naturalis.nba.api.annotations.NotStored;

public class Specimen extends NbaTraceableObject implements IDocumentObject {

	@NotStored
	private String id;
	@Analyzers({ CASE_INSENSITIVE, DEFAULT, LIKE })
	private String unitID;
	@NotIndexed
	private String unitGUID;
	@Analyzers({ CASE_INSENSITIVE, DEFAULT, LIKE })
	private String collectorsFieldNumber;
	private String assemblageID;
	private String sourceInstitutionID;
	private String sourceID;
	private String previousSourceID;
	private String owner;
	private String licenseType;
	private String license;
	private String recordBasis;
	private String kindOfUnit;
	private String collectionType;
	private Sex sex;
	private PhaseOrStage phaseOrStage;
	private String title;
	@Analyzers({ CASE_INSENSITIVE, DEFAULT, LIKE })
	private String notes;
	private String preparationType;
	private Integer numberOfSpecimen;
	private boolean fromCaptivity;
	private boolean objectPublic;
	private boolean multiMediaPublic;

	private Agent acquiredFrom;
	private GatheringEvent gatheringEvent;
	private List<SpecimenIdentification> identifications;
	private List<ServiceAccessPoint> associatedMultiMediaUris;
	private List<String> theme;

	@NotStored
	private List<MultiMediaObject> associatedMultiMediaObjects;

	public void addIndentification(SpecimenIdentification identification)
	{
		if (identifications == null) {
			identifications = new ArrayList<>(3);
		}
		identifications.add(identification);
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public void setId(String id)
	{
		this.id = id;
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

	public String getCollectorsFieldNumber()
	{
		return collectorsFieldNumber;
	}

	public void setCollectorsFieldNumber(String collectorsFieldNumber)
	{
		this.collectorsFieldNumber = collectorsFieldNumber;
	}

	public String getAssemblageID()
	{
		return assemblageID;
	}

	public void setAssemblageID(String assemblageID)
	{
		this.assemblageID = assemblageID;
	}

	public String getSourceInstitutionID()
	{
		return sourceInstitutionID;
	}

	public void setSourceInstitutionID(String sourceInstitutionID)
	{
		this.sourceInstitutionID = sourceInstitutionID;
	}

	public String getSourceID()
	{
		return sourceID;
	}

	public void setSourceID(String sourceID)
	{
		this.sourceID = sourceID;
	}
	
	public String getPreviousSourceID()
	{
	  return previousSourceID;
	}
	
	public void setPreviousSourceID(String previousSourceID)
	{
	  this.previousSourceID = previousSourceID;
	}

	public String getOwner()
	{
		return owner;
	}

	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	public String getLicenseType()
	{
		return licenseType;
	}

	public void setLicenseType(String licenseType)
	{
		this.licenseType = licenseType;
	}

	public String getLicense()
	{
		return license;
	}

	public void setLicense(String license)
	{
		this.license = license;
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

	public String getCollectionType()
	{
		return collectionType;
	}

	public void setCollectionType(String collectionType)
	{
		this.collectionType = collectionType;
	}

	public Sex getSex()
	{
		return sex;
	}

	public void setSex(Sex sex)
	{
		this.sex = sex;
	}

	public PhaseOrStage getPhaseOrStage()
	{
		return phaseOrStage;
	}

	public void setPhaseOrStage(PhaseOrStage phaseOrStage)
	{
		this.phaseOrStage = phaseOrStage;
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

	public String getPreparationType()
	{
		return preparationType;
	}

	public void setPreparationType(String preparationType)
	{
		this.preparationType = preparationType;
	}

	public Integer getNumberOfSpecimen()
	{
		return numberOfSpecimen;
	}

	public void setNumberOfSpecimen(Integer numberOfSpecimen)
	{
		this.numberOfSpecimen = numberOfSpecimen;
	}

	public boolean isFromCaptivity()
	{
		return fromCaptivity;
	}

	public void setFromCaptivity(boolean fromCaptivity)
	{
		this.fromCaptivity = fromCaptivity;
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

	public Agent getAcquiredFrom()
	{
		return acquiredFrom;
	}

	public void setAcquiredFrom(Agent acquiredFrom)
	{
		this.acquiredFrom = acquiredFrom;
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

	public List<ServiceAccessPoint> getAssociatedMultiMediaUris()
	{
		return associatedMultiMediaUris;
	}

	public void setAssociatedMultiMediaUris(List<ServiceAccessPoint> associatedMultiMediaUris)
	{
		this.associatedMultiMediaUris = associatedMultiMediaUris;
	}

	public List<String> getTheme()
	{
		return theme;
	}

	public void setTheme(List<String> theme)
	{
		this.theme = theme;
	}

	public List<MultiMediaObject> getAssociatedMultiMediaObjects()
	{
		return associatedMultiMediaObjects;
	}

	public void setAssociatedMultiMediaObjects(List<MultiMediaObject> associatedMultiMediaObjects)
	{
		this.associatedMultiMediaObjects = associatedMultiMediaObjects;
	}

}
