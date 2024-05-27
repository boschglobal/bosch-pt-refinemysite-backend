/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.craft.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.asProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.ProjectCraftListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class ProjectCraftRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: ProjectCraftAggregateG2Avro

  lateinit var aggregateV1: ProjectCraftAggregateG2Avro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query project craft`() {
    submitEvents()

    // Execute query
    val craftList = query(false)

    // Validate payload
    assertThat(craftList.crafts).hasSize(2)
    val craftV0 = craftList.crafts[0]

    assertThat(craftV0.id).isEqualTo(aggregateV0.getIdentifier().asProjectCraftId())
    assertThat(craftV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(craftV0.name).isEqualTo(aggregateV0.name)
    assertThat(craftV0.color).isEqualTo(aggregateV0.color)
    assertThat(craftV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(craftV0.deleted).isFalse()

    val craftV1 = craftList.crafts[1]
    assertThat(craftV1.id).isEqualTo(aggregateV1.getIdentifier().asProjectCraftId())
    assertThat(craftV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(craftV1.name).isEqualTo(aggregateV1.name)
    assertThat(craftV1.color).isEqualTo(aggregateV1.color)
    assertThat(craftV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(craftV1.deleted).isFalse()
  }

  @Test
  fun `query project craft latest only`() {
    submitEvents()

    // Execute query
    val craftList = query(true)

    // Validate payload
    assertThat(craftList.crafts).hasSize(1)
    val craft = craftList.crafts.first()

    assertThat(craft.id).isEqualTo(aggregateV1.getIdentifier().asProjectCraftId())
    assertThat(craft.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(craft.name).isEqualTo(aggregateV1.name)
    assertThat(craft.color).isEqualTo(aggregateV1.color)
    assertThat(craft.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(craft.deleted).isFalse()
  }

  @Test
  fun `query deleted project craft`() {
    submitAsDeletedEvents()

    // Execute query
    val craftList = query(false)

    // Validate payload
    assertThat(craftList.crafts).hasSize(2)
    val craftV0 = craftList.crafts[0]

    assertThat(craftV0.id).isEqualTo(aggregateV0.getIdentifier().asProjectCraftId())
    assertThat(craftV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(craftV0.name).isEqualTo(aggregateV0.name)
    assertThat(craftV0.color).isEqualTo(aggregateV0.color)
    assertThat(craftV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(craftV0.deleted).isFalse()

    val craftV1 = craftList.crafts[1]
    assertThat(craftV1.id).isEqualTo(aggregateV1.getIdentifier().asProjectCraftId())
    assertThat(craftV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(craftV1.name).isEqualTo(aggregateV1.name)
    assertThat(craftV1.color).isEqualTo(aggregateV1.color)
    assertThat(craftV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(craftV1.deleted).isTrue()
  }

  @Test
  fun `query deleted project craft latest only`() {
    submitAsDeletedEvents()

    // Execute query
    val craftList = query(true)

    // Validate payload
    assertThat(craftList.crafts).isEmpty()
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

  private fun submitAsDeletedEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 = eventStreamGenerator.submitProjectCraftG2().get("projectCraft")!!
    aggregateV1 =
        eventStreamGenerator
            .submitProjectCraftG2(eventType = ProjectCraftEventEnumAvro.DELETED)
            .get("projectCraft")!!
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/crafts"),
          latestOnly,
          ProjectCraftListResource::class.java)
}
