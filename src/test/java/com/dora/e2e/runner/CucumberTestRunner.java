package com.dora.e2e.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * JUnit Platform Suite that drives the Cucumber engine.
 *
 * <p>Run all scenarios:
 * <pre>mvn test</pre>
 *
 * <p>Run only LLD-02 scenarios:
 * <pre>mvn test -Dcucumber.filter.tags="@LLD-02"</pre>
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,
        value = "com.dora.e2e.steps,com.dora.e2e.support")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty,html:target/cucumber-reports/report.html,json:target/cucumber-reports/report.json")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip")
public class CucumberTestRunner {
    // intentionally empty — the @Suite annotations drive everything
}
