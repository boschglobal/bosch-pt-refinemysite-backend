/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getCompanyIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.project.copy.boundary.GenericImporter.ImportEverythingMergeStrategy
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
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.BAD_WEATHER
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.APPROVED
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.DONE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.NOTDONE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.api.CreateProjectCommand
import com.bosch.pt.iot.smartsite.project.project.command.handler.CreateProjectCommandHandler
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import io.mockk.spyk
import io.mockk.verify
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class GenericImporterTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var genericExporter: GenericExporter
  @Autowired private lateinit var genericImporter: GenericImporter
  @Autowired private lateinit var createProjectCommandHandler: CreateProjectCommandHandler

  private val copyingUser: UserId by lazy { getIdentifier("userCsm2").asUserId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
    setAuthentication(copyingUser.toUuid())
  }

  @Test
  fun `creates Project if it does not exist`() {
    genericImporter.import(basicProject)

    assertThat(exportOf(basicProject).withoutCopyingUserAsParticipant()).isEqualTo(basicProject)
  }

  @Test
  fun `imports into existing Project using the specified MergeStrategy`() {
    createEmptyProject(basicProject)
    val mergeStrategy = spyk(ImportEverythingMergeStrategy())

    genericImporter.import(basicProject, mergeStrategy)

    verify { mergeStrategy.merge(basicProject, any()) }
  }

  @Test
  fun `imports active Participants`() {
    val participantFM =
        EventStreamGeneratorStaticExtensions.get<ParticipantAggregateG3Avro>("participant")!!
    val project =
        basicProject.copy(
            participants =
                setOf(
                    ActiveParticipantDto(
                        identifier = ParticipantId("69cb5497-2a4f-4a01-a6d0-2a9a94f55ad0"),
                        companyId = participantFM.getCompanyIdentifier()!!.asCompanyId(),
                        userId = participantFM.getUserIdentifier()!!.asUserId(),
                        role = FM)))

    genericImporter.import(project)

    assertThat(exportOf(project).withoutCopyingUserAsParticipant()).isEqualTo(project)
  }

  @Test
  fun `does not import other Participants`() {
    val invitedParticipantId = ParticipantId("4d123af2-a17e-4f9d-8972-b3713848054e")
    val project =
        basicProject.copy(
            participants =
                setOf(
                    OtherParticipantDto(
                        identifier = invitedParticipantId,
                        companyId = null,
                        userId = null,
                        role = FM,
                        status = INVITED)))

    genericImporter.import(project)

    assertThat(exportOf(project).participants.map { it.identifier })
        .doesNotContain(invitedParticipantId)
  }

  @Test
  fun `imports ProjectCrafts`() {
    val project =
        basicProject.copy(
            projectCrafts =
                listOf(
                    ProjectCraftDto(
                        identifier = ProjectCraftId(), name = "Craft A", color = "#ff0000"),
                    ProjectCraftDto(
                        identifier = ProjectCraftId(), name = "Craft B", color = "pink")))

    genericImporter.import(project)

    assertThat(exportOf(project).projectCrafts).isEqualTo(project.projectCrafts)
  }

  @Test
  fun `imports WorkAreas`() {
    val project =
        basicProject.copy(
            workAreas =
                listOf(
                    WorkAreaDto(identifier = WorkAreaId(), name = "Working Area 1"),
                    WorkAreaDto(identifier = WorkAreaId(), name = "Working Area 2")))

    genericImporter.import(project)

    assertThat(exportOf(project).workAreas).isEqualTo(project.workAreas)
  }

  @Test
  fun `imports Milestones`() {
    val projectCraftId = ProjectCraftId()
    val workAreaId = WorkAreaId()
    val project =
        basicProject.copy(
            projectCrafts =
                listOf(
                    ProjectCraftDto(
                        identifier = projectCraftId, name = "Electrician", color = "sparkly blue")),
            workAreas = listOf(WorkAreaDto(identifier = workAreaId, name = "Cellar")),
            milestones =
                listOf(
                    MilestoneDto(
                        identifier = MilestoneId(),
                        name = "Milestone 1",
                        type = PROJECT,
                        date = LocalDate.of(2023, 2, 12),
                        header = true,
                        description = "Important Project Milestone"),
                    MilestoneDto(
                        identifier = MilestoneId(),
                        name = "Milestone 2",
                        type = CRAFT,
                        date = LocalDate.of(2023, 2, 14),
                        header = false,
                        projectCraft = projectCraftId,
                        workArea = workAreaId,
                        description = "Electrician Milestone")))

    genericImporter.import(project)

    assertThat(exportOf(project).milestones).isEqualTo(project.milestones)
  }

  @Test
  fun `appends Milestones to existing list`() {
    val projectWithOneMilestone =
        basicProject.copy(
            milestones =
                listOf(
                    MilestoneDto(
                        identifier = MilestoneId(),
                        name = "Milestone 1",
                        type = PROJECT,
                        date = LocalDate.of(2023, 2, 12),
                        header = true)))
    val milestoneToAppend =
        MilestoneDto(
            identifier = MilestoneId(),
            name = "Milestone 2",
            type = PROJECT,
            date = LocalDate.of(2023, 2, 12),
            header = true)
    val projectWithMilestoneToAppend = basicProject.copy(milestones = listOf(milestoneToAppend))
    genericImporter.import(projectWithOneMilestone)

    genericImporter.import(projectWithMilestoneToAppend)

    assertThat(exportOf(projectWithOneMilestone).milestones)
        .isEqualTo(projectWithOneMilestone.milestones + milestoneToAppend)
  }

  @Nested
  inner class `For Projects with Tasks` {

    private val projectCraft =
        ProjectCraftDto(identifier = ProjectCraftId(), name = "Electrician", color = "sparkly blue")
    private val workArea = WorkAreaDto(identifier = WorkAreaId(), name = "Cellar")
    private val simpleTask =
        TaskDto(
            identifier = TaskId(),
            name = "Task 1",
            description = "Simple unscheduled task",
            location = "Cloud No. 9",
            projectCraft = projectCraft.identifier,
            assignee = null,
            workArea = null,
            status = DRAFT,
            start = null,
            end = null)

    @Test
    fun `imports simple, unscheduled Task`() {
      val projectWithSimpleTask =
          basicProject.copy(projectCrafts = listOf(projectCraft), tasks = listOf(simpleTask))

      genericImporter.import(projectWithSimpleTask)

      assertThat(exportOf(projectWithSimpleTask).tasks).isEqualTo(projectWithSimpleTask.tasks)
    }

    @ParameterizedTest
    @EnumSource(TaskStatusEnum::class)
    fun `imports Tasks in all states`(status: TaskStatusEnum) {
      val projectWithSimpleTask =
          basicProject.copy(
              projectCrafts = listOf(projectCraft),
              tasks = listOf(simpleTask.copy(status = status)))

      genericImporter.import(projectWithSimpleTask)

      assertThat(exportOf(projectWithSimpleTask).tasks).isEqualTo(projectWithSimpleTask.tasks)
    }

    @Test
    fun `imports Task with start date and no end date`() {
      val project =
          basicProject.copy(
              projectCrafts = listOf(projectCraft),
              tasks = listOf(simpleTask.copy(start = LocalDate.of(2023, 2, 5))))

      genericImporter.import(project)

      assertThat(exportOf(project).tasks).isEqualTo(project.tasks)
    }

    @Test
    fun `imports Task with end date and no start date`() {
      val project =
          basicProject.copy(
              projectCrafts = listOf(projectCraft),
              tasks = listOf(simpleTask.copy(end = LocalDate.of(2023, 2, 12))))

      genericImporter.import(project)

      assertThat(exportOf(project).tasks).isEqualTo(project.tasks)
    }

    @Test
    fun `imports fully scheduled Task with assignee`() {
      val participantFM =
          EventStreamGeneratorStaticExtensions.get<ParticipantAggregateG3Avro>("participant")!!
      val fmParticipantId = ParticipantId()
      val project =
          basicProject.copy(
              participants =
                  setOf(
                      ActiveParticipantDto(
                          identifier = fmParticipantId,
                          companyId = participantFM.getCompanyIdentifier()!!.asCompanyId(),
                          userId = participantFM.getUserIdentifier()!!.asUserId(),
                          role = FM)),
              projectCrafts = listOf(projectCraft),
              workAreas = listOf(workArea),
              tasks =
                  listOf(
                      simpleTask.copy(
                          assignee = fmParticipantId,
                          workArea = workArea.identifier,
                          start = LocalDate.of(2023, 2, 5),
                          end = LocalDate.of(2023, 2, 12),
                          dayCards = emptyList())))

      genericImporter.import(project)

      assertThat(exportOf(project).tasks).isEqualTo(project.tasks)
    }

    @Test
    fun `imports Task with DayCards in all states`() {
      val project =
          basicProject.copy(
              projectCrafts = listOf(projectCraft),
              tasks =
                  listOf(
                      simpleTask.copy(
                          start = LocalDate.of(2023, 2, 5),
                          end = LocalDate.of(2023, 2, 12),
                          dayCards =
                              listOf(
                                  DayCardDto(
                                      identifier = DayCardId(),
                                      date = LocalDate.of(2023, 2, 6),
                                      title = "Get set up",
                                      manpower = BigDecimal(3).setScale(2),
                                      status = DONE),
                                  DayCardDto(
                                      identifier = DayCardId(),
                                      date = LocalDate.of(2023, 2, 7),
                                      title = "Do the work",
                                      manpower = BigDecimal(5).setScale(2),
                                      notes = "No notes",
                                      status = NOTDONE,
                                      reason = BAD_WEATHER),
                                  DayCardDto(
                                      identifier = DayCardId(),
                                      date = LocalDate.of(2023, 2, 8),
                                      title = "Do the work again",
                                      manpower = BigDecimal(5).setScale(2),
                                      notes = "No notes",
                                      status = APPROVED),
                                  DayCardDto(
                                      identifier = DayCardId(),
                                      date = LocalDate.of(2023, 2, 9),
                                      title = "Finish up",
                                      manpower = BigDecimal(2).setScale(2),
                                      notes = "No notes",
                                      status = OPEN)))))

      genericImporter.import(project)

      assertThat(exportOf(project).tasks).isEqualTo(project.tasks)
    }

    @Test
    fun `imports Task with DayCards fails when the task has no task schedules`() {
      val project =
          basicProject.copy(
              projectCrafts = listOf(projectCraft),
              tasks =
                  listOf(
                      simpleTask.copy(
                          start = null,
                          end = null,
                          dayCards =
                              listOf(
                                  DayCardDto(
                                      identifier = DayCardId(),
                                      date = LocalDate.of(2023, 2, 9),
                                      title = "Finish up",
                                      manpower = BigDecimal(2).setScale(2),
                                      notes = "No notes",
                                      status = OPEN)))))

      val response =
          Assertions.catchThrowableOfType(
              { genericImporter.import(project) }, AggregateNotFoundException::class.java)

      assertThat(response.messageKey).isEqualTo(Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND)
      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `imports Task with Topics`() {
      val projectWithTaskWithTopic =
          basicProject.copy(
              projectCrafts = listOf(projectCraft),
              tasks =
                  listOf(
                      simpleTask.copy(
                          topics =
                              listOf(
                                  TopicDto(
                                      identifier =
                                          "9e24b968-0388-4a7c-aa59-d34fa189b352".asTopicId(),
                                      criticality = CRITICAL,
                                      description = "Critical Topic",
                                      messages =
                                          listOf(
                                              MessageDto(
                                                  identifier =
                                                      "deb92357-4f6e-4e79-b8db-8003d052c2d6"
                                                          .toUUID()
                                                          .asMessageId(),
                                                  timestamp =
                                                      LocalDateTime.of(2023, 3, 1, 14, 31, 23),
                                                  author = getIdentifier("user").asUserId(),
                                                  content = "My message")))))))

      genericImporter.import(projectWithTaskWithTopic)

      assertThat(exportOf(projectWithTaskWithTopic).tasks)
          .usingRecursiveComparison()
          // currently, we use auditingInformation to track authorship and timestamp, so we cannot
          // import this info yet
          .ignoringFields("topics.messages.timestamp", "topics.messages.author")
          .isEqualTo(projectWithTaskWithTopic.tasks)
    }
  }

  @Test
  fun `imports Relations`() {
    val projectCraft =
        ProjectCraftDto(identifier = ProjectCraftId(), name = "Electrician", color = "sparkly blue")
    val workArea = WorkAreaDto(identifier = WorkAreaId(), name = "Cellar")
    val milestone =
        MilestoneDto(
            identifier = MilestoneId(),
            name = "Milestone",
            type = PROJECT,
            date = LocalDate.of(2023, 2, 12),
            header = true)
    val task =
        TaskDto(
            identifier = TaskId(),
            name = "Task",
            description = null,
            location = null,
            projectCraft = projectCraft.identifier,
            assignee = null,
            workArea = null,
            status = DRAFT,
            start = LocalDate.of(2023, 2, 5),
            end = LocalDate.of(2023, 2, 12))
    val project =
        basicProject.copy(
            projectCrafts = listOf(projectCraft),
            workAreas = listOf(workArea),
            milestones = listOf(milestone),
            tasks = listOf(task),
            relations =
                listOf(
                    RelationDto(
                        type = FINISH_TO_START,
                        source = RelationElementDto(id = task.identifier.toUuid(), type = TASK),
                        target =
                            RelationElementDto(
                                id = milestone.identifier.toUuid(), type = MILESTONE),
                        criticality = false)))

    genericImporter.import(project)

    assertThat(exportOf(project).milestones).isEqualTo(project.milestones)
  }

  private val basicProject =
      ProjectDto(
          identifier = ProjectId("48206f03-70f0-4030-bc72-01a57c99cf66"),
          client = "Bosch PT",
          description = "New Offices",
          start = LocalDate.of(2023, 2, 5),
          end = LocalDate.of(2023, 3, 21),
          projectNumber = "123",
          title = "Import Project",
          category = ProjectCategoryEnum.NB,
          address =
              ProjectAddressVo(
                  street = "Test Street", houseNumber = "1", city = "Test Town", zipCode = "12345"),
          participants = emptySet(),
          projectCrafts = emptyList(),
          workAreas = emptyList(),
          milestones = emptyList(),
          tasks = emptyList(),
          relations = emptyList())

  private fun createEmptyProject(project: ProjectDto) {
    transactionTemplate.execute {
      createProjectCommandHandler.handle(
          CreateProjectCommand(
              identifier = project.identifier,
              client = project.client,
              description = project.description,
              start = project.start,
              end = project.end,
              projectNumber = project.projectNumber,
              title = project.title,
              category = project.category,
              address = project.address))
    }
  }

  private fun exportOf(projectToExport: ProjectDto) =
      genericExporter.export(projectToExport.identifier)

  private fun ProjectDto.withoutCopyingUserAsParticipant(): ProjectDto =
      this.copy(participants = this.participants.filter { it.userId != copyingUser }.toSet())
}
