/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.handler

import com.bosch.pt.csm.cloud.job.user.query.security.JobServiceUserDetails
import com.bosch.pt.csm.cloud.job.job.api.JobResultReadEvent
import com.bosch.pt.csm.cloud.job.job.api.MarkJobResultReadCommand
import com.bosch.pt.csm.cloud.job.job.command.JobEventPublisher
import com.bosch.pt.csm.cloud.job.job.command.handler.exception.OnlyCompletedJobsCanBeMarkedAsReadException
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobSnapshot
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobSnapshotStore
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState.COMPLETED
import java.time.LocalDateTime
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder.getContext
import org.springframework.stereotype.Component

@Component
class MarkJobResultReadCommandHandler(
    private val jobSnapshotStore: JobSnapshotStore,
    private val jobEventPublisher: JobEventPublisher
) {

  fun handle(command: MarkJobResultReadCommand, now: LocalDateTime) {
    jobSnapshotStore.findOrFail(command.jobIdentifier).apply {
      authorizeUser(this)
      if (state != COMPLETED) {
        throw OnlyCompletedJobsCanBeMarkedAsReadException(this)
      }
      JobResultReadEvent(now, command.jobIdentifier, version.inc()).also {
        jobEventPublisher.publish(it)
      }
    }
  }

  private fun authorizeUser(job: JobSnapshot) {
    if (job.userIdentifier !=
        (getContext().authentication.principal as JobServiceUserDetails).identifier) {
      throw AccessDeniedException("User not authorized to access this job.")
    }
  }
}
