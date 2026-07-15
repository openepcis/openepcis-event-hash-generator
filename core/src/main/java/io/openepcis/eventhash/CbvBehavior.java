/*
 * Copyright 2022-2026 benelog GmbH & Co. KG
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package io.openepcis.eventhash;

import io.openepcis.constants.CBVVersion;

import java.util.Map;

/**
 * Encapsulates all CBV-version-specific behaviour used during pre-hash canonicalization and
 * hash-id construction. Adding a new CBV version is intended to be a single new constant +
 * a single new {@link #REGISTRY} entry — never edits to {@link ContextNode} or
 * {@link HashIdGenerator}.
 *
 * @param inlineUserExtensions  whether user-extension fields appear inline within the
 *                              standard EPCIS-field stream (true in CBV 2.1) or are appended
 *                              in a separate pass at the end (false in CBV 2.0).
 * @param keepSensorElementList whether the {@code sensorElementList} wrapper tag is included
 *                              in the pre-hash output (true in CBV 2.1, false in CBV 2.0).
 * @param hashUriSuffix         suffix appended to the hash-id URI, e.g. "?ver=CBV2.1".
 */
public record CbvBehavior(boolean inlineUserExtensions, boolean keepSensorElementList, String hashUriSuffix) {

    //  Behaviour configuration for CBV 2.0.
    public static final CbvBehavior CBV_2_0 = new CbvBehavior(false, false, "?ver=CBV2.0");

    //  Behaviour configuration for CBV 2.1.
    public static final CbvBehavior CBV_2_1 = new CbvBehavior(true, true, "?ver=CBV2.1");

    // Default value for the CBV for hash generation current default: 2.0; change only this line to promote to 2.1 / 2.2 later.
    public static final CBVVersion DEFAULT_VERSION = CBVVersion.VERSION_2_0_0;

    /**
     * New CBV versions should be added here explicitly rather than relying on the {@link #DEFAULT} fallback.
     */
    private static final Map<CBVVersion, CbvBehavior> REGISTRY = Map.of(CBVVersion.VERSION_2_0_0, CBV_2_0, CBVVersion.VERSION_2_1_0, CBV_2_1);

    // Default: Behaviour applied when a caller passes an unknown CBV version — Always the latest known version
    public static final CbvBehavior DEFAULT = REGISTRY.get(DEFAULT_VERSION);


    /**
     * Returns the behaviour configuration for a given CBV version, or {@link #DEFAULT} when the version is not registered.
     */
    public static CbvBehavior of(final CBVVersion version) {
        return REGISTRY.getOrDefault(version, DEFAULT);
    }
}
