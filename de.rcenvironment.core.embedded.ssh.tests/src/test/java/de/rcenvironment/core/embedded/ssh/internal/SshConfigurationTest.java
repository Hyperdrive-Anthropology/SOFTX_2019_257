/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test for the class SshConfiguration.
 * 
 * @author Sebastian Holtappels (validation)
 * @author Robert Mischke (new JSON loading)
 */
public class SshConfigurationTest extends TestCase {

    private static final String TEST_FILE_HOST_VALUE = "127.0.0.2";

    private static final int TEST_FILE_PORT_VALUE = 31007;

    private Log logger = LogFactory.getLog(SshConfigurationTest.class);

    /**
     * Tests default values when using the default constructor.
     * 
     * @throws IOException on uncaught exceptions
     */
    @Test
    public void testDefaultConstructor() throws IOException {
        SshConfiguration configuration = new SshConfiguration();
        verifyDefaultValues(configuration);
    }

    /**
     * Tests default values when providing an empty {@link ConfigurationSegment}.
     * 
     * @throws IOException on uncaught exceptions
     * @throws ConfigurationException on configuraion exceptions
     */
    @Test
    public void testEmptyConfiguration() throws IOException, ConfigurationException {
        ConfigurationSegment configurationSegment = ConfigurationSegmentUtils.createEmptySegment();
        SshConfiguration configuration = new SshConfiguration(configurationSegment);
        verifyDefaultValues(configuration);
    }

    /**
     * Tests loading a test configuration file.
     * 
     * @throws IOException on uncaught exceptions
     * @throws ConfigurationException on configuraion exceptions.*/
    @Test
    public void testLoading() throws IOException, ConfigurationException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        ConfigurationSegment configurationSegment =
            ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream("/sample1.json"));
        SshConfiguration configuration = new SshConfiguration(configurationSegment);

        assertEquals(true, configuration.isEnabled());
        assertEquals(TEST_FILE_PORT_VALUE, configuration.getPort());
        assertEquals(TEST_FILE_HOST_VALUE, configuration.getHost());
        assertEquals(4, configuration.getAccounts().size());
    }

    /** Test. */
    @Test
    public void testValidationPositive() {
        SshConfiguration validConfig = SshTestUtils.getValidConfig();
        assertTrue("SshConfiguration.validateConfiguration returned false but true was expected",
            validConfig.validateConfiguration(logger));
    }

    /** Test. */
    @Test
    public void testValidationPortnumber() {
        SshConfiguration config = SshTestUtils.getValidConfig();
        config.setPort(Integer.MIN_VALUE);
        assertFalse("SshConfiguration.validateConfiguration returned true but false was expected",
            config.validateConfiguration(logger));
        config.setPort(Integer.MAX_VALUE);
        assertFalse("SshConfiguration.validateConfiguration returned true but false was expected",
            config.validateConfiguration(logger));
    }

    /** Test. */
    @Test
    public void testValidationNoUsers() {
        SshConfiguration config = SshTestUtils.getValidConfig();
        config.setAccounts(null);
        assertFalse("SshConfiguration.validateConfiguration() returned true but false was expected",
            config.validateConfiguration(logger));
        config.setAccounts(new ArrayList<SshAccountImpl>());
        assertFalse("SshConfiguration.validateConfiguration() returned true but false was expected",
            config.validateConfiguration(logger));
    }

    /** Test. */
    @Test
    public void testValidationDuplicateUsers() {
        SshConfiguration config = SshTestUtils.getValidConfig();
        List<SshAccountImpl> users = config.getAccounts();
        if (users.addAll(users)) {
            config.setAccounts(users);
            assertFalse("SshConfiguration.validateConfiguration() returned true but false was expected.",
                config.validateConfiguration(logger));
        } else {
            fail("Could not create duplicates for users");
        }
    }

    private void verifyDefaultValues(SshConfiguration configuration) {
        assertEquals(false, configuration.isEnabled());
        assertEquals(SshConfiguration.DEFAULT_PORT, configuration.getPort());
        assertEquals(SshConfiguration.DEFAULT_HOST, configuration.getHost());
        assertEquals(0, configuration.getAccounts().size());
    }
}
