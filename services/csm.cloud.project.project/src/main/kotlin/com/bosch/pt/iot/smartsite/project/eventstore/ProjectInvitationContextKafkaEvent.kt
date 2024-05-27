/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.AbstractKafkaEvent
import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "invitation_kafka_event")
class ProjectInvitationContextKafkaEvent(
    key: ByteArray,
    payload: ByteArray?,
    partition: Int,
    transactionId: UUID?,
    traceHeaderKey: String = "",
    traceHeaderValue: String = "",
) : AbstractKafkaEvent(traceHeaderKey, traceHeaderValue, partition, key, payload, transactionId)
