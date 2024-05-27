/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskCraftGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val craft = "projects[0].tasks[0].craft"

  val query =
      """
      query {
        projects {
          tasks {
            craft {
              id
              version
              name
              color
              eventDate
            }
          }
        }
      }
      """
          .trimIndent()

  lateinit var aggregate: ProjectCraftAggregateG2Avro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `query craft`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$craft.id").isEqualTo(aggregate.aggregateIdentifier.identifier)
    response.get("$craft.version").isEqualTo(aggregate.aggregateIdentifier.version.toString())
    response.get("$craft.name").isEqualTo(aggregate.name)
    response.get("$craft.color").isEqualTo(aggregate.color)
    response.get("$craft.eventDate").isEqualTo(aggregate.eventDate())
  }

  private fun submitEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregate = eventStreamGenerator.submitProjectCraftG2().get("projectCraft")!!

    eventStreamGenerator.submitTask()
  }
}
