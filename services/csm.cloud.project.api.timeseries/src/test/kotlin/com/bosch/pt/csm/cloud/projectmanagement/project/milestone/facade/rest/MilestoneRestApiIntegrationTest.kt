/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.asProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.asMilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.extension.asMilestoneType
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response.MilestoneListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.asWorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class MilestoneRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: MilestoneAggregateAvro

  lateinit var aggregateV1: MilestoneAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query milestone with all parameters set`() {
    submitEvents(true)

    // Execute query
    val milestoneList = query(false)

    // Validate payload
    assertThat(milestoneList.milestones).hasSize(2)
    val milestoneV0 = milestoneList.milestones[0]

    assertThat(milestoneV0.id).isEqualTo(aggregateV0.getIdentifier().asMilestoneId())
    assertThat(milestoneV0.version).isEqualTo(aggregateV0.getVersion())
    assertThat(milestoneV0.name).isEqualTo(aggregateV0.name)
    assertThat(milestoneV0.type).isEqualTo(aggregateV0.type.asMilestoneType().key)
    assertThat(milestoneV0.date).isEqualTo(aggregateV0.date.toLocalDateByMillis())
    assertThat(milestoneV0.global).isEqualTo(aggregateV0.header)
    assertThat(milestoneV0.description).isEqualTo(aggregateV0.description)
    assertThat(milestoneV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(milestoneV0.craft)
        .isEqualTo(aggregateV0.craft.identifier.toUUID().asProjectCraftId())
    assertThat(milestoneV0.workArea)
        .isEqualTo(aggregateV0.workarea.identifier.toUUID().asWorkAreaId())
    assertThat(milestoneV0.deleted).isFalse()

    val milestoneV1 = milestoneList.milestones[1]
    assertThat(milestoneV1.id).isEqualTo(aggregateV1.getIdentifier().asMilestoneId())
    assertThat(milestoneV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(milestoneV1.name).isEqualTo(aggregateV1.name)
    assertThat(milestoneV1.type).isEqualTo(aggregateV1.type.asMilestoneType().key)
    assertThat(milestoneV1.date).isEqualTo(aggregateV1.date.toLocalDateByMillis())
    assertThat(milestoneV1.global).isEqualTo(aggregateV1.header)
    assertThat(milestoneV1.description).isEqualTo(aggregateV1.description)
    assertThat(milestoneV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(milestoneV1.craft)
        .isEqualTo(aggregateV1.craft.identifier.toUUID().asProjectCraftId())
    assertThat(milestoneV1.workArea)
        .isEqualTo(aggregateV1.workarea.identifier.toUUID().asWorkAreaId())
    assertThat(milestoneV1.deleted).isFalse()
  }

  @Test
  fun `query milestone with all parameters set latest only`() {
    submitEvents(true)

    // Execute query
    val milestoneList = query(true)

    // Validate payload
    assertThat(milestoneList.milestones).hasSize(1)
    val milestoneV1 = milestoneList.milestones.first()

    assertThat(milestoneV1.id).isEqualTo(aggregateV1.getIdentifier().asMilestoneId())
    assertThat(milestoneV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(milestoneV1.name).isEqualTo(aggregateV1.name)
    assertThat(milestoneV1.type).isEqualTo(aggregateV1.type.asMilestoneType().key)
    assertThat(milestoneV1.date).isEqualTo(aggregateV1.date.toLocalDateByMillis())
    assertThat(milestoneV1.global).isEqualTo(aggregateV1.header)
    assertThat(milestoneV1.description).isEqualTo(aggregateV1.description)
    assertThat(milestoneV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(milestoneV1.craft)
        .isEqualTo(aggregateV1.craft.identifier.toUUID().asProjectCraftId())
    assertThat(milestoneV1.workArea)
        .isEqualTo(aggregateV1.workarea.identifier.toUUID().asWorkAreaId())
    assertThat(milestoneV1.deleted).isFalse()
  }

  @Test
  fun `query milestone without optional parameters`() {
    submitEvents(false)

    // Execute query
    val milestoneList = query(false)

    // Validate payload
    assertThat(milestoneList.milestones).hasSize(2)
    val milestoneV0 = milestoneList.milestones[0]

    assertThat(milestoneV0.id).isEqualTo(aggregateV0.getIdentifier().asMilestoneId())
    assertThat(milestoneV0.version).isEqualTo(aggregateV0.getVersion())
    assertThat(milestoneV0.name).isEqualTo(aggregateV0.name)
    assertThat(milestoneV0.type).isEqualTo(aggregateV0.type.asMilestoneType().key)
    assertThat(milestoneV0.date).isEqualTo(aggregateV0.date.toLocalDateByMillis())
    assertThat(milestoneV0.global).isEqualTo(aggregateV0.header)
    assertThat(milestoneV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(milestoneV0.deleted).isFalse()

    assertThat(milestoneV0.description).isNull()
    assertThat(milestoneV0.craft).isNull()
    assertThat(milestoneV0.workArea).isNull()

    val milestoneV1 = milestoneList.milestones[1]
    assertThat(milestoneV1.id).isEqualTo(aggregateV1.getIdentifier().asMilestoneId())
    assertThat(milestoneV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(milestoneV1.name).isEqualTo(aggregateV1.name)
    assertThat(milestoneV1.type).isEqualTo(aggregateV1.type.asMilestoneType().key)
    assertThat(milestoneV1.date).isEqualTo(aggregateV1.date.toLocalDateByMillis())
    assertThat(milestoneV1.global).isEqualTo(aggregateV1.header)
    assertThat(milestoneV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(milestoneV1.deleted).isFalse()

    assertThat(milestoneV1.description).isNull()
    assertThat(milestoneV1.craft).isNull()
    assertThat(milestoneV1.workArea).isNull()
  }

  @Test
  fun `query milestone without optional parameters latest only`() {
    submitEvents(false)

    // Execute query
    val milestoneList = query(true)

    // Validate mandatory fields
    assertThat(milestoneList.milestones).hasSize(1)
    val milestoneV1 = milestoneList.milestones.first()

    assertThat(milestoneV1.id).isEqualTo(aggregateV1.getIdentifier().asMilestoneId())
    assertThat(milestoneV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(milestoneV1.name).isEqualTo(aggregateV1.name)
    assertThat(milestoneV1.type).isEqualTo(aggregateV1.type.asMilestoneType().key)
    assertThat(milestoneV1.date).isEqualTo(aggregateV1.date.toLocalDateByMillis())
    assertThat(milestoneV1.global).isEqualTo(aggregateV1.header)
    assertThat(milestoneV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(milestoneV1.deleted).isFalse()

    // Check optional parameters
    assertThat(milestoneV1.description).isNull()
    assertThat(milestoneV1.craft).isNull()
    assertThat(milestoneV1.workArea).isNull()
  }

  @Test
  fun `query deleted milestone`() {
    submitAsDeletedEvents()

    // Execute query
    val milestoneList = query(false)

    // Validate payload
    assertThat(milestoneList.milestones).hasSize(2)
    val milestoneV0 = milestoneList.milestones[0]

    assertThat(milestoneV0.id).isEqualTo(aggregateV0.getIdentifier().asMilestoneId())
    assertThat(milestoneV0.version).isEqualTo(aggregateV0.getVersion())
    assertThat(milestoneV0.name).isEqualTo(aggregateV0.name)
    assertThat(milestoneV0.type).isEqualTo(aggregateV0.type.asMilestoneType().key)
    assertThat(milestoneV0.date).isEqualTo(aggregateV0.date.toLocalDateByMillis())
    assertThat(milestoneV0.global).isEqualTo(aggregateV0.header)
    assertThat(milestoneV0.description).isEqualTo(aggregateV0.description)
    assertThat(milestoneV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(milestoneV0.craft)
        .isEqualTo(aggregateV0.craft.identifier.toUUID().asProjectCraftId())
    assertThat(milestoneV0.workArea)
        .isEqualTo(aggregateV0.workarea.identifier.toUUID().asWorkAreaId())
    assertThat(milestoneV0.deleted).isFalse()

    val milestoneV1 = milestoneList.milestones[1]
    assertThat(milestoneV1.id).isEqualTo(aggregateV1.getIdentifier().asMilestoneId())
    assertThat(milestoneV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(milestoneV1.name).isEqualTo(aggregateV1.name)
    assertThat(milestoneV1.type).isEqualTo(aggregateV1.type.asMilestoneType().key)
    assertThat(milestoneV1.date).isEqualTo(aggregateV1.date.toLocalDateByMillis())
    assertThat(milestoneV1.global).isEqualTo(aggregateV1.header)
    assertThat(milestoneV1.description).isEqualTo(aggregateV1.description)
    assertThat(milestoneV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(milestoneV1.craft)
        .isEqualTo(aggregateV1.craft.identifier.toUUID().asProjectCraftId())
    assertThat(milestoneV1.workArea)
        .isEqualTo(aggregateV1.workarea.identifier.toUUID().asWorkAreaId())
    assertThat(milestoneV1.deleted).isTrue()
  }

  @Test
  fun `query deleted milestone latest only`() {
    submitAsDeletedEvents()

    // Execute query
    val milestoneList = query(true)

    // Validate payload
    assertThat(milestoneList.milestones).isEmpty()
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

  private fun submitAsDeletedEvents() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitWorkArea()
        .submitWorkAreaList()

    aggregateV0 =
        eventStreamGenerator
            .submitMilestone {
              it.header = false
              // Set optional fields
              it.craft = getByReference("projectCraft")
              it.workarea = getByReference("workArea")
              it.description = "Description 1"
            }
            .get("milestone")!!

    aggregateV1 =
        eventStreamGenerator
            .submitMilestone(eventType = MilestoneEventEnumAvro.DELETED)
            .get("milestone")!!
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/milestones"),
          latestOnly,
          MilestoneListResource::class.java)
}
