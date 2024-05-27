/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_CONSTRAINT_VALIDATION_ERROR_DEACTIVATION_NOT_POSSIBLE
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.request.UpdateTaskConstraintResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.CUSTOM1
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.CUSTOM2
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.MATERIAL
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class TaskConstraintIntegrationTest : AbstractApiDocumentationTestV2() {

  @Autowired private lateinit var cut: TaskConstraintController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify that by default only the default constraints are active`() {
    val constraints = cut.findAll(projectIdentifier)

    val listResource = constraints.body!!
    assertThat(listResource.items).hasSize(12)

    // 8 active standard constraints
    assertThat(filterResources(listResource, false)).containsOnly(true).hasSize(8)

    // 4 inactive custom constraints
    assertThat(filterResources(listResource, true)).containsOnly(false).hasSize(4)
  }

  @Test
  fun `verify that only standard plus active constraints are returned`() {
    activateAndNameCustom1Constraint()

    val constraints = cut.findAll(projectIdentifier)

    val listResource = constraints.body!!
    assertThat(listResource.items).hasSize(12)

    // 8 active standard constraints
    assertThat(filterResources(listResource, false)).containsOnly(true).hasSize(8)

    // 1 active custom constraint
    assertThat(filterResources(listResource, true).filter { it }).hasSize(1)

    // 3 inactive custom constraints
    assertThat(filterResources(listResource, true).filter { !it }).hasSize(3)
  }

  @Test
  fun `verify that a standard constraint with a name is accepted (PUT = CREATED event) and the name is ignored`() {
    cut.update(projectIdentifier, UpdateTaskConstraintResource(MATERIAL, false, "abcdef"))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(MATERIAL.name)
    assertThat(aggregate.getName()).isNull()
    assertThat(aggregate.getActive()).isFalse
  }

  @Test
  fun `verify that a standard constraint with a name is accepted (PUT = DELETED event) and the name is ignored`() {
    deactivateMaterialConstraint()

    cut.update(projectIdentifier, UpdateTaskConstraintResource(MATERIAL, true, "abcdef"))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, DELETED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(MATERIAL.name)
    assertThat(aggregate.getName()).isNull()
    assertThat(aggregate.getActive()).isFalse
  }

  @Test
  fun `verify that a standard constraint without a name is accepted (PUT = CREATED event)`() {
    cut.update(projectIdentifier, UpdateTaskConstraintResource(MATERIAL, false, null))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(MATERIAL.name)
    assertThat(aggregate.getName()).isNull()
    assertThat(aggregate.getActive()).isFalse
  }

  @Test
  fun `verify that a standard constraint without a name is accepted (PUT = DELETED event)`() {
    deactivateMaterialConstraint()

    cut.update(projectIdentifier, UpdateTaskConstraintResource(MATERIAL, true, null))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, DELETED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(MATERIAL.name)
    assertThat(aggregate.getName()).isNull()
    assertThat(aggregate.getActive()).isFalse
  }

  @Test
  fun `verify that a standard constraint with default settings is ignored`() {
    cut.update(projectIdentifier, UpdateTaskConstraintResource(MATERIAL, true, null))
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify that a standard constraint with default settings (and text) is ignored`() {
    cut.update(projectIdentifier, UpdateTaskConstraintResource(MATERIAL, true, "abd"))
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify that a custom constraint with a name is accepted (PUT = CREATED event)`() {
    cut.update(projectIdentifier, UpdateTaskConstraintResource(CUSTOM2, true, "Custom Reason"))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(CUSTOM2.name)
    assertThat(aggregate.getName()).isEqualTo("Custom Reason")
    assertThat(aggregate.getActive()).isTrue
  }

  @Test
  fun `verify that a custom constraint without a name is accepted (PUT = CREATED event)`() {
    cut.update(projectIdentifier, UpdateTaskConstraintResource(CUSTOM2, true, null))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(CUSTOM2.name)
    assertThat(aggregate.getName()).isNull()
    assertThat(aggregate.getActive()).isTrue
  }

  @Test
  fun `verify that a custom constraint with a name is accepted (PUT = UPDATED event)`() {
    activateAndNameCustom1Constraint()

    cut.update(projectIdentifier, UpdateTaskConstraintResource(CUSTOM1, true, "Changed Name"))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, UPDATED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(CUSTOM1.name)
    assertThat(aggregate.getName()).isEqualTo("Changed Name")
    assertThat(aggregate.getActive()).isTrue
  }

  @Test
  fun `verify that a custom constraint with a name is accepted (PUT = DELETED event)`() {
    activateAndNameCustom1Constraint()

    // Call the update endpoint with the default values to delete the constraint
    cut.update(projectIdentifier, UpdateTaskConstraintResource(CUSTOM1, false, null))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, DELETED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(CUSTOM1.name)
    assertThat(aggregate.getName()).isEqualTo("This is a custom constraint")
    assertThat(aggregate.getActive()).isTrue
  }

  @Test
  fun `verify that a custom constraint without a name is accepted (PUT = UPDATED event)`() {
    activateAndNameCustom1Constraint()

    cut.update(projectIdentifier, UpdateTaskConstraintResource(CUSTOM1, true, null))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, UPDATED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(CUSTOM1.name)
    assertThat(aggregate.getName()).isNull()
    assertThat(aggregate.getActive()).isTrue
  }

  @Test
  fun `verify that at least one constraint has to be active`() {
    val allReasons = TaskConstraintEnum.standardConstraints.toMutableList()
    val singleReason = allReasons.removeLast()
    allReasons.forEach { reason ->
      eventStreamGenerator.submitTaskConstraintCustomization(asReference = reason.name) {
        it.active = false
        it.key = TaskActionEnumAvro.valueOf(reason.name)
        it.name = null
      }
    }
    projectEventStoreUtils.reset()

    assertThatThrownBy {
          cut.update(projectIdentifier, UpdateTaskConstraintResource(singleReason, false, null))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(
                TASK_CONSTRAINT_VALIDATION_ERROR_DEACTIVATION_NOT_POSSIBLE))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify saving a disabled custom constraint is possible if only one other constraint is active`() {
    val allReasons = TaskConstraintEnum.standardConstraints.toMutableList()
    allReasons.remove(MATERIAL)
    allReasons.forEach { reason ->
      eventStreamGenerator.submitTaskConstraintCustomization(asReference = reason.name) {
        it.active = false
        it.key = TaskActionEnumAvro.valueOf(reason.name)
        it.name = null
      }
    }
    projectEventStoreUtils.reset()

    cut.update(projectIdentifier, UpdateTaskConstraintResource(CUSTOM1, false, "Updated name"))

    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskConstraintCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.getAggregate()
    assertThat(aggregate.getKey().name).isEqualTo(CUSTOM1.name)
    assertThat(aggregate.getName()).isEqualTo("Updated name")
    assertThat(aggregate.getActive()).isFalse
  }

  private fun activateAndNameCustom1Constraint() {
    eventStreamGenerator.submitTaskConstraintCustomization(asReference = "constraint-custom1") {
      it.active = true
      it.key = TaskActionEnumAvro.CUSTOM1
      it.name = "This is a custom constraint"
    }

    projectEventStoreUtils.reset()
  }

  private fun deactivateMaterialConstraint() {
    eventStreamGenerator.submitTaskConstraintCustomization(asReference = "constraint-custom1") {
      it.active = false
      it.key = TaskActionEnumAvro.MATERIAL
    }

    projectEventStoreUtils.reset()
  }

  private fun filterResources(
      constraints: BatchResponseResource<TaskConstraintResource>,
      custom: Boolean
  ) = constraints.items.filter { it.key.isCustom == custom }.map { it.active }
}
