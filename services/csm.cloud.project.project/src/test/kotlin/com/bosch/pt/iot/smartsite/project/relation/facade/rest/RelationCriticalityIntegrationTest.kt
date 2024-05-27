/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.FINISH_TO_START
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.PART_OF
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class RelationCriticalityIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: RelationController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "task2")
        .submitTaskSchedule(asReference = "schedule2")
        .submitMilestone(asReference = "milestone2")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
    useOnlineListener()
  }

  @Test
  fun `verify criticality calculated for finish-to-start relation`() {
    val relationIdentifier = createRelation {
      it.type = FINISH_TO_START
      it.source = getByReference("task2")
      it.target = getByReference("milestone")
    }

    val resource = cut.find(projectIdentifier, relationIdentifier).body!!

    assertThat(resource.version).isEqualTo(1)
    assertThat(resource.critical).isFalse
  }

  @Test
  fun `verify criticality not calculated for part-of relation`() {
    val relationIdentifier = createRelation {
      it.type = PART_OF
      it.source = getByReference("task2")
      it.target = getByReference("milestone")
    }

    val resource = cut.find(projectIdentifier, relationIdentifier).body!!

    assertThat(resource.version).isEqualTo(0)
    assertThat(resource.critical).isNull()
  }

  @Test
  fun `verify criticality not calculated if source date of successor is missing`() {
    val task = createTaskWithSchedule(start = null, end = LocalDate.now())
    val relationIdentifier = createRelation {
      it.type = FINISH_TO_START
      it.source = getByReference("milestone")
      it.target = task
    }

    val resource = cut.find(projectIdentifier, relationIdentifier).body!!

    assertThat(resource.version).isEqualTo(0)
    assertThat(resource.critical).isNull()
  }

  @Test
  fun `verify criticality not calculated if end date of predecessor is missing`() {
    val task = createTaskWithSchedule(start = LocalDate.now(), end = null)
    val relationIdentifier = createRelation {
      it.type = FINISH_TO_START
      it.source = task
      it.target = getByReference("milestone")
    }

    val resource = cut.find(projectIdentifier, relationIdentifier).body!!

    assertThat(resource.version).isEqualTo(0)
    assertThat(resource.critical).isNull()
  }

  @Test
  fun `verify criticality not calculated if task schedule is missing`() {
    eventStreamGenerator.submitTask(asReference = "taskWithoutSchedule")

    val relationIdentifier = createRelation {
      it.type = FINISH_TO_START
      it.source = getByReference("taskWithoutSchedule")
      it.target = getByReference("milestone")
    }

    val resource = cut.find(projectIdentifier, relationIdentifier).body!!

    assertThat(resource.version).isEqualTo(0)
    assertThat(resource.critical).isNull()
  }

  @DisplayName("task-milestone relations")
  @Nested
  inner class TaskMilestoneRelations {

    @Test
    fun `verify critical if finish date of task predecessor is after date of milestone successor`() {
      val date = LocalDate.now()
      assertCriticality(
          critical = true,
          source = createTaskWithSchedule(date, date.plusDays(3)),
          target = createMilestone(date.plusDays(2)))
    }

    @Test
    fun `verify critical if date of milestone predecessor is after start date of task successor`() {
      val date = LocalDate.now()
      assertCriticality(
          critical = true,
          source = createMilestone(date.plusDays(1)),
          target = createTaskWithSchedule(date, date.plusDays(2)))
    }

    @Test
    fun `verify not critical if finish date of task predecessor and date of milestone successor have the same date`() {
      val date = LocalDate.now()
      assertCriticality(
          critical = false,
          source = createTaskWithSchedule(date, date.plusDays(1)),
          target = createMilestone(date.plusDays(1)))
    }

    @Test
    fun `verify not critical if date of milestone predecessor and start date of task successor have the same date`() {
      val date = LocalDate.now()
      assertCriticality(
          critical = false,
          source = createMilestone(date),
          target = createTaskWithSchedule(date, date.plusDays(1)))
    }
  }

  @DisplayName("task-task relations")
  @Nested
  inner class TaskTaskRelations {

    @Test
    fun `verify critical if finish date of task predecessor is after start date of task successor`() {
      val date = LocalDate.now()
      assertCriticality(
          critical = true,
          source = createTaskWithSchedule(date, date.plusDays(2)),
          target = createTaskWithSchedule(date.plusDays(1), date.plusDays(3)))
    }

    @Test
    fun `verify critical if finish date of task predecessor and the start date of task successor are the same`() {
      val date = LocalDate.now()
      assertCriticality(
          critical = true,
          source = createTaskWithSchedule(date, date.plusDays(1)),
          target = createTaskWithSchedule(date.plusDays(1), date.plusDays(2)))
    }

    @Test
    fun `verify uncritical if finish date of task predecessor is before start date of task successor`() {
      val date = LocalDate.now()
      assertCriticality(
          critical = false,
          source = createTaskWithSchedule(date, date.plusDays(1)),
          target = createTaskWithSchedule(date.plusDays(2), date.plusDays(3)))
    }
  }

  @DisplayName("milestone-milestone relations")
  @Nested
  inner class MilestoneMilestoneRelations {

    @Test
    fun `verify critical if date of milestone predecessor is after date of milestone successor`() {
      val date = LocalDate.now()
      assertCriticality(
          critical = true,
          source = createMilestone(date.plusDays(1)),
          target = createMilestone(date))
    }

    @Test
    fun `verify not critical if two milestones have the same date`() {
      val date = LocalDate.now()
      assertCriticality(
          critical = false, source = createMilestone(date), target = createMilestone(date))
    }
  }

  private fun assertCriticality(
      critical: Boolean,
      source: AggregateIdentifierAvro,
      target: AggregateIdentifierAvro
  ) {
    val relationIdentifier = createRelation {
      it.type = FINISH_TO_START
      it.source = source
      it.target = target
    }

    val resource = cut.find(projectIdentifier, relationIdentifier).body!!

    assertThat(resource.critical).isEqualTo(critical)
  }

  private fun createMilestone(date: LocalDate): AggregateIdentifierAvro {
    val reference = randomString()

    eventStreamGenerator.submitMilestone(asReference = reference) { it.date = date.toEpochMilli() }

    return getByReference(reference)
  }

  private fun createTaskWithSchedule(start: LocalDate?, end: LocalDate?): AggregateIdentifierAvro {
    val reference = randomString()

    eventStreamGenerator.submitTask(asReference = reference).submitTaskSchedule(
        asReference = randomString()) {
      it.start = start?.toEpochMilli()
      it.end = end?.toEpochMilli()
    }

    return getByReference(reference)
  }

  private fun createRelation(
      aggregateModifications: ((RelationAggregateAvro.Builder) -> Unit)? = null
  ): UUID {
    val reference = randomString()

    eventStreamGenerator.submitRelation(
        asReference = reference, aggregateModifications = aggregateModifications)

    return getIdentifier(reference)
  }
}
