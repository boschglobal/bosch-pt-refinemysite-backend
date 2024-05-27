/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.api

sealed class JobCommand(open val jobIdentifier: JobIdentifier)

data class EnqueueJobCommand(
    val jobType: String,
    override val jobIdentifier: JobIdentifier,
    val userIdentifier: UserIdentifier,
    val serializedContext: JsonSerializedObject,
    val serializedCommand: JsonSerializedObject
) : JobCommand(jobIdentifier)

data class StartJobCommand(override val jobIdentifier: JobIdentifier) : JobCommand(jobIdentifier)

data class CompleteJobCommand(
    override val jobIdentifier: JobIdentifier,
    val serializedResult: JsonSerializedObject
) : JobCommand(jobIdentifier)

data class FailJobCommand(override val jobIdentifier: JobIdentifier) : JobCommand(jobIdentifier)

data class MarkJobResultReadCommand(override val jobIdentifier: JobIdentifier) :
    JobCommand(jobIdentifier)
