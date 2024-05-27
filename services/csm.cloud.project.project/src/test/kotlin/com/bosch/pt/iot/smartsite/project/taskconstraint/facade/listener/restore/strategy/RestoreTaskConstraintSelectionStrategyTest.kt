/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.EQUIPMENT
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.INFORMATION
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import com.bosch.pt.iot.smartsite.util.getIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("validate that task constraint selection")
open class RestoreTaskConstraintSelectionStrategyTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate().plainProjectWithCsm().submitProjectCraftG2()
  }

  @Test
  open fun `created event was processed successfully`() {
    eventStreamGenerator.repeat { eventStreamGenerator.submitTask().submitTaskAction() }

    val aggregate = get<TaskActionSelectionAggregateAvro>("taskAction")!!

    val taskConstraintSelection =
        repositories.taskConstraintSelectionRepository.findOneWithDetailsByIdentifier(
            aggregate.getIdentifier())!!

    validateBasicAttributes(taskConstraintSelection, aggregate)
    validateAuditableAndVersionedEntityAttributes(taskConstraintSelection, aggregate)
  }

  @Test
  open fun `updated event was processed successfully`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitTask().submitTaskAction().submitTaskAction(eventType = UPDATED) {
        it.actions = listOf(EQUIPMENT, INFORMATION)
      }
    }

    val aggregate = get<TaskActionSelectionAggregateAvro>("taskAction")!!

    val taskConstraintSelection =
        repositories.taskConstraintSelectionRepository.findOneWithDetailsByIdentifier(
            aggregate.getIdentifier())!!

    validateBasicAttributes(taskConstraintSelection, aggregate)
    validateAuditableAndVersionedEntityAttributes(taskConstraintSelection, aggregate)
  }

  @Test
  open fun `deleted event deletes a task action`() {
    eventStreamGenerator.submitTask().submitTaskAction()

    assertThat(repositories.taskConstraintSelectionRepository.findAll()).hasSize(1)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitTask().submitTaskAction(eventType = DELETED)
    }

    assertThat(repositories.taskConstraintSelectionRepository.findAll()).isEmpty()
  }

  private fun validateBasicAttributes(
      constraintSelection: TaskConstraintSelection,
      aggregate: TaskActionSelectionAggregateAvro
  ) {
    assertThat(constraintSelection.identifier).isEqualTo(aggregate.getIdentifier())
    assertThat(constraintSelection.version)
        .isEqualTo(aggregate.aggregateIdentifier.version)
    assertThat(constraintSelection.task.identifier)
        .isEqualTo(aggregate.task.identifier.asTaskId())
    assertThat(constraintSelection.constraints)
        .isEqualTo(
            aggregate
                .actions
                .map { action -> TaskConstraintEnum.valueOf(action.name) }
                .toSet())
  }
}
