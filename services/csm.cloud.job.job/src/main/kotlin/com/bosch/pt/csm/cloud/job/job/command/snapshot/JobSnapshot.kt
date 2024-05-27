/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.snapshot

import com.bosch.pt.csm.cloud.job.job.api.JobCompletedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobEvent
import com.bosch.pt.csm.cloud.job.job.api.JobFailedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JobQueuedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobRejectedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobResultReadEvent
import com.bosch.pt.csm.cloud.job.job.api.JobStartedEvent
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.handler.exception.InvalidJobStateTransitionException
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = JobSnapshot.COLLECTION_NAME)
@TypeAlias(JobSnapshot.COLLECTION_NAME)
data class JobSnapshot(
    @Id val jobIdentifier: JobIdentifier,
    val version: Long,
    @Indexed val userIdentifier: UserIdentifier,
    val state: JobState,
    val lastModifiedDate: LocalDateTime
) {
  constructor(
      jobQueuedEvent: JobQueuedEvent
  ) : this(
      jobQueuedEvent.aggregateIdentifier,
      jobQueuedEvent.version,
      jobQueuedEvent.userIdentifier,
      JobState.QUEUED,
      jobQueuedEvent.timestamp)

  constructor(
      jobRejectedEvent: JobRejectedEvent
  ) : this(
      jobRejectedEvent.aggregateIdentifier,
      jobRejectedEvent.version,
      jobRejectedEvent.userIdentifier,
      JobState.REJECTED,
      jobRejectedEvent.timestamp)

  fun update(event: JobEvent): JobSnapshot {
    return when (event) {
      is JobQueuedEvent -> {
        assertTransitionValid(JobState.QUEUED)
        JobSnapshot(event)
      }
      is JobStartedEvent -> {
        assertTransitionValid(JobState.RUNNING)
        copy(state = JobState.RUNNING, lastModifiedDate = event.timestamp, version = event.version)
      }
      is JobCompletedEvent -> {
        assertTransitionValid(JobState.COMPLETED)
        copy(
            state = JobState.COMPLETED, lastModifiedDate = event.timestamp, version = event.version)
      }
      is JobFailedEvent -> {
        assertTransitionValid(JobState.FAILED)
        copy(state = JobState.FAILED, lastModifiedDate = event.timestamp, version = event.version)
      }
      is JobRejectedEvent -> {
        assertTransitionValid(JobState.REJECTED)
        JobSnapshot(event)
      }
      is JobResultReadEvent -> {
        copy(version = event.version)
      }
    }
  }

  private fun assertTransitionValid(next: JobState) {
    if (!state.isValidTransition(next)) {
      throw InvalidJobStateTransitionException(state, next)
    }
  }

  companion object Collection {
    const val COLLECTION_NAME = "JobSnapshot"
  }
}

enum class JobState {
  QUEUED {
    override fun isValidTransition(next: JobState) = next == RUNNING
  },
  RUNNING {
    override fun isValidTransition(next: JobState) = setOf(COMPLETED, FAILED).contains(next)
  },
  COMPLETED {
    override fun isValidTransition(next: JobState) = false
  },
  FAILED {
    override fun isValidTransition(next: JobState) = false
  },
  REJECTED {
    override fun isValidTransition(next: JobState) = false
  };

  abstract fun isValidTransition(next: JobState): Boolean
}
