/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.command

import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.project.calendar.command.dto.DownloadableResult
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.handler.UrlBuilder
import com.bosch.pt.iot.smartsite.project.download.facade.rest.DownloadController
import com.bosch.pt.iot.smartsite.project.download.repository.DownloadBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportZipCommand
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ZipProjectExportService
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshotStore
import datadog.trace.api.Trace
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.slf4j.Logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
open class ZipProjectExportCommandHandler(
    private val downloadBlobStorageRepository: DownloadBlobStorageRepository,
    private val logger: Logger,
    private val projectSnapshotStore: ProjectSnapshotStore,
    private val urlBuilder: UrlBuilder,
    private val zipProjectExportService: ZipProjectExportService
) {

  @Trace
  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: ProjectExportZipCommand): DownloadableResult {
    val projectIdentifier = command.projectIdentifier.asProjectId()
    val project = projectSnapshotStore.findOrFail(projectIdentifier)

    val fileName = getFileName(project.title)
    val documentId =
        downloadBlobStorageRepository.uploadBlobStream(fileName, "application/zip", emptyMap()) {
            blobUploadStream ->
          zipProjectExportService.exportZip(projectIdentifier, blobUploadStream)
        }

    logger.info(
        "Exported $fileName to blob ${SecurityContextHelper.getInstance().getCurrentUser().identifier}/$documentId}.")

    return urlBuilder
        .withPath(DownloadController.EXPORT_DOWNLOAD_BY_ID_ENDPOINT)
        .buildAndExpand(documentId.toString())
        .let { DownloadableResult(it.toString(), fileName) }
  }

  private fun getFileName(title: String) =
      "${DateTimeFormatter.ISO_DATE.format(LocalDate.now())} " +
          "${title.replace(ILLEGAL_CHARACTERS_REGEX.toRegex(), "")}.zip"

  companion object {

    private val ILLEGAL_CHARACTERS_REGEX =
        listOf(
                "/",
                "\n",
                "\r",
                "\t",
                "\u0000",
                "\u000C",
                "`",
                "?",
                "*",
                "\\\\",
                "<",
                ">",
                "|",
                "\"",
                ":")
            .joinToString(separator = "", prefix = "[", postfix = "]")
  }
}
