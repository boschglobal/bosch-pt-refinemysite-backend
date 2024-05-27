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
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.ITEMREMOVED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.REORDERED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestoneList
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.util.TimeUtilities
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaListId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreMilestoneListSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
    projectEventStoreUtils.reset()
  }

  @Test
  open fun `validate that milestone list created event was processed successfully`() {
    val milestoneList =
        repositories.findMilestoneListWithDetails(
            getIdentifier("milestoneList").asMilestoneListId())
    val aggregate = get<MilestoneListAggregateAvro>("milestoneList")!!

    validateMilestoneList(milestoneList, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that milestone list item added event was processed successfully`() {
    eventStreamGenerator.submitMilestone(asReference = "milestone2").submitMilestoneList(
        asReference = "milestoneList", eventType = ITEMADDED) { milestoneList ->
          milestoneList.milestones =
              listOf(getByReference("milestone"), getByReference("milestone2"))
        }

    val milestoneList =
        repositories.findMilestoneListWithDetails(
            getIdentifier("milestoneList").asMilestoneListId())
    val aggregate = get<MilestoneListAggregateAvro>("milestoneList")!!

    validateMilestoneList(milestoneList, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that milestone reordered event was processed successfully`() {
    eventStreamGenerator
        .submitMilestone(asReference = "milestone2")
        .submitMilestoneList(asReference = "milestoneList", eventType = ITEMADDED) { milestoneList
          ->
          milestoneList.milestones =
              listOf(getByReference("milestone"), getByReference("milestone2"))
        }
        .submitMilestoneList(asReference = "milestoneList", eventType = REORDERED) { milestoneList
          ->
          milestoneList.milestones =
              listOf(getByReference("milestone2"), getByReference("milestone"))
        }

    val milestoneList =
        repositories.findMilestoneListWithDetails(
            getIdentifier("milestoneList").asMilestoneListId())
    val aggregate = get<MilestoneListAggregateAvro>("milestoneList")!!

    validateMilestoneList(milestoneList, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that milestone list item removed event was processed successfully`() {
    eventStreamGenerator
        .submitMilestone(asReference = "milestone2")
        .submitMilestoneList(asReference = "milestoneList", eventType = ITEMADDED) { milestoneList
          ->
          milestoneList.milestones =
              listOf(getByReference("milestone"), getByReference("milestone2"))
        }
        .submitMilestoneList(asReference = "milestoneList", eventType = ITEMREMOVED) { milestoneList
          ->
          milestoneList.milestones = listOf(getByReference("milestone2"))
        }
        .submitMilestone(asReference = "milestone", eventType = DELETED)

    val milestoneList =
        repositories.findMilestoneListWithDetails(
            getIdentifier("milestoneList").asMilestoneListId())
    val aggregate = get<MilestoneListAggregateAvro>("milestoneList")!!

    validateMilestoneList(milestoneList, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that milestone list deleted event deletes a milestone list`() {
    // Send the delete event twice to test idempotency
    eventStreamGenerator
        .submitMilestoneList(asReference = "milestoneList", eventType = REORDERED) {
          it.milestones = listOf()
        }
        .submitMilestone(asReference = "milestone", eventType = DELETED)
        .submitMilestoneList(
            asReference = "milestoneList", eventType = MilestoneListEventEnumAvro.DELETED)
        .submitMilestone(asReference = "milestone", eventType = DELETED)

    assertThat(repositories.findWorkAreaList(getIdentifier("milestoneList").asWorkAreaListId()))
        .isNull()
  }

  private fun validateMilestoneList(
      milestoneList: MilestoneList,
      aggregate: MilestoneListAggregateAvro,
      projectIdentifier: ProjectId
  ) =
      with(milestoneList) {
        validateAuditingInformationAndIdentifierAndVersion(this, aggregate)
        assertThat(project.identifier).isEqualTo(projectIdentifier)
        assertThat(date).isEqualTo(TimeUtilities.asLocalDate(aggregate.date))
        assertThat(header).isEqualTo(aggregate.header)
        validateWorkArea(this, aggregate)
        validateMilestoneIdentifiers(this, aggregate)
      }

  private fun validateWorkArea(
      milestoneList: MilestoneList,
      aggregate: MilestoneListAggregateAvro
  ) =
      with(aggregate) {
        if (workarea == null) {
          assertThat(milestoneList.workArea).isNull()
        } else {
          assertThat(milestoneList.workArea!!.identifier).isEqualTo(workarea.identifier.toUUID())
        }
      }

  private fun validateMilestoneIdentifiers(
      milestoneList: MilestoneList,
      aggregate: MilestoneListAggregateAvro
  ) {
    val aggregateMilestoneIdentifiers = aggregate.milestones.map { it.identifier.asMilestoneId() }

    assertThat(milestoneList.milestones)
        .extracting<MilestoneId> { it.identifier }
        .containsExactlyElementsOf(aggregateMilestoneIdentifiers)
  }
}
