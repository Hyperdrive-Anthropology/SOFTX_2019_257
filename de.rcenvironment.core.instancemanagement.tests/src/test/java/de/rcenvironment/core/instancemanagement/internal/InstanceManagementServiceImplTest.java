/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.internal.ConfigurationStore;
import de.rcenvironment.core.configuration.internal.ConfigurationStoreImpl;
import de.rcenvironment.core.instancemanagement.InstanceConfigurationOperationSequence;
import de.rcenvironment.core.instancemanagement.internal.ConfigurationSegmentFactory.SegmentBuilder;
import de.rcenvironment.core.utils.common.ComparatorUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.receivers.LoggingTextOutReceiver;

/**
 * Unit tests for {@link InstanceManagementServiceImpl} that can be run without triggering heavy-weight I/O operations.
 * 
 * @author Robert Mischke
 * @author David Scholz
 * @author Brigitte Boden
 */
public class InstanceManagementServiceImplTest {

    private static final String CONFIG_PATH = "/instanceManagementTest/configuration.json";

    private static final String OK_ID = "ok";

    private static final String NOT_EXISTING_SHUTDOWN_ID = "bob";

    private static final String INVALID_ID_1 = "";

    private static final String INVALID_ID_2 = "..";

    /**
     * Expected exception.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private LoggingTextOutReceiver userOutputReceiver;

    private InstanceManagementServiceImpl imService;

    private SegmentBuilder segmentBuilder = ConfigurationSegmentFactory.getSegmentBuilder();

    private ConfigurationStore configStore;

    private InstanceConfigurationOperationSequence sequence;

    private TempFileService tempFileService;

    private File testFile;

    /**
     * Common setup.
     * 
     * @throws IOException on failure.
     */
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        File tempDir = tempFileService.createManagedTempDir();
        testFile = tempFileService.createTempFileWithFixedFilename("configuration.json");
        imService = new InstanceManagementServiceImpl();
        sequence = imService.newConfigurationOperationSequence();
        userOutputReceiver = new LoggingTextOutReceiver("");
        copyResourceToFile(CONFIG_PATH, testFile);
        configStore = new ConfigurationStoreImpl(testFile);
        imService.setProfilesRootDir(new File(tempDir.getParentFile().getPath()));
        imService.validateLocalConfig();
    }

    private void copyResourceToFile(String resourcePath, File file) throws IOException, FileNotFoundException {
        InputStream testDataStream = getClass().getResourceAsStream(resourcePath);
        assertNotNull("Expected test resource not found", testDataStream);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.copy(testDataStream, fos);
        }
    }

    /**
     * 
     * Clean up.
     * 
     * @throws IOException on failure.
     * @throws ConfigurationException on config failure.
     */
    @After
    public void cleanUp() throws IOException, ConfigurationException {

    }

    /**
     * Tests that stopping an instance, which doesn't exist, won't lead to new folder creation.
     * 
     * @throws IOException on expected failure.
     */
    @Test
    public void stopingNotExistingInstanceIsProperlyHandled() {
        List<String> paramList = new ArrayList<>();
        Exception exception = null;
        paramList.add(NOT_EXISTING_SHUTDOWN_ID);
        try {
            imService.stopInstance(paramList, userOutputReceiver, 0);
        } catch (IOException e) {
            exception = e;
        } finally {
            assertTrue(exception != null);
            assertTrue(exception.getMessage().contains("tried to shutdown instance, which doesn't exist"));
            final File profileDir = new File(imService.getProfilesRootDir(), NOT_EXISTING_SHUTDOWN_ID);
            assertFalse(profileDir.exists());
        }

        // fallback solution if {@link InstanceManagementServiceImpl#stopInstance(List<String>, String, TextOutputReceiver)} method
        // actually succeeds.
        assertTrue(exception != null);
    }

    /**
     * Tests that identical ids in the parameter list of the {@link InstanceManagementServiceImpl#startInstance(List<String>, String,
     * de.rcenvironment.core.utils.common.textstream.TextOutputReceiver)} method are rejected.
     * 
     * @throws IOException on expected failure.
     */
    @Test
    public void identicalIdsAreProperlyRejected() {
        List<String> paramList = new ArrayList<>();
        List<Exception> exceptionList = new ArrayList<>();
        paramList.add(OK_ID);
        paramList.add(OK_ID);
        String message = "";
        boolean success = false;
        try {
            imService.startInstance(OK_ID, paramList, userOutputReceiver, 0, false);
        } catch (IOException e) {
            exceptionList.add(e);
        } finally {
            try {
                imService.stopInstance(paramList, userOutputReceiver, 0);
            } catch (IOException e) {
                exceptionList.add(e);
            } finally {
                assertTrue(exceptionList.size() == 2);
                for (Exception e : exceptionList) {
                    message = e.getMessage();
                    if (message.contains("multiple instances with identical id")) {
                        success = true;
                        break;
                    }
                }
            }
        }
        if (success) {
            assertTrue(message.contains("multiple instances with identical id"));
        }
        // fallback solution if {@link InstanceManagementServiceImpl#startInstance(List<String>, String, TextOutputReceiver)} method
        // actually succeeds.
        assertTrue(exceptionList.size() == 2);
    }

    /**
     * Tests that malformed instance ids are properly rejected by all public methods, ie an exception with an appropriate text is thrown.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void invalidInstanceIdsAreProperlyRejected() throws IOException {
        List<String> paramList = new ArrayList<>();

        paramList.add(INVALID_ID_1);

        thrown.expect(IOException.class);
        thrown.expectMessage("Malformed id:");

        imService.stopInstance(paramList, userOutputReceiver, 0);

        paramList.remove(INVALID_ID_1);
        paramList.add(INVALID_ID_2);

        imService.stopInstance(paramList, userOutputReceiver, 0);

        paramList.remove(INVALID_ID_2);
        paramList.add(null);

        thrown.expectMessage("Malformed command: either no installation id or instance id defined.");
        imService.stopInstance(paramList, userOutputReceiver, 0);
    }

    /**
     * Tests that malformed installation ids are properly rejected by all public methods, ie an exception with an appropriate text is
     * thrown.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void invalidInstalaltionIdsAreProperlyRejected() throws IOException {
        List<String> paramList = new ArrayList<>();

        paramList.add(OK_ID);

        thrown.expect(IOException.class);
        thrown.expectMessage("Malformed command: either no installation id or instance id defined.");

        imService.startInstance(null, paramList, userOutputReceiver, 0, false);

        String id = "Berta";
        thrown.expectMessage("Installation with id: " + id + " does not exist.");
        paramList.add(id);
        imService.startInstance(OK_ID, paramList, userOutputReceiver, 0, false);
    }

    /**
     * 
     * Test if resetting the config works correctly.
     * 
     * @throws InstanceConfigurationException on failure.
     * @throws IOException on I/O specific failures.
     */
    @Test
    public void testConfigureOperationReset() throws InstanceConfigurationException, IOException {
        // First write something to the config
        final String testName = "Bruno";
        sequence.setName(testName);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        sequence = imService.newConfigurationOperationSequence();
        sequence.resetConfiguration();
        // Now reset config
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertNull(segment.getString(segmentBuilder.general().instanceName().getConfigurationKey()));
    }

    /**
     * 
     * Test if setting the instance name works correctly.
     * 
     * @throws InstanceConfigurationException on failure.
     * @throws IOException on I/O specific failures.
     */
    @Test
    public void testConfigureOperationSetInstanceName() throws InstanceConfigurationException, IOException {
        final String testName = "Bruno";
        sequence.setName(testName);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getString(segmentBuilder.general().instanceName().getConfigurationKey()), testName);
    }

    /**
     * 
     * Test if setting the instance comment works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationSetInstanceComment() throws InstanceConfigurationException, IOException {
        final String testComment = "some comment";
        sequence.setComment(testComment);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getString(segmentBuilder.general().comment().getConfigurationKey()), testComment);
    }

    /**
     * 
     * Test if setting the workflow host flag works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationSetWorkflowHost() throws InstanceConfigurationException, IOException {
        final boolean testValue = true;
        sequence.setWorkflowHostFlag(testValue);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.general().isWorkflowHost().getConfigurationKey()), testValue);
    }

    /**
     * 
     * Test if setting the relay flag works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationSetRelayFlag() throws InstanceConfigurationException, IOException {
        sequence.setRelayFlag(true);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.general().isRelay().getConfigurationKey()), true);
    }

    /**
     * 
     * Test if setting the temp directory works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationSetTempDir() throws InstanceConfigurationException, IOException {
        final String testPath = "Rachel";
        sequence.setTempDirPath(testPath);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getString(segmentBuilder.general().tempDirectory().getConfigurationKey()), testPath);
    }

    /**
     * 
     * Test if setting the background monitoring works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testEnableBackgroundMonitoring() throws InstanceConfigurationException, IOException {
        String id = "Donna";
        int interval = 2;
        sequence.setBackgroundMonitoring(id, interval);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.backgroundMonitoring().getPath());
        assertEquals(segment.getString(segmentBuilder.backgroundMonitoring().enableIds().getConfigurationKey()), "Donna");
        assertTrue(segment.getInteger(segmentBuilder.backgroundMonitoring().intervalSeconds().getConfigurationKey()) == 2);
    }

    /**
     * 
     * Test if setting the request timeout works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testSettingRequestTimeout() throws InstanceConfigurationException, IOException {
        final long niceValue = 42;
        sequence.setRequestTimeout(niceValue);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().getPath());
        assertTrue(segment.getLong(segmentBuilder.network().requestTimeoutMsec().getConfigurationKey()) == niceValue);
    }

    /**
     * 
     * Test if setting the forwarding timeout works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testSettingForwardingTimeout() throws InstanceConfigurationException, IOException {
        final long niceValue = 42;
        sequence.setForwardingTimeout(niceValue);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().getPath());
        assertTrue(segment.getLong(segmentBuilder.network().forwardingTimeoutMsec().getConfigurationKey()) == niceValue);
    }

    /**
     * 
     * Test if adding a new connection and removing a connection works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationsAddAndRemoveConnection() throws InstanceConfigurationException, IOException {
        final String name = "Mike";
        final String host = "Ross";
        final int port = 42;
        final boolean connectOnStartup = false;
        final int autoRetrylInitDelay = 42;
        final int autoRetryMax = 42;
        final float multiplier = 2.7f;
        sequence.addNetworkConnection(name, host, port, connectOnStartup, autoRetrylInitDelay, autoRetryMax, multiplier);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        final double maxDoubleDelta = 0.0001;
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().connections().getOrCreateConnection(name).getPath());
        assertTrue(ComparatorUtils.doubleEqualWithinDelta(
            segment.getDouble(segmentBuilder.network().connections().getOrCreateConnection(name).autoRetryDelayMultiplier()
                .getConfigurationKey()),
            multiplier, maxDoubleDelta));
        assertTrue(segment.getLong(segmentBuilder.network().connections().getOrCreateConnection(name).autoRetryInitialDelay()
            .getConfigurationKey()) == autoRetrylInitDelay);
        assertTrue(segment.getLong(segmentBuilder.network().connections().getOrCreateConnection(name).autoRetryMaximumDelay()
            .getConfigurationKey()) == autoRetryMax);
        assertEquals(
            segment.getBoolean(segmentBuilder.network().connections().getOrCreateConnection(name).connectOnStartup().getConfigurationKey()),
            false);
        assertTrue(segment.getInteger(segmentBuilder.network().connections().getOrCreateConnection(name).port()
            .getConfigurationKey()) == port);
        assertEquals(segment.getString(segmentBuilder.network().connections().getOrCreateConnection(name).host()
            .getConfigurationKey()), host);
        
        
        //Remove the connection
        sequence = imService.newConfigurationOperationSequence();
        sequence.removeConnection(name);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        root = configStore.getSnapshotOfRootSegment();
        segment = root.getSubSegment(segmentBuilder.network().connections().getOrCreateConnection(name).getPath());
        assertFalse(segment.isPresentInCurrentConfiguration());
    }

    /**
     * 
     * Test if adding a new server port works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationAddServerPort() throws InstanceConfigurationException, IOException {
        String name = "sillyPort";
        String ip = "niceIP";
        final int port = 42;
        sequence.addServerPort(name, ip, port);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().ports().getOrCreateServerPort(name).getPath());
        assertEquals(segment.getString(segmentBuilder.network().ports().getOrCreateServerPort(name).ip().getConfigurationKey()), ip);
        assertEquals(Integer.valueOf(port),
            segment.getInteger(segmentBuilder.network().ports().getOrCreateServerPort(name).port().getConfigurationKey()));
    }

    /**
     * 
     * Test if adding and removing an allowed ip works correctly.
     * 
     * @throws IOException on failure.
     * @throws ConfigurationException on config failure.
     */
    @Test
    public void testConfigureOperationsAddAndRemoveAllowedIP() throws IOException, ConfigurationException {
        //Add an allowed IP
        String ip = "Jessica";
        sequence.addAllowedInboundIP(ip);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().ipFilter().getPath());
        assertTrue(segment.getStringArray(segmentBuilder.network().ipFilter().allowedIps().getConfigurationKey()).contains(ip));
   
        //Remove the allowed IP
        sequence = imService.newConfigurationOperationSequence();
        sequence.removeAllowedInboundIP(ip);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        root = configStore.getSnapshotOfRootSegment();
        segment = root.getSubSegment(segmentBuilder.network().ipFilter().getPath());
        assertFalse(segment.getStringArray(segmentBuilder.network().ipFilter().allowedIps().getConfigurationKey()).contains(ip));
    }


    /**
     * 
     * Test if adding and removing an ssh connection works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationAddSshConnection() throws InstanceConfigurationException, IOException {
        
        //Add SSH connection
        final String name = "niceConnection";
        final String displayName = "nice";
        final String host = "bestHost";
        final int port = 42;
        final String loginName = "nicerLoginName";

        sequence.addSshConnection(name, displayName, host, port, loginName);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment =
            root.getSubSegment(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).getPath());

        assertEquals(
            segment.getString(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).displayName()
                .getConfigurationKey()),
            displayName);
        assertEquals(segment.getString(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).host()
            .getConfigurationKey()), host);
        assertEquals(
            segment.getString(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).loginName()
                .getConfigurationKey()),
            loginName);
        assertTrue(segment.getInteger(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).port()
            .getConfigurationKey()) == port);
        
        //Remove the connection again
        sequence = imService.newConfigurationOperationSequence();
        sequence.removeSshConnection(name);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        root = configStore.getSnapshotOfRootSegment();
        segment =
            root.getSubSegment(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).getPath());
        assertFalse(segment.isPresentInCurrentConfiguration());
    }
    

    /**
     * 
     * Test if publishing and unpublishing a component works correctly.
     * 
     * @throws IOException on failure.
     * @throws ConfigurationException on config failure.
     */
    @Test
    public void testConfigureOperationsAddAndRemovePublishedComponent() throws IOException, ConfigurationException {
        //Publish component
        String component = "heftigeKomponente";
        sequence.publishComponent(component);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.publishing().getPath());
        assertTrue(segment.getStringArray(segmentBuilder.publishing().components().getConfigurationKey()).contains(component));
   
        //Unpublish it again
        sequence = imService.newConfigurationOperationSequence();
        sequence.unpublishComponent(component);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        root = configStore.getSnapshotOfRootSegment();
        segment = root.getSubSegment(segmentBuilder.publishing().getPath());
        assertFalse(segment.getStringArray(segmentBuilder.publishing().components().getConfigurationKey()).contains(component));
    }

    /**
     * Test if setting up the SSH server works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationEnableSshServer() throws InstanceConfigurationException, IOException {
        final String ip = "1.2.3.4";
        final int port = 22222;
        sequence.enableSshServer(ip, port);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.sshServer().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.sshServer().enabled().getConfigurationKey()), true);
        assertEquals(segment.getString(segmentBuilder.sshServer().ip().getConfigurationKey()), ip);
        assertTrue(segment.getInteger(segmentBuilder.sshServer().port().getConfigurationKey()) == port);
    }

    /**
     * Test if disabling the SSH server works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationDisableSshServer() throws InstanceConfigurationException, IOException {
        sequence.disableSshServer();
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.sshServer().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.sshServer().enabled().getConfigurationKey()), false);
    }

    /**
     * 
     * Test if setting the ip filter flag works correctly.
     * 
     * @throws IOException on failure.
     * @throws InstanceConfigurationException .
     */
    @Test
    public void testConfigureOperationSetIpFilterFlag() throws InstanceConfigurationException, IOException {
        sequence.setIpFilterEnabled(true);
        imService.applyInstanceConfigurationOperations(testFile.getParentFile().getName(), sequence, userOutputReceiver);

        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().ipFilter().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.network().ipFilter().enabled().getConfigurationKey()), true);
    }

}
