/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.eventstore

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import jakarta.persistence.Column
import jakarta.persistence.Lob
import jakarta.persistence.MappedSuperclass
import java.util.UUID

/**
 * Abstract implementation of a JPA entity representing the kafka events table from where they are
 * transmitted to kafka via the external kafka connector. A sub-class of this one is used by an
 * implementation of [AbstractEventStore] to define the table name that those events are stored in.
 */
@MappedSuperclass
abstract class AbstractKafkaEvent(
    // can be val later once all services are migrated
    @Column(nullable = false) var traceHeaderKey: String,
    // can be val later once all services are migrated
    @Column(nullable = false) var traceHeaderValue: String,
    @Column(nullable = false) val partitionNumber: Int,
    @Column(nullable = false, length = MAX_EVENT_KEY_SIZE) @Lob val eventKey: ByteArray,
    @Column(length = MAX_EVENT_SIZE) @Lob val event: ByteArray? = null,
    @Column(length = 36) val transactionIdentifier: UUID? = null
) : AbstractPersistable<Long>()

private const val MEGABYTE = 1024 * 1024

/**
 * Maximum size of a kafka message, see:
 * https://docs.confluent.io/cloud/current/clusters/cluster-types.html#standard-cluster-limits-per-partition
 */
private const val MAX_KAFKA_MESSAGE_SIZE = 8 * MEGABYTE

/** Size buffer to account for kafka message overhead like headers and others */
private const val BUFFER = 1 * MEGABYTE
private const val MAX_EVENT_KEY_SIZE = 1 * MEGABYTE
private const val MAX_EVENT_SIZE = MAX_KAFKA_MESSAGE_SIZE - MAX_EVENT_KEY_SIZE - BUFFER
