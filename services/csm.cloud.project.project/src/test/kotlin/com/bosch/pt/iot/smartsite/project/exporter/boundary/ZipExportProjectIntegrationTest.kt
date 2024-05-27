/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MESSAGE
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ZipExportProjectIntegrationTest : AbstractExportIntegrationTest() {

  @Autowired private lateinit var zipProjectExportService: ZipProjectExportService

  @MockkBean private lateinit var attachmentService: AttachmentService

  private val projectIdentifier by lazy { getIdentifier("p2").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitProjectImportFeatureToggle()
        .submitProject("p2") {
          it.title = "Elbphilharmonie"
          it.projectNumber = "123"
          it.start = LocalDate.of(2023, 12, 12).toEpochMilli()
          it.end = LocalDate.of(2023, 12, 25).toEpochMilli()
        }
        .submitProjectCraftG2("pc1") { it.name = "pc1" }
        .submitParticipantG3("p2Csm1") {
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.aggregateIdentifier =
              AggregateIdentifierAvro().apply {
                type = TASK.name
                identifier = "a5cb75e7-0646-4f54-ace5-0424720e7c56"
              }
          it.name = "t1"
          it.description = "description"
        }
        .submitTaskAttachment(asReference = "ta1")
        .submitTopicG2(asReference = "topic1") {
          it.aggregateIdentifier =
              AggregateIdentifierAvro().apply {
                type = TOPIC.name
                identifier = "511cdb7e-1cb7-4bf6-8064-99fa49d76736"
              }
          it.task = getByReference("t1")
          it.description = "Topic Description"
          it.criticality = TopicCriticalityEnumAvro.UNCRITICAL
        }
        .submitTopicAttachment(asReference = "topicAttachment1")
        .submitMessage(asReference = "message1") {
          it.aggregateIdentifier =
              AggregateIdentifierAvro().apply {
                type = MESSAGE.name
                identifier = "3f9c0261-d0e0-4123-87b0-26a5eac76dd5"
              }
          it.topic = getByReference("topic1")
          it.content = "Content"
        }
        .submitMessageAttachment(asReference = "messageAttachment1")
        .submitProjectCraftG2("pc1") { it.name = "pc1" }

    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @Test
  fun exportsToZipFile() {
    every { attachmentService.openAttachment(any(), any()) } returns
        "test-content".byteInputStream()
    val baos = ByteArrayOutputStream()

    baos.use { zipProjectExportService.exportZip(projectIdentifier, baos) }

    ZipInputStream(baos.toByteArray().inputStream()).use { zipStream ->
      assertThat(
              generateSequence { zipStream.nextEntry }
                  .flatMap { listOf(it.name, String(zipStream.readAllBytes())) }
                  .filter { it.isNotEmpty() }
                  .toList())
          .isEqualTo(
              listOf(
                  "/Elbphilharmonie/",
                  "/Elbphilharmonie/project.json",
                  """{
                    | "title": "Elbphilharmonie",
                    | "projectNumber": "123",
                    | "description": "No description",
                    | "start": "2023-12-12",
                    | "end": "2023-12-25",
                    | "projectAddress": {
                    |  "street": "Default test street",
                    |  "houseNumber": "1",
                    |  "zipCode": "12345",
                    |  "city": "Default test town"
                    | },
                    | "tasks": ["a5cb75e7-0646-4f54-ace5-0424720e7c56"]
                    |}"""
                      .trimMargin(),
                  "/Elbphilharmonie/a5cb75e7-0646-4f54-ace5-0424720e7c56/",
                  "/Elbphilharmonie/a5cb75e7-0646-4f54-ace5-0424720e7c56/task.json",
                  """{
                    | "name": "t1",
                    | "status": "OPEN",
                    | "topics": ["511cdb7e-1cb7-4bf6-8064-99fa49d76736"],
                    | "attachments": ["myPicture.jpg"]
                    |}"""
                      .trimMargin(),
                  "/Elbphilharmonie/a5cb75e7-0646-4f54-ace5-0424720e7c56/myPicture.jpg",
                  "test-content",
                  "/Elbphilharmonie/a5cb75e7-0646-4f54-ace5-0424720e7c56/511cdb7e-1cb7-4bf6-8064-99fa49d76736/",
                  "/Elbphilharmonie/a5cb75e7-0646-4f54-ace5-0424720e7c56/511cdb7e-1cb7-4bf6-8064-99fa49d76736" +
                      "/topic.json",
                  """{
                    | "description": "Topic Description",
                    | "criticality": "UNCRITICAL",
                    | "messages": ["3f9c0261-d0e0-4123-87b0-26a5eac76dd5"],
                    | "attachments": ["myPicture.jpg"]
                    |}"""
                      .trimMargin(),
                  "/Elbphilharmonie/a5cb75e7-0646-4f54-ace5-0424720e7c56/511cdb7e-1cb7-4bf6-8064-99fa49d76736" +
                      "/myPicture.jpg",
                  "/Elbphilharmonie/a5cb75e7-0646-4f54-ace5-0424720e7c56/511cdb7e-1cb7-4bf6-8064-99fa49d76736" +
                      "/3f9c0261-d0e0-4123-87b0-26a5eac76dd5/",
                  "/Elbphilharmonie/a5cb75e7-0646-4f54-ace5-0424720e7c56/511cdb7e-1cb7-4bf6-8064-99fa49d76736" +
                      "/3f9c0261-d0e0-4123-87b0-26a5eac76dd5/message.json",
                  """{
                    | "content": "Content",
                    | "attachments": ["myPicture.jpg"]
                    |}"""
                      .trimMargin(),
                  "/Elbphilharmonie/a5cb75e7-0646-4f54-ace5-0424720e7c56" +
                      "/511cdb7e-1cb7-4bf6-8064-99fa49d76736/3f9c0261-d0e0-4123-87b0-26a5eac76dd5" +
                      "/myPicture.jpg"))
    }
  }
}
