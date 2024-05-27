/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.asWorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNotNull
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class ProjectWorkAreaGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val workArea = "projects[0].workAreas.items[0]"

  val query =
      """
      query {
        projects {
          workAreas {
            items {
              id
              version
              name
              eventDate
            }
          }
        }
      }
      """
          .trimIndent()

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query work area`() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitWorkArea()
        .submitWorkAreaList()

    val aggregateV1 =
        eventStreamGenerator
            .submitWorkArea(eventType = WorkAreaEventEnumAvro.UPDATED) {
              it.name = "Updated work area"
            }
            .get<WorkAreaAggregateAvro>("workArea")!!

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$workArea.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$workArea.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$workArea.name").isEqualTo(aggregateV1.name)
    response.get("$workArea.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query work area without work area list`() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitWorkArea()

    // Execute query and validate payload doesn't contain the working area
    val response = graphQlTester.document(query).execute()
    response.isNotNull("projects")
    response.isNull("projects[0].workAreas")

    // Check that the working area is in the database though
    assertThat(
            repositories.workAreaRepository.findOneByIdentifier(
                getIdentifier("workArea").asWorkAreaId()))
        .isNotNull
  }

  @Test
  fun `query deleted work area`() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitWorkArea()
        .submitWorkAreaList()
        .submitWorkArea(eventType = WorkAreaEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(workArea)
  }
}
