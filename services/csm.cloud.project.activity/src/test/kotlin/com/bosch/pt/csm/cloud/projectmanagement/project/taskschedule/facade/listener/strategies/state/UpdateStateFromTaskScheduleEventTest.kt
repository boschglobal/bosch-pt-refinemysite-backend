/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateFromTaskScheduleEventTest : AbstractIntegrationTest() {

  private val startDate = LocalDate.now().toEpochMilli()

  private val endDate1 = LocalDate.now().plusDays(8).toEpochMilli()

  private val endDate2 = LocalDate.now().plusDays(16).toEpochMilli()

  @BeforeEach
  fun init() {
    repositories.taskScheduleRepository.deleteAll()
    eventStreamGenerator.setUserContext("fm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
    }
  }

  @Test
  fun `is saved after task schedule created event`() {
    assertThat(repositories.taskScheduleRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat { eventStreamGenerator.submitTaskSchedule() }
    assertThat(repositories.taskScheduleRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is updated and cleaned up after task schedule updated event`() {
    assertThat(repositories.taskScheduleRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTaskSchedule()
          .submitTaskSchedule(eventType = UPDATED) {
            it.start = startDate
            it.end = endDate1
          }
          .submitTaskSchedule(eventType = UPDATED) {
            it.start = startDate
            it.end = endDate2
          }
    }

    val schedules = repositories.taskScheduleRepository.findAll()
    assertThat(schedules).hasSize(2)
    assertThat(schedules)
        .extracting("identifier")
        .extracting("identifier")
        .containsOnly(getIdentifier("taskSchedule"))
    assertThat(schedules).extracting("identifier").extracting("version").containsAll(listOf(1L, 2L))
  }
}
