/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.common.internal.UplinkProtocolMessageConverter;
import de.rcenvironment.core.communication.uplink.network.api.AsyncMessageBlockSender;
import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.communication.uplink.network.channel.api.ChannelEndpoint;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * The common base class of all Uplink channel endpoints. The responsibilities of this base class are:
 * <ul>
 * <li>Provide a framework for in-order message block processing with channel state tracking (not used for the default channel)
 * <li>Basic error/exception handling and initiating session teardown on errors
 * <li>Generalizing logging with session ids, including providing labeled {@link UplinkProtocolMessageConverter} instances
 * </ul>
 * 
 * @author Robert Mischke
 */
public abstract class AbstractChannelEndpoint implements ChannelEndpoint {

    private static final boolean DEBUG_OUTPUT_ENABLED = DebugSettings.getVerboseLoggingEnabled("uplink.endpoints");

    protected final AsyncMessageBlockSender asyncMessageBlockSender;

    protected final UplinkProtocolMessageConverter messageConverter;

    protected final String sessionId;

    protected final long channelId;

    protected final Log log = LogFactory.getLog(getClass());

    public AbstractChannelEndpoint(AsyncMessageBlockSender asyncMessageBlockSender, String sessionId, long channelId) {
        this.asyncMessageBlockSender = asyncMessageBlockSender;
        this.sessionId = sessionId;
        this.messageConverter = new UplinkProtocolMessageConverter(this.sessionId + "/c" + channelId);
        this.channelId = channelId;
    }

    @Override
    public final void processMessage(MessageBlock messageBlock) throws IOException {
        if (DEBUG_OUTPUT_ENABLED) {
            log.debug(StringUtils.format("Processing a message of type %s in channel '%s'", messageBlock.getType(), sessionId));
        }
        final MessageType messageType = messageBlock.getType();
        if (messageType == MessageType.HEARTBEAT) {
            // attempt to send a response no matter the local session state
            try {
                // implicitly send the response to the channel the heartbeat was received from; typically, this is the default channel
                enqueueMessageBlockForSending(new MessageBlock(MessageType.HEARTBEAT_RESPONSE), MessageBlockPriority.HIGH);
            } catch (IOException e) {
                log.debug("Error attempting to send an Uplink heartbeat response: " + e.toString());
            }
            // this could received further processing in the future; for now, only send the response
            return;
        } else if (messageType == MessageType.HEARTBEAT_RESPONSE) {
            // heartbeat messages are currently supposed to be handled on the session level only, so log any that come through
            log.warn("Unexpected " + MessageType.HEARTBEAT_RESPONSE.name() + " message at channel endpoint " + sessionId);
            return;
        }
        try {
            synchronized (this) { // ensure state visibility when switching execution threads
                if (!processMessageInternal(messageBlock)) {
                    if (DEBUG_OUTPUT_ENABLED) {
                        log.debug("Uplink channel " + channelId + " is terminating");
                    }
                    // TODO additional steps to actually close/delete the channel?
                }
            }
        } catch (IOException e) {
            // wrap with context information
            throw new IOException(
                StringUtils.format("Error while processing a message of type %s in channel '%s'", messageBlock.getType(), sessionId), e);
        }
    }

    /**
     * @param message the received {@link MessageBlock}
     * @return whether further messages should be processed, i.e. returning false means that this channel shut be shut down
     * @throws IOException on failure to process the message
     */
    protected abstract boolean processMessageInternal(MessageBlock message) throws IOException;

    /**
     * Sends a {@link MessageBlock} the the stored channel id of this channel endpoint with {@link MessageBlockPriority#DEFAULT} priority.
     * 
     * @param messageBlock the {@link MessageBlock} to send
     * @throws IOException on errors or interruption while waiting to enqueue this message
     */
    protected final void enqueueMessageBlockForSending(MessageBlock messageBlock) throws IOException {
        asyncMessageBlockSender.enqueueMessageBlockForSending(channelId, messageBlock);
    }

    /**
     * Sends a {@link MessageBlock} the the stored channel id of this channel endpoint with the given {@link MessageBlockPriority}.
     * 
     * @param messageBlock the {@link MessageBlock} to send
     * @param priority the {@link MessageBlockPriority} to use for scheduling
     * @throws IOException on errors or interruption while waiting to enqueue this message
     */
    protected final void enqueueMessageBlockForSending(MessageBlock messageBlock, MessageBlockPriority priority) throws IOException {
        asyncMessageBlockSender.enqueueMessageBlockForSending(channelId, messageBlock, priority);
    }

    protected final boolean refuseUnexpectedMessageType(MessageBlock message) throws ProtocolException {
        log.error("Received an invalid or unexpected message of type " + message.getType() + " from session "
            + sessionId + "; the session will be terminated");
        return false;
    }

}
