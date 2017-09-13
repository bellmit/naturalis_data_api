package nl.naturalis.nba.api;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
//@formatter:off
@SuiteClasses({
	PathTest.class,
	ApiUtilTest.class,
	QueryConditionTest.class,
	QuerySpecTest.class
})
//@formatter:on
public class AllTests {

}
