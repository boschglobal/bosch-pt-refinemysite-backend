/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql.resource.response.TaskConstraintPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.getList
import com.bosch.pt.csm.cloud.projectmanagement.test.single
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskConstraintGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val lastTaskConstraint = "projects[0].constraints[-1]"

  val query =
      """
      query {
        projects {
          constraints {
            id
            version
            key
            name
            active
            eventDate
          }
        }
      }
      """
          .trimIndent()

  lateinit var aggregateV0: TaskConstraintCustomizationAggregateAvro

  lateinit var aggregateV1: TaskConstraintCustomizationAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query task-constraint with all parameters set`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    response.get("$lastTaskConstraint.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response
        .get("$lastTaskConstraint.version")
        .isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$lastTaskConstraint.key").isEqualTo(aggregateV1.key.toString())
    response.get("$lastTaskConstraint.name").isEqualTo("Material")
    response.get("$lastTaskConstraint.active").isEqualTo(aggregateV1.active)
    response.get("$lastTaskConstraint.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query deleted task-constraint customization return default constraint`() {

    submitEvents()
    eventStreamGenerator.submitTaskConstraintCustomization(
        eventType = TaskConstraintCustomizationEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    val deletedTaskConstraint = "projects[0].constraints[?(@.key=='${aggregateV1.key.name}')]"
    response.getList(deletedTaskConstraint, TaskConstraintPayloadV1::class.java).hasSize(1)

    response
        .getList("$deletedTaskConstraint.id", String::class.java)
        .single()
        .isEqualTo(TaskConstraintEnum.valueOf(aggregateV1.key.name).id.toString())
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
}
