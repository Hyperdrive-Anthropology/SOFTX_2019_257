/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.wizards.exampleproject.internal;

import de.rcenvironment.core.gui.wizards.exampleproject.NewExampleProjectWizard;

/**
 * Wizard for the RCE examples project.
 * @author Sascha Zur
 */
public class RCEExampleProjectWizard extends NewExampleProjectWizard{

    @Override
    public String getPluginID() {
        return "de.rcenvironment.core.gui.wizards.exampleproject";
    }

    @Override
    public String getTemplateFoldername() {
        return "workflows_examples";
    }

    @Override
    public String getProjectDefaultName() {
        return "Workflow Examples Project";
    }

}
