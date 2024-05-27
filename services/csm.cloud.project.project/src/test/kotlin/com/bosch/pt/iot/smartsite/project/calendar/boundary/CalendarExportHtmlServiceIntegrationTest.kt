/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro.CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.project.event.SubmitMilestoneWithListDto
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestonesWithList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.FINISH_TO_START
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.PART_OF
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.calendar.api.CalendarExportParameters
import com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.CalendarAssembler
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.workday.domain.asWorkdayConfigurationId
import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import java.time.LocalDate.now
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteMockKTest
@EnableAllKafkaListeners
class CalendarExportHtmlServiceIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: CalendarExportHtmlService

  @SpykBean private lateinit var calendarAssembler: CalendarAssembler

  private val projectId by lazy { getIdentifier("project").asProjectId() }
  private val workdayConfigurationId by lazy {
    getIdentifier("workdayConfiguration").asWorkdayConfigurationId()
  }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify fetch of project successfully`() {
    val calendarExportParameters =
        CalendarExportParameters(
            from = now().minusMonths(5),
            to = now().plusMonths(5),
            includeDayCards = true,
            includeMilestones = true)

    assertThat(cut.generateHtml(calendarExportParameters, projectId)).isNotBlank

    verify(exactly = 1) {
      calendarAssembler.assemble(
          withArg { assertThat(it.identifier).isEqualTo(projectId) },
          withArg { assertThat(it.identifier).isEqualTo(workdayConfigurationId) },
          calendarExportParameters.from,
          calendarExportParameters.to,
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          calendarExportParameters.includeDayCards,
          calendarExportParameters.includeMilestones)
    }
  }

  @Test
  fun `verify fetch of task with corresponding constraints and schedules successfully`() {
    eventStreamGenerator
        .submitProjectCraftG2(asReference = "projectCraft2")
        .submitTask(asReference = "task2") {
          it.assignee = getByReference("participant")
          it.craft = getByReference("projectCraft2")
        }
        .submitTaskSchedule(asReference = "taskSchedule2")
        .submitTaskAction(asReference = "taskSelection2") {
          it.task = getByReference("task2")
          it.actions = listOf(TaskActionEnumAvro.EQUIPMENT)
        }
        .submitTask(asReference = "task3") {
          it.assignee = getByReference("participant")
          it.craft = getByReference("projectCraft2")
        }
        .submitTaskSchedule(asReference = "taskSchedule3")
        .submitTaskAction(asReference = "taskSelection3") {
          it.task = getByReference("task3")
          it.actions = listOf(TaskActionEnumAvro.CUSTOM1)
        }

    projectEventStoreUtils.reset()

    val calendarExportParameters =
        CalendarExportParameters(
            from = now().minusMonths(5),
            to = now().plusMonths(5),
            projectCraftIds = listOf(getIdentifier("projectCraft2").asProjectCraftId()),
            includeDayCards = true,
            includeMilestones = true)

    assertThat(cut.generateHtml(calendarExportParameters, projectId)).isNotBlank

    verify(exactly = 1) {
      calendarAssembler.assemble(
          any(),
          any(),
          any(),
          any(),
          withArg { tasks ->
            assertThat(tasks).hasSize(2)
            assertThat(tasks)
                .extracting<TaskId> { it.identifier }
                .containsExactlyInAnyOrder(
                    getIdentifier("task2").asTaskId(), getIdentifier("task3").asTaskId())
          },
          withArg { constraints ->
            assertThat(constraints).hasSize(2)
            assertThat(constraints)
                .extracting<UUID> { it.identifier }
                .containsExactlyInAnyOrder(
                    getIdentifier("taskSelection2"), getIdentifier("taskSelection3"))
          },
          withArg { schedules ->
            assertThat(schedules).hasSize(2)
            assertThat(schedules)
                .extracting<UUID> { it.identifier.toUuid() }
                .containsExactlyInAnyOrder(
                    getIdentifier("taskSchedule2"), getIdentifier("taskSchedule3"))
          },
          any(),
          any(),
          any(),
          calendarExportParameters.includeDayCards,
          any())
    }
  }

  @Test
  fun `verify fetch of milestones with corresponding critical relations successfully`() {
    eventStreamGenerator
        .submitProjectCraftG2(asReference = "projectCraft2")
        .submitMilestonesWithList(
            listReference = "milestoneList2",
            date = now().plusDays(1),
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(
                        type = CRAFT, craft = getByReference("projectCraft2"))))
        .submitMilestonesWithList(
            listReference = "milestoneList3",
            date = now().plusDays(2),
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(
                        type = CRAFT, craft = getByReference("projectCraft2"))))
        .submitProjectCraftG2(asReference = "projectCraft3")
        .submitMilestonesWithList(
            listReference = "milestoneList4",
            date = now().plusDays(2),
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(
                        type = CRAFT, craft = getByReference("projectCraft3"))))
        .submitRelation(asReference = "criticalRelation1") {
          it.critical = false
          it.type = PART_OF
          it.source = getByReference("task")
          it.target = getByReference("milestone")
        }
        .submitRelation(asReference = "criticalRelation2") {
          it.critical = true
          it.type = FINISH_TO_START
          it.source = getByReference("task")
          it.target = getByReference("milestoneList2M0")
        }
        .submitRelation(asReference = "criticalRelation3") {
          it.critical = true
          it.type = FINISH_TO_START
          it.source = getByReference("task")
          it.target = getByReference("milestoneList3M0")
        }
        .submitRelation(asReference = "criticalRelation4") {
          it.critical = true
          it.type = FINISH_TO_START
          it.source = getByReference("task")
          it.target = getByReference("milestoneList4M0")
        }

    projectEventStoreUtils.reset()

    val calendarExportParameters =
        CalendarExportParameters(
            from = now().minusMonths(5),
            to = now().plusMonths(5),
            projectCraftIds = listOf(getIdentifier("projectCraft2").asProjectCraftId()),
            includeDayCards = true,
            includeMilestones = true)

    assertThat(cut.generateHtml(calendarExportParameters, projectId)).isNotBlank

    verify(exactly = 1) {
      calendarAssembler.assemble(
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          withArg { milestones ->
            assertThat(milestones).hasSize(3)
            assertThat(milestones)
                .extracting<MilestoneId> { it.identifier }
                .containsExactlyInAnyOrder(
                    getIdentifier("milestone").asMilestoneId(),
                    getIdentifier("milestoneList2M0").asMilestoneId(),
                    getIdentifier("milestoneList3M0").asMilestoneId())
          },
          withArg { criticalMilestoneRelations ->
            assertThat(criticalMilestoneRelations).hasSize(2)
            assertThat(criticalMilestoneRelations)
                .extracting<UUID> { it.identifier }
                .containsExactlyInAnyOrder(
                    getIdentifier("criticalRelation2"), getIdentifier("criticalRelation3"))
          },
          any(),
          any(),
          calendarExportParameters.includeMilestones)
    }
  }

  @Test
  fun `verify no schedules are fetched for false include day cards parameter`() {
    eventStreamGenerator
        .submitProjectCraftG2(asReference = "projectCraft2")
        .submitTask(asReference = "task2") {
          it.assignee = getByReference("participant")
          it.craft = getByReference("projectCraft2")
        }
        .submitTaskSchedule(asReference = "taskSchedule2")
        .submitTaskAction(asReference = "taskSelection2") {
          it.task = getByReference("task2")
          it.actions = listOf(TaskActionEnumAvro.EQUIPMENT)
        }
        .submitTask(asReference = "task3") {
          it.assignee = getByReference("participant")
          it.craft = getByReference("projectCraft2")
        }
        .submitTaskSchedule(asReference = "taskSchedule3")
        .submitTaskAction(asReference = "taskSelection3") {
          it.task = getByReference("task3")
          it.actions = listOf(TaskActionEnumAvro.CUSTOM1)
        }

    projectEventStoreUtils.reset()

    val calendarExportParameters =
        CalendarExportParameters(
            from = now().minusMonths(5),
            to = now().plusMonths(5),
            projectCraftIds = listOf(getIdentifier("projectCraft2").asProjectCraftId()),
            includeDayCards = false,
            includeMilestones = true)

    assertThat(cut.generateHtml(calendarExportParameters, projectId)).isNotBlank

    verify(exactly = 1) {
      calendarAssembler.assemble(
          any(),
          any(),
          any(),
          any(),
          withArg { tasks ->
            assertThat(tasks).hasSize(2)
            assertThat(tasks)
                .extracting<TaskId> { it.identifier }
                .containsExactlyInAnyOrder(
                    getIdentifier("task2").asTaskId(), getIdentifier("task3").asTaskId())
          },
          any(),
          emptyList(),
          any(),
          any(),
          any(),
          calendarExportParameters.includeDayCards,
          any())
    }
  }

  @Test
  fun `verify no milestones and critical relations are fetched for non include milestones`() {
    eventStreamGenerator
        .submitMilestonesWithList(
            listReference = "milestoneList2",
            date = now().plusDays(1),
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(
                        type = CRAFT, craft = getByReference("projectCraft"))))
        .submitRelation(asReference = "criticalRelation1") {
          it.critical = true
          it.type = FINISH_TO_START
          it.source = getByReference("task")
          it.target = getByReference("milestoneList2M0")
        }

    projectEventStoreUtils.reset()

    val calendarExportParameters =
        CalendarExportParameters(
            from = now().minusMonths(5),
            to = now().plusMonths(5),
            includeDayCards = true,
            includeMilestones = false)

    assertThat(cut.generateHtml(calendarExportParameters, projectId)).isNotBlank

    verify(exactly = 1) {
      calendarAssembler.assemble(
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          emptyList(),
          emptyList(),
          any(),
          any(),
          calendarExportParameters.includeMilestones)
    }
  }

  @Test
  fun `verify no task constraints and schedules are fetched for no selectable tasks`() {
    eventStreamGenerator.submitMilestonesWithList(
        listReference = "milestoneList2",
        date = now().plusMonths(2),
        milestones =
            listOf(
                SubmitMilestoneWithListDto(type = CRAFT, craft = getByReference("projectCraft"))))

    projectEventStoreUtils.reset()

    val calendarExportParameters =
        CalendarExportParameters(
            from = now().plusMonths(1),
            to = now().plusMonths(3),
            includeDayCards = true,
            includeMilestones = true)

    assertThat(cut.generateHtml(calendarExportParameters, projectId)).isNotBlank

    verify(exactly = 1) {
      calendarAssembler.assemble(
          any(),
          any(),
          any(),
          any(),
          emptyList(),
          emptyList(),
          emptyList(),
          any(),
          any(),
          any(),
          calendarExportParameters.includeDayCards,
          any())
    }
  }

  @Test
  fun `verify no milestone relations are fetched for no selectable milestones`() {
    eventStreamGenerator
        .submitProjectCraftG2(asReference = "projectCraft2")
        .submitTask(asReference = "task2") {
          it.assignee = getByReference("participant")
          it.craft = getByReference("projectCraft2")
        }
        .submitTaskSchedule(asReference = "taskSchedule2") {
          it.start = now().plusMonths(1).toEpochMilli()
          it.end = now().plusMonths(1).plusDays(5).toEpochMilli()
        }
        .submitTaskAction(asReference = "taskSelection2") {
          it.task = getByReference("task2")
          it.actions = listOf(TaskActionEnumAvro.EQUIPMENT)
        }

    projectEventStoreUtils.reset()

    val calendarExportParameters =
        CalendarExportParameters(
            from = now().plusMonths(1),
            to = now().plusMonths(3),
            projectCraftIds = listOf(getIdentifier("projectCraft2").asProjectCraftId()),
            includeDayCards = true,
            includeMilestones = true)

    assertThat(cut.generateHtml(calendarExportParameters, projectId)).isNotBlank

    verify(exactly = 1) {
      calendarAssembler.assemble(
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          emptyList(),
          emptyList(),
          any(),
          any(),
          calendarExportParameters.includeMilestones)
    }
  }

  @Test
  fun `verify empty calendar for no selectable tasks and milestones`() {
    val calendarExportParameters =
        CalendarExportParameters(
            from = now().plusMonths(1),
            to = now().plusMonths(3),
            includeDayCards = true,
            includeMilestones = true)

    assertThat(cut.generateHtml(calendarExportParameters, projectId)).isNotBlank

    verify(exactly = 1) {
      calendarAssembler.assembleEmpty(
          withArg { assertThat(it.identifier).isEqualTo(projectId) },
          withArg { assertThat(it.identifier).isEqualTo(workdayConfigurationId) },
          calendarExportParameters.from,
          calendarExportParameters.to,
          any(),
          calendarExportParameters.includeDayCards,
          calendarExportParameters.includeMilestones)
    }
  }

  @Test
  fun `verify fetch of project fails for a non existe`() {
    val calendarExportParameters =
        CalendarExportParameters(
            from = now().minusMonths(5),
            to = now().plusMonths(5),
            includeDayCards = true,
            includeMilestones = true)

    assertThat(cut.generateHtml(calendarExportParameters, projectId)).isNotBlank

    verify(exactly = 1) {
      calendarAssembler.assemble(
          withArg { assertThat(it.identifier).isEqualTo(projectId) },
          withArg { assertThat(it.identifier).isEqualTo(workdayConfigurationId) },
          calendarExportParameters.from,
          calendarExportParameters.to,
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          calendarExportParameters.includeDayCards,
          calendarExportParameters.includeMilestones)
    }
  }
}
