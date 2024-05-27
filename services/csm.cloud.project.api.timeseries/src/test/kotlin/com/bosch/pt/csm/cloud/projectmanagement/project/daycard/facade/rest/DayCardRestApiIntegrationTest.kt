/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getDayCardVersion
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.asDayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.DayCardListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.extension.asReason
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.extension.asStatus
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class DayCardRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: DayCardAggregateG2Avro

  lateinit var aggregateV1: DayCardAggregateG2Avro

  lateinit var schedule: TaskScheduleAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
    submitBaseEvents()
  }

  @Test
  fun `query day card with all parameters set`() {
    submitEvents(true)

    // Execute query
    val dayCardList = query(false)

    // Validate payload
    assertThat(dayCardList.dayCards).hasSize(2)
    val dayCardV0 = dayCardList.dayCards[0]

    assertThat(dayCardV0.id).isEqualTo(aggregateV0.getIdentifier().asDayCardId())
    assertThat(dayCardV0.version).isEqualTo(0)
    assertThat(dayCardV0.date).isEqualTo(schedule.slots.first().date.toLocalDateByMillis())
    assertThat(dayCardV0.status).isEqualTo(aggregateV0.status.asStatus().key)
    assertThat(dayCardV0.title).isEqualTo(aggregateV0.title)
    assertThat(dayCardV0.manpower.setScale(2)).isEqualTo(aggregateV0.manpower)
    assertThat(dayCardV0.notes).isEqualTo(aggregateV0.notes)
    assertThat(dayCardV0.reason).isEqualTo(aggregateV0.reason.asReason().key)
    assertThat(dayCardV0.eventTimestamp).isEqualTo(schedule.eventTimestamp())
    assertThat(dayCardV0.deleted).isFalse()

    val dayCardV1 = dayCardList.dayCards[1]
    assertThat(dayCardV1.id).isEqualTo(aggregateV1.getIdentifier().asDayCardId())
    assertThat(dayCardV1.version).isEqualTo(1)
    assertThat(dayCardV1.date).isEqualTo(schedule.slots.first().date.toLocalDateByMillis())
    assertThat(dayCardV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(dayCardV1.title).isEqualTo(aggregateV1.title)
    assertThat(dayCardV1.manpower.setScale(2)).isEqualTo(aggregateV1.manpower)
    assertThat(dayCardV1.notes).isEqualTo(aggregateV1.notes)
    assertThat(dayCardV1.reason).isEqualTo(aggregateV1.reason.asReason().key)
    assertThat(dayCardV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(dayCardV1.deleted).isFalse()
  }

  @Test
  fun `query day card with all parameters set latest only`() {
    submitEvents(true)

    // Execute query
    val dayCardList = query(true)

    // Validate payload
    assertThat(dayCardList.dayCards).hasSize(1)
    val dayCard = dayCardList.dayCards.first()

    assertThat(dayCard.id).isEqualTo(aggregateV1.getIdentifier().asDayCardId())
    assertThat(dayCard.version).isEqualTo(aggregateV1.getDayCardVersion())
    assertThat(dayCard.date).isEqualTo(schedule.slots.first().date.toLocalDateByMillis())
    assertThat(dayCard.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(dayCard.title).isEqualTo(aggregateV1.title)
    assertThat(dayCard.manpower.setScale(2)).isEqualTo(aggregateV1.manpower)
    assertThat(dayCard.notes).isEqualTo(aggregateV1.notes)
    assertThat(dayCard.reason).isEqualTo(aggregateV1.reason.asReason().key)
    assertThat(dayCard.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(dayCard.deleted).isFalse()
  }

  @Test
  fun `query day card without optional parameters`() {
    submitEvents(false)

    // Execute query
    val dayCardList = query(false)

    // Validate payload
    assertThat(dayCardList.dayCards).hasSize(2)
    val dayCardV0 = dayCardList.dayCards[0]

    assertThat(dayCardV0.id).isEqualTo(aggregateV0.getIdentifier().asDayCardId())
    assertThat(dayCardV0.version).isEqualTo(0)
    assertThat(dayCardV0.date).isEqualTo(schedule.slots.first().date.toLocalDateByMillis())
    assertThat(dayCardV0.status).isEqualTo(aggregateV0.status.asStatus().key)
    assertThat(dayCardV0.title).isEqualTo(aggregateV0.title)
    assertThat(dayCardV0.manpower.setScale(2)).isEqualTo(aggregateV0.manpower)
    assertThat(dayCardV0.eventTimestamp).isEqualTo(schedule.eventTimestamp())
    assertThat(dayCardV0.deleted).isFalse()
    assertThat(dayCardV0.notes).isNull()
    assertThat(dayCardV0.reason).isNull()

    val dayCardV1 = dayCardList.dayCards[1]
    assertThat(dayCardV1.id).isEqualTo(aggregateV1.getIdentifier().asDayCardId())
    assertThat(dayCardV1.version).isEqualTo(1)
    assertThat(dayCardV1.date).isEqualTo(schedule.slots.first().date.toLocalDateByMillis())
    assertThat(dayCardV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(dayCardV1.title).isEqualTo(aggregateV1.title)
    assertThat(dayCardV1.manpower.setScale(2)).isEqualTo(aggregateV1.manpower)
    assertThat(dayCardV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(dayCardV1.deleted).isFalse()
    assertThat(dayCardV1.notes).isNull()
    assertThat(dayCardV1.reason).isNull()
  }

  @Test
  fun `query day card without optional parameters latest only`() {
    submitEvents(false)

    // Execute query
    val dayCardList = query(true)

    // Validate mandatory fields
    assertThat(dayCardList.dayCards).hasSize(1)
    val dayCard = dayCardList.dayCards.first()

    assertThat(dayCard.id).isEqualTo(aggregateV1.getIdentifier().asDayCardId())
    assertThat(dayCard.version).isEqualTo(aggregateV1.getDayCardVersion())
    assertThat(dayCard.date).isEqualTo(schedule.slots.first().date.toLocalDateByMillis())
    assertThat(dayCard.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(dayCard.title).isEqualTo(aggregateV1.title)
    assertThat(dayCard.manpower.setScale(2)).isEqualTo(aggregateV1.manpower)
    assertThat(dayCard.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(dayCard.deleted).isFalse()

    // Check optional attributes
    assertThat(dayCard.notes).isNull()
    assertThat(dayCard.reason).isNull()
  }

  @Test
  fun `query deleted day card`() {
    submitAsDeletedEvents()

    // Execute query
    val dayCardList = query(false)

    // Validate payload
    assertThat(dayCardList.dayCards).hasSize(2)
    val dayCardV0 = dayCardList.dayCards[0]

    assertThat(dayCardV0.id).isEqualTo(aggregateV0.getIdentifier().asDayCardId())
    assertThat(dayCardV0.version).isEqualTo(0)
    assertThat(dayCardV0.date).isEqualTo(schedule.slots.first().date.toLocalDateByMillis())
    assertThat(dayCardV0.status).isEqualTo(aggregateV0.status.asStatus().key)
    assertThat(dayCardV0.title).isEqualTo(aggregateV0.title)
    assertThat(dayCardV0.manpower.setScale(2)).isEqualTo(aggregateV0.manpower)
    assertThat(dayCardV0.notes).isEqualTo(aggregateV0.notes)
    assertThat(dayCardV0.reason).isEqualTo(aggregateV0.reason.asReason().key)
    assertThat(dayCardV0.eventTimestamp).isEqualTo(schedule.eventTimestamp())
    assertThat(dayCardV0.deleted).isFalse()

    val dayCardV1 = dayCardList.dayCards[1]
    assertThat(dayCardV1.id).isEqualTo(aggregateV1.getIdentifier().asDayCardId())
    assertThat(dayCardV1.version).isEqualTo(1)
    assertThat(dayCardV1.date).isEqualTo(schedule.slots.first().date.toLocalDateByMillis())
    assertThat(dayCardV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(dayCardV1.title).isEqualTo(aggregateV1.title)
    assertThat(dayCardV1.manpower.setScale(2)).isEqualTo(aggregateV1.manpower)
    assertThat(dayCardV1.notes).isEqualTo(aggregateV1.notes)
    assertThat(dayCardV1.reason).isEqualTo(aggregateV1.reason.asReason().key)
    assertThat(dayCardV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(dayCardV1.deleted).isTrue()
  }

  @Test
  fun `query deleted day card latest only`() {
    submitAsDeletedEvents()

    // Execute query
    val dayCardList = query(true)

    // Validate payload
    assertThat(dayCardList.dayCards).isEmpty()
  }

  @Test
  fun `query day card without schedule`() {
    eventStreamGenerator.submitDayCardG2()

    // Validate that the day card is not returned if the schedule is missing
    assertThat(query(false).dayCards).hasSize(0)
  }

  @Test
  fun `query day card without schedule latest only`() {
    eventStreamGenerator.submitDayCardG2()

    // Validate that the day card is not returned if the schedule is missing
    assertThat(query(true).dayCards).hasSize(0)
  }

  @Test
  fun `query day card without reference in schedule slot`() {
    eventStreamGenerator.submitTaskSchedule().submitDayCardG2()

    // Validate that the day card is not returned if the schedule reference is missing
    assertThat(query(false).dayCards).hasSize(0)
  }

  @Test
  fun `query day card without reference in schedule slot latest only`() {
    eventStreamGenerator.submitTaskSchedule().submitDayCardG2()

    // Validate that the day card is not returned if the schedule reference is missing
    assertThat(query(true).dayCards).hasSize(0)
  }

  private fun submitEvents(includeOptionals: Boolean) {
    eventStreamGenerator.submitTaskSchedule()

    aggregateV0 =
        eventStreamGenerator
            .submitDayCardG2 {
              if (includeOptionals) {
                it.reason = DayCardReasonNotDoneEnumAvro.BAD_WEATHER
                it.status = DayCardStatusEnumAvro.NOTDONE
              } else {
                it.reason = null
                it.notes = null
              }
            }
            .get("dayCard")!!

    schedule =
        eventStreamGenerator
            .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
              it.slots = listOf(getByReference("dayCard").asSlot(LocalDate.now()))
            }
            .get("taskSchedule")!!

    aggregateV1 =
        eventStreamGenerator
            .submitDayCardG2(eventType = DayCardEventEnumAvro.UPDATED) {
              it.manpower = BigDecimal.ONE
            }
            .get("dayCard")!!
  }

  private fun submitAsDeletedEvents() {
    eventStreamGenerator.submitTaskSchedule()

    aggregateV0 =
        eventStreamGenerator
            .submitDayCardG2 {
              it.reason = DayCardReasonNotDoneEnumAvro.BAD_WEATHER
              it.status = DayCardStatusEnumAvro.NOTDONE
            }
            .get("dayCard")!!

    schedule =
        eventStreamGenerator
            .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
              it.slots = listOf(getByReference("dayCard").asSlot(LocalDate.now()))
            }
            .get("taskSchedule")!!

    aggregateV1 =
        eventStreamGenerator
            .submitDayCardG2(eventType = DayCardEventEnumAvro.DELETED)
            .get("dayCard")!!
  }

  private fun submitBaseEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitProjectCraftG2().submitTask()
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/tasks/schedules/daycards"),
          latestOnly,
          DayCardListResource::class.java)
}
