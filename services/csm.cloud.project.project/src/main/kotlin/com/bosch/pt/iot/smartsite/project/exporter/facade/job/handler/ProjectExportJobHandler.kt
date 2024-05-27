/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.facade.job.handler

import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.iot.smartsite.job.facade.listener.JobQueuedEventHandler
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.job.integration.toJsonSerializedObject
import com.bosch.pt.iot.smartsite.project.calendar.command.dto.DownloadableResult
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.handler.UrlBuilder
import com.bosch.pt.iot.smartsite.project.download.facade.rest.DownloadController.Companion.EXPORT_DOWNLOAD_BY_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.download.repository.DownloadBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportCommand
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportZipCommand
import com.bosch.pt.iot.smartsite.project.exporter.command.ProjectExportCommandHandler
import com.bosch.pt.iot.smartsite.project.exporter.command.ZipProjectExportCommandHandler
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.dto.ProjectExportJobType.PROJECT_EXPORT
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.dto.ProjectExportJobType.PROJECT_EXPORT_ZIP
import datadog.trace.api.Trace
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType.APPLICATION_XML_VALUE
import org.springframework.stereotype.Component

@Profile("!restore-db")
@Component
open class ProjectExportJobHandler(
    private val projectExportCommandHandler: ProjectExportCommandHandler,
    private val zipProjectExportCommandHandler: ZipProjectExportCommandHandler,
    private val jobJsonSerializer: JobJsonSerializer,
    private val blobStorageRepository: DownloadBlobStorageRepository,
    private val urlBuilder: UrlBuilder
) : JobQueuedEventHandler {

  override fun handles(job: JobQueuedEventAvro) =
      job.jobType == PROJECT_EXPORT.name || job.jobType == PROJECT_EXPORT_ZIP.name

  /**
   * The maximum time between kafka consumer polls is 5 minutes (by default). This need to be
   * considered when setting the max attempts for retries here. Otherwise, we might run in an
   * endless loop in the kafka job listener when the time use for all retries is longer than the
   * configured maximum allowed between polls.
   */
  @Trace
  override fun handle(job: JobQueuedEventAvro) =
      when (job.jobType) {
        PROJECT_EXPORT.name -> handleExportJob(job)
        PROJECT_EXPORT_ZIP.name -> handleExportZipJob(job)
        else -> error("Unknown import job type received.")
      }

  private fun handleExportJob(event: JobQueuedEventAvro): DownloadableResult =
      projectExportCommandHandler.handle(event.toExportProjectCommand()).let {
        saveExport(it.file, it.filename, APPLICATION_XML_VALUE)
      }

  private fun handleExportZipJob(event: JobQueuedEventAvro): DownloadableResult =
      zipProjectExportCommandHandler.handle(event.toExportZipProjectCommand())

  private fun JobQueuedEventAvro.toExportProjectCommand() =
      jobJsonSerializer.deserialize(jsonSerializedCommand.toJsonSerializedObject())
          as ProjectExportCommand

  private fun JobQueuedEventAvro.toExportZipProjectCommand() =
      jobJsonSerializer.deserialize(jsonSerializedCommand.toJsonSerializedObject())
          as ProjectExportZipCommand

  private fun saveExport(
      file: ByteArray,
      fileName: String,
      mimeType: String,
      metadata: MutableMap<String, String> = mutableMapOf()
  ): DownloadableResult {
    val documentId = blobStorageRepository.save(file, fileName, mimeType, metadata)
    return urlBuilder
        .withPath(EXPORT_DOWNLOAD_BY_ID_ENDPOINT)
        .buildAndExpand(documentId.toString())
        .let { DownloadableResult(it.toString(), fileName) }
  }
}
