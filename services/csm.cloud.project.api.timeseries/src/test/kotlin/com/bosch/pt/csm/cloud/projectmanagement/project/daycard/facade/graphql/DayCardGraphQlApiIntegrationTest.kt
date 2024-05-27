/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.graphql

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getDayCardVersion
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
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

@RmsSpringBootTest
class DayCardGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  @Autowired lateinit var messageSource: MessageSource

  val dayCard = "projects[0].tasks[0].dayCards[0]"

  val query =
      """
      query {
        projects {
          tasks {
            dayCards {
              id
              version
              date
              status
              title
              manpower
              notes
              reason {
                key
                displayName
              }
              eventDate
            }
          }
        }
      }
      """
          .trimIndent()

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

    val translatedReason = translate(aggregateV1.reason.asReason().messageKey)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$dayCard.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$dayCard.version").isEqualTo(aggregateV1.getDayCardVersion().toString())
    response
        .get("$dayCard.date")
        .isEqualTo(schedule.slots.first().date.toLocalDateByMillis().toString())
    response.get("$dayCard.status").isEqualTo(aggregateV1.status.asStatus().shortKey)
    response.get("$dayCard.title").isEqualTo(aggregateV1.title)
    response.get("$dayCard.manpower").isEqualTo(aggregateV1.manpower.setScale(0).toString())
    response.get("$dayCard.notes").isEqualTo(aggregateV1.notes)
    response.get("$dayCard.reason.key").isEqualTo(aggregateV1.reason.asReason().shortKey)
    response.get("$dayCard.reason.displayName").isEqualTo(translatedReason)
    response.get("$dayCard.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query day card without optional parameters`() {
    submitEvents(false)

    // Execute query and validate mandatory fields
    val response = graphQlTester.document(query).execute()
    response.get("$dayCard.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$dayCard.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response
        .get("$dayCard.date")
        .isEqualTo(schedule.slots.first().date.toLocalDateByMillis().toString())
    response.get("$dayCard.status").isEqualTo(aggregateV1.status.asStatus().shortKey)
    response.get("$dayCard.title").isEqualTo(aggregateV1.title)
    response.get("$dayCard.manpower").isEqualTo(aggregateV1.manpower.setScale(0).toString())
    response.get("$dayCard.eventDate").isEqualTo(aggregateV1.eventDate())

    // Check optional attributes
    response.isNull("$dayCard.reason")
    response.isNull("$dayCard.notes")
  }

  @Test
  fun `query day card without schedule`() {
    eventStreamGenerator.submitDayCardG2()

    // Validate that the day card is not returned if the schedule is missing
    val response = graphQlTester.document(query).execute()
    response.isNull(dayCard)
  }

  @Test
  fun `query day card without reference in schedule slot`() {
    eventStreamGenerator.submitTaskSchedule().submitDayCardG2()

    // Validate that the day card is not returned if the schedule reference is missing
    val response = graphQlTester.document(query).execute()
    response.isNull(dayCard)
  }

  @Test
  fun `query deleted day card`() {
    submitEvents(true)
    eventStreamGenerator.submitDayCardG2("dayCard", eventType = DayCardEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(dayCard)
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

  private fun submitBaseEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitProjectCraftG2().submitTask()
  }

  private fun translate(key: String) =
      messageSource.getMessage(key, emptyArray(), LocaleContextHolder.getLocale())
}
