/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.extension.asDay
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.HolidayAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class WorkDayConfigurationGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val workDay = "projects[0].workDayConfiguration"

  val query =
      """
      query {
        projects {
          workDayConfiguration {
            id
            version
            startOfWeek,
            workingDays,
            holidays {
              name
              date
            }
            allowWorkOnNonWorkingDays
            eventDate
          }
        }
      }
      """
          .trimIndent()

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query work day configuration`() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitWorkdayConfiguration {
      it.startOfWeek = DayEnumAvro.MONDAY
      it.workingDays = listOf(DayEnumAvro.MONDAY)
      it.holidays = listOf(HolidayAvro("New year", LocalDate.now().toEpochMilli()))
      it.allowWorkOnNonWorkingDays = false
    }

    val aggregateV1 =
        eventStreamGenerator
            .submitWorkdayConfiguration(eventType = WorkdayConfigurationEventEnumAvro.UPDATED) {
              it.allowWorkOnNonWorkingDays = true
            }
            .get<WorkdayConfigurationAggregateAvro>("workdayConfiguration")!!

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$workDay.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$workDay.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$workDay.startOfWeek").isEqualTo(aggregateV1.startOfWeek.asDay().shortKey)
    response.get("$workDay.workingDays[0]").isEqualTo(aggregateV1.workingDays[0].asDay().shortKey)
    response.get("$workDay.holidays[0].name").isEqualTo(aggregateV1.holidays[0].name)
    response
        .get("$workDay.holidays[0].date")
        .isEqualTo(aggregateV1.holidays[0].date.toLocalDateByMillis().toString())
    response
        .get("$workDay.allowWorkOnNonWorkingDays")
        .isEqualTo(aggregateV1.allowWorkOnNonWorkingDays)
    response.get("$workDay.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query deleted work day configuration`() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitWorkdayConfiguration {
          it.startOfWeek = DayEnumAvro.MONDAY
          it.workingDays = listOf(DayEnumAvro.MONDAY)
          it.holidays = listOf(HolidayAvro("New year", LocalDate.now().toEpochMilli()))
          it.allowWorkOnNonWorkingDays = false
        }
        .submitWorkdayConfiguration(eventType = WorkdayConfigurationEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(workDay)
  }
}
