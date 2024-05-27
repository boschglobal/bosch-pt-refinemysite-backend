/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.util.getIdentifier
import java.math.BigDecimal
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RestoreDayCardSnapshotTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitDayCardG2().submitTaskSchedule(
        eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.slots = listOf(TaskScheduleSlotAvro(now().toEpochMilli(), getByReference("dayCard")))
        }
  }

  @Test
  fun `validate that day card created event was processed successfully`() {
    val dayCardAggregate = get<DayCardAggregateG2Avro>("dayCard")!!

    transactionTemplate.executeWithoutResult {
      val dayCard =
          repositories.dayCardRepository.findEntityByIdentifier(
              dayCardAggregate.getIdentifier().asDayCardId())!!

      validateBasicAttributes(dayCard, dayCardAggregate)
      validateAuditingInformationAndIdentifierAndVersion(dayCard, dayCardAggregate)
    }
  }

  @Test
  fun `validate that day card updated event was processed successfully`() {
    eventStreamGenerator.submitDayCardG2(eventType = DayCardEventEnumAvro.UPDATED) {
      it.manpower = BigDecimal.valueOf(2.5)
      it.notes = "Work finished"
    }

    val dayCardAggregate = get<DayCardAggregateG2Avro>("dayCard")!!

    transactionTemplate.executeWithoutResult {
      val dayCard =
          repositories.dayCardRepository.findEntityByIdentifier(
              dayCardAggregate.getIdentifier().asDayCardId())!!

      assertThat(dayCard.manpower).isEqualByComparingTo(BigDecimal.valueOf(2.5))
      assertThat(dayCard.notes).isEqualTo("Work finished")
      validateBasicAttributes(dayCard, dayCardAggregate)
      validateAuditingInformationAndIdentifierAndVersion(dayCard, dayCardAggregate)
    }
  }

  @Test
  fun `validate that day card completed event was processed successfully`() {
    eventStreamGenerator.submitDayCardG2(eventType = DayCardEventEnumAvro.COMPLETED) {
      it.status = DayCardStatusEnumAvro.DONE
    }

    val dayCardAggregate = get<DayCardAggregateG2Avro>("dayCard")!!

    transactionTemplate.executeWithoutResult {
      val dayCard =
          repositories.dayCardRepository.findEntityByIdentifier(
              dayCardAggregate.getIdentifier().asDayCardId())!!

      assertThat(dayCard.status).isEqualTo(DayCardStatusEnum.DONE)
      validateBasicAttributes(dayCard, dayCardAggregate)
      validateAuditingInformationAndIdentifierAndVersion(dayCard, dayCardAggregate)
    }
  }

  @Test
  fun `validate that day card approved event was processed successfully`() {
    eventStreamGenerator
        .submitDayCardG2(eventType = DayCardEventEnumAvro.COMPLETED) {
          it.status = DayCardStatusEnumAvro.DONE
        }
        .submitDayCardG2(eventType = DayCardEventEnumAvro.APPROVED) {
          it.status = DayCardStatusEnumAvro.APPROVED
        }

    val dayCardAggregate = get<DayCardAggregateG2Avro>("dayCard")!!

    transactionTemplate.executeWithoutResult {
      val dayCard =
          repositories.dayCardRepository.findEntityByIdentifier(
              dayCardAggregate.getIdentifier().asDayCardId())!!

      assertThat(dayCard.status).isEqualTo(DayCardStatusEnum.APPROVED)
      validateBasicAttributes(dayCard, dayCardAggregate)
      validateAuditingInformationAndIdentifierAndVersion(dayCard, dayCardAggregate)
    }
  }

  @Test
  fun `validate that day card not done event was processed successfully`() {
    eventStreamGenerator
        .submitDayCardG2(eventType = DayCardEventEnumAvro.COMPLETED) {
          it.status = DayCardStatusEnumAvro.DONE
        }
        .submitDayCardG2(eventType = DayCardEventEnumAvro.CANCELLED) {
          it.status = DayCardStatusEnumAvro.NOTDONE
        }

    val dayCardAggregate = get<DayCardAggregateG2Avro>("dayCard")!!

    transactionTemplate.executeWithoutResult {
      val dayCard =
          repositories.dayCardRepository.findEntityByIdentifier(
              dayCardAggregate.getIdentifier().asDayCardId())!!

      assertThat(dayCard.status).isEqualTo(DayCardStatusEnum.NOTDONE)
      validateBasicAttributes(dayCard, dayCardAggregate)
      validateAuditingInformationAndIdentifierAndVersion(dayCard, dayCardAggregate)
    }
  }

  @Test
  fun `validate that day card reset event was processed successfully`() {
    eventStreamGenerator
        .submitDayCardG2(eventType = DayCardEventEnumAvro.COMPLETED) {
          it.status = DayCardStatusEnumAvro.DONE
        }
        .submitDayCardG2(eventType = DayCardEventEnumAvro.CANCELLED) {
          it.status = DayCardStatusEnumAvro.NOTDONE
        }
        .submitDayCardG2(eventType = DayCardEventEnumAvro.RESET) {
          it.status = DayCardStatusEnumAvro.OPEN
        }

    val dayCardAggregate = get<DayCardAggregateG2Avro>("dayCard")!!

    transactionTemplate.executeWithoutResult {
      val dayCard =
          repositories.dayCardRepository.findEntityByIdentifier(
              dayCardAggregate.getIdentifier().asDayCardId())!!

      assertThat(dayCard.status).isEqualTo(DayCardStatusEnum.OPEN)
      validateBasicAttributes(dayCard, dayCardAggregate)
      validateAuditingInformationAndIdentifierAndVersion(dayCard, dayCardAggregate)
    }
  }

  @Test
  fun `validate day card deleted event deletes a day card`() {
    val dayCardAggregate = get<DayCardAggregateG2Avro>("dayCard")!!
    assertThat(
            repositories.dayCardRepository.findEntityByIdentifier(
                dayCardAggregate.getIdentifier().asDayCardId()))
        .isNotNull

    eventStreamGenerator
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) { it.slots = listOf() }
        .submitDayCardG2(eventType = DayCardEventEnumAvro.DELETED)

    assertThat(
            repositories.dayCardRepository.findEntityByIdentifier(
                dayCardAggregate.getIdentifier().asDayCardId()))
        .isNull()

    val scheduleAggregate = get<TaskScheduleAggregateAvro>("taskSchedule")!!
    assertThat(scheduleAggregate.slots).isEmpty()

    assertThat(
            repositories.taskScheduleRepository
                .findWithDetailsByIdentifier(scheduleAggregate.getIdentifier().asTaskScheduleId())!!
                .slots)
        .isEmpty()

    // Send event again to test idempotency
    eventStreamGenerator.repeat(2)
  }

  private fun validateBasicAttributes(dayCard: DayCard, dayCardAggregate: DayCardAggregateG2Avro) {
    assertThat(dayCard.taskSchedule.task.identifier)
        .isEqualTo(dayCardAggregate.task.identifier.asTaskId())
    assertThat(dayCard.status.name).isEqualTo(dayCardAggregate.status.name)
    assertThat(dayCard.manpower.minus(dayCardAggregate.manpower)).isZero
    assertThat(dayCard.manpower.precision()).isEqualTo(3)
    assertThat(dayCard.notes).isEqualTo(dayCardAggregate.notes)
    assertThat(dayCard.title).isEqualTo(dayCardAggregate.title)

    if (dayCardAggregate.reason != null) {
      assertThat(dayCard.reason!!.name).isEqualTo(dayCardAggregate.reason.name)
    }
  }
}
