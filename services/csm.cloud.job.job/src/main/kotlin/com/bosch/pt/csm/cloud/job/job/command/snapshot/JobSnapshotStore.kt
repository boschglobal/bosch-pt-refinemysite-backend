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
import com.bosch.pt.csm.cloud.job.job.command.handler.exception.JobNotFoundException
import org.springframework.stereotype.Component

@Component
class JobSnapshotStore(private val repository: JobSnapshotRepository) {

  fun findOrFail(identifier: JobIdentifier): JobSnapshot =
      repository.findById(identifier).orElseThrow { JobNotFoundException(identifier) }

  fun exists(identifier: JobIdentifier) = repository.existsById(identifier)

  fun update(event: JobEvent): JobSnapshot {
    val snapshotToSave =
        when (event) {
          is JobQueuedEvent -> JobSnapshot(event)
          is JobRejectedEvent -> JobSnapshot(event)
          is JobStartedEvent -> findOrFail(event.aggregateIdentifier).update(event)
          is JobCompletedEvent -> findOrFail(event.aggregateIdentifier).update(event)
          is JobFailedEvent -> findOrFail(event.aggregateIdentifier).update(event)
          is JobResultReadEvent -> findOrFail(event.aggregateIdentifier).update(event)
        }
    return repository.save(snapshotToSave)
  }
}
