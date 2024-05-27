/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.api

import java.time.LocalDateTime

sealed class JobEvent(
    open val timestamp: LocalDateTime,
    open val aggregateIdentifier: JobIdentifier,
    open val version: Long
)

data class JobQueuedEvent(
    override val timestamp: LocalDateTime,
    override val aggregateIdentifier: JobIdentifier,
    override val version: Long,
    val jobType: String,
    val userIdentifier: UserIdentifier,
    val serializedContext: JsonSerializedObject?,
    val serializedCommand: JsonSerializedObject
) : JobEvent(timestamp, aggregateIdentifier, version)

data class JobStartedEvent(
    override val timestamp: LocalDateTime,
    override val aggregateIdentifier: JobIdentifier,
    override val version: Long
) : JobEvent(timestamp, aggregateIdentifier, version)

data class JobCompletedEvent(
    override val timestamp: LocalDateTime,
    override val aggregateIdentifier: JobIdentifier,
    override val version: Long,
    val serializedResult: JsonSerializedObject
) : JobEvent(timestamp, aggregateIdentifier, version)

data class JobFailedEvent(
    override val timestamp: LocalDateTime,
    override val aggregateIdentifier: JobIdentifier,
    override val version: Long
) : JobEvent(timestamp, aggregateIdentifier, version)

data class JobRejectedEvent(
    override val timestamp: LocalDateTime,
    override val aggregateIdentifier: JobIdentifier,
    override val version: Long,
    val jobType: String,
    val userIdentifier: UserIdentifier,
    val serializedContext: JsonSerializedObject?
// leave out reason for now (only reason is max active jobs per user exceeded)
) : JobEvent(timestamp, aggregateIdentifier, version)

data class JobResultReadEvent(
    override val timestamp: LocalDateTime,
    override val aggregateIdentifier: JobIdentifier,
    override val version: Long
) : JobEvent(timestamp, aggregateIdentifier, version)
