/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.util.TimeUtilities.asLocalDate
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreMilestoneSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
    projectEventStoreUtils.reset()
  }

  @Test
  open fun `validate that milestone created event was processed successfully`() {
    val milestone = repositories.findMilestone(getIdentifier("milestone").asMilestoneId())!!
    val aggregate = get<MilestoneAggregateAvro>("milestone")!!

    validateMilestone(milestone, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that milestone updated event was processed successfully`() {
    eventStreamGenerator.submitMilestone(asReference = "milestone", eventType = UPDATED) { milestone
      ->
      milestone.name = "Updated milestone"
      milestone.description = "This milestone is the first goal..."
    }

    val milestone = repositories.findMilestone(getIdentifier("milestone").asMilestoneId())!!
    val aggregate = get<MilestoneAggregateAvro>("milestone")!!

    validateMilestone(milestone, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that milestone updated event with null values was processed successfully`() {
    eventStreamGenerator.submitMilestone(asReference = "milestone", eventType = UPDATED) { milestone
      ->
      milestone.name = "Updated milestone"
      milestone.workarea = null
      milestone.craft = null
      milestone.description = null
    }

    val milestone = repositories.findMilestone(getIdentifier("milestone").asMilestoneId())!!
    val aggregate = get<MilestoneAggregateAvro>("milestone")!!

    validateMilestone(milestone, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate milestone deleted event deletes a milestone`() {
    // Send the delete event twice to test idempotency
    eventStreamGenerator.submitMilestone(asReference = "milestone", eventType = DELETED).repeat(1)

    assertThat(
            repositories.milestoneRepository.existsByIdentifierAndProjectIdentifier(
                getIdentifier("milestone").asMilestoneId(), projectIdentifier))
        .isFalse
  }

  private fun validateMilestone(
      milestone: Milestone,
      aggregate: MilestoneAggregateAvro,
      projectIdentifier: ProjectId
  ) =
      with(milestone) {
        validateAuditingInformationAndIdentifierAndVersion(this, aggregate)
        assertThat(project.identifier).isEqualTo(projectIdentifier)
        assertThat(name).isEqualTo(aggregate.name)
        assertThat(header).isEqualTo(aggregate.header)
        assertThat(description).isEqualTo(aggregate.description)
        assertThat(date).isEqualTo(asLocalDate(aggregate.date))
        validateCraft(this, aggregate)
        validateWorkArea(this, aggregate)
      }

  private fun validateCraft(milestone: Milestone, aggregate: MilestoneAggregateAvro) =
      with(aggregate) {
        if (craft == null) {
          assertThat(milestone.craft).isNull()
        } else {
          assertThat(milestone.craft!!.identifier).isEqualTo(craft.identifier.asProjectCraftId())
        }
      }

  private fun validateWorkArea(milestone: Milestone, aggregate: MilestoneAggregateAvro) =
      with(aggregate) {
        if (workarea == null) {
          assertThat(milestone.workArea).isNull()
        } else {
          assertThat(milestone.workArea!!.identifier).isEqualTo(workarea.identifier.toUUID())
        }
      }
}
