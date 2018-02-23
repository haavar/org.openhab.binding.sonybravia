/**
 * Copyright (c) 2018,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sonybravia.handler;

import static org.openhab.binding.sonybravia.SonyBraviaBindingConstants.*;


import com.google.gson.Gson;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.sonybravia.internal.SonyBraviaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The {@link SonyBraviaHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Haavar Valeur - Initial contribution
 */
 @NonNullByDefault
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

        logger.info("config ip=" + config.ipAddress + " PSK=" + config.preSharedKey + " pullInterval=" + config.pullInterval);
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
            Boolean powerStatus = getPowerStatus();
            if (powerStatus != previousPowerState) {
                previousPowerState = powerStatus;
                OnOffType state = powerStatus ? OnOffType.ON : OnOffType.OFF;
                logger.info("Updating power state to " + state);
                updateState(CHANNEL_POWER, state);
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


    private boolean getPowerStatus() {
        StringContentProvider contentProvider = new StringContentProvider("{\"method\":\"getPowerStatus\",\"params\":[],\"id\":1,\"version\":\"1.0\"}");
        ContentResponse response = null;
        try {
            response = httpClient.POST("http://" + config.ipAddress + "/sony/system").content(contentProvider).send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Exception while getting power status", e);
            return false;
        }
        Map map = gson.fromJson(response.getContentAsString(), Map.class);
        String status = (String)((Map)((List)map.get("result")).get(0)).get("status");
        return "active".equals(status);

    }

    private void setPowerStatus(boolean status) {
        logger.info("set power status=" + status);

        StringContentProvider contentProvider = new StringContentProvider("{\"id\": 2, \"method\": \"setPowerStatus\", \"version\": \"1.0\", \"params\": [{ \"status\": " + status + " }]}");
        try {
            httpClient.POST("http://" + config.ipAddress + "/sony/system").content(contentProvider).header("X-Auth-PSK", config.preSharedKey).send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Exception while setting power status", e);
        }

    }
}
