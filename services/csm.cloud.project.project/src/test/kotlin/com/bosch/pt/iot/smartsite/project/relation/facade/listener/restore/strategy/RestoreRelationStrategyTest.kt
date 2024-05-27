/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getSourceIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getTargetIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.UNCRITICAL
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

open class RestoreRelationStrategyTest : AbstractRestoreIntegrationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
  }

  @Test
  open fun `validate that relation created event was processed successfully`() {
    val aggregate by lazy { get<RelationAggregateAvro>("relation")!! }
    val relation by lazy {
      repositories.findRelation(getIdentifier("relation"), projectIdentifier)!!
    }

    validateRelation(relation, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that critical relation event was processed successfully`() {
    eventStreamGenerator.submitRelation(eventType = CRITICAL) { it.critical = true }

    val aggregate = get<RelationAggregateAvro>("relation")!!
    val relation = repositories.findRelation(getIdentifier("relation"), projectIdentifier)!!

    validateRelation(relation, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that uncritical relation event was processed successfully`() {
    eventStreamGenerator.submitRelation(eventType = UNCRITICAL) { it.critical = false }

    val aggregate = get<RelationAggregateAvro>("relation")!!
    val relation = repositories.findRelation(getIdentifier("relation"), projectIdentifier)!!

    validateRelation(relation, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate relation deleted event deletes the relation`() {
    assertThat(repositories.findRelation(getIdentifier("relation"), projectIdentifier)).isNotNull

    // send the delete event twice to test idempotency
    eventStreamGenerator.submitRelation(eventType = DELETED).repeat(1)

    assertThat(repositories.findRelation(getIdentifier("relation"), projectIdentifier)).isNull()
  }

  private fun validateRelation(
      relation: Relation,
      aggregate: RelationAggregateAvro,
      projectIdentifier: ProjectId
  ) =
      with(relation) {
        validateAuditableAndVersionedEntityAttributes(this, aggregate)
        assertThat(project.identifier).isEqualTo(projectIdentifier)
        assertThat(type.name).isEqualTo(aggregate.getType().name)
        assertThat(source.type.name).isEqualTo(aggregate.getSource().getType())
        assertThat(source.identifier).isEqualTo(aggregate.getSourceIdentifier())
        assertThat(target.type.name).isEqualTo(aggregate.getTarget().getType())
        assertThat(target.identifier).isEqualTo(aggregate.getTargetIdentifier())
        validateCriticality(this, aggregate)
      }

  private fun validateCriticality(
      relation: Relation,
      aggregate: RelationAggregateAvro,
  ) =
      with(aggregate) {
        if (getCritical() == null) {
          assertThat(relation.critical).isNull()
        } else {
          assertThat(getCritical()).isEqualTo(relation.critical)
        }
      }
}
