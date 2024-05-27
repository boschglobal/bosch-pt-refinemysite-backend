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
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getCraftIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestoneList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class UpdateStateFromMilestoneAndMilestoneListEventsTest : BaseNotificationTest() {

  @BeforeEach
  override fun setup() {
    super.setup()
    eventStreamGenerator.submitProject().submitWorkArea().submitMilestone().submitMilestoneList()
  }
  @Test
  fun `State is updated from milestone created and milestone list created event`() {
    eventStreamGenerator.repeat { eventStreamGenerator.submitMilestone().submitMilestoneList() }
    val milestones = repositories.milestoneRepository.findMilestones(getIdentifier(PROJECT))
    assertThat(milestones).hasSize(1)
    validateAttributes(milestones.first(), get(MILESTONE)!!, 0)
  }

  @Test
  fun `State is updated from milestone updated event only`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitMilestone(
          eventType = MilestoneEventEnumAvro.UPDATED,
          aggregateModifications = { it.name = "updated milestone" })
    }

    val milestones = repositories.milestoneRepository.findMilestones(getIdentifier(PROJECT))
    assertThat(milestones).hasSize(2)
    validateAttributes(milestones.first(), get(MILESTONE)!!, 0)
  }

  @Test
  fun `State is updated from milestone created and milestone list item added event`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitMilestone(asReference = "milestone2").submitMilestoneList(
              eventType = MilestoneListEventEnumAvro.ITEMADDED) {
        it.milestones = listOf(getByReference(MILESTONE), getByReference("milestone2"))
      }
    }

    val milestone1 =
        repositories.milestoneRepository.findMilestone(
            getIdentifier(PROJECT), getByReference(MILESTONE).toAggregateIdentifier())
    val milestone2 =
        repositories.milestoneRepository.findMilestone(
            getIdentifier(PROJECT), getByReference("milestone2").toAggregateIdentifier())
    validateAttributes(milestone1!!, get(MILESTONE)!!, 0)
    validateAttributes(milestone2!!, get("milestone2")!!, 1)
  }

  @Test
  fun `State is updated when milestone was moved to a new list`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitMilestone(asReference = "milestone2")
          .submitMilestoneList(asReference = "milestoneList2") {
            it.milestones = listOf(getByReference("milestone2"))
          }
          .submitMilestoneList(eventType = MilestoneListEventEnumAvro.DELETED)
          .submitMilestone()
          .submitMilestoneList(
              asReference = "milestoneList2", eventType = MilestoneListEventEnumAvro.ITEMADDED) {
            it.milestones = listOf(getByReference("milestone2"), getByReference(MILESTONE))
          }
    }

    val milestone1 =
        repositories.milestoneRepository.findMilestone(
            getIdentifier(PROJECT), getByReference(MILESTONE).toAggregateIdentifier())!!
    val milestone2 =
        repositories.milestoneRepository.findMilestone(
            getIdentifier(PROJECT), getByReference("milestone2").toAggregateIdentifier())!!
    validateAttributes(milestone2, get("milestone2")!!, 0)
    validateAttributes(milestone1, get(MILESTONE)!!, 1)
  }

  @Test
  fun `State is updated when milestones where moved inside a list (milestone list reorderedevent)`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitMilestone(asReference = "milestone2")
          .submitMilestoneList(asReference = "milestoneList2") {
            it.milestones = listOf(getByReference(MILESTONE), getByReference("milestone2"))
          }
          .submitMilestoneList(
              asReference = "milestoneList2", eventType = MilestoneListEventEnumAvro.REORDERED) {
            it.milestones = listOf(getByReference("milestone2"), getByReference(MILESTONE))
          }
    }

    val milestone1 =
        repositories.milestoneRepository.findMilestone(
            getIdentifier(PROJECT), getByReference(MILESTONE).toAggregateIdentifier())!!
    val milestone2 =
        repositories.milestoneRepository.findMilestone(
            getIdentifier(PROJECT), getByReference("milestone2").toAggregateIdentifier())!!
    validateAttributes(milestone2, get("milestone2")!!, 0)
    validateAttributes(milestone1, get(MILESTONE)!!, 1)
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

    if (aggregate.getCraft() == null) {
      assertThat(milestone.craftIdentifier).isNull()
    } else {
      assertThat(milestone.craftIdentifier).isEqualTo(aggregate.getCraftIdentifier())
    }

    if (aggregate.getWorkarea() == null) {
      assertThat(milestone.workAreaIdentifier).isNull()
    } else {
      assertThat(milestone.workAreaIdentifier).isEqualTo(aggregate.getWorkAreaIdentifier())
    }
  }
}
