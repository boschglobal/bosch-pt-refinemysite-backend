/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.domain.asWorkDayConfigurationId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.extension.asDay
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.WorkDayConfigurationListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.Holiday
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import com.bosch.pt.csm.cloud.projectmanagement.workday.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.HolidayAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class WorkDayConfigurationRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: WorkdayConfigurationAggregateAvro

  lateinit var aggregateV1: WorkdayConfigurationAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
    submitBaseEvents()
  }

  @Test
  fun `query work day configuration`() {
    submitEvents()

    // Execute query
    val workDayConfigurationList = query(false)

    // Validate payload
    assertThat(workDayConfigurationList.workDayConfigurations).hasSize(2)

    val configV0 = workDayConfigurationList.workDayConfigurations[0]
    assertThat(configV0.id).isEqualTo(aggregateV0.getIdentifier().asWorkDayConfigurationId())
    assertThat(configV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(configV0.project).isEqualTo(aggregateV0.project.identifier.toUUID().asProjectId())
    assertThat(configV0.startOfWeek).isEqualTo(aggregateV0.startOfWeek.asDay().key)
    assertThat(configV0.workingDays).isEqualTo(aggregateV0.workingDays.map { it.asDay().key })
    assertThat(configV0.holidays)
        .isEqualTo(aggregateV0.holidays.map { Holiday(it.name, it.date.toLocalDateByMillis()) })
    assertThat(configV0.allowWorkOnNonWorkingDays).isEqualTo(aggregateV0.allowWorkOnNonWorkingDays)
    assertThat(configV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(configV0.deleted).isFalse()

    val configV1 = workDayConfigurationList.workDayConfigurations[1]
    assertThat(configV1.id).isEqualTo(aggregateV1.getIdentifier().asWorkDayConfigurationId())
    assertThat(configV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(configV1.project).isEqualTo(aggregateV1.project.identifier.toUUID().asProjectId())
    assertThat(configV1.startOfWeek).isEqualTo(aggregateV1.startOfWeek.asDay().key)
    assertThat(configV1.workingDays).isEqualTo(aggregateV1.workingDays.map { it.asDay().key })
    assertThat(configV1.holidays)
        .isEqualTo(aggregateV1.holidays.map { Holiday(it.name, it.date.toLocalDateByMillis()) })
    assertThat(configV1.allowWorkOnNonWorkingDays).isEqualTo(aggregateV1.allowWorkOnNonWorkingDays)
    assertThat(configV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(configV1.deleted).isFalse()
  }

  @Test
  fun `query work day configuration latest only`() {
    submitEvents()

    // Execute query
    val workDayConfigurationList = query(true)

    // Validate payload
    assertThat(workDayConfigurationList.workDayConfigurations).hasSize(1)
    val config = workDayConfigurationList.workDayConfigurations.first()

    assertThat(config.id).isEqualTo(aggregateV1.getIdentifier().asWorkDayConfigurationId())
    assertThat(config.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(config.project).isEqualTo(aggregateV1.project.identifier.toUUID().asProjectId())
    assertThat(config.startOfWeek).isEqualTo(aggregateV1.startOfWeek.asDay().key)
    assertThat(config.workingDays).isEqualTo(aggregateV1.workingDays.map { it.asDay().key })
    assertThat(config.holidays)
        .isEqualTo(aggregateV1.holidays.map { Holiday(it.name, it.date.toLocalDateByMillis()) })
    assertThat(config.allowWorkOnNonWorkingDays).isEqualTo(aggregateV1.allowWorkOnNonWorkingDays)
    assertThat(config.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(config.deleted).isFalse()
  }

  @Test
  fun `query deleted work day configuration`() {
    submitAsDeletedEvents()

    // Execute query
    val workDayConfigurationList = query(false)

    // Validate payload
    assertThat(workDayConfigurationList.workDayConfigurations).hasSize(2)

    val configV0 = workDayConfigurationList.workDayConfigurations[0]
    assertThat(configV0.id).isEqualTo(aggregateV0.getIdentifier().asWorkDayConfigurationId())
    assertThat(configV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(configV0.project).isEqualTo(aggregateV0.project.identifier.toUUID().asProjectId())
    assertThat(configV0.startOfWeek).isEqualTo(aggregateV0.startOfWeek.asDay().key)
    assertThat(configV0.workingDays).isEqualTo(aggregateV0.workingDays.map { it.asDay().key })
    assertThat(configV0.holidays)
        .isEqualTo(aggregateV0.holidays.map { Holiday(it.name, it.date.toLocalDateByMillis()) })
    assertThat(configV0.allowWorkOnNonWorkingDays).isEqualTo(aggregateV0.allowWorkOnNonWorkingDays)
    assertThat(configV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(configV0.deleted).isFalse()

    val configV1 = workDayConfigurationList.workDayConfigurations[1]
    assertThat(configV1.id).isEqualTo(aggregateV1.getIdentifier().asWorkDayConfigurationId())
    assertThat(configV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(configV1.project).isEqualTo(aggregateV1.project.identifier.toUUID().asProjectId())
    assertThat(configV1.startOfWeek).isEqualTo(aggregateV1.startOfWeek.asDay().key)
    assertThat(configV1.workingDays).isEqualTo(aggregateV1.workingDays.map { it.asDay().key })
    assertThat(configV1.holidays)
        .isEqualTo(aggregateV1.holidays.map { Holiday(it.name, it.date.toLocalDateByMillis()) })
    assertThat(configV1.allowWorkOnNonWorkingDays).isEqualTo(aggregateV1.allowWorkOnNonWorkingDays)
    assertThat(configV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(configV1.deleted).isTrue()
  }

  @Test
  fun `query deleted work day configuration latest only`() {
    submitAsDeletedEvents()

    // Execute query
    val workDayConfigurationList = query(true)

    // Validate payload
    assertThat(workDayConfigurationList.workDayConfigurations).isEmpty()
  }

  fun submitEvents() {
    aggregateV1 =
        eventStreamGenerator
            .submitWorkdayConfiguration(eventType = WorkdayConfigurationEventEnumAvro.UPDATED) {
              it.allowWorkOnNonWorkingDays = true
            }
            .get("workdayConfiguration")!!
  }

  fun submitAsDeletedEvents() {
    aggregateV1 =
        eventStreamGenerator
            .submitWorkdayConfiguration(eventType = WorkdayConfigurationEventEnumAvro.DELETED)
            .get("workdayConfiguration")!!
  }

  fun submitBaseEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
    aggregateV0 =
        eventStreamGenerator
            .submitWorkdayConfiguration {
              it.startOfWeek = DayEnumAvro.MONDAY
              it.workingDays = listOf(DayEnumAvro.MONDAY)
              it.holidays = listOf(HolidayAvro("New year", LocalDate.now().toEpochMilli()))
              it.allowWorkOnNonWorkingDays = false
            }
            .get("workdayConfiguration")!!
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/workdays"),
          latestOnly,
          WorkDayConfigurationListResource::class.java)
}
