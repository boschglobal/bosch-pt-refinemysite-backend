/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.AbstractKafkaEvent
import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "user_kafka_event")
class UserContextKafkaEvent(
    traceHeaderKey: String,
    traceHeaderValue: String,
    partition: Int,
    key: ByteArray,
    payload: ByteArray?,
    transactionIdentifier: UUID? = null
) :
    AbstractKafkaEvent(
        traceHeaderKey, traceHeaderValue, partition, key, payload, transactionIdentifier)
