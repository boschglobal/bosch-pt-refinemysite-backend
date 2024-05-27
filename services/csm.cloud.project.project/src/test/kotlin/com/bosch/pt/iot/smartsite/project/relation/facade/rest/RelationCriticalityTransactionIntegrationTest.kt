/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitBatchOperationFinished
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitBatchOperationStarted
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.FINISH_TO_START
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractDeleteIntegrationTest
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
internal class RelationCriticalityTransactionIntegrationTest : AbstractDeleteIntegrationTest() {

  @Autowired private lateinit var cut: RelationController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  private val referenceDate = LocalDate.now()

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask("task2")
        .submitTaskSchedule("schedule2") {
          it.start = referenceDate.toEpochMilli()
          it.end = referenceDate.toEpochMilli()
        }
        .submitMilestone("milestone2") { it.date = referenceDate.plusDays(1).toEpochMilli() }
    useOnlineListener()
    eventStreamGenerator.submitRelation("relation2") {
      it.type = FINISH_TO_START
      it.source = getByReference("task2")
      it.target = getByReference("milestone2")
    }

    setAuthentication(eventStreamGenerator.getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify criticality not updated when criticality toggles inside business transaction`() {
    eventStreamGenerator
        .submitBatchOperationStarted()
        // make task -> milestone dependency critical
        .moveMilestone(referenceDate.minusDays(1))
        // make task -> milestone dependency uncritical again
        .moveMilestone(referenceDate.plusDays(1))
        .submitBatchOperationFinished()

    val relation = cut.find(projectIdentifier, getIdentifier("relation2")).body!!

    assertThat(relation.version).isEqualTo(1)
    assertThat(relation.critical).isFalse()

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify idempotency on duplicated business transaction started event`() {
    eventStreamGenerator
        .submitBatchOperationStarted()
        // make task -> milestone dependency critical
        .moveMilestone(referenceDate.minusDays(1))
        .repeat(2)
        .submitBatchOperationFinished()

    val relation = cut.find(projectIdentifier, getIdentifier("relation2")).body!!

    assertThat(relation.version).isEqualTo(2)
    assertThat(relation.critical).isTrue()

    projectEventStoreUtils.verifyContains(RelationEventAvro::class.java, CRITICAL, 1)
  }

  @Test
  fun `verify idempotency on duplicated business transaction finished event`() {
    eventStreamGenerator
        .submitBatchOperationStarted()
        // make task -> milestone dependency critical
        .moveMilestone(referenceDate.minusDays(1))
        .submitBatchOperationFinished()
        .repeat(2)

    val relation = cut.find(projectIdentifier, getIdentifier("relation2")).body!!

    assertThat(relation.version).isEqualTo(2)
    assertThat(relation.critical).isTrue()

    projectEventStoreUtils.verifyContains(RelationEventAvro::class.java, CRITICAL, 1)
  }

  private fun EventStreamGenerator.moveMilestone(
      date: LocalDate,
  ) =
      this.submitMilestone(asReference = "milestone2", eventType = MilestoneEventEnumAvro.UPDATED) {
        it.date = date.toEpochMilli()
      }
}
