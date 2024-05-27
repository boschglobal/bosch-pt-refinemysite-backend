/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.job.handler

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.job.common.JobAggregateTypeEnum.JOB
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.TypesFilterDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.WorkAreaFilterDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.reschedule.command.dto.RescheduleResultDto
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobContext
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobType.PROJECT_RESCHEDULE
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.AssigneesFilterDto
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import java.time.LocalDate
import java.time.LocalDateTime.now
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class RescheduleJobHandlerIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: RescheduleJobHandler

  @Autowired private lateinit var jobJsonSerializer: JobJsonSerializer

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }

  private val shiftDays by lazy { 2L }

  private val taskCriteria by lazy {
    SearchTasksDto(
        projectIdentifier = project.identifier,
        taskStatus = listOf(*TaskStatusEnum.values()),
        projectCraftIdentifiers = listOf(ProjectCraftId()),
        workAreaIdentifiers = listOf(WorkAreaIdOrEmpty(WorkAreaId())),
        assignees =
            AssigneesFilterDto(
                participantIdentifiers = listOf(ParticipantId()),
                companyIdentifiers = listOf(randomUUID())),
        rangeStartDate = LocalDate.now().minusMonths(1),
        rangeEndDate = LocalDate.now().plusMonths(1),
        topicCriticality = listOf(TopicCriticalityEnum.UNCRITICAL),
        hasTopics = false,
        allDaysInDateRange = true)
  }

  private val milestoneCriteria by lazy {
    SearchMilestonesDto(
        projectIdentifier = project.identifier,
        typesFilter =
            TypesFilterDto(
                types = setOf(*MilestoneTypeEnum.values()),
                craftIdentifiers = setOf(ProjectCraftId())),
        workAreas =
            WorkAreaFilterDto(
                header = true, workAreaIdentifiers = setOf(WorkAreaIdOrEmpty(WorkAreaId()))),
        from = LocalDate.now().minusMonths(1),
        to = LocalDate.now().plusMonths(1),
        milestoneListIdentifiers = setOf(MilestoneListId()))
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "closedTask") {
          it.assignee = EventStreamGeneratorStaticExtensions.getByReference("participant")
          it.status = TaskStatusEnumAvro.CLOSED
        }
        .submitTaskSchedule(asReference = "closedTaskSchedule")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verifies handles is valid for job queued event`() {
    val event =
        jobQueuedEvent(
            jobId = randomUUID(),
            project = project,
            shiftDays = shiftDays,
            useTaskCriteria = true,
            useMilestoneCriteria = true,
            taskCriteria = taskCriteria,
            milestoneCriteria = milestoneCriteria)

    assertThat(cut.handles(event)).isTrue
  }

  @Test
  fun `verifies handle result for job queued event`() {
    val event =
        jobQueuedEvent(
            jobId = randomUUID(),
            project = project,
            shiftDays = shiftDays,
            useTaskCriteria = true,
            useMilestoneCriteria = true,
            taskCriteria = taskCriteria,
            milestoneCriteria = milestoneCriteria)

    val result = cut.handle(event)

    assertThat(result).isNotNull
    assertThat(result).isExactlyInstanceOf(RescheduleResultDto::class.java)
    assertThat(jobJsonSerializer.serialize(result)).isNotNull
  }

  private fun jobQueuedEvent(
      jobId: UUID,
      project: Project,
      shiftDays: Long,
      useTaskCriteria: Boolean,
      useMilestoneCriteria: Boolean,
      taskCriteria: SearchTasksDto,
      milestoneCriteria: SearchMilestonesDto,
      projectIdentifier: ProjectId = project.identifier
  ) =
      JobQueuedEventAvro.newBuilder()
          .setAggregateIdentifier(AggregateIdentifierAvro(jobId.toString(), 0, JOB.name))
          .setJobType(PROJECT_RESCHEDULE.name)
          .setUserIdentifier(getIdentifier("userCsm2").toString())
          .setTimestamp(now().toEpochMilli())
          .setJsonSerializedContext(
              jobJsonSerializer
                  .serialize(RescheduleJobContext(ResourceReference.from(project)))
                  .toAvro())
          .setJsonSerializedCommand(
              jobJsonSerializer
                  .serialize(
                      RescheduleCommand(
                          shiftDays,
                          useTaskCriteria,
                          useMilestoneCriteria,
                          taskCriteria,
                          milestoneCriteria,
                          projectIdentifier))
                  .toAvro())
          .build()
}
