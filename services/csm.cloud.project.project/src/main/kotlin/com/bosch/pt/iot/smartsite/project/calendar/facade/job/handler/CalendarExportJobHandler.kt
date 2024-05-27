/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.facade.job.handler

import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.iot.smartsite.job.facade.listener.JobQueuedEventHandler
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.job.integration.toJsonSerializedObject
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsCsvCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsJsonCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsPdfCommand
import com.bosch.pt.iot.smartsite.project.calendar.command.ExportCalendarCommandHandler
import com.bosch.pt.iot.smartsite.project.calendar.command.dto.DownloadableResult
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType.CALENDAR_EXPORT_CSV
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType.CALENDAR_EXPORT_JSON
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.CalendarExportJobType.CALENDAR_EXPORT_PDF
import com.bosch.pt.iot.smartsite.project.download.facade.rest.DownloadController.Companion.EXPORT_DOWNLOAD_BY_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.download.repository.DownloadBlobStorageRepository
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType.APPLICATION_PDF_VALUE
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Profile("!restore-db")
@Component
open class CalendarExportJobHandler(
    private val jobJsonSerializer: JobJsonSerializer,
    private val exportCalendarCommandHandler: ExportCalendarCommandHandler,
    private val blobStorageRepository: DownloadBlobStorageRepository,
    private val urlBuilder: UrlBuilder
) : JobQueuedEventHandler {

  override fun handles(job: JobQueuedEventAvro) =
      job.jobType in CalendarExportJobType.values().map { it.name }

  /**
   * The maximum time between kafka consumer polls is 5 minutes (by default). This need to be
   * considered when setting the max attempts for retries here in combination with the max read time
   * configured by the [RestTemplateConfiguration]. Otherwise, we might run in an endless loop in
   * the kafka job listener when the time use for all retries is longer than the configured maximum
   * allowed between polls.
   */
  @Retryable(backoff = Backoff(5000), maxAttempts = 2)
  override fun handle(job: JobQueuedEventAvro) =
      when (job.jobType) {
        CALENDAR_EXPORT_CSV.name -> handleCsvExportJob(job)
        CALENDAR_EXPORT_JSON.name -> handleJsonExportJob(job)
        CALENDAR_EXPORT_PDF.name -> handlePdfExportJob(job)
        else -> throw IllegalStateException("Unknown calendar export job type received.")
      }

  private fun handleCsvExportJob(event: JobQueuedEventAvro): DownloadableResult =
      exportCalendarCommandHandler.handle(event.toExportCalendarAsCsvCommand()).let {
        saveExport(it.file, it.filename, TEXT_PLAIN_VALUE)
      }

  private fun handleJsonExportJob(event: JobQueuedEventAvro): DownloadableResult =
      exportCalendarCommandHandler.handle(event.toExportCalendarAsJsonCommand()).let {
        saveExport(it.file, it.filename, TEXT_PLAIN_VALUE)
      }

  private fun handlePdfExportJob(event: JobQueuedEventAvro): DownloadableResult =
      exportCalendarCommandHandler.handle(event.toExportCalendarAsPdfCommand()).let {
        saveExport(it.file, it.filename, APPLICATION_PDF_VALUE)
      }

  private fun JobQueuedEventAvro.toExportCalendarAsCsvCommand() =
      jobJsonSerializer.deserialize(jsonSerializedCommand.toJsonSerializedObject())
          as ExportCalendarAsCsvCommand

  private fun JobQueuedEventAvro.toExportCalendarAsJsonCommand() =
      jobJsonSerializer.deserialize(jsonSerializedCommand.toJsonSerializedObject())
          as ExportCalendarAsJsonCommand

  private fun JobQueuedEventAvro.toExportCalendarAsPdfCommand() =
      jobJsonSerializer.deserialize(jsonSerializedCommand.toJsonSerializedObject())
          as ExportCalendarAsPdfCommand

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
