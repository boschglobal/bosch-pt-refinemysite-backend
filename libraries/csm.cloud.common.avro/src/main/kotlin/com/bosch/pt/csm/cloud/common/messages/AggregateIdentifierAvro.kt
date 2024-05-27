/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.messages

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier

fun AggregateIdentifierAvro.buildAggregateIdentifier(version: Long = getVersion()) =
    AggregateIdentifier(type = getType(), identifier = getIdentifier().toUUID(), version = version)
