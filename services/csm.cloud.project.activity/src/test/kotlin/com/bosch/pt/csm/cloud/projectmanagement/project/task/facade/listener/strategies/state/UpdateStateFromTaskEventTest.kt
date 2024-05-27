/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateFromTaskEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    repositories.taskRepository.deleteAll()
    eventStreamGenerator.setUserContext("fm-user")
  }

  @Test
  fun `is saved after task created event`() {
    assertThat(repositories.taskRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitTask {
        it.assignee = getByReference("fm-participant")
        it.status = DRAFT
      }
    }

    assertThat(repositories.taskRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is updated and cleaned up after task updated event`() {
    assertThat(repositories.taskRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTask {
            it.assignee = getByReference("fm-participant")
            it.status = DRAFT
          }
          .submitTask(eventType = UPDATED) { it.name = "update 1" }
          .submitTask(eventType = UPDATED) { it.name = "update 2" }
    }

    val tasks = repositories.taskRepository.findAll()
    assertThat(tasks).hasSize(2)
    assertThat(tasks)
        .extracting("identifier")
        .extracting("identifier")
        .containsOnly(getIdentifier("task"))
    assertThat(tasks).extracting("identifier").extracting("version").containsAll(listOf(1L, 2L))
  }
}
