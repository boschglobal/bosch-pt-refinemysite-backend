/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.CUSTOM1
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RestoreTaskConstraintCustomizationStrategyTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate().plainProjectWithCsm()
  }

  @Test
  fun `validate that constraint created event was processed successfully`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitTaskConstraintCustomization("constraint1") {
        it.key = CUSTOM1
        it.name = "Custom constraint"
        it.active = false
      }
    }

    val aggregate = get<TaskConstraintCustomizationAggregateAvro>("constraint1")!!

    transactionTemplate.executeWithoutResult {
      val constraint = repositories.findTaskConstraintWithDetails(getIdentifier("constraint1"))!!

      assertThat(constraint.version).isEqualTo(0L)
      validateBasicAttributes(constraint, aggregate)
      validateAuditableAndVersionedEntityAttributes(constraint, aggregate)
    }
  }

  @Test
  fun `validate that constraint updated event was processed successfully`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTaskConstraintCustomization("constraint1") {
            it.key = CUSTOM1
            it.name = "Custom constraint"
            it.active = false
          }
          .submitTaskConstraintCustomization(asReference = "constraint1", eventType = UPDATED) {
            it.active = true
          }
    }

    val aggregate = get<TaskConstraintCustomizationAggregateAvro>("constraint1")!!

    transactionTemplate.executeWithoutResult {
      val constraint = repositories.findTaskConstraintWithDetails(getIdentifier("constraint1"))!!

      assertThat(constraint.version).isEqualTo(1L)
      validateBasicAttributes(constraint, aggregate)
      validateAuditableAndVersionedEntityAttributes(constraint, aggregate)
    }
  }

  @Test
  fun `validate that constraint delete event was processed successfully`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTaskConstraintCustomization("constraint1") {
            it.key = CUSTOM1
            it.name = "Custom constraint"
            it.active = false
          }
          .submitTaskConstraintCustomization(asReference = "constraint1", eventType = DELETED)
    }

    transactionTemplate.executeWithoutResult {
      repositories.findTaskConstraintWithDetails(getIdentifier("constraint1")).also {
        assertThat(it).isNull()
      }
    }
  }

  private fun validateBasicAttributes(
      constraintCustomization: TaskConstraintCustomization,
      aggregate: TaskConstraintCustomizationAggregateAvro
  ) {
    assertThat(constraintCustomization.identifier)
        .isEqualTo(aggregate.aggregateIdentifier.identifier.toUUID())
    assertThat(constraintCustomization.version).isEqualTo(aggregate.aggregateIdentifier.version)
    assertThat(constraintCustomization.project.identifier)
        .isEqualTo(aggregate.getProjectIdentifier().asProjectId())
    assertThat(constraintCustomization.name).isEqualTo(aggregate.name)
    assertThat(constraintCustomization.key.name).isEqualTo(aggregate.key.name)
    assertThat(constraintCustomization.active).isEqualTo(aggregate.active)
  }
}
