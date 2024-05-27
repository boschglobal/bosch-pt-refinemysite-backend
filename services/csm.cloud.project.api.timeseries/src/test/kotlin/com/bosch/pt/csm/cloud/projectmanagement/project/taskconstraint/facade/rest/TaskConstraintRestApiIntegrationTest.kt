/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.extension.asTaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.TaskConstraintListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskConstraintRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: TaskConstraintCustomizationAggregateAvro

  lateinit var aggregateV1: TaskConstraintCustomizationAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query task with all parameters set`() {
    submitEvents()

    // Execute query
    val taskConstraintList = query(false)

    // Validate payload
    assertThat(taskConstraintList.taskConstraints)
        .hasSize(PREEXISTING_CONSTRAINTS + FIVE_LANGUAGES * TWO_VERSIONS)

    val constraintsUnderTest =
        taskConstraintList.taskConstraints.filter {
          it.id.value.toString() == aggregateV1.aggregateIdentifier.identifier
        }
    assertThat(constraintsUnderTest).hasSize(FIVE_LANGUAGES * TWO_VERSIONS)

    val constraintUnderTestV0 = constraintsUnderTest.first { it.language == "English" }
    assertThat(constraintUnderTestV0.active).isEqualTo(aggregateV0.active)
    assertThat(constraintUnderTestV0.project.value.toString())
        .isEqualTo(aggregateV0.project.identifier)
    assertThat(constraintUnderTestV0.key).isEqualTo(aggregateV0.key.asTaskConstraintEnum().key)
    assertThat(constraintUnderTestV0.deleted).isFalse

    val constraintUnderTestV1 = constraintsUnderTest.last() { it.language == "English" }
    assertThat(constraintUnderTestV1.active).isEqualTo(aggregateV1.active)
    assertThat(constraintUnderTestV1.project.value.toString())
        .isEqualTo(aggregateV1.project.identifier)
    assertThat(constraintUnderTestV1.key).isEqualTo(aggregateV1.key.asTaskConstraintEnum().key)
    assertThat(constraintUnderTestV1.deleted).isFalse
  }
  @Test
  fun `query task with all parameters set - only latest`() {
    submitEvents()

    // Execute query
    val taskConstraintList = query(true)

    // Validate payload
    assertThat(taskConstraintList.taskConstraints)
        .hasSize(PREEXISTING_CONSTRAINTS + FIVE_LANGUAGES * ONE_VERSION)

    val constraintsUnderTest =
        taskConstraintList.taskConstraints.filter {
          it.id.value.toString() == aggregateV1.aggregateIdentifier.identifier
        }
    assertThat(constraintsUnderTest).hasSize(FIVE_LANGUAGES * ONE_VERSION)

    val constraintUnderTest = constraintsUnderTest.first { it.language == "English" }
    assertThat(constraintUnderTest.active).isEqualTo(aggregateV1.active)
    assertThat(constraintUnderTest.project.value.toString())
        .isEqualTo(aggregateV1.project.identifier)
    assertThat(constraintUnderTest.key).isEqualTo(aggregateV1.key.asTaskConstraintEnum().key)
    assertThat(constraintUnderTest.deleted).isFalse
  }

  @Test
  fun `query deleted task-constraint`() {

    submitEvents()
    eventStreamGenerator.submitTaskConstraintCustomization(
        eventType = TaskConstraintCustomizationEventEnumAvro.DELETED)

    // Execute query
    val taskConstraintList = query(false)

    // Validate payload
    assertThat(taskConstraintList.taskConstraints)
        .hasSize(PREEXISTING_CONSTRAINTS + FIVE_LANGUAGES * THREE_VERSIONS)

    val constraintsUnderTest =
        taskConstraintList.taskConstraints.filter {
          it.id.value.toString() == aggregateV1.aggregateIdentifier.identifier
        }
    assertThat(constraintsUnderTest).hasSize(FIVE_LANGUAGES * THREE_VERSIONS)

    val constraintUnderTestV1 = constraintsUnderTest.last() { it.language == "English" }
    assertThat(constraintUnderTestV1.active).isEqualTo(aggregateV1.active)
    assertThat(constraintUnderTestV1.project.value.toString())
        .isEqualTo(aggregateV0.project.identifier)
    assertThat(constraintUnderTestV1.key).isEqualTo(aggregateV0.key.asTaskConstraintEnum().key)
    assertThat(constraintUnderTestV1.deleted).isTrue
  }

  @Test
  fun `query deleted task-constraint latest only`() {

    submitEvents()
    eventStreamGenerator.submitTaskConstraintCustomization(
        eventType = TaskConstraintCustomizationEventEnumAvro.DELETED)

    // Execute query
    val taskConstraintList = query(true)

    // Validate payload
    assertThat(taskConstraintList.taskConstraints).hasSize(PREEXISTING_CONSTRAINTS + FIVE_LANGUAGES)

    val constraints =
        taskConstraintList.taskConstraints
            .map { ConstraintRef(it.id.value, it.version, it.key) }
            .distinct()

    val expectedConstraints =
        TaskConstraintEnum.values()
            .map { ConstraintRef(it.id, -1, it.key) }
            .sortedWith(compareBy({ it.id }, { it.version }, { it.key }))

    assertThat(constraints).isEqualTo(expectedConstraints)
  }

  private fun submitEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitProjectCraftG2()

    aggregateV0 =
        eventStreamGenerator
            .submitTaskConstraintCustomization() { it.active = true }
            .get("taskConstraintCustomization")!!

    aggregateV1 =
        eventStreamGenerator
            .submitTaskConstraintCustomization(
                eventType = TaskConstraintCustomizationEventEnumAvro.UPDATED) {
                  it.active = false
                }
            .get("taskConstraintCustomization")!!
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/constraints"),
          latestOnly,
          TaskConstraintListResource::class.java)

  companion object {
    private const val PREEXISTING_CONSTRAINTS = 55
    private const val FIVE_LANGUAGES = 5
    private const val ONE_VERSION = 1
    private const val TWO_VERSIONS = 2
    private const val THREE_VERSIONS = 3
  }

  data class ConstraintRef(val id: UUID, val version: Long, val key: String)
}
