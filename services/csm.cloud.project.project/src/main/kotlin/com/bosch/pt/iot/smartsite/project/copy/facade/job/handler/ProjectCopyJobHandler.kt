/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.facade.job.handler

import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.iot.smartsite.job.facade.listener.JobQueuedEventHandler
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.job.integration.toJsonSerializedObject
import com.bosch.pt.iot.smartsite.project.copy.api.ProjectCopyCommand
import com.bosch.pt.iot.smartsite.project.copy.command.ProjectCopyCommandHandler
import com.bosch.pt.iot.smartsite.project.copy.command.dto.CopiedProjectResult
import com.bosch.pt.iot.smartsite.project.copy.facade.job.dto.ProjectCopyJobType.PROJECT_COPY
import datadog.trace.api.Trace
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!restore-db")
@Component
class ProjectCopyJobHandler(
    private val projectCopyCommandHandler: ProjectCopyCommandHandler,
    private val jobJsonSerializer: JobJsonSerializer,
) : JobQueuedEventHandler {

  override fun handles(job: JobQueuedEventAvro): Boolean = job.jobType == PROJECT_COPY.name

  @Trace
  override fun handle(job: JobQueuedEventAvro) =
      when (job.jobType) {
        PROJECT_COPY.name -> handleCopyJob(job)
        else -> error("Unknown import job type received.")
      }

  private fun handleCopyJob(event: JobQueuedEventAvro): CopiedProjectResult {
    return projectCopyCommandHandler.handle(event.toCopyProjectCommand())
  }

  private fun JobQueuedEventAvro.toCopyProjectCommand() =
      jobJsonSerializer.deserialize(jsonSerializedCommand.toJsonSerializedObject())
          as ProjectCopyCommand
}
