/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getCraftIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.REORDERED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.toAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestoneList
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateFromMilestoneEventAndMilestoneListEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    repositories.milestoneRepository.deleteAll()
  }

  @Test
  fun `is saved after project milestone created event and milestone list created event`() {
    assertThat(repositories.milestoneRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitMilestone().submitMilestoneList {
        it.milestones = listOf(getByReference("milestone"))
      }
    }

    val milestones = repositories.milestoneRepository.findAll()
    assertThat(milestones).hasSize(1)
    validateAttributes(milestones.first(), get("milestone")!!, 0)
  }

  @Test
  fun `is updated and cleaned up after project milestone updated event`() {
    assertThat(repositories.milestoneRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitMilestone()
          .submitMilestoneList { it.milestones = listOf(getByReference("milestone")) }
          .submitMilestone(eventType = UPDATED) { it.name = "update 1" }
          .submitMilestone(eventType = UPDATED) { it.name = "update 2" }
    }

    val milestones = repositories.milestoneRepository.findAll()
    assertThat(milestones).hasSize(2)

    val milestoneAggregate = context["milestone"] as MilestoneAggregateAvro
    validateAttributes(
        milestones.sortedByDescending { it.identifier.version }.first(), milestoneAggregate, 0)
  }

  @Test
  fun `is updated after project milestone list item added event`() {
    assertThat(repositories.milestoneRepository.findAll()).hasSize(0)

    eventStreamGenerator
        .submitMilestone()
        .submitMilestoneList { it.milestones = listOf(getByReference("milestone")) }
        .submitMilestone(asReference = "milestone2")
        .submitMilestoneList(eventType = ITEMADDED) {
          it.milestones = listOf(getByReference("milestone"), getByReference("milestone2"))
        }

    val milestoneAggregate = context["milestone"] as MilestoneAggregateAvro
    val milestone2Aggregate = context["milestone2"] as MilestoneAggregateAvro

    val milestones = repositories.milestoneRepository.findAll()
    assertThat(milestones).hasSize(2)

    val milestone =
        repositories.milestoneRepository.find(
            milestoneAggregate.getAggregateIdentifier().toAggregateIdentifier().identifier,
            0,
            milestoneAggregate.getProjectIdentifier())!!
    val milestone2 =
        repositories.milestoneRepository.find(
            milestone2Aggregate.getAggregateIdentifier().toAggregateIdentifier().identifier,
            0,
            milestone2Aggregate.getProjectIdentifier())!!

    validateAttributes(milestone, milestoneAggregate, 0)
    validateAttributes(milestone2, milestone2Aggregate, 1)
  }

  @Test
  fun `is updated after project milestone moved to new list`() {
    assertThat(repositories.milestoneRepository.findAll()).hasSize(0)

    eventStreamGenerator
        .submitMilestone()
        .submitMilestoneList { it.milestones = listOf(getByReference("milestone")) }
        .submitMilestone(asReference = "milestone2") { it.workarea = getByReference("workArea1") }
        .submitMilestoneList(asReference = "milestonelist2") {
          it.workarea = getByReference("workArea1")
          it.milestones = listOf(getByReference("milestone2"))
        }
        .submitMilestoneList(eventType = DELETED)
        .submitMilestone { it.workarea = getByReference("workArea1") }
        .submitMilestoneList(eventType = ITEMADDED, asReference = "milestonelist2") {
          it.workarea = getByReference("workArea1")
          it.milestones = listOf(getByReference("milestone2"), getByReference("milestone"))
        }

    val milestoneAggregate = context["milestone"] as MilestoneAggregateAvro
    val milestone2Aggregate = context["milestone2"] as MilestoneAggregateAvro

    val milestones = repositories.milestoneRepository.findAll()
    assertThat(milestones).hasSize(2)

    val milestone =
        repositories.milestoneRepository.find(
            milestoneAggregate.getAggregateIdentifier().toAggregateIdentifier().identifier,
            0,
            milestoneAggregate.getProjectIdentifier())!!
    val milestone2 =
        repositories.milestoneRepository.find(
            milestone2Aggregate.getAggregateIdentifier().toAggregateIdentifier().identifier,
            0,
            milestone2Aggregate.getProjectIdentifier())!!

    validateAttributes(milestone2, milestone2Aggregate, 0)
    validateAttributes(milestone, milestoneAggregate, 1)
  }

  @Test
  fun `is updated after project milestone list is reordered`() {
    assertThat(repositories.milestoneRepository.findAll()).hasSize(0)

    eventStreamGenerator
        .submitMilestone()
        .submitMilestoneList(asReference = "milestoneList")
        .submitMilestone(asReference = "milestone2")
        .submitMilestoneList(eventType = ITEMADDED) {
          it.milestones = listOf(getByReference("milestone"), getByReference("milestone2"))
        }
        .submitMilestoneList(eventType = REORDERED) {
          it.milestones = listOf(getByReference("milestone2"), getByReference("milestone"))
        }

    val milestoneAggregate = context["milestone"] as MilestoneAggregateAvro
    val milestone2Aggregate = context["milestone2"] as MilestoneAggregateAvro

    val milestones = repositories.milestoneRepository.findAll()
    assertThat(milestones).hasSize(2)

    val milestone =
        repositories.milestoneRepository.find(
            milestoneAggregate.getAggregateIdentifier().toAggregateIdentifier().identifier,
            0,
            milestoneAggregate.getProjectIdentifier())!!
    val milestone2 =
        repositories.milestoneRepository.find(
            milestone2Aggregate.getAggregateIdentifier().toAggregateIdentifier().identifier,
            0,
            milestone2Aggregate.getProjectIdentifier())!!

    validateAttributes(milestone2, milestone2Aggregate, 0)
    validateAttributes(milestone, milestoneAggregate, 1)
  }

  private fun validateAttributes(
      milestone: Milestone,
      aggregate: MilestoneAggregateAvro,
      position: Int
  ) {
    assertThat(milestone.identifier)
        .isEqualTo(aggregate.getAggregateIdentifier().toAggregateIdentifier())
    assertThat(milestone.name).isEqualTo(aggregate.getName())
    assertThat(milestone.type.name).isEqualTo(aggregate.getType().name)
    assertThat(milestone.date).isEqualTo(aggregate.getDate().toLocalDateByMillis())
    assertThat(milestone.header).isEqualTo(aggregate.getHeader())
    assertThat(milestone.projectIdentifier).isEqualTo(aggregate.getProjectIdentifier())
    assertThat(milestone.position).isEqualTo(position)

    // Optional fields
    assertThat(milestone.description).isEqualTo(aggregate.getDescription())

    when (aggregate.getCraft() == null) {
      true -> assertThat(milestone.projectCraftIdentifier).isNull()
      else -> assertThat(milestone.projectCraftIdentifier).isEqualTo(aggregate.getCraftIdentifier())
    }

    when (aggregate.getWorkarea() == null) {
      true -> assertThat(milestone.workAreaIdentifier).isNull()
      else -> assertThat(milestone.workAreaIdentifier).isEqualTo(aggregate.getWorkAreaIdentifier())
    }
  }
}
