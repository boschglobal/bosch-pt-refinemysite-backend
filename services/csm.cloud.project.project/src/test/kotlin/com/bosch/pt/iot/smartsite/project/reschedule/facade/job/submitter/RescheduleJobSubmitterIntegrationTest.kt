/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.job.submitter

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.RESCHEDULE_VALIDATION_ERROR_INVALID_SHIFT_DAYS
import com.bosch.pt.iot.smartsite.job.integration.JobIntegrationService
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.TypesFilterDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.WorkAreaFilterDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobContext
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobType.PROJECT_RESCHEDULE
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.AssigneesFilterDto
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.util.withMessageKey
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.LocalDate.now
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class RescheduleJobSubmitterIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: RescheduleJobSubmitter

  @MockkBean(relaxed = true) private lateinit var jobIntegrationService: JobIntegrationService

  private val shiftDays by lazy { 2L }

  private val taskCriteria by lazy {
    SearchTasksDto(
        projectIdentifier = projectIdentifier,
        taskStatus = listOf(ACCEPTED),
        projectCraftIdentifiers = listOf(ProjectCraftId()),
        workAreaIdentifiers = listOf(WorkAreaIdOrEmpty(WorkAreaId())),
        assignees =
            AssigneesFilterDto(
                participantIdentifiers = listOf(ParticipantId()),
                companyIdentifiers = listOf(randomUUID())),
        rangeStartDate = now().minusMonths(1),
        rangeEndDate = now().plusMonths(1),
        topicCriticality = listOf(UNCRITICAL),
        hasTopics = false,
        allDaysInDateRange = true)
  }

  private val milestoneCriteria by lazy {
    SearchMilestonesDto(
        projectIdentifier = projectIdentifier,
        typesFilter =
            TypesFilterDto(
                types = setOf(*MilestoneTypeEnum.values()),
                craftIdentifiers = setOf(ProjectCraftId())),
        workAreas =
            WorkAreaFilterDto(
                header = true, workAreaIdentifiers = setOf(WorkAreaIdOrEmpty(WorkAreaId()))),
        from = now().minusMonths(1),
        to = now().plusMonths(1),
        milestoneListIdentifiers = setOf(MilestoneListId()))
  }

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "closedTask") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.CLOSED
        }
        .submitTaskSchedule(asReference = "closedTaskSchedule")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify enqueue reschedule works as expected`() {
    val expectedJobIdentifier = randomUUID()
    every { jobIntegrationService.enqueueJob(any(), any(), any(), any(), any()) } answers
        {
          assertThat(it.invocation.args[0]).isEqualTo(PROJECT_RESCHEDULE.name)
          assertThat(it.invocation.args[1])
              .isEqualTo(SecurityContextHelper.getInstance().getCurrentUser().identifier!!)
          assertJobContext(it.invocation.args[2], projectIdentifier)
          assertJobCommand(
              it.invocation.args[3],
              shiftDays = shiftDays,
              useTaskCriteria = true,
              useMilestoneCriteria = true,
              taskCriteria = taskCriteria,
              milestoneCriteria = milestoneCriteria,
              projectIdentifier = projectIdentifier)
          expectedJobIdentifier
        }

    val jobIdentifier =
        cut.enqueueRescheduleJob(
            shiftDays = 2L,
            useTaskCriteria = true,
            useMilestoneCriteria = true,
            taskCriteria = taskCriteria,
            milestoneCriteria = milestoneCriteria,
            projectIdentifier = projectIdentifier)
    assertThat(jobIdentifier).isEqualTo(expectedJobIdentifier)
  }

  @Test
  fun `verify enqueue import not possible if kafka rejects submit`() {
    val expectedMessage = "Cannot enqueue message"
    every { jobIntegrationService.enqueueJob(any(), any(), any(), any(), any()) } throws
        IllegalArgumentException(expectedMessage)

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy {
          cut.enqueueRescheduleJob(
              shiftDays = 2L,
              useTaskCriteria = true,
              useMilestoneCriteria = true,
              taskCriteria = taskCriteria,
              milestoneCriteria = milestoneCriteria,
              projectIdentifier = projectIdentifier)
        }
        .withMessage(expectedMessage)
  }

  @Test
  fun `verify enqueue reschedule returns an error for inconsistent task filter project identifier`() {
    val expectedMessage =
        "The project identifier needs to be equal in all elements of the function signature."
    val taskCriteria =
        SearchTasksDto(
            projectIdentifier = ProjectId(),
            taskStatus = listOf(ACCEPTED),
            projectCraftIdentifiers = listOf(ProjectCraftId()),
            workAreaIdentifiers = listOf(WorkAreaIdOrEmpty(WorkAreaId())),
            assignees =
                AssigneesFilterDto(
                    participantIdentifiers = listOf(ParticipantId()),
                    companyIdentifiers = listOf(randomUUID())),
            rangeStartDate = now().minusMonths(1),
            rangeEndDate = now().plusMonths(1),
            topicCriticality = listOf(UNCRITICAL),
            hasTopics = false,
            allDaysInDateRange = true)

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy {
          cut.enqueueRescheduleJob(
              shiftDays = 2L,
              useTaskCriteria = true,
              useMilestoneCriteria = true,
              taskCriteria = taskCriteria,
              milestoneCriteria = milestoneCriteria,
              projectIdentifier = projectIdentifier)
        }
        .withMessage(expectedMessage)
  }

  @Test
  fun `verify enqueue reschedule returns an error for inconsistent milestone filter project identifier`() {
    val expectedMessage =
        "The project identifier needs to be equal in all elements of the function signature."

    val milestoneCriteria =
        SearchMilestonesDto(
            projectIdentifier = ProjectId(),
            typesFilter =
                TypesFilterDto(
                    types = setOf(*MilestoneTypeEnum.values()),
                    craftIdentifiers = setOf(ProjectCraftId())),
            workAreas =
                WorkAreaFilterDto(
                    header = true, workAreaIdentifiers = setOf(WorkAreaIdOrEmpty(WorkAreaId()))),
            from = now().minusMonths(1),
            to = now().plusMonths(1),
            milestoneListIdentifiers = setOf(MilestoneListId()))

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy {
          cut.enqueueRescheduleJob(
              shiftDays = 2L,
              useTaskCriteria = true,
              useMilestoneCriteria = true,
              taskCriteria = taskCriteria,
              milestoneCriteria = milestoneCriteria,
              projectIdentifier = projectIdentifier)
        }
        .withMessage(expectedMessage)
  }

  @Test
  fun `verify enqueue reschedule returns an error for an invalid shift days`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.enqueueRescheduleJob(
              shiftDays = 0L,
              useTaskCriteria = true,
              useMilestoneCriteria = true,
              taskCriteria = taskCriteria,
              milestoneCriteria = milestoneCriteria,
              projectIdentifier = projectIdentifier)
        }
        .withMessageKey(RESCHEDULE_VALIDATION_ERROR_INVALID_SHIFT_DAYS)
  }

  private fun assertJobContext(element: Any?, projectIdentifier: ProjectId) {
    assertThat(element).isNotNull
    assertThat(element).isExactlyInstanceOf(RescheduleJobContext::class.java)
    (element as RescheduleJobContext).run {
      assertThat(this.project.identifier).isEqualTo(projectIdentifier.identifier)
    }
  }

  private fun assertJobCommand(
      element: Any?,
      shiftDays: Long,
      useTaskCriteria: Boolean,
      useMilestoneCriteria: Boolean,
      taskCriteria: SearchTasksDto,
      milestoneCriteria: SearchMilestonesDto,
      projectIdentifier: ProjectId
  ) {
    assertThat(element).isNotNull
    assertThat(element).isExactlyInstanceOf(RescheduleCommand::class.java)
    (element as RescheduleCommand).run {
      assertThat(this.shiftDays).isEqualTo(shiftDays)
      assertThat(this.useTaskCriteria).isEqualTo(useTaskCriteria)
      assertThat(this.useMilestoneCriteria).isEqualTo(useMilestoneCriteria)
      assertThat(this.taskCriteria).isEqualTo(taskCriteria)
      assertThat(this.milestoneCriteria).isEqualTo(milestoneCriteria)
      assertThat(this.projectIdentifier).isEqualTo(projectIdentifier)
    }
  }
}
