package nl.naturalis.nba.dao;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	SpecimenDaoTest_Aggregations.class,
	SpecimenDaoTest_Between.class,
	SpecimenDaoTest_Equals.class,
	// TODO: re-enable geoqueries test
	//SpecimenDaoTest_GeoQueries.class,
	SpecimenDaoTest_In.class,
	SpecimenDaoTest_IsNotNull.class,
	SpecimenDaoTest_IsNull.class,
	SpecimenDaoTest_LessThan.class,
	SpecimenDaoTest_Like.class,
	SpecimenDaoTest_Matches.class,
	SpecimenDaoTest_Miscellaneous.class,
	SpecimenDaoTest_SortingSizingPaging.class,
	SpecimenDaoTest_Dwca.class
})

public class SpecimenDaoTest_AllTests_Test {}
