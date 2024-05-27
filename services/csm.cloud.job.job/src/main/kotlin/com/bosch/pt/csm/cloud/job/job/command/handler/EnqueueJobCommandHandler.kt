/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.handler

import com.bosch.pt.csm.cloud.job.job.api.EnqueueJobCommand
import com.bosch.pt.csm.cloud.job.job.api.JobQueuedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobRejectedEvent
import com.bosch.pt.csm.cloud.job.job.command.JobEventPublisher
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobSnapshotRepository
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobSnapshotStore
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState
import java.time.LocalDateTime
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EnqueueJobCommandHandler(
    private val jobEventPublisher: JobEventPublisher,
    private val jobSnapshotStore: JobSnapshotStore,
    private val jobSnapshotRepository: JobSnapshotRepository,
    @Value("\${custom.job.max-active-per-user}") private val maxActiveJobsPerUser: Long,
    private val logger: Logger
) {

  fun handle(command: EnqueueJobCommand, now: LocalDateTime) {
    /* Deduplication: Commands could arrive more than once (e.g. producer retries after broker
    connection fault), so we drop duplicate commands here.

    We update the snapshot *before* finishing the Kafka transaction.  If the Kafka transaction
    fails after the snapshot update, this check will prevent that job events are written, and the
    job will never start.  The alternative would be to update *after* the transaction.  In that
    case, we wouldn't be able to deduplicate commands (if the snapshot write fails) and would risk
    that a job is started more than once. */
    if (jobSnapshotStore.exists(command.jobIdentifier)) {
      logger.info("Dropping duplicate command: $command")
      return
    }

    if (maxNumberOfActiveJobsExceededBy(command)) {
      logger.warn(
          "Max allowed active jobs per user exceeded. Rejecting job ${command.jobIdentifier}.")

      val jobRejectedEvent =
          JobRejectedEvent(
              now,
              command.jobIdentifier,
              1L,
              command.jobType,
              command.userIdentifier,
              command.serializedContext)

      jobSnapshotStore.update(jobRejectedEvent)
      jobEventPublisher.publish(jobRejectedEvent)
    } else {

      val jobQueuedEvent =
          JobQueuedEvent(
              now,
              command.jobIdentifier,
              1L,
              command.jobType,
              command.userIdentifier,
              command.serializedContext,
              command.serializedCommand)

      jobSnapshotStore.update(jobQueuedEvent)
      jobEventPublisher.publish(jobQueuedEvent)
    }
  }

  private fun maxNumberOfActiveJobsExceededBy(command: EnqueueJobCommand) =
      jobSnapshotRepository.countByUserIdentifierAndStateIn(
          command.userIdentifier, setOf(JobState.QUEUED, JobState.RUNNING)) >= maxActiveJobsPerUser
}
