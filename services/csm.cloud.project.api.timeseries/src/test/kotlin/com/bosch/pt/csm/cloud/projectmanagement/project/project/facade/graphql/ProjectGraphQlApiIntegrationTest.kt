/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.project.extension.asCategory
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class ProjectGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val project = "projects[0]"

  val query =
      """
      query {
        projects {
          id
          version
          title
          start
          end
          projectNumber
          client
          description
          category
          projectAddress {
            city
            houseNumber
            street
            zipCode
          }
          eventDate
        }
      }
      """
          .trimIndent()

  lateinit var aggregateV0: ProjectAggregateAvro

  lateinit var aggregateV1: ProjectAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query project with all parameters set`() {
    submitEvents(true)

    // Extract project address for comparison
    val projectAddress = aggregateV1.projectAddress

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$project.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$project.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$project.title").isEqualTo(aggregateV1.title)
    response.get("$project.start").isEqualTo(aggregateV1.start.toLocalDateByMillis().toString())
    response.get("$project.end").isEqualTo(aggregateV1.end.toLocalDateByMillis().toString())
    response.get("$project.projectNumber").isEqualTo(aggregateV1.projectNumber)
    response.get("$project.client").isEqualTo(aggregateV1.client)
    response.get("$project.description").isEqualTo(aggregateV1.description)
    response.get("$project.category").isEqualTo(aggregateV1.category.asCategory().shortKey)
    response.get("$project.projectAddress.city").isEqualTo(projectAddress.city)
    response.get("$project.projectAddress.houseNumber").isEqualTo(projectAddress.houseNumber)
    response.get("$project.projectAddress.street").isEqualTo(projectAddress.street)
    response.get("$project.projectAddress.zipCode").isEqualTo(projectAddress.zipCode)
    response.get("$project.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query project without optional parameters`() {
    submitEvents(false)

    // Extract project address for comparison
    val projectAddress = aggregateV1.projectAddress

    // Execute query and validate mandatory fields
    val response = graphQlTester.document(query).execute()
    response.get("$project.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$project.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$project.title").isEqualTo(aggregateV1.title)
    response.get("$project.start").isEqualTo(aggregateV1.start.toLocalDateByMillis().toString())
    response.get("$project.end").isEqualTo(aggregateV1.end.toLocalDateByMillis().toString())
    response.get("$project.projectNumber").isEqualTo(aggregateV1.projectNumber)
    response.get("$project.projectAddress.city").isEqualTo(projectAddress.city)
    response.get("$project.projectAddress.houseNumber").isEqualTo(projectAddress.houseNumber)
    response.get("$project.projectAddress.street").isEqualTo(projectAddress.street)
    response.get("$project.projectAddress.zipCode").isEqualTo(projectAddress.zipCode)
    response.get("$project.eventDate").isEqualTo(aggregateV1.eventDate())

    // Check optional attributes
    response.isNull("$project.client")
    response.isNull("$project.description")
    response.isNull("$project.category")
  }

  private fun submitEvents(includeOptionals: Boolean) {
    aggregateV0 =
        eventStreamGenerator
            .submitProject {
              if (includeOptionals) {
                it.category = ProjectCategoryEnumAvro.NB
                it.client = "Client 1"
                it.description = "Description 1"
              } else {
                it.category = null
                it.client = null
                it.description = null
              }
            }
            .get("project")!!

    aggregateV1 =
        eventStreamGenerator
            .submitProject(eventType = ProjectEventEnumAvro.UPDATED) { it.title = "Updated title" }
            .get("project")!!

    eventStreamGenerator.submitCsmParticipant()
  }
}
