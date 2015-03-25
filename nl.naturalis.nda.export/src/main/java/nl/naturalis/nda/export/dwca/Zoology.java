package nl.naturalis.nda.export.dwca;

import nl.naturalis.nda.domain.Agent;
import nl.naturalis.nda.domain.Person;
import nl.naturalis.nda.elasticsearch.dao.estypes.ESSpecimen;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class Zoology
{

	public Zoology()
	{
		// TODO Auto-generated constructor stub
	}

	public void addZoologyOccurrencefield(List<ESSpecimen> list, CsvFileWriter filewriter,
			String MAPPING_FILE_NAME)
	{
		for (ESSpecimen specimen : list)
		{
			CsvFileWriter.CsvRow dataRow = filewriter.new CsvRow();

			/* 01_RecordBasis */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "basisOfRecord,1"))
			{
				dataRow.add(specimen.getRecordBasis());
			}

			/* 02_CatalogNumber */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "catalogNumber,1"))
			{
				dataRow.add(specimen.getSourceSystemId());
			}

			/* 03_ClassName */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "class,1"))
			{
				if (specimen.getIdentifications().iterator().next().getDefaultClassification() != null)
				{
					dataRow.add(specimen.getIdentifications().iterator().next().getDefaultClassification().getClassName());
				}
				else
				{
					dataRow.add(null);
				}
			}

			/* 04_CollectionType */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "collectionCode,1"))
			{
				dataRow.add(specimen.getCollectionType());
			}

			/* 05_Continent */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "continent,1"))
			{
				dataRow.add(specimen.getGatheringEvent().getWorldRegion());
			}

			/* 06_Country */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "country,1"))
			{
				dataRow.add(specimen.getGatheringEvent().getCountry());
			}

			/* ToDo: GetCity moet County worden. */
			/* 07_County */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "county,1"))
			{
				dataRow.add(specimen.getGatheringEvent().getCity());
			}

			/* 08_DateIdentified */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "dateIdentified,1"))
			{
				if (specimen.getIdentifications().iterator().next().getDateIdentified() != null)
				{
					SimpleDateFormat dateidentified = new SimpleDateFormat("yyyy-MM-dd");
					String dateiden = dateidentified.format(specimen.getIdentifications().iterator().next().getDateIdentified());
					dataRow.add(dateiden);
				}
				else
				{
					dataRow.add(null);
				}
			}

			if (specimen.getGatheringEvent().getSiteCoordinates() != null)
			{
				/* 09_LatitudeDecimal */
				if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "decimalLatitude,1"))
				{
					if (specimen.getGatheringEvent().getSiteCoordinates().iterator().next()
							.getLatitudeDecimal() != null)
					{
						dataRow.add(Double.toString(specimen.getGatheringEvent().getSiteCoordinates()
								.iterator().next().getLatitudeDecimal()));
					}
					else
					{
						dataRow.add(" ");
					}
				}
				   

				/* 10_LongitudeDecimal */
				if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "decimalLongitude,1"))
				{
					if (specimen.getGatheringEvent().getSiteCoordinates().iterator().next()
							.getLongitudeDecimal() != null)
					{
						dataRow.add(Double.toString(specimen.getGatheringEvent().getSiteCoordinates()
								.iterator().next().getLongitudeDecimal()));
					}
					else
					{
						dataRow.add(" ");
					}
				}
			}

			/* 11_DateTimeBegin */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "eventDate,1"))
			{
				if (specimen.getGatheringEvent().getDateTimeBegin() != null)
				{
					SimpleDateFormat datetimebegin = new SimpleDateFormat("yyyy-MM-dd");
					String datebegin = datetimebegin.format(specimen.getGatheringEvent().getDateTimeBegin());
					dataRow.add(datebegin);
				}
				else
				{
					dataRow.add(null);
				}
			}

			/* 12_DateTimeEnd */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "eventDate,1"))
			{
				if (specimen.getGatheringEvent().getDateTimeEnd() != null)
				{
					SimpleDateFormat datetimenend = new SimpleDateFormat("yyyy-MM-dd");
					String dateEnd = datetimenend.format(specimen.getGatheringEvent().getDateTimeEnd());
					dataRow.add(dateEnd);
				}
				else
				{
					dataRow.add(null);
				}
			}

			/* 13_Family */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "family,0"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getDefaultClassification()
						.getFamily());
			}

			/* 14_GenusOrMonomial */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "genus,1"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getScientificName()
						.getGenusOrMonomial());
			}

			/* 15_Dummy1 */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "geodeticDatum,1"))
			{
				dataRow.add(null);
			}

			/* 16_Dummy2 */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "habitat,0"))
			{
				dataRow.add(null);
			}

			/* 17_Dummy3 */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "higherClassification,0"))
			{
				dataRow.add(null);
			}

			/* 18_SourceSystemId */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "id,1"))
			{
				dataRow.add(specimen.getSourceSystemId());
			}

			/* 19_identifications.identifiers.fullName | BRAHMS ONLY ?? */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "identifiedBy,1"))
			{
				if (specimen.getIdentifications().iterator().next().getIdentifiers() != null)
				{
					Agent ag = specimen.getIdentifications().iterator().next().getIdentifiers().iterator()
							.next();
					if (ag instanceof Person)
					{
						Person per = (Person) ag;
						dataRow.add(per.getFullName());
					} else
					{
						dataRow.add(null);
					}
				}
			}

			/* 20_NumberOfSpecimen */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "individualCount,1"))
			{
				dataRow.add(Integer.toString(specimen.getNumberOfSpecimen()));
			}

			/* 21_DummyDefault ToDO */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "informationWithheld,1"))
			{
				dataRow.add(" ");
			}

			/* 22_InfraspecificEpithet */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "infraSpecificEpithet,1"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getScientificName()
						.getInfraspecificEpithet());
			}

			/* 23_Island */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "island,0"))
			{
				dataRow.add(specimen.getGatheringEvent().getIsland());
			}

			/* 24_SourceInstitutionID */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "institutionCode,1"))
			{
				if(specimen.getSourceInstitutionID().contains("Naturalis"))
				{
    			  dataRow.add(specimen.getSourceInstitutionID().substring(0, 9));
				}
				else
				{
					  dataRow.add(specimen.getSourceInstitutionID());
				}
			}

			/* 25_Kingdom */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "kingdom,1"))
			{
				if (specimen.getIdentifications().iterator().next().getDefaultClassification() != null)
				{
					dataRow.add(specimen.getIdentifications().iterator().next().getDefaultClassification().getKingdom());
				}
				else
				{
					dataRow.add(null);
				}
			}
		
			/* 26_PhaseOrStage */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "lifeStage,1"))
			{
				dataRow.add(specimen.getPhaseOrStage());
			}

			/* 27_Locality */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "locality,1"))
			{
				dataRow.add(specimen.getGatheringEvent().getLocality());
			}

			/* 28_Dummy4 */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "maximumElevationInMeters,0"))
			{
				dataRow.add(specimen.getGatheringEvent().getLocality());
			}

			/* 29_Dummy5 */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "minimumElevationInMeters,0"))
			{
				dataRow.add(specimen.getGatheringEvent().getLocality());
			}

			/* 30_Dummy6 ToDO ? */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "nomenclaturalCode,1"))
			{
				dataRow.add("ICZN");
			}

			/* 31_DummyDefault */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "occurrenceID,1"))
			{
				dataRow.add(specimen.getSourceSystemId());
			}

			/* 32_Order */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "order,0"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getDefaultClassification()
						.getOrder());
			}

			/* 33_Phylum */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "phylum,0"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getDefaultClassification()
						.getPhylum());
			}

			/* 34_PreparationType */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "preparations,1"))
			{
				dataRow.add(specimen.getPreparationType());
			}

			/* 35_gatheringEvent.gatheringAgents.fullName */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "recordedBy,1"))
			{
				if (specimen.getGatheringEvent().getGatheringPersons() != null)
				{
					dataRow.add(specimen.getGatheringEvent().getGatheringPersons().iterator().next()
							.getFullName());
				}
				else
				{
					dataRow.add(null);
				}
			}

			/* 36_FullScientificName */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "scientificName,1"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getScientificName()
						.getFullScientificName());
			}

			/* 37_AuthorshipVerbatim */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "scientificnameAuthorship,1"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getScientificName()
						.getAuthorshipVerbatim());
			}

			/* 38_Sex */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "sex,1"))
			{
				dataRow.add(specimen.getSex());
			}

			/* 39_SpecificEpithet */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "specificEpithet,1"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getScientificName()
						.getSpecificEpithet());
			}

			/* 40_ProvinceState */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "stateProvince,1"))
			{
				dataRow.add(specimen.getGatheringEvent().getProvinceState());
			}

			/* 41_Subgenus */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "subGenus,1"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getScientificName().getSubgenus());
			}

			/* 42_TaxonRank */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "taxonRank,0"))
			{
				if (specimen.getIdentifications().iterator().next().getTaxonRank() != null)
				{
					dataRow.add(specimen.getIdentifications().iterator().next().getTaxonRank());
				}
				else
				{
					dataRow.add(null);
				}
			}

			/* 43_Remarks */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "taxonRemarks,0"))
			{
				if (specimen.getIdentifications().iterator().next().getRemarks() != null)
				{
					dataRow.add(specimen.getIdentifications().iterator().next().getRemarks());
				}
				else
				{
					dataRow.add(null);
				}
			}

			/* 44_TypeStatus */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "typeStatus,1"))
			{
				dataRow.add(specimen.getTypeStatus());
			}
				
			/* 45_Depth */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "verbatimDepth,1"))
			{
				if (specimen.getGatheringEvent().getDepth() != null)
				{
					dataRow.add(specimen.getGatheringEvent().getDepth());
				}
				else
				{
					dataRow.add(" ");
				}
			}
			
			/* 46_AltitudeUnifOfMeasurement */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "verbatimElevation,1"))
			{
				dataRow.add(specimen.getGatheringEvent().getAltitudeUnifOfMeasurement());
			}
			
			/* 47_Dummy7 */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "verbatimEventDate,1"))
			{
				if (specimen.getGatheringEvent().getDateTimeBegin() != null)
				{
					SimpleDateFormat datetimebegin = new SimpleDateFormat("yyyy-MM-dd");
					String datebegin = datetimebegin.format(specimen.getGatheringEvent().getDateTimeBegin());
					dataRow.add(datebegin);
				}
				else
				{
					dataRow.add(null);
				}
			}

			/* 48_TaxonRank */
			if (StringUtilities.isFieldChecked(MAPPING_FILE_NAME, "verbatimTaxonRank,1"))
			{
				dataRow.add(specimen.getIdentifications().iterator().next().getTaxonRank());
			}

			/**
			 * adding data row
			 */
			try
			{
				filewriter.WriteRow(dataRow);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

}
