package com.govener.e2e;

import org.junit.platform.suite.api.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.glue", value = "com.govener.e2e")
@ConfigurationParameter(
    key = "cucumber.plugin",
    value = "pretty,json:target/cucumber/report.json,html:target/cucumber/report.html")
@ConfigurationParameter(key = "cucumber.publish.quiet", value = "true")
public class AcceptanceIT {}
