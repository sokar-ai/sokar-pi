package org.fuin.sokar.agent.impl.pi.acceptance;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runs the feature files against a real machine that has this agent's package installed.
 * <p>
 * The glue is Sokar's acceptance kit, and only that: every step these scenarios use is one the
 * kit provides, so this repository carries scenarios and no test code of its own to hold them.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("org/fuin/sokar/agent/impl/pi/acceptance")
@ConfigurationParameter(key = "cucumber.glue", value = "org.fuin.sokar.acceptance")
public class AcceptanceIT {
}
