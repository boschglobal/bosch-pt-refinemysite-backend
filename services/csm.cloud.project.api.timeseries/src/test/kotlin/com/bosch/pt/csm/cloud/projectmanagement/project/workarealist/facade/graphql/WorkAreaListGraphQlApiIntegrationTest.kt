/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class WorkAreaListGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val list = "projects[0].workAreas"

  val query =
      """
      query {
        projects {
          workAreas {
            id
            version
            items {
              id
            }
            eventDate
          }
        }
      }
      """
          .trimIndent()

  val aggregate by lazy { get<WorkAreaListAggregateAvro>("workAreaList")!! }

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query work area list`() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitWorkArea()
        .submitWorkAreaList()
        .submitWorkArea("workArea2")
        .submitWorkAreaList(eventType = WorkAreaListEventEnumAvro.ITEMADDED) {
          it.workAreas = listOf(getByReference("workArea"), getByReference("workArea2"))
        }

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$list.id").isEqualTo(aggregate.aggregateIdentifier.identifier)
    response.get("$list.version").isEqualTo(aggregate.aggregateIdentifier.version.toString())
    response.get("$list.items[0].id").isEqualTo(aggregate.workAreas[0].identifier.toString())
    response.get("$list.items[1].id").isEqualTo(aggregate.workAreas[1].identifier.toString())
    response.get("$list.eventDate").isEqualTo(aggregate.eventDate())
  }
}
