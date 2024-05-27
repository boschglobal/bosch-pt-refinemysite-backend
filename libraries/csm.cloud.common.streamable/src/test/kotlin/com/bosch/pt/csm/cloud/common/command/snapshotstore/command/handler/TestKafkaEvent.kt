/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore.command.handler

import com.bosch.pt.csm.cloud.common.eventstore.AbstractKafkaEvent
import java.util.UUID

class TestKafkaEvent(
    traceHeaderKey: String,
    traceHeaderValue: String,
    partitionNumber: Int,
    eventKey: ByteArray,
    event: ByteArray? = null,
    transactionIdentifier: UUID? = null
) :
    AbstractKafkaEvent(
        traceHeaderKey, traceHeaderValue, partitionNumber, eventKey, event, transactionIdentifier)
