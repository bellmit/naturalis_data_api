package nl.naturalis.nba.api.model.summary;

import static nl.naturalis.nba.api.annotations.Analyzer.CASE_INSENSITIVE;
import static nl.naturalis.nba.api.annotations.Analyzer.DEFAULT;
import static nl.naturalis.nba.api.annotations.Analyzer.LIKE;

import java.util.ArrayList;
import java.util.List;

import nl.naturalis.nba.api.annotations.Analyzers;
import nl.naturalis.nba.api.annotations.NotIndexed;
import nl.naturalis.nba.api.model.INbaModelObject;
import nl.naturalis.nba.api.model.PhaseOrStage;
import nl.naturalis.nba.api.model.Sex;
import nl.naturalis.nba.api.model.SpecimenTypeStatus;

public class SummarySpecimen implements INbaModelObject {

	@NotIndexed
	private String id;
	private SummarySourceSystem sourceSystem;
	@Analyzers({ CASE_INSENSITIVE, DEFAULT, LIKE })
	private String unitID;
	@Analyzers({ CASE_INSENSITIVE, DEFAULT, LIKE })
	private String collectorsFieldNumber;
	private SpecimenTypeStatus typeStatus;
	private Sex sex;
	private PhaseOrStage phaseOrStage;
	private SummaryGatheringEvent gatheringEvent;
	private SummarySpecimenIdentification matchingIdentification;
	private List<SummarySpecimenIdentification> otherIdentifications;

	public void addOtherIdentification(SummarySpecimenIdentification ssi)
	{
		if (otherIdentifications == null) {
			otherIdentifications = new ArrayList<>(3);
		}
		otherIdentifications.add(ssi);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj instanceof SummarySpecimen) {
			return ((SummarySpecimen) obj).getId().equals(id);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public SummarySourceSystem getSourceSystem()
	{
		return sourceSystem;
	}

	public void setSourceSystem(SummarySourceSystem sourceSystem)
	{
		this.sourceSystem = sourceSystem;
	}

	public String getUnitID()
	{
		return unitID;
	}

	public void setUnitID(String unitID)
	{
		this.unitID = unitID;
	}

	public String getCollectorsFieldNumber()
	{
		return collectorsFieldNumber;
	}

	public void setCollectorsFieldNumber(String collectorsFieldNumber)
	{
		this.collectorsFieldNumber = collectorsFieldNumber;
	}

	public SpecimenTypeStatus getTypeStatus()
	{
		return typeStatus;
	}

	public void setTypeStatus(SpecimenTypeStatus typeStatus)
	{
		this.typeStatus = typeStatus;
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

	public SummaryGatheringEvent getGatheringEvent()
	{
		return gatheringEvent;
	}

	public void setGatheringEvent(SummaryGatheringEvent gatheringEvent)
	{
		this.gatheringEvent = gatheringEvent;
	}

	public SummarySpecimenIdentification getMatchingIdentification()
	{
		return matchingIdentification;
	}

	public void setMatchingIdentification(SummarySpecimenIdentification matchingIdentification)
	{
		this.matchingIdentification = matchingIdentification;
	}

	public List<SummarySpecimenIdentification> getOtherIdentifications()
	{
		return otherIdentifications;
	}

	public void setOtherIdentifications(List<SummarySpecimenIdentification> otherIdentifications)
	{
		this.otherIdentifications = otherIdentifications;
	}

}