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
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class ProjectCraftGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val craft = "projects[0].crafts[0]"

  val query =
      """
      query {
        projects {
          crafts {
            id
            version
            name
            color
            eventDate
          }
        }
      }
      """
          .trimIndent()

  lateinit var aggregateV0: ProjectCraftAggregateG2Avro

  lateinit var aggregateV1: ProjectCraftAggregateG2Avro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `query project craft`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$craft.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$craft.version").isEqualTo("1")
    response.get("$craft.name").isEqualTo(aggregateV1.name)
    response.get("$craft.color").isEqualTo("#CCBBAA")
    response.get("$craft.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query deleted project craft`() {
    submitEvents()
    eventStreamGenerator.submitProjectCraftG2(
        "projectCraft", eventType = ProjectCraftEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(craft)
  }

  private fun submitEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 = eventStreamGenerator.submitProjectCraftG2().get("projectCraft")!!
    aggregateV1 =
        eventStreamGenerator
            .submitProjectCraftG2(eventType = ProjectCraftEventEnumAvro.UPDATED) {
              it.color = "#CCBBAA"
            }
            .get("projectCraft")!!
  }
}
