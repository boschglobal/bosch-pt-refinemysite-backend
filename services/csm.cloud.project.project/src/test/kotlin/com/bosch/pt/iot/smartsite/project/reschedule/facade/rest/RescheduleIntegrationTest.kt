/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.CLOSED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.OPEN
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractDeleteIntegrationTest
import com.bosch.pt.iot.smartsite.common.i18n.Key.RESCHEDULE_VALIDATION_ERROR_INVALID_SHIFT_DAYS
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.request.RescheduleResource
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.request.RescheduleResource.CriteriaResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import com.bosch.pt.iot.smartsite.util.withMessageKey
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class RescheduleIntegrationTest : AbstractDeleteIntegrationTest() {

  @Autowired private lateinit var cut: RescheduleController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  private val startDate = now().minusMonths(2)
  private val endDate = now().minusMonths(1)

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTaskWithSchedule("openTask1", "00000000-2222-0000-0000-000000000000", OPEN)
        .submitTaskWithSchedule("openTask2", "00000000-1111-0000-0000-000000000000", OPEN)
        .submitTaskWithSchedule("closedTask1", "00000000-5555-0000-0000-000000000000", CLOSED)
        .submitTaskWithSchedule("closedTask2", "00000000-4444-0000-0000-000000000000", CLOSED)
        .submitMilestone("milestone1") {
          it.aggregateIdentifierBuilder.identifier = "00000000-2220-0000-0000-000000000000"
          it.date = startDate.toEpochMilli()
        }
        .submitMilestone("milestone2") {
          it.aggregateIdentifierBuilder.identifier = "00000000-1110-0000-0000-000000000000"
          it.date = startDate.toEpochMilli()
        }

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `reschedule validation returns a sorted result`() {
    val milestonesFilter = FilterMilestoneListResource(from = startDate, to = endDate)
    val tasksFilter = FilterTaskListResource(from = startDate, to = endDate)
    val resource =
        RescheduleResource(
            shiftDays = 2L,
            useTaskCriteria = true,
            useMilestoneCriteria = true,
            criteria = CriteriaResource(milestonesFilter, tasksFilter))

    val result = cut.validate(projectIdentifier, resource).body!!

    assertThat(result).isNotNull
    assertThat(result.successful).isNotNull
    assertThat(result.successful.milestones)
        .containsExactly(
            getIdentifier("milestone2").asMilestoneId(),
            getIdentifier("milestone1").asMilestoneId())
    assertThat(result.successful.tasks)
        .containsExactly(getIdentifier("openTask2"), getIdentifier("openTask1"))
    assertThat(result.failed).isNotNull
    assertThat(result.failed.milestones).isEmpty()
    assertThat(result.failed.tasks)
        .containsExactly(getIdentifier("closedTask2"), getIdentifier("closedTask1"))
  }

  @Test
  fun `reschedule validation returns an error for an invalid shift days`() {
    val milestonesFilter = FilterMilestoneListResource(from = startDate, to = endDate)
    val tasksFilter = FilterTaskListResource(from = startDate, to = endDate)
    val resource =
        RescheduleResource(
            shiftDays = 0L,
            useTaskCriteria = true,
            useMilestoneCriteria = true,
            criteria = CriteriaResource(milestonesFilter, tasksFilter))

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.validate(projectIdentifier, resource) }
        .withMessageKey(RESCHEDULE_VALIDATION_ERROR_INVALID_SHIFT_DAYS)

    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()
  }

  private fun EventStreamGenerator.submitTaskWithSchedule(
      asReference: String,
      identifier: String,
      status: TaskStatusEnumAvro
  ) =
      submitTask(asReference = asReference) {
            it.aggregateIdentifierBuilder.identifier = identifier
            it.assignee = getByReference("participant")
            it.status = status
          }
          .submitTaskSchedule(asReference = asReference + "Schedule") {
            it.start = startDate.toEpochMilli()
            it.end = endDate.toEpochMilli()
          }
}
