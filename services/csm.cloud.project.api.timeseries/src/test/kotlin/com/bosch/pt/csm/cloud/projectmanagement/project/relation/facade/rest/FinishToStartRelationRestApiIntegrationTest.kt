/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.domain.asRelationId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.extension.toRelationType
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.RelationListResource
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class FinishToStartRelationRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: RelationAggregateAvro

  lateinit var aggregateV1: RelationAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query relations`() {
    submitEvents()

    // Execute query
    val relationList = query(false)

    // Validate payload
    assertThat(relationList.relations).hasSize(2)

    val relationV0 = relationList.relations[0]
    assertThat(relationV0.id).isEqualTo(aggregateV0.getIdentifier().asRelationId())
    assertThat(relationV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(relationV0.project).isEqualTo(aggregateV0.project.identifier.toUUID().asProjectId())
    assertThat(relationV0.critical).isFalse()
    assertThat(relationV0.type).isEqualTo(aggregateV0.type.toRelationType().key)
    assertThat(relationV0.source.identifier).isEqualTo(aggregateV0.source.identifier.toUUID())
    assertThat(relationV0.source.type).isEqualTo(aggregateV0.source.type)
    assertThat(relationV0.target.identifier).isEqualTo(aggregateV0.target.identifier.toUUID())
    assertThat(relationV0.target.type).isEqualTo(aggregateV0.target.type)
    assertThat(relationV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(relationV0.deleted).isFalse()

    val relationV1 = relationList.relations[1]
    assertThat(relationV1.id).isEqualTo(aggregateV1.getIdentifier().asRelationId())
    assertThat(relationV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(relationV1.project).isEqualTo(aggregateV1.project.identifier.toUUID().asProjectId())
    assertThat(relationV1.critical).isTrue()
    assertThat(relationV1.type).isEqualTo(aggregateV0.type.toRelationType().key)
    assertThat(relationV1.source.identifier).isEqualTo(aggregateV1.source.identifier.toUUID())
    assertThat(relationV1.source.type).isEqualTo(aggregateV1.source.type)
    assertThat(relationV1.target.identifier).isEqualTo(aggregateV1.target.identifier.toUUID())
    assertThat(relationV1.target.type).isEqualTo(aggregateV1.target.type)
    assertThat(relationV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(relationV1.deleted).isFalse()
  }

  @Test
  fun `query relations latest only`() {
    submitEvents()

    // Execute query
    val relationList = query(true)

    // Validate payload
    assertThat(relationList.relations).hasSize(1)
    val relationV1 = relationList.relations.first()

    assertThat(relationV1.id).isEqualTo(aggregateV1.getIdentifier().asRelationId())
    assertThat(relationV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(relationV1.project).isEqualTo(aggregateV1.project.identifier.toUUID().asProjectId())
    assertThat(relationV1.critical).isTrue()
    assertThat(relationV1.type).isEqualTo(aggregateV1.type.toRelationType().key)
    assertThat(relationV1.source.identifier).isEqualTo(aggregateV1.source.identifier.toUUID())
    assertThat(relationV1.source.type).isEqualTo(aggregateV1.source.type)
    assertThat(relationV1.target.identifier).isEqualTo(aggregateV1.target.identifier.toUUID())
    assertThat(relationV1.target.type).isEqualTo(aggregateV1.target.type)
    assertThat(relationV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(relationV1.deleted).isFalse()
  }

  @Test
  fun `query deleted relation`() {
    submitEvents()
    eventStreamGenerator.submitRelation("r1", eventType = RelationEventEnumAvro.DELETED)

    // Execute query
    val relationList = query(false)

    // Validate payload
    assertThat(relationList.relations).hasSize(3)

    val relationV0 = relationList.relations[0]
    assertThat(relationV0.id).isEqualTo(aggregateV0.getIdentifier().asRelationId())
    assertThat(relationV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(relationV0.project).isEqualTo(aggregateV0.project.identifier.toUUID().asProjectId())
    assertThat(relationV0.critical).isFalse()
    assertThat(relationV0.type).isEqualTo(aggregateV0.type.toRelationType().key)
    assertThat(relationV0.source.identifier).isEqualTo(aggregateV0.source.identifier.toUUID())
    assertThat(relationV0.source.type).isEqualTo(aggregateV0.source.type)
    assertThat(relationV0.target.identifier).isEqualTo(aggregateV0.target.identifier.toUUID())
    assertThat(relationV0.target.type).isEqualTo(aggregateV0.target.type)
    assertThat(relationV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(relationV0.deleted).isFalse()

    val relationV1 = relationList.relations[1]
    assertThat(relationV1.id).isEqualTo(aggregateV1.getIdentifier().asRelationId())
    assertThat(relationV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(relationV1.project).isEqualTo(aggregateV1.project.identifier.toUUID().asProjectId())
    assertThat(relationV1.critical).isTrue()
    assertThat(relationV1.type).isEqualTo(aggregateV1.type.toRelationType().key)
    assertThat(relationV1.source.identifier).isEqualTo(aggregateV1.source.identifier.toUUID())
    assertThat(relationV1.source.type).isEqualTo(aggregateV1.source.type)
    assertThat(relationV1.target.identifier).isEqualTo(aggregateV1.target.identifier.toUUID())
    assertThat(relationV1.target.type).isEqualTo(aggregateV1.target.type)
    assertThat(relationV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(relationV1.deleted).isFalse()

    val relationV2 = relationList.relations[2]
    val aggregateV2 = get<RelationAggregateAvro>("r1")!!
    assertThat(relationV2.id).isEqualTo(aggregateV2.getIdentifier().asRelationId())
    assertThat(relationV2.version).isEqualTo(aggregateV2.aggregateIdentifier.version)
    assertThat(relationV2.project).isEqualTo(aggregateV2.project.identifier.toUUID().asProjectId())
    assertThat(relationV2.critical).isTrue()
    assertThat(relationV2.type).isEqualTo(aggregateV2.type.toRelationType().key)
    assertThat(relationV2.source.identifier).isEqualTo(aggregateV2.source.identifier.toUUID())
    assertThat(relationV2.source.type).isEqualTo(aggregateV2.source.type)
    assertThat(relationV2.target.identifier).isEqualTo(aggregateV2.target.identifier.toUUID())
    assertThat(relationV2.target.type).isEqualTo(aggregateV2.target.type)
    assertThat(relationV2.eventTimestamp).isEqualTo(aggregateV2.eventTimestamp())
    assertThat(relationV2.deleted).isTrue()
  }

  @Test
  fun `query deleted relation latest only`() {
    submitEvents()
    eventStreamGenerator.submitRelation("r1", eventType = RelationEventEnumAvro.DELETED)

    // Execute query
    val relationList = query(true)

    // Validate payload
    assertThat(relationList.relations).isEmpty()
  }

  private fun submitEvents() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitUser("user2")
        .submitParticipantG3("participant2") { it.user = getByReference("user2") }
        .submitProjectCraftG2()
        .submitWorkArea()
        .submitWorkAreaList { it.workAreas = listOf(getByReference("workArea")) }
        .submitMilestone {
          it.workarea = getByReference("workArea")
          it.craft = getByReference("projectCraft")
          it.description = "description"
        }
        .submitMilestone("predecessorMilestone") {
          it.workarea = getByReference("workArea")
          it.craft = getByReference("projectCraft")
        }

    aggregateV0 =
        eventStreamGenerator
            .submitRelation("r1", eventType = RelationEventEnumAvro.CREATED) {
              it.critical = false
              it.source = getByReference("predecessorMilestone")
              it.target = getByReference("milestone")
              it.type = RelationTypeEnumAvro.FINISH_TO_START
            }
            .get("r1")!!

    aggregateV1 =
        eventStreamGenerator
            .submitRelation("r1", eventType = RelationEventEnumAvro.CRITICAL) { it.critical = true }
            .get("r1")!!
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/relations"),
          latestOnly,
          RelationListResource::class.java)
}
