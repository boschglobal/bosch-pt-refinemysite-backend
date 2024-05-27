/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.PROJECT_EXPORT_ZIP
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureQueryService
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.download.repository.DownloadBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.message.boundary.MessageQueryService
import com.bosch.pt.iot.smartsite.project.message.command.snapshotstore.asSnapshot
import com.bosch.pt.iot.smartsite.project.messageattachment.boundary.MessageAttachmentQueryService
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshot
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshotStore
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.asSnapshot
import com.bosch.pt.iot.smartsite.project.task.query.TaskQueryService
import com.bosch.pt.iot.smartsite.project.taskattachment.boundary.TaskAttachmentQueryService
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.asSnapshot
import com.bosch.pt.iot.smartsite.project.topic.query.TopicQueryService
import com.bosch.pt.iot.smartsite.project.topicattachment.boundary.TopicAttachmentQueryService
import datadog.trace.api.Trace
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ZipProjectExportService(
    private val attachmentService: AttachmentService,
    private val featureQueryService: FeatureQueryService,
    private val messageAttachmentQueryService: MessageAttachmentQueryService,
    private val messageQueryService: MessageQueryService,
    private val projectSnapshotStore: ProjectSnapshotStore,
    private val taskAttachmentQueryService: TaskAttachmentQueryService,
    private val taskQueryService: TaskQueryService,
    private val topicAttachmentQueryService: TopicAttachmentQueryService,
    private val topicQueryService: TopicQueryService
) {

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  open fun isExportPossible(project: Project): Boolean =
      featureQueryService.isFeatureEnabled(PROJECT_EXPORT_ZIP, project.identifier)

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun exportZip(projectIdentifier: ProjectId, blobUploadStream: OutputStream) {
    val contentsToExport = collectExportEntries(projectIdentifier)
    writeZip(blobUploadStream, contentsToExport)
  }

  private fun collectExportEntries(
      projectIdentifier: ProjectId
  ): List<ZipExportEntryCollector.ZipExportEntry> {
    val project = projectSnapshotStore.findOrFail(projectIdentifier)

    val collector = createExportEntryCollector(project)
    return collector.collect()
  }

  private fun createExportEntryCollector(project: ProjectSnapshot): ZipExportEntryCollector {
    val tasks =
        taskQueryService.findTasksByProjectIdentifier(project.identifier).map { it.asSnapshot() }
    val taskAttachments: Map<UUID, List<AttachmentDto>> =
        taskAttachmentQueryService.findAllByTaskIdentifiers(tasks.map { it.identifier }).groupBy {
          it.taskIdentifier.toUuid()
        }

    val topics =
        topicQueryService.findAllByTaskIdentifiers(tasks.map { it.identifier }).map {
          it.asSnapshot()
        }
    val topicAttachments =
        topicAttachmentQueryService.findAllAndMappedByTopicIdentifierIn(
            topics.map { it.identifier })

    val messages =
        messageQueryService.findAllMessagesByTaskIdentifiers(tasks.map { it.identifier }).map {
          it.asSnapshot()
        }
    val messageAttachments =
        messageAttachmentQueryService.findAllAndMappedByMessageIdentifierIn(
            messages.map { it.identifier })

    return ZipExportEntryCollector(
        project, tasks, taskAttachments, topics, topicAttachments, messages, messageAttachments)
  }

  private fun writeZip(
      os: OutputStream,
      contentsToExport: List<ZipExportEntryCollector.ZipExportEntry>
  ) {
    ZipOutputStream(os).use { zipStream ->
      contentsToExport.forEach { entry ->
        when (entry) {
          is ZipExportEntryCollector.DirectoryZipExportEntry -> {
            zipStream.putNextEntry(ZipEntry("/${entry.fileName}/"))
          }
          is ZipExportEntryCollector.MetadataZipExportEntry -> {
            zipStream.putNextEntry(ZipEntry("/${entry.fileName}"))
            entry.content.toByteArray().inputStream().use { it.copyTo(zipStream) }
          }
          is ZipExportEntryCollector.AttachmentBlobZipExportEntry -> {
            zipStream.putNextEntry(ZipEntry("/${entry.fileName}"))
            entry.open(attachmentService).use {
              it.copyTo(zipStream, DownloadBlobStorageRepository.BUFFER_SIZE)
            }
          }
        }
        zipStream.closeEntry()
      }
      zipStream.finish()
    }
  }
}
