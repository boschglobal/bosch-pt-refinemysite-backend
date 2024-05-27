/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.query

import com.bosch.pt.csm.cloud.job.job.api.JobCompletedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobEvent
import com.bosch.pt.csm.cloud.job.job.api.JobFailedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JobQueuedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobRejectedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobResultReadEvent
import com.bosch.pt.csm.cloud.job.job.api.JobStartedEvent
import com.bosch.pt.csm.cloud.job.job.api.JsonSerializedObject
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Represents a view on a Job for the User. Contains all relevant fields, but not the
 * `serializedCommand`, which is used for further processing in the backend and can contain
 * sensitive information.
 */
@Document(collection = JobProjection.COLLECTION_NAME)
@TypeAlias(JobProjection.COLLECTION_NAME)
data class JobProjection(
    @Id val jobIdentifier: JobIdentifier,
    @Indexed val userIdentifier: UserIdentifier,
    val jobType: String,
    val state: JobState,
    val serializedContext: JsonSerializedObject?,
    val createdDate: LocalDateTime,
    val lastModifiedDate: LocalDateTime,
    val serializedResult: JsonSerializedObject? = null,
    val read: Boolean = false
) {

  constructor(
      jobQueuedEvent: JobQueuedEvent
  ) : this(
      jobQueuedEvent.aggregateIdentifier,
      jobQueuedEvent.userIdentifier,
      jobQueuedEvent.jobType,
      JobState.QUEUED,
      jobQueuedEvent.serializedContext,
      jobQueuedEvent.timestamp,
      jobQueuedEvent.timestamp)

  constructor(
      jobRejectedEvent: JobRejectedEvent
  ) : this(
      jobRejectedEvent.aggregateIdentifier,
      jobRejectedEvent.userIdentifier,
      jobRejectedEvent.jobType,
      JobState.REJECTED,
      jobRejectedEvent.serializedContext,
      jobRejectedEvent.timestamp,
      jobRejectedEvent.timestamp)

  fun update(event: JobEvent): JobProjection {
    return when (event) {
      is JobQueuedEvent -> JobProjection(event)
      is JobStartedEvent -> copy(state = JobState.RUNNING, lastModifiedDate = event.timestamp)
      is JobCompletedEvent ->
          copy(
              state = JobState.COMPLETED,
              serializedResult = event.serializedResult,
              lastModifiedDate = event.timestamp)
      is JobFailedEvent -> copy(state = JobState.FAILED, lastModifiedDate = event.timestamp)
      is JobRejectedEvent -> JobProjection(event)
      // Do not update lastModifiedDate when a User marks a Job as read, to avoid reordering issues
      // in the web frontend.
      is JobResultReadEvent -> copy(read = true)
    }
  }

  companion object Collection {
    const val COLLECTION_NAME = "JobProjection"
  }
}
