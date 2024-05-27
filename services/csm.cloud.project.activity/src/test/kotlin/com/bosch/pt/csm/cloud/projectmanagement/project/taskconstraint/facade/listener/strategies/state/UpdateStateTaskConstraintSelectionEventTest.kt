/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.COMMON_UNDERSTANDING
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.CUSTOM1
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.EQUIPMENT
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateTaskConstraintSelectionEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    repositories.taskConstraintSelectionRepository.deleteAll()
    eventStreamGenerator.setUserContext("fm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
    }
  }

  @Test
  fun `is saved after task constraint selection created event with default constraints`() {
    assertThat(repositories.taskConstraintSelectionRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat { eventStreamGenerator.submitTaskAction() }
    assertThat(repositories.taskConstraintSelectionRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is saved after task constraint selection created event with custom constraints`() {
    assertThat(repositories.taskConstraintSelectionRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitTaskAction { it.actions = listOf(MATERIAL, CUSTOM1) }
    }
    assertThat(repositories.taskConstraintSelectionRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is updated and cleaned up after task constraint selection updated event`() {
    assertThat(repositories.taskConstraintSelectionRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTaskAction()
          .submitTaskAction(eventType = UPDATED) { it.actions = listOf(EQUIPMENT) }
          .submitTaskAction(eventType = UPDATED) { it.actions = listOf(COMMON_UNDERSTANDING) }
    }

    val constraints = repositories.taskConstraintSelectionRepository.findAll()
    assertThat(constraints).hasSize(2)
    assertThat(constraints)
        .extracting("aggregateIdentifier")
        .extracting("identifier")
        .containsOnly(getIdentifier("taskAction"))
    assertThat(constraints)
        .extracting("aggregateIdentifier")
        .extracting("version")
        .containsAll(listOf(1L, 2L))
  }
}
