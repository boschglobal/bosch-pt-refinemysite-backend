/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.job.handler

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.job.messages.JobCompletedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.iot.smartsite.job.facade.listener.JobCompletedEventHandler
import com.bosch.pt.iot.smartsite.job.facade.listener.JobQueuedEventHandler
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.job.integration.toJsonSerializedObject
import com.bosch.pt.iot.smartsite.project.importer.api.ProjectImportCommand
import com.bosch.pt.iot.smartsite.project.importer.boundary.ProjectImportDeleteService
import com.bosch.pt.iot.smartsite.project.importer.boundary.ProjectImportService
import com.bosch.pt.iot.smartsite.project.importer.command.ProjectImportCommandHandler
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobType.PROJECT_IMPORT
import datadog.trace.api.Trace
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!restore-db")
@Component
open class ProjectImportJobHandler(
    private val projectImportService: ProjectImportService,
    private val projectImportDeleteService: ProjectImportDeleteService,
    private val jobJsonSerializer: JobJsonSerializer,
    private val projectImportCommandHandler: ProjectImportCommandHandler
) : JobQueuedEventHandler, JobCompletedEventHandler {

  override fun handles(job: JobQueuedEventAvro) = job.jobType == PROJECT_IMPORT.name

  override fun handles(job: JobCompletedEventAvro) =
      projectImportService.existsByJobId(job.aggregateIdentifier.identifier.toUUID())

  @Trace
  override fun handle(job: JobCompletedEventAvro) =
      projectImportDeleteService.deleteByJobId(job.aggregateIdentifier.identifier.toUUID())

  /**
   * The maximum time between kafka consumer polls is 5 minutes (by default). This need to be
   * considered when setting the max attempts for retries here. Otherwise, we might run in an
   * endless loop in the kafka job listener when the time use for all retries is longer than the
   * configured maximum allowed between polls.
   */
  @Trace
  override fun handle(job: JobQueuedEventAvro) =
      when (job.jobType) {
        PROJECT_IMPORT.name -> handleImportJob(job)
        else -> error("Unknown import job type received.")
      }

  private fun handleImportJob(event: JobQueuedEventAvro): Unit =
      projectImportCommandHandler.handle(event.toImportProjectCommand())

  private fun JobQueuedEventAvro.toImportProjectCommand() =
      jobJsonSerializer.deserialize(jsonSerializedCommand.toJsonSerializedObject())
          as ProjectImportCommand
}
