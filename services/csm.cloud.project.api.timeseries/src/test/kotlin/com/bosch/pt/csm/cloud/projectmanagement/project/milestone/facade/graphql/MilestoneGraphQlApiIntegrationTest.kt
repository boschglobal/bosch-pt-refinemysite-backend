/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.extension.asMilestoneType
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class MilestoneGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val milestone = "projects[0].milestones[0]"

  val query =
      """
      query {
        projects {
          milestones {
            id
            version
            name
            type
            date
            global
            description
            eventDate
            craft {
              id
            }
            workArea {
              id
            }
          }
        }
      }
      """
          .trimIndent()

  lateinit var aggregateV0: MilestoneAggregateAvro

  lateinit var aggregateV1: MilestoneAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query milestone with all parameters set`() {
    submitEvents(true)

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$milestone.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$milestone.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$milestone.name").isEqualTo(aggregateV1.name)
    response.get("$milestone.type").isEqualTo(aggregateV1.type.asMilestoneType().shortKey)
    response.get("$milestone.date").isEqualTo(aggregateV1.date.toLocalDateByMillis().toString())
    response.get("$milestone.global").isEqualTo(aggregateV1.header)
    response.get("$milestone.description").isEqualTo(aggregateV1.description)
    response.get("$milestone.eventDate").isEqualTo(aggregateV1.eventDate())
    response.get("$milestone.craft.id").isEqualTo(aggregateV1.craft.identifier)
    response.get("$milestone.workArea.id").isEqualTo(aggregateV1.workarea.identifier)
  }

  @Test
  fun `query milestone without optional parameters`() {
    submitEvents(false)

    // Execute query and validate mandatory fields
    val response = graphQlTester.document(query).execute()
    response.get("$milestone.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$milestone.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$milestone.name").isEqualTo(aggregateV1.name)
    response.get("$milestone.type").isEqualTo(aggregateV1.type.asMilestoneType().shortKey)
    response.get("$milestone.date").isEqualTo(aggregateV1.date.toLocalDateByMillis().toString())
    response.get("$milestone.global").isEqualTo(aggregateV1.header)
    response.get("$milestone.eventDate").isEqualTo(aggregateV1.eventDate())

    // Check optional parameters
    response.isNull("$milestone.description")
    response.isNull("$milestone.craft")
    response.isNull("$milestone.workArea")
  }

  @Test
  fun `query deleted milestone`() {
    submitEvents(true)
    eventStreamGenerator.submitMilestone("milestone", eventType = MilestoneEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(milestone)
  }

  private fun submitEvents(includeOptionals: Boolean) {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitWorkArea()
        .submitWorkAreaList()

    aggregateV0 =
        eventStreamGenerator
            .submitMilestone {
              if (includeOptionals) {
                it.header = false
                // Set optional fields
                it.craft = getByReference("projectCraft")
                it.workarea = getByReference("workArea")
                it.description = "Description 1"
              }
            }
            .get("milestone")!!

    aggregateV1 =
        eventStreamGenerator
            .submitMilestone(eventType = MilestoneEventEnumAvro.UPDATED) {
              it.date = LocalDate.now().plusDays(1).toEpochMilli()
            }
            .get("milestone")!!
  }
}
