/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.spi;

import org.eclipse.swt.graphics.Image;

/**
 * A tree element generated by a {@link NetworkViewContributor} that can be queried for its text, image, and child presence without an
 * external content or label provider. Fetching the actual children is still done by the {@link NetworkViewContributor}.
 * 
 * @author Robert Mischke
 */
public interface SelfRenderingNetworkViewNode extends ContributedNetworkViewNode {

    /**
     * @return the visible text for this node
     */
    String getText();

    /**
     * @return the visible {@link Image} for this node; make sure to reuse/share Image instances where possible
     */
    Image getImage();

    /**
     * @return true if this element has children (or if this cannot be known in advance, if it *may* have them)
     */
    boolean getHasChildren();

}
