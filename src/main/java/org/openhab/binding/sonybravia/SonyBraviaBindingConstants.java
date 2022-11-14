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
package org.openhab.binding.sonybravia;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link SonyBraviaBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Haavar Valeur - Initial contribution
 */
@NonNullByDefault
public class SonyBraviaBindingConstants {

    private static final String BINDING_ID = "sonybravia";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_TV = new ThingTypeUID(BINDING_ID, "tv");

    // List of all Channel ids
    public static final String CHANNEL_POWER = "power";
}
