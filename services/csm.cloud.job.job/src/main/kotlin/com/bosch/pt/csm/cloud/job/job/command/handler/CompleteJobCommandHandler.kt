/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.handler

import com.bosch.pt.csm.cloud.job.job.api.CompleteJobCommand
import com.bosch.pt.csm.cloud.job.job.api.JobCompletedEvent
import com.bosch.pt.csm.cloud.job.job.command.JobEventPublisher
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobSnapshotStore
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class CompleteJobCommandHandler(
    private val jobSnapshotStore: JobSnapshotStore,
    private val jobEventPublisher: JobEventPublisher
) {

  fun handle(command: CompleteJobCommand, now: LocalDateTime) {
    jobSnapshotStore.findOrFail(command.jobIdentifier).apply {
      JobCompletedEvent(now, command.jobIdentifier, version.inc(), command.serializedResult).also {
        jobSnapshotStore.update(it)
        jobEventPublisher.publish(it)
      }
    }
  }
}
