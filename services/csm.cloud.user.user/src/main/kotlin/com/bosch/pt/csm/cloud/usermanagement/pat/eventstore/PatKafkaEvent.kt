/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.AbstractKafkaEvent
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "pat_kafka_event")
@Suppress("SerialVersionUIDInSerializableClass")
class PatKafkaEvent(
    traceHeaderKey: String,
    traceHeaderValue: String,
    partition: Int,
    key: ByteArray,
    payload: ByteArray?,
    transactionIdentifier: UUID? = null
) :
    AbstractKafkaEvent(
        traceHeaderKey,
        traceHeaderValue,
        partition,
        key,
        payload,
        transactionIdentifier,
    )
