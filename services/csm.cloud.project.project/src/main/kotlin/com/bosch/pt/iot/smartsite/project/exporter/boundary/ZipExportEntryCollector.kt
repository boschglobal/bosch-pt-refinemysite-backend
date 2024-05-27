/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.message.command.snapshotstore.MessageSnapshot
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshot
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshot
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.TopicSnapshot
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.io.File
import java.io.InputStream
import java.util.UUID

class ZipExportEntryCollector(
    private val project: ProjectSnapshot,
    private val tasks: List<TaskSnapshot> = emptyList(),
    private val taskAttachments: Map<UUID, List<AttachmentDto>> = emptyMap(),
    private val topics: List<TopicSnapshot> = emptyList(),
    private val topicAttachments: Map<TopicId, List<AttachmentDto>> = emptyMap(),
    private val messages: List<MessageSnapshot> = emptyList(),
    private val messageAttachments: Map<MessageId, List<AttachmentDto>> = emptyMap()
) {

  fun collect(): List<ZipExportEntry> {
    val path = project.title
    return createProjectDirectoryAndMetadata(path, project, tasks) +
        tasks.flatMap { task -> createExportEntriesForTask(path, task) }
  }

  private fun createProjectDirectoryAndMetadata(
      path: String,
      project: ProjectSnapshot,
      tasks: List<TaskSnapshot>
  ): List<ZipExportEntry> {
    val projectDirectoryEntry = DirectoryZipExportEntry(path)
    val projectMetadataEntry =
        MetadataZipExportEntry("$path/project.json", createProjectMetadataJson(project, tasks))
    return listOf(projectDirectoryEntry, projectMetadataEntry)
  }

  private fun createExportEntriesForTask(
      pathPrefix: String,
      task: TaskSnapshot
  ): List<ZipExportEntry> {
    val path = "$pathPrefix/${task.identifier}"
    val taskDirectoryEntry = DirectoryZipExportEntry(path)
    val renamedTaskAttachments =
        renameDuplicateAttachmentFileNames(taskAttachments[task.identifier.toUuid()] ?: emptyList())
    val taskMetadataEntry =
        MetadataZipExportEntry(
            "$path/task.json", createTaskMetadataJson(renamedTaskAttachments, task, topics))

    return listOf(taskDirectoryEntry, taskMetadataEntry) +
        renamedTaskAttachments
            .filter { it.taskIdentifier == task.identifier }
            .map { taskAttachment -> createTaskAttachment(path, taskAttachment) } +
        topics
            .filter { it.taskIdentifier == task.identifier }
            .flatMap { topic -> createExportEntriesForTopic(path, topic) }
  }

  private fun createTaskAttachment(path: String, taskAttachment: AttachmentDto) =
      AttachmentBlobZipExportEntry("$path/${taskAttachment.fileName}", taskAttachment)

  private fun createExportEntriesForTopic(
      pathPrefix: String,
      topic: TopicSnapshot
  ): List<ZipExportEntry> {
    val path = "$pathPrefix/${topic.identifier}"
    val topicDirectoryEntry = DirectoryZipExportEntry(path)
    val topicMessages = messages.filter { it.topicIdentifier == topic.identifier }
    val renamedTopicAttachments =
        renameDuplicateAttachmentFileNames(topicAttachments[topic.identifier] ?: emptyList())
    val topicMetadataEntry =
        MetadataZipExportEntry(
            "$path/topic.json",
            createTopicMetadataJson(renamedTopicAttachments, topic, topicMessages))

    return listOf(topicDirectoryEntry, topicMetadataEntry) +
        renamedTopicAttachments.map { topicAttachment ->
          createTopicAttachment(path, topicAttachment)
        } +
        topicMessages.flatMap { message -> createExportEntriesForMessage(path, message) }
  }

  private fun createTopicAttachment(path: String, topicAttachment: AttachmentDto) =
      AttachmentBlobZipExportEntry("$path/${topicAttachment.fileName}", topicAttachment)

  private fun createExportEntriesForMessage(
      pathPrefix: String,
      message: MessageSnapshot
  ): List<ZipExportEntry> {
    val path = "$pathPrefix/${message.identifier}"
    val messageDirectoryEntry = DirectoryZipExportEntry(path)
    val renamedMessageAttachments =
        renameDuplicateAttachmentFileNames(messageAttachments[message.identifier] ?: emptyList())
    val messageMetadataEntry =
        MetadataZipExportEntry(
            "$path/message.json", createMessageMetadataJson(renamedMessageAttachments, message))
    return listOf(messageDirectoryEntry, messageMetadataEntry) +
        renamedMessageAttachments.map { messageAttachment ->
          createMessageAttachment(path, messageAttachment)
        }
  }

  private fun createMessageAttachment(path: String, messageAttachment: AttachmentDto) =
      AttachmentBlobZipExportEntry("$path/${messageAttachment.fileName}", messageAttachment)

  private fun createProjectMetadataJson(
      project: ProjectSnapshot,
      tasks: List<TaskSnapshot>
  ): String {
    val projectBasicInfo =
        """
        | "title": "${project.title}",
        | "projectNumber": "${project.projectNumber}",
        | "description": "${project.description ?: "No description"}",
        | "start": "${project.start}",
        | "end": "${project.end}",
        """
            .trimMargin()

    val projectAddressBlock =
        project.address?.let {
          """
        | "projectAddress": {
        |  "street": "${it.street}",
        |  "houseNumber": "${it.houseNumber}",
        |  "zipCode": "${it.zipCode}",
        |  "city": "${it.city}"
        | },
        """
              .trimMargin()
        }

    val tasksBlock =
        """ "tasks": [${createJsonListObject(tasks.map { it.identifier.toString() })}]"""

    return "{\n" +
        projectBasicInfo +
        "\n" +
        (if (projectAddressBlock == null) "" else projectAddressBlock + "\n") +
        tasksBlock +
        "\n}"
  }

  private fun createTaskMetadataJson(
      renamedAttachments: List<AttachmentDto>,
      task: TaskSnapshot,
      topics: List<TopicSnapshot>
  ): String {
    return """{
      | "name": "${task.name}",
      | "status": "${task.status.name}",
      | "topics": [${createJsonListObject(
      topics.filter { it.taskIdentifier == task.identifier }
        .map { it.identifier.toString() }
    )}],
      | "attachments": [${createJsonListObject(
      renamedAttachments.filter { it.taskIdentifier == task.identifier }
        .map { it.fileName })}]
      |}
    """
        .trimMargin()
  }

  private fun createTopicMetadataJson(
      renamedAttachments: List<AttachmentDto>,
      topic: TopicSnapshot,
      messages: List<MessageSnapshot>
  ): String {
    return """{
      | "description": "${topic.description ?: "No description"}",
      | "criticality": "${topic.criticality.name}",
      | "messages": [${createJsonListObject(
      messages.filter { it.topicIdentifier == topic.identifier }
        .map { it.identifier.toString() }
    )}],
      | "attachments": [${createJsonListObject(renamedAttachments.map { it.fileName })}]
      |}
    """
        .trimMargin()
  }

  private fun createMessageMetadataJson(
      renamedAttachments: List<AttachmentDto>,
      message: MessageSnapshot
  ): String {
    return """{
      | "content": "${message.content ?: "No content"}",
      | "attachments": [${createJsonListObject(renamedAttachments.map { it.fileName })}]
      |}
    """
        .trimMargin()
  }

  private fun createJsonListObject(strings: List<String>): String {
    return strings.joinToString { "\"${it}\"" }
  }

  private fun renameDuplicateAttachmentFileNames(
      attachments: List<AttachmentDto>
  ): List<AttachmentDto> =
      attachments
          .groupBy { it.fileName }
          .values
          .flatMap { attachmentsWithSameFileName ->
            if (attachmentsWithSameFileName.count() > 1) {
              attachmentsWithSameFileName.mapIndexed { count, attachment ->
                val file = File(attachment.fileName)
                attachment.copy(
                    fileName = "${file.nameWithoutExtension}-${count + 1}.${file.extension}")
              }
            } else {
              attachmentsWithSameFileName
            }
          }

  sealed class ZipExportEntry(open val fileName: String)

  data class DirectoryZipExportEntry(override val fileName: String) : ZipExportEntry(fileName)

  data class MetadataZipExportEntry(override val fileName: String, val content: String) :
      ZipExportEntry(fileName)

  data class AttachmentBlobZipExportEntry(
      override val fileName: String,
      val attachmentDto: AttachmentDto
  ) : ZipExportEntry(fileName) {
    fun open(attachmentService: AttachmentService): InputStream =
        attachmentService.openAttachment(
            attachmentDto.identifier, AttachmentImageResolution.ORIGINAL)
  }
}
