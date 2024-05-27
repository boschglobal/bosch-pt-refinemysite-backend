/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql.resource.response.TaskConstraintPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.getList
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskConstraintSelectionGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val taskConstraintSelection = "projects[0].tasks[0].constraints"

  val query =
      """
      query {
        projects {
          tasks {
            constraints {
              id
              version
              items {
                id
                version
                key
                name
                active
                eventDate
              }
              eventDate
            }
          }
        }
      }
      """.trimIndent()

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query empty constraints of task`() {

    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    response.isNull(taskConstraintSelection)
  }

  @Test
  fun `query task constraint selection after multiple task actions`() {

    submitEvents().submitTaskAction()

    val taskActionV1: TaskActionSelectionAggregateAvro =
        eventStreamGenerator
            .submitTaskAction(asReference = "anotherAction") {
              it.actions =
                  listOf(TaskActionEnumAvro.EXTERNAL_FACTORS, TaskActionEnumAvro.INFORMATION)
            }
            .get("anotherAction")!!

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Verify the TaskConstraintSelection
    response
        .get("$taskConstraintSelection.id")
        .isEqualTo(taskActionV1.aggregateIdentifier.identifier.toString())
    response
        .get("$taskConstraintSelection.version")
        .isEqualTo(taskActionV1.aggregateIdentifier.version.toString())
    response.get("$taskConstraintSelection.eventDate").isEqualTo(taskActionV1.eventDate())
    response
        .getList("$taskConstraintSelection.items", TaskConstraintPayloadV1::class.java)
        .hasSize(2)

    // Verify the selected TaskConstraint in the selection
    response.get("$taskConstraintSelection.items[0].version").isEqualTo("-1")
    response
        .get("$taskConstraintSelection.items[0].key")
        .isEqualTo(TaskConstraintEnum.EXTERNAL_FACTORS.name)

    response.get("$taskConstraintSelection.items[1].version").isEqualTo("-1")
    response
        .get("$taskConstraintSelection.items[1].key")
        .isEqualTo(TaskConstraintEnum.INFORMATION.name)
  }

  @Test
  fun `query task constraint selection after task action with deleted type`() {

    submitEvents().submitTaskAction().submitTaskAction(eventType = DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()

    // Verify the selected task-constraint in the selection
    response.isNull(taskConstraintSelection)
  }

  private fun submitEvents() =
      eventStreamGenerator
          .submitProject()
          .submitCsmParticipant()
          .submitProjectCraftG2()
          .submitTask()
}
