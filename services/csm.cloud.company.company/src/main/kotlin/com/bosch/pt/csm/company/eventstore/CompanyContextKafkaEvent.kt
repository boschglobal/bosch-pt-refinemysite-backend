/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.AbstractKafkaEvent
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "company_kafka_event")
class CompanyContextKafkaEvent(
    traceHeaderKey: String,
    traceHeaderValue: String,
    partition: Int,
    key: ByteArray,
    payload: ByteArray?
) : AbstractKafkaEvent(traceHeaderKey, traceHeaderValue, partition, key, payload)
