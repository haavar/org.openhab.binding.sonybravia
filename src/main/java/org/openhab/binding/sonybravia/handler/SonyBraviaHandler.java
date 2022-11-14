/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sonybravia.handler;

import static org.openhab.binding.sonybravia.SonyBraviaBindingConstants.CHANNEL_POWER;

import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.openhab.binding.sonybravia.internal.SonyBraviaConfiguration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link SonyBraviaHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Haavar Valeur - Initial contribution
 */
// @NonNullByDefault
public class SonyBraviaHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SonyBraviaHandler.class);
    private ScheduledExecutorService executor;
    private Boolean previousPowerState;
    private Gson gson = new Gson();
    private HttpClient httpClient;

    @Nullable
    private SonyBraviaConfiguration config;

    public SonyBraviaHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_POWER)) {

            if (OnOffType.class.isAssignableFrom(command.getClass())) {
                setPowerStatus(command == OnOffType.ON);
            }
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(SonyBraviaConfiguration.class);

        logger.info("config ip={} PSK={} pullInterval={}", config.ipAddress, config.preSharedKey, config.pullInterval);
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

        httpClient = new HttpClient();
        try {
            httpClient.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Runnable runnable = () -> {
            try {
                boolean powerStatus = getPowerStatus();
                if (getThing().getStatus() != ThingStatus.ONLINE) {
                    logger.error("TV connection is restored");
                    updateStatus(ThingStatus.ONLINE);
                }
                if (previousPowerState == null || powerStatus != previousPowerState) {
                    previousPowerState = powerStatus;
                    OnOffType state = powerStatus ? OnOffType.ON : OnOffType.OFF;
                    logger.info("Updating power state to {} ", state);
                    updateState(CHANNEL_POWER, state);
                }
            } catch (ConnectException e) {
                logger.error("Unable to connect to TV");
                updateStatus(ThingStatus.OFFLINE);
            } catch (Exception e) {
                logger.error("Uncaught exception in status loop", e);
                updateStatus(ThingStatus.OFFLINE);
            }
        };
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(runnable, 0, config.pullInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() {
        logger.info("Disposing");
        executor.shutdown();

        try {
            httpClient.stop();
        } catch (Exception e) {
            logger.error("Exception while shutting down http client", e);
        }
    }

    private boolean getPowerStatus() throws Exception {
        StringContentProvider contentProvider = new StringContentProvider(
                "{\"method\":\"getPowerStatus\",\"params\":[],\"id\":1,\"version\":\"1.0\"}");
        ContentResponse response;
        try {
            response = httpClient.POST("http://" + config.ipAddress + "/sony/system").content(contentProvider).send();
        } catch (ExecutionException e) {
            if (e.getCause() != null && Exception.class.isAssignableFrom(e.getCause().getClass())) { // hack to expose
                                                                                                     // the
                                                                                                     // ConnectException
                throw (Exception) e.getCause();
            }
            throw e;
        }
        Map map = gson.fromJson(response.getContentAsString(), Map.class);
        String status = (String) ((Map) ((List) map.get("result")).get(0)).get("status");
        return "active".equals(status);
    }

    private void setPowerStatus(boolean status) {
        logger.info("set power status={}", status);

        StringContentProvider contentProvider = new StringContentProvider(
                "{\"id\": 2, \"method\": \"setPowerStatus\", \"version\": \"1.0\", \"params\": [{ \"status\": " + status
                        + " }]}");
        try {
            httpClient.POST("http://" + config.ipAddress + "/sony/system").content(contentProvider)
                    .header("X-Auth-PSK", config.preSharedKey).send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Exception while setting power status", e);
        }
    }
}
