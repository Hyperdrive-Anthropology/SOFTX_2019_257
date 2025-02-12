/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import java.io.IOException;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;

/**
 * Ensures that the actual configuration values are applied.
 * 
 * @author Doreen Seider
 */
public class LoggingConfiguredProperlyValidator extends DefaultInstanceValidator {

    private ConfigurationAdmin configurationAdmin;

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "Logging configuration";
        
        boolean isConfiguredProperly = false;
        Configuration paxLoggingConfiguration = null;
        try {
            String paxLoggingPid = "org.ops4j.pax.logging";
            paxLoggingConfiguration = configurationAdmin.getConfiguration(paxLoggingPid);
        } catch (IOException e) {
            // Failed to get configuration of pax logging from the configuration admin service. Most likely, logging is not configured
            // properly.
            return createInstanceValidationResultForFailure(validationDisplayName);
        }
        // TODO (p2) - in the remaining error cases, does this property actually detect them?
        String nonDefaultPaxConfigKey = "log4j.appender.CONSOLE";
        isConfiguredProperly = paxLoggingConfiguration.getProperties() != null
            && paxLoggingConfiguration.getProperties().get(nonDefaultPaxConfigKey) != null;            
        if (!isConfiguredProperly) {
            return createInstanceValidationResultForFailure(validationDisplayName);
        }

        return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
    }
    
    private InstanceValidationResult createInstanceValidationResultForFailure(String validationDisplayName) {
        // TODO (p2) - this message is obsolete, update it with any remaining failure cases and info - March 2018
        final String errorMessage1 = "Failed to initialize background logging properly."
            + " Most likely, because RCE was started from another directory than its installation directory. "
            + "(The installation directory is the directory that contains the 'rce' executable.)";
        final String errorMessage2 = " It is recommended to start RCE again from its installation directory.";
        return InstanceValidationResultFactory.createResultForFailureWhichAllowsToProceed(validationDisplayName,
            errorMessage1 + " " + errorMessage2, errorMessage1 + "\n\n" + errorMessage2);
    }
    
    protected void bindConfigurationAdmin(ConfigurationAdmin newService) {
        configurationAdmin = newService;
    }
    
    

}
