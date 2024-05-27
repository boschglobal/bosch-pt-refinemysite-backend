/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getCompanyIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestoneList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAddressAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.project.copy.boundary.ExportSettings.Companion.exportOnlyBasicProjectData
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ActiveParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.DayCardDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.MessageDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.MilestoneDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.OtherParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectCraftDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.TaskDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.TopicDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.WorkAreaDto
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.BAD_WEATHER
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INACTIVE
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class GenericExporterTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var genericExporter: GenericExporter

  private val projectIdToExport: ProjectId by lazy { getIdentifier("export-project").asProjectId() }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData()
    eventStreamGenerator
        .setUserContext("userCsm1")
        .submitProject("export-project") {
          it.title = "Export Project"
          it.client = "Bosch PT"
          it.description = "New Offices"
          it.start = LocalDate.of(2023, 2, 5).toEpochMilli()
          it.end = LocalDate.of(2023, 3, 21).toEpochMilli()
          it.projectNumber = "123"
          it.category = ProjectCategoryEnumAvro.NB
          it.projectAddress =
              ProjectAddressAvro().apply {
                street = "Test Street"
                houseNumber = "1"
                city = "Test Town"
                zipCode = "12345"
              }
        }
        .submitWorkdayConfiguration() // TODO: How do we want to export WorkDayConfigurations?
        .submitParticipantG3("export-participant-csm") {
          it.user = getByReference("userCsm2")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3("export-participant-fm-inactive") {
          it.user = getByReference("user")
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        .submitProjectCraftG2("export-craft") {
          it.name = "Export Craft"
          it.color = "#ff0000"
        }
        .submitWorkArea("export-workarea-1") { it.name = "Export WorkArea 1" }
        .submitWorkArea("export-workarea-2") { it.name = "Export WorkArea 2" }
        .submitWorkAreaList("export-workarea-list") {
          it.workAreas =
              // order intentionally reversed
              listOf(getByReference("export-workarea-2"), getByReference("export-workarea-1"))
        }
        .submitMilestone("export-milestone-1") {
          it.name = "My first Milestone"
          it.type = MilestoneTypeEnumAvro.PROJECT
          it.date = LocalDate.of(2023, 2, 12).toEpochMilli()
          it.header = false
          it.craft = getByReference("export-craft")
          it.workarea = getByReference("export-workarea-2")
          it.description = "My project milestone"
        }
        .submitMilestone("export-milestone-2") {
          it.name = "My second Milestone"
          it.type = MilestoneTypeEnumAvro.PROJECT
          it.date = LocalDate.of(2023, 2, 12).toEpochMilli()
          it.header = false
          it.craft = getByReference("export-craft")
          it.workarea = getByReference("export-workarea-2")
          it.description = "My project milestone"
        }
        .submitMilestoneList("export-milestonelist") {
          it.date = LocalDate.of(2023, 2, 12).toEpochMilli()
          it.header = false
          it.workarea = getByReference("export-workarea-2")
          it.milestones =
              // order intentionally reversed
              listOf(getByReference("export-milestone-2"), getByReference("export-milestone-1"))
        }
        .submitTask("export-task") {
          it.name = "Export Task"
          it.description = "My task to export"
          it.location = "On top of a large tree"
          it.craft = getByReference("export-craft")
          it.assignee = getByReference("export-participant-fm-inactive")
          it.workarea = getByReference("export-workarea-1")
          it.status = TaskStatusEnumAvro.OPEN
        }
        .submitTaskSchedule("export-task-schedule")
        .submitDayCardG2("export-task-daycard") {
          it.title = "My first DayCard"
          it.manpower = BigDecimal.TEN
          it.notes = "my notes"
          it.status = DayCardStatusEnumAvro.OPEN
          it.reason = DayCardReasonNotDoneEnumAvro.BAD_WEATHER
        }
        .submitTaskSchedule("export-task-schedule", eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = LocalDate.of(2023, 2, 13).toEpochMilli()
          it.end = LocalDate.of(2023, 2, 17).toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(
                      LocalDate.of(2023, 2, 14).toEpochMilli(),
                      getByReference("export-task-daycard")))
        }
        .submitRelation("export-relation") {
          it.critical = false
          it.source = getByReference("export-task")
          it.target = getByReference("export-milestone-1")
        }
        .submitTopicG2(asReference = "export-topic") {
          it.criticality = TopicCriticalityEnumAvro.CRITICAL
          it.description = "My wonderful Topic"
        }
        .submitMessage("export-message-1") {
          it.topic = getByReference("export-topic")
          it.content = "My first message"
          it.auditingInformation =
              AuditingInformationAvro().apply {
                createdDate = LocalDateTime.of(2023, 3, 1, 14, 31, 23).toEpochMilli()
                createdBy = getByReference("user")
                lastModifiedBy = getByReference("user")
              }
        }
        .submitMessage("export-message-2") {
          it.topic = getByReference("export-topic")
          it.content = "My second message"
          it.auditingInformation =
              AuditingInformationAvro().apply {
                createdDate = LocalDateTime.of(2023, 3, 1, 14, 44, 49).toEpochMilli()
                createdBy = getByReference("userCsm2")
                lastModifiedBy = getByReference("userCsm2")
              }
        }

    setAuthentication("userCsm2")
  }

  @Test
  fun `exports only basic Project parameters`() {
    val export = genericExporter.export(projectIdToExport, exportOnlyBasicProjectData)

    assertThat(export)
        .isEqualTo(
            ProjectDto(
                identifier = projectIdToExport,
                client = "Bosch PT",
                description = "New Offices",
                start = LocalDate.of(2023, 2, 5),
                end = LocalDate.of(2023, 3, 21),
                projectNumber = "123",
                title = "Export Project",
                category = ProjectCategoryEnum.NB,
                address =
                    ProjectAddressVo(
                        street = "Test Street",
                        houseNumber = "1",
                        city = "Test Town",
                        zipCode = "12345"),
                participants = emptySet(),
                projectCrafts = emptyList(),
                workAreas = emptyList(),
                milestones = emptyList(),
                tasks = emptyList(),
                relations = emptyList()))
  }

  @Test
  fun `exports Participants`() {
    val activeParticipantCsm = get<ParticipantAggregateG3Avro>("export-participant-csm")!!
    val inactiveParticipantFm = get<ParticipantAggregateG3Avro>("export-participant-fm-inactive")!!

    val export =
        genericExporter.export(
            projectIdToExport, exportOnlyBasicProjectData.copy(exportParticipants = true))

    assertThat(export.participants)
        .isEqualTo(
            setOf(
                ActiveParticipantDto(
                    identifier = activeParticipantCsm.getIdentifier().asParticipantId(),
                    userId = activeParticipantCsm.getUserIdentifier()!!.asUserId(),
                    companyId = activeParticipantCsm.getCompanyIdentifier()!!.asCompanyId(),
                    role = CSM),
                OtherParticipantDto(
                    identifier = inactiveParticipantFm.getIdentifier().asParticipantId(),
                    userId = inactiveParticipantFm.getUserIdentifier()?.asUserId(),
                    companyId = inactiveParticipantFm.getCompanyIdentifier()?.asCompanyId(),
                    role = FM,
                    status = INACTIVE)))
  }

  @Test
  fun `exports ProjectCrafts`() {
    val projectCraftId = getIdentifier("export-craft").asProjectCraftId()

    val export =
        genericExporter.export(
            projectIdToExport, exportOnlyBasicProjectData.copy(exportCrafts = true))

    assertThat(export.projectCrafts)
        .isEqualTo(
            listOf(
                ProjectCraftDto(
                    identifier = projectCraftId, name = "Export Craft", color = "#ff0000")))
  }

  @Test
  fun `exports WorkAreas`() {
    val workAreaId1 = getIdentifier("export-workarea-1").asWorkAreaId()
    val workAreaId2 = getIdentifier("export-workarea-2").asWorkAreaId()

    val export =
        genericExporter.export(
            projectIdToExport, exportOnlyBasicProjectData.copy(exportWorkAreas = true))

    assertThat(export.workAreas)
        .isEqualTo(
            listOf(
                WorkAreaDto(identifier = workAreaId2, name = "Export WorkArea 2"),
                WorkAreaDto(identifier = workAreaId1, name = "Export WorkArea 1")))
  }

  @Test
  fun `exports Milestones`() {
    val milestoneId1 = getIdentifier("export-milestone-1").asMilestoneId()
    val milestoneId2 = getIdentifier("export-milestone-2").asMilestoneId()
    val projectCraftId = getIdentifier("export-craft").asProjectCraftId()
    val workAreaId2 = getIdentifier("export-workarea-2").asWorkAreaId()

    val export =
        genericExporter.export(
            projectIdToExport, exportOnlyBasicProjectData.copy(exportMilestones = true))

    assertThat(export.milestones)
        .isEqualTo(
            listOf(
                MilestoneDto(
                    identifier = milestoneId2,
                    name = "My second Milestone",
                    type = PROJECT,
                    date = LocalDate.of(2023, 2, 12),
                    header = false,
                    projectCraft = projectCraftId,
                    workArea = workAreaId2,
                    description = "My project milestone"),
                MilestoneDto(
                    identifier = milestoneId1,
                    name = "My first Milestone",
                    type = PROJECT,
                    date = LocalDate.of(2023, 2, 12),
                    header = false,
                    projectCraft = projectCraftId,
                    workArea = workAreaId2,
                    description = "My project milestone")))
  }

  @Test
  fun `exports Tasks without Daycards`() {
    val taskId = getIdentifier("export-task").asTaskId()
    val projectCraftId = getIdentifier("export-craft").asProjectCraftId()
    val inactiveParticipantId = getIdentifier("export-participant-fm-inactive").asParticipantId()
    val workAreaId1 = getIdentifier("export-workarea-1").asWorkAreaId()

    val export =
        genericExporter.export(
            projectIdToExport,
            exportOnlyBasicProjectData.copy(exportTasks = true, exportDayCards = false))

    assertThat(export.tasks)
        .isEqualTo(
            listOf(
                TaskDto(
                    identifier = taskId,
                    name = "Export Task",
                    description = "My task to export",
                    location = "On top of a large tree",
                    projectCraft = projectCraftId,
                    assignee = inactiveParticipantId,
                    workArea = workAreaId1,
                    status = DRAFT,
                    start = LocalDate.of(2023, 2, 13),
                    end = LocalDate.of(2023, 2, 17),
                    dayCards = emptyList(),
                    topics = emptyList())))
  }

  @Test
  fun `exports Tasks with Daycards`() {
    val dayCardId = getIdentifier("export-task-daycard").asDayCardId()

    val export =
        genericExporter.export(
            projectIdToExport,
            exportOnlyBasicProjectData.copy(exportTasks = true, exportDayCards = true))

    assertThat(export.tasks.first().dayCards)
        .isEqualTo(
            listOf(
                DayCardDto(
                    identifier = dayCardId,
                    date = LocalDate.of(2023, 2, 14),
                    title = "My first DayCard",
                    manpower = BigDecimal.TEN.setScale(2),
                    notes = "my notes",
                    status = OPEN,
                    reason = BAD_WEATHER)))
  }

  @Test
  fun `exports Tasks with status`() {
    val export =
        genericExporter.export(
            projectIdToExport,
            exportOnlyBasicProjectData.copy(exportTasks = true, exportTaskStatus = true))

    assertThat(export.tasks.first().status).isEqualTo(TaskStatusEnum.OPEN)
  }

  @Test
  fun `exports Tasks without status`() {
    val export =
        genericExporter.export(
            projectIdToExport, exportOnlyBasicProjectData.copy(exportTasks = true))

    assertThat(export.tasks.first().status).isEqualTo(DRAFT)
  }

  @Test
  fun `exports Relation between exported Tasks and Milestones`() {
    val milestoneId = getIdentifier("export-milestone-1").asMilestoneId()
    val taskId = getIdentifier("export-task")

    val export =
        genericExporter.export(
            projectIdToExport,
            exportOnlyBasicProjectData.copy(
                exportMilestones = true, exportTasks = true, exportRelations = true))

    assertThat(export.relations)
        .isEqualTo(
            listOf(
                RelationDto(
                    type = FINISH_TO_START,
                    source = RelationElementDto(id = taskId, type = TASK),
                    target = RelationElementDto(id = milestoneId.toUuid(), type = MILESTONE),
                    criticality = false)))
  }

  @Test
  fun `does not export Relation if source element is not exported either`() {
    val export =
        genericExporter.export(
            projectIdToExport,
            exportOnlyBasicProjectData.copy(
                exportMilestones = true, exportTasks = false, exportRelations = true))

    assertThat(export.relations).isEmpty()
  }

  @Test
  fun `does not export Relation if target element is not exported either`() {
    val export =
        genericExporter.export(
            projectIdToExport,
            exportOnlyBasicProjectData.copy(
                exportMilestones = false, exportTasks = true, exportRelations = true))

    assertThat(export.relations).isEmpty()
  }

  @Test
  fun `exports Topic with Messages`() {
    val export =
        genericExporter.export(
            projectIdToExport,
            exportOnlyBasicProjectData.copy(exportTasks = true, exportTopics = true))

    assertThat(export.tasks.first().topics)
        .isEqualTo(
            listOf(
                TopicDto(
                    identifier = getIdentifier("export-topic").asTopicId(),
                    criticality = CRITICAL,
                    description = "My wonderful Topic",
                    messages =
                        listOf(
                            MessageDto(
                                identifier = getIdentifier("export-message-1").asMessageId(),
                                timestamp = LocalDateTime.of(2023, 3, 1, 14, 31, 23),
                                author = getIdentifier("user").asUserId(),
                                content = "My first message"),
                            MessageDto(
                                identifier = getIdentifier("export-message-2").asMessageId(),
                                timestamp = LocalDateTime.of(2023, 3, 1, 14, 44, 49),
                                author = getIdentifier("userCsm2").asUserId(),
                                content = "My second message")))))
  }
}
