/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.task.extension.asStatus
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val task = "projects[0].tasks[0]"

  val query =
      """
      query {
        projects {
          tasks {
            id
            version
            name
            description
            location
            status
            editDate
            eventDate
          }
        }
      }
      """
          .trimIndent()

  lateinit var aggregateV0: TaskAggregateAvro

  lateinit var aggregateV1: TaskAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query task with all parameters set`() {
    submitEvents(true)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$task.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$task.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$task.name").isEqualTo(aggregateV1.name)
    response.get("$task.description").isEqualTo(aggregateV1.description)
    response.get("$task.location").isEqualTo(aggregateV1.location)
    response.get("$task.status").isEqualTo(aggregateV1.status.asStatus().shortKey)
    response
        .get("$task.editDate")
        .isEqualTo(aggregateV1.editDate.toLocalDateTimeByMillis().toString())
    response.get("$task.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query task without optional attributes`() {
    submitEvents(false)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$task.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$task.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$task.name").isEqualTo(aggregateV1.name)
    response.get("$task.status").isEqualTo(aggregateV1.status.asStatus().shortKey)
    response.get("$task.eventDate").isEqualTo(aggregateV1.eventDate())

    response.isNull("$task.description")
    response.isNull("$task.location")
    response.isNull("$task.editDate")
  }

  @Test
  fun `query deleted task`() {
    submitEvents(true)
    eventStreamGenerator.submitTask("task", eventType = TaskEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(task)
  }

  private fun submitEvents(includeOptionals: Boolean) {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitProjectCraftG2()

    aggregateV0 =
        eventStreamGenerator
            .submitTask {
              if (includeOptionals) {
                it.description = "Description"
                it.location = "Location"
                it.editDate = LocalDateTime.now().toEpochMilli()
              } else {
                it.description = null
                it.location = null
                it.editDate = null
              }
            }
            .get("task")!!

    aggregateV1 =
        eventStreamGenerator
            .submitTask(eventType = TaskEventEnumAvro.UPDATED) { it.name = "Updated task" }
            .get("task")!!
  }
}
