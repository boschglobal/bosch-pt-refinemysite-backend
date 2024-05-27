/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskWorkAreaGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val workArea = "projects[0].tasks[0].workArea"

  val query =
      """
      query {
        projects {
          tasks {
            workArea {
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
        .submitProjectCraftG2()
        .submitWorkArea()
        .submitWorkAreaList()
        .submitTask { it.workarea = getByReference("workArea") }

    val aggregate = get<WorkAreaAggregateAvro>("workArea")!!

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$workArea.id").isEqualTo(aggregate.aggregateIdentifier.identifier)
    response.get("$workArea.version").isEqualTo(aggregate.aggregateIdentifier.version.toString())
    response.get("$workArea.name").isEqualTo(aggregate.name)
    response.get("$workArea.eventDate").isEqualTo(aggregate.eventDate())
  }

  @Test
  fun `query deleted work area`() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitWorkArea()
        .submitWorkAreaList()
        .submitTask { it.workarea = getByReference("workArea") }
        .submitWorkArea(eventType = WorkAreaEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(workArea)
  }
}
