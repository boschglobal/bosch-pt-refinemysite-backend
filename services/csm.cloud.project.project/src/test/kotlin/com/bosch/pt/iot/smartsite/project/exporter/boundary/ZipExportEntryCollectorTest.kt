/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ZipExportEntryCollector.AttachmentBlobZipExportEntry
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ZipExportEntryCollector.DirectoryZipExportEntry
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ZipExportEntryCollector.MetadataZipExportEntry
import com.bosch.pt.iot.smartsite.project.message.command.snapshotstore.MessageSnapshot
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshot
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshot
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.TopicSnapshot
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import java.sql.Date
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ZipExportEntryCollectorTest {

  @Test
  fun `exports Project metadata`() {
    val collector =
        ZipExportEntryCollector(
            ProjectSnapshot(
                identifier = ProjectId("e52c6377-95d2-4d91-9257-dbc3f70fcdfe".toUUID()),
                version = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
                description = "My favorite project",
                start = LocalDate.of(2023, 11, 27),
                end = LocalDate.of(2023, 12, 24),
                projectNumber = "123",
                title = "Elbphilharmonie",
                address =
                    ProjectAddressVo(
                        street = "Test Street",
                        houseNumber = "1",
                        zipCode = "54321",
                        city = "Echterdingen")))

    val entries = collector.collect()

    assertThat(entries)
        .isEqualTo(
            listOf(
                DirectoryZipExportEntry("Elbphilharmonie"),
                MetadataZipExportEntry(
                    "Elbphilharmonie/project.json",
                    """{
                      | "title": "Elbphilharmonie",
                      | "projectNumber": "123",
                      | "description": "My favorite project",
                      | "start": "2023-11-27",
                      | "end": "2023-12-24",
                      | "projectAddress": {
                      |  "street": "Test Street",
                      |  "houseNumber": "1",
                      |  "zipCode": "54321",
                      |  "city": "Echterdingen"
                      | },
                      | "tasks": []
                      |}
                    """
                        .trimMargin())))
  }

  @Test
  fun `exports Project metadata without optional fields`() {
    val collector =
        ZipExportEntryCollector(
            ProjectSnapshot(
                identifier = ProjectId("e52c6377-95d2-4d91-9257-dbc3f70fcdfe".toUUID()),
                version = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
                start = LocalDate.of(2023, 11, 27),
                end = LocalDate.of(2023, 12, 24),
                projectNumber = "123",
                title = "Elbphilharmonie"))

    val entries = collector.collect()

    assertThat(entries)
        .isEqualTo(
            listOf(
                DirectoryZipExportEntry("Elbphilharmonie"),
                MetadataZipExportEntry(
                    "Elbphilharmonie/project.json",
                    """{
                      | "title": "Elbphilharmonie",
                      | "projectNumber": "123",
                      | "description": "No description",
                      | "start": "2023-11-27",
                      | "end": "2023-12-24",
                      | "tasks": []
                      |}
                    """
                        .trimMargin())))
  }

  @Nested
  inner class `When exporting a Task` {

    val project =
        ProjectSnapshot(
            identifier = ProjectId("e52c6377-95d2-4d91-9257-dbc3f70fcdfe".toUUID()),
            version = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
            start = LocalDate.of(2023, 11, 27),
            end = LocalDate.of(2023, 12, 24),
            projectNumber = "123",
            title = "Elbphilharmonie")

    val task =
        TaskSnapshot(
            identifier = "6c643268-4ea6-404a-9eca-55d3364f5071".toUUID().asTaskId(),
            projectIdentifier = ProjectId("e52c6377-95d2-4d91-9257-dbc3f70fcdfe".toUUID()),
            name = "Task 1",
            status = OPEN,
            projectCraftIdentifier =
                "f39e84bc-2234-4793-ac6d-1abdd0e198fa".toUUID().asProjectCraftId())

    val taskAttachment =
        AttachmentDto(
            identifier = "1cc61d67-5e65-4287-b4de-20d6c80d0f91".toUUID(),
            version = 0L,
            fileName = "attachment.jpg",
            fileSize = 123L,
            fullAvailable = true,
            smallAvailable = false,
            createdByIdentifier = "3a6ca40f-c79a-4e38-83ac-1f3e2af63af7".toUUID(),
            createdDate = Date.valueOf(LocalDate.of(2023, 11, 27)),
            createdByDeleted = false,
            lastModifiedByIdentifier = "3a6ca40f-c79a-4e38-83ac-1f3e2af63af7".toUUID(),
            lastModifiedDate = Date.valueOf(LocalDate.of(2023, 11, 27)),
            lastModifiedByDeleted = false,
            taskIdentifier = task.identifier)

    @Test
    fun `lists that Task in Project metadata`() {
      val collector = ZipExportEntryCollector(project, listOf(task))

      val entries = collector.collect()

      assertThat(
              entries
                  .filterIsInstance<MetadataZipExportEntry>()
                  .first { it.fileName.contains("project.json") }
                  .content)
          .contains(""""tasks": ["6c643268-4ea6-404a-9eca-55d3364f5071"]""")
    }

    @Test
    fun `exports Task metadata into a subdirectory`() {
      val collector = ZipExportEntryCollector(project, listOf(task))

      val entries = collector.collect()

      assertThat(entries.drop(2))
          .isEqualTo(
              listOf(
                  DirectoryZipExportEntry("Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071"),
                  MetadataZipExportEntry(
                      "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071/task.json",
                      """{
                      | "name": "Task 1",
                      | "status": "OPEN",
                      | "topics": [],
                      | "attachments": []
                      |}
                    """
                          .trimMargin())))
    }

    @Test
    fun `lists Task Attachment in Task metadata`() {
      val collector =
          ZipExportEntryCollector(
              project, listOf(task), mapOf(task.identifier.toUuid() to listOf(taskAttachment)))

      val entries = collector.collect()

      assertThat(
              entries
                  .filterIsInstance<MetadataZipExportEntry>()
                  .first { it.fileName.contains("task.json") }
                  .content)
          .contains(""""attachments": ["attachment.jpg"]""")
    }

    @Test
    fun `exports Task Attachment`() {
      val collector =
          ZipExportEntryCollector(
              project, listOf(task), mapOf(task.identifier.toUuid() to listOf(taskAttachment)))

      val entries = collector.collect()

      assertThat(
              entries.filterIsInstance<AttachmentBlobZipExportEntry>().first {
                it.fileName.contains("attachment.jpg")
              })
          .isEqualTo(
              AttachmentBlobZipExportEntry(
                  "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071/attachment.jpg",
                  taskAttachment))
    }

    @Test
    fun `renames Task Attachments when exporting two with identical names`() {
      val collector =
          ZipExportEntryCollector(
              project,
              listOf(task),
              mapOf(
                  task.identifier.toUuid() to
                      listOf(
                          taskAttachment,
                          taskAttachment.copy(
                              identifier = "a38bebf0-37eb-4f13-aa4d-bcbf0bcdbd3d".toUUID()),
                          taskAttachment.copy(
                              identifier = "8b00dd46-8a11-4387-9ce0-a4589bd0b958".toUUID(),
                              fileName = "different-name.pdf"))))

      val entries = collector.collect()

      assertThat(entries.filterIsInstance<AttachmentBlobZipExportEntry>())
          .isEqualTo(
              listOf(
                  AttachmentBlobZipExportEntry(
                      "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071/attachment-1.jpg",
                      taskAttachment.copy(fileName = "attachment-1.jpg")),
                  AttachmentBlobZipExportEntry(
                      "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071/attachment-2.jpg",
                      taskAttachment.copy(
                          identifier = "a38bebf0-37eb-4f13-aa4d-bcbf0bcdbd3d".toUUID(),
                          fileName = "attachment-2.jpg")),
                  AttachmentBlobZipExportEntry(
                      "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071/different-name.pdf",
                      taskAttachment.copy(
                          identifier = "8b00dd46-8a11-4387-9ce0-a4589bd0b958".toUUID(),
                          fileName = "different-name.pdf"))))
    }

    @Nested
    inner class `With a Topic` {

      val topic =
          TopicSnapshot(
              identifier = "6708c38b-c2a3-4610-b298-42d26667ed3f".asTopicId(),
              description = "My topic",
              criticality = CRITICAL,
              taskIdentifier = task.identifier,
              projectIdentifier = task.projectIdentifier)

      val topicAttachment =
          AttachmentDto(
              identifier = "8cc067bd-1e9d-4a43-833e-78b3c68d8467".toUUID(),
              version = 0L,
              fileName = "topic-attachment.jpg",
              fileSize = 123L,
              fullAvailable = true,
              smallAvailable = false,
              createdByIdentifier = "3a6ca40f-c79a-4e38-83ac-1f3e2af63af7".toUUID(),
              createdDate = Date.valueOf(LocalDate.of(2023, 11, 27)),
              createdByDeleted = false,
              lastModifiedByIdentifier = "3a6ca40f-c79a-4e38-83ac-1f3e2af63af7".toUUID(),
              lastModifiedDate = Date.valueOf(LocalDate.of(2023, 11, 27)),
              lastModifiedByDeleted = false,
              taskIdentifier = task.identifier,
              topicIdentifier = topic.identifier)

      @Test
      fun `lists that Topic in the metadata of the Task`() {
        val collector =
            ZipExportEntryCollector(project = project, tasks = listOf(task), topics = listOf(topic))

        val entries = collector.collect()

        assertThat(
                entries
                    .filterIsInstance<MetadataZipExportEntry>()
                    .first { it.fileName.contains("task.json") }
                    .content)
            .contains(""""topics": ["6708c38b-c2a3-4610-b298-42d26667ed3f"]""")
      }

      @Test
      fun `exports Topic metadata into a subdirectory`() {
        val collector =
            ZipExportEntryCollector(project = project, tasks = listOf(task), topics = listOf(topic))

        val entries = collector.collect()

        assertThat(entries.drop(4))
            .isEqualTo(
                listOf(
                    DirectoryZipExportEntry(
                        "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071" +
                            "/6708c38b-c2a3-4610-b298-42d26667ed3f"),
                    MetadataZipExportEntry(
                        "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071" +
                            "/6708c38b-c2a3-4610-b298-42d26667ed3f" +
                            "/topic.json",
                        """{
                          | "description": "My topic",
                          | "criticality": "CRITICAL",
                          | "messages": [],
                          | "attachments": []
                          |}
                        """
                            .trimMargin())))
      }

      @Test
      fun `lists Topic Attachment in Topic metadata`() {
        val collector =
            ZipExportEntryCollector(
                project = project,
                tasks = listOf(task),
                topics = listOf(topic),
                topicAttachments = mapOf(topic.identifier to listOf(topicAttachment)))

        val entries = collector.collect()

        assertThat(
                entries
                    .filterIsInstance<MetadataZipExportEntry>()
                    .first { it.fileName.contains("topic.json") }
                    .content)
            .contains(""""attachments": ["topic-attachment.jpg"]""")
      }

      @Test
      fun `exports Topic Attachment`() {
        val collector =
            ZipExportEntryCollector(
                project = project,
                tasks = listOf(task),
                topics = listOf(topic),
                topicAttachments = mapOf(topic.identifier to listOf(topicAttachment)))

        val entries = collector.collect()

        assertThat(
                entries.filterIsInstance<AttachmentBlobZipExportEntry>().first {
                  it.fileName.contains("topic-attachment.jpg")
                })
            .isEqualTo(
                AttachmentBlobZipExportEntry(
                    "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071" +
                        "/6708c38b-c2a3-4610-b298-42d26667ed3f" +
                        "/topic-attachment.jpg",
                    topicAttachment))
      }

      @Nested
      inner class `With a Message` {

        val message =
            MessageSnapshot(
                identifier = "ff484e11-f5aa-4ee4-8fbc-360d9e628662".toUUID().asMessageId(),
                content = "My message.",
                topicIdentifier = topic.identifier,
                projectIdentifier = project.identifier)

        val messageAttachment =
            AttachmentDto(
                identifier = "e73818d2-a0c0-488a-a6ce-47879b3438ba".toUUID(),
                version = 0L,
                fileName = "message-attachment.jpg",
                fileSize = 123L,
                fullAvailable = true,
                smallAvailable = false,
                createdByIdentifier = "3a6ca40f-c79a-4e38-83ac-1f3e2af63af7".toUUID(),
                createdDate = Date.valueOf(LocalDate.of(2023, 11, 27)),
                createdByDeleted = false,
                lastModifiedByIdentifier = "3a6ca40f-c79a-4e38-83ac-1f3e2af63af7".toUUID(),
                lastModifiedDate = Date.valueOf(LocalDate.of(2023, 11, 27)),
                lastModifiedByDeleted = false,
                taskIdentifier = task.identifier,
                topicIdentifier = topic.identifier,
                messageIdentifier = message.identifier)

        @Test
        fun `lists that Message in the metadata of the Topic`() {
          val collector =
              ZipExportEntryCollector(
                  project = project,
                  tasks = listOf(task),
                  topics = listOf(topic),
                  messages = listOf(message))

          val entries = collector.collect()

          assertThat(
                  entries
                      .filterIsInstance<MetadataZipExportEntry>()
                      .first { it.fileName.contains("topic.json") }
                      .content)
              .contains(""""messages": ["ff484e11-f5aa-4ee4-8fbc-360d9e628662"]""")
        }

        @Test
        fun `exports Message metadata into a subdirectory`() {
          val collector =
              ZipExportEntryCollector(
                  project = project,
                  tasks = listOf(task),
                  topics = listOf(topic),
                  messages = listOf(message))

          val entries = collector.collect()

          assertThat(entries.drop(6))
              .isEqualTo(
                  listOf(
                      DirectoryZipExportEntry(
                          "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071" +
                              "/6708c38b-c2a3-4610-b298-42d26667ed3f" +
                              "/ff484e11-f5aa-4ee4-8fbc-360d9e628662"),
                      MetadataZipExportEntry(
                          "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071" +
                              "/6708c38b-c2a3-4610-b298-42d26667ed3f/ff484e11-f5aa-4ee4-8fbc-360d9e628662" +
                              "/message.json",
                          """{
                          | "content": "My message.",
                          | "attachments": []
                          |}
                        """
                              .trimMargin())))
        }

        @Test
        fun `lists Message Attachment in Message metadata`() {
          val collector =
              ZipExportEntryCollector(
                  project = project,
                  tasks = listOf(task),
                  topics = listOf(topic),
                  messages = listOf(message),
                  messageAttachments = mapOf(message.identifier to listOf(messageAttachment)))

          val entries = collector.collect()

          assertThat(
                  entries
                      .filterIsInstance<MetadataZipExportEntry>()
                      .first { it.fileName.contains("message.json") }
                      .content)
              .contains(""""attachments": ["message-attachment.jpg"]""")
        }

        @Test
        fun `exports Message Attachment`() {
          val collector =
              ZipExportEntryCollector(
                  project = project,
                  tasks = listOf(task),
                  topics = listOf(topic),
                  messages = listOf(message),
                  messageAttachments = mapOf(message.identifier to listOf(messageAttachment)))

          val entries = collector.collect()

          assertThat(
                  entries.filterIsInstance<AttachmentBlobZipExportEntry>().first {
                    it.fileName.contains("message-attachment.jpg")
                  })
              .isEqualTo(
                  AttachmentBlobZipExportEntry(
                      "Elbphilharmonie/6c643268-4ea6-404a-9eca-55d3364f5071" +
                          "/6708c38b-c2a3-4610-b298-42d26667ed3f/ff484e11-f5aa-4ee4-8fbc-360d9e628662" +
                          "/message-attachment.jpg",
                      messageAttachment))
        }
      }
    }
  }
}
