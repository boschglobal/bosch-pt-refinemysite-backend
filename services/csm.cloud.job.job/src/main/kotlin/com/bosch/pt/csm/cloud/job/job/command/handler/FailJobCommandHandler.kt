/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.handler

import com.bosch.pt.csm.cloud.job.job.api.FailJobCommand
import com.bosch.pt.csm.cloud.job.job.api.JobFailedEvent
import com.bosch.pt.csm.cloud.job.job.command.JobEventPublisher
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobSnapshotStore
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class FailJobCommandHandler(
    private val jobSnapshotStore: JobSnapshotStore,
    private val jobEventPublisher: JobEventPublisher
) {

  fun handle(command: FailJobCommand, now: LocalDateTime) {
    jobSnapshotStore.findOrFail(command.jobIdentifier).apply {
      JobFailedEvent(now, command.jobIdentifier, version.inc()).also {
        jobSnapshotStore.update(it)
        jobEventPublisher.publish(it)
      }
    }
  }
}
