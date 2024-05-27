/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.extension.asCategory
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.ProjectListResource
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class ProjectRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: ProjectAggregateAvro

  lateinit var aggregateV1: ProjectAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query project with all parameters set`() {
    submitEvents(true)

    // Execute query
    val projectList = query(false)

    // Validate payload
    assertThat(projectList.projects).hasSize(2)

    val projectV0 = projectList.projects[0]
    val projectAddressV0 = aggregateV0.projectAddress

    assertThat(projectV0.id).isEqualTo(aggregateV0.getIdentifier().asProjectId())
    assertThat(projectV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(projectV0.title).isEqualTo(aggregateV0.title)
    assertThat(projectV0.start).isEqualTo(aggregateV0.start.toLocalDateByMillis())
    assertThat(projectV0.end).isEqualTo(aggregateV0.end.toLocalDateByMillis())
    assertThat(projectV0.projectNumber).isEqualTo(aggregateV0.projectNumber)
    assertThat(projectV0.client).isEqualTo(aggregateV0.client)
    assertThat(projectV0.description).isEqualTo(aggregateV0.description)
    assertThat(projectV0.category).isEqualTo(aggregateV0.category.asCategory().key)
    assertThat(projectV0.projectAddress.city).isEqualTo(projectAddressV0.city)
    assertThat(projectV0.projectAddress.houseNumber).isEqualTo(projectAddressV0.houseNumber)
    assertThat(projectV0.projectAddress.street).isEqualTo(projectAddressV0.street)
    assertThat(projectV0.projectAddress.zipCode).isEqualTo(projectAddressV0.zipCode)
    assertThat(projectV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())

    val projectV1 = projectList.projects[1]
    val projectAddressV1 = aggregateV1.projectAddress

    assertThat(projectV1.id).isEqualTo(aggregateV1.getIdentifier().asProjectId())
    assertThat(projectV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(projectV1.title).isEqualTo(aggregateV1.title)
    assertThat(projectV1.start).isEqualTo(aggregateV1.start.toLocalDateByMillis())
    assertThat(projectV1.end).isEqualTo(aggregateV1.end.toLocalDateByMillis())
    assertThat(projectV1.projectNumber).isEqualTo(aggregateV1.projectNumber)
    assertThat(projectV1.client).isEqualTo(aggregateV1.client)
    assertThat(projectV1.description).isEqualTo(aggregateV1.description)
    assertThat(projectV1.category).isEqualTo(aggregateV1.category.asCategory().key)
    assertThat(projectV1.projectAddress.city).isEqualTo(projectAddressV1.city)
    assertThat(projectV1.projectAddress.houseNumber).isEqualTo(projectAddressV1.houseNumber)
    assertThat(projectV1.projectAddress.street).isEqualTo(projectAddressV1.street)
    assertThat(projectV1.projectAddress.zipCode).isEqualTo(projectAddressV1.zipCode)
    assertThat(projectV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
  }

  @Test
  fun `query project with all parameters set latest only`() {
    submitEvents(true)

    // Extract project address for comparison
    val projectAddress = aggregateV1.projectAddress

    // Execute query
    val projectList = query(true)

    // Validate payload
    assertThat(projectList.projects).hasSize(1)
    val project = projectList.projects.first()

    assertThat(project.id).isEqualTo(aggregateV1.getIdentifier().asProjectId())
    assertThat(project.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(project.title).isEqualTo(aggregateV1.title)
    assertThat(project.start).isEqualTo(aggregateV1.start.toLocalDateByMillis())
    assertThat(project.end).isEqualTo(aggregateV1.end.toLocalDateByMillis())
    assertThat(project.projectNumber).isEqualTo(aggregateV1.projectNumber)
    assertThat(project.client).isEqualTo(aggregateV1.client)
    assertThat(project.description).isEqualTo(aggregateV1.description)
    assertThat(project.category).isEqualTo(aggregateV1.category.asCategory().key)
    assertThat(project.projectAddress.city).isEqualTo(projectAddress.city)
    assertThat(project.projectAddress.houseNumber).isEqualTo(projectAddress.houseNumber)
    assertThat(project.projectAddress.street).isEqualTo(projectAddress.street)
    assertThat(project.projectAddress.zipCode).isEqualTo(projectAddress.zipCode)
    assertThat(project.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
  }

  @Test
  fun `query project without optional parameters`() {
    submitEvents(false)

    // Execute query
    val projectList = query(false)

    // Validate payload
    assertThat(projectList.projects).hasSize(2)

    val projectV0 = projectList.projects[0]
    val projectAddressV0 = aggregateV0.projectAddress

    assertThat(projectV0.id).isEqualTo(aggregateV0.getIdentifier().asProjectId())
    assertThat(projectV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(projectV0.title).isEqualTo(aggregateV0.title)
    assertThat(projectV0.start).isEqualTo(aggregateV0.start.toLocalDateByMillis())
    assertThat(projectV0.end).isEqualTo(aggregateV0.end.toLocalDateByMillis())
    assertThat(projectV0.projectNumber).isEqualTo(aggregateV0.projectNumber)
    assertThat(projectV0.projectAddress.city).isEqualTo(projectAddressV0.city)
    assertThat(projectV0.projectAddress.houseNumber).isEqualTo(projectAddressV0.houseNumber)
    assertThat(projectV0.projectAddress.street).isEqualTo(projectAddressV0.street)
    assertThat(projectV0.projectAddress.zipCode).isEqualTo(projectAddressV0.zipCode)
    assertThat(projectV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(projectV0.client).isNull()
    assertThat(projectV0.description).isNull()
    assertThat(projectV0.category).isNull()

    val projectV1 = projectList.projects[1]
    val projectAddressV1 = aggregateV1.projectAddress

    assertThat(projectV1.id).isEqualTo(aggregateV1.getIdentifier().asProjectId())
    assertThat(projectV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(projectV1.title).isEqualTo(aggregateV1.title)
    assertThat(projectV1.start).isEqualTo(aggregateV1.start.toLocalDateByMillis())
    assertThat(projectV1.end).isEqualTo(aggregateV1.end.toLocalDateByMillis())
    assertThat(projectV1.projectNumber).isEqualTo(aggregateV1.projectNumber)
    assertThat(projectV1.projectAddress.city).isEqualTo(projectAddressV1.city)
    assertThat(projectV1.projectAddress.houseNumber).isEqualTo(projectAddressV1.houseNumber)
    assertThat(projectV1.projectAddress.street).isEqualTo(projectAddressV1.street)
    assertThat(projectV1.projectAddress.zipCode).isEqualTo(projectAddressV1.zipCode)
    assertThat(projectV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(projectV1.client).isNull()
    assertThat(projectV1.description).isNull()
    assertThat(projectV1.category).isNull()
  }

  @Test
  fun `query project without optional parameters latest only`() {
    submitEvents(false)

    // Extract project address for comparison
    val projectAddress = aggregateV1.projectAddress

    // Execute query
    val projectList = query(true)

    // Validate mandatory fields
    assertThat(projectList.projects).hasSize(1)
    val project = projectList.projects.first()

    assertThat(project.id).isEqualTo(aggregateV1.getIdentifier().asProjectId())
    assertThat(project.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(project.title).isEqualTo(aggregateV1.title)
    assertThat(project.start).isEqualTo(aggregateV1.start.toLocalDateByMillis())
    assertThat(project.end).isEqualTo(aggregateV1.end.toLocalDateByMillis())
    assertThat(project.projectNumber).isEqualTo(aggregateV1.projectNumber)
    assertThat(project.projectAddress.city).isEqualTo(projectAddress.city)
    assertThat(project.projectAddress.houseNumber).isEqualTo(projectAddress.houseNumber)
    assertThat(project.projectAddress.street).isEqualTo(projectAddress.street)
    assertThat(project.projectAddress.zipCode).isEqualTo(projectAddress.zipCode)
    assertThat(project.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())

    // Check optional attributes
    assertThat(project.client).isNull()
    assertThat(project.description).isNull()
    assertThat(project.category).isNull()
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

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects"), latestOnly, ProjectListResource::class.java)
}
