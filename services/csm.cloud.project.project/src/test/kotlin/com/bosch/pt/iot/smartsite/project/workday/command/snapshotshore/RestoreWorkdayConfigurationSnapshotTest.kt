/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.snapshotshore

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.FRIDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.MONDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.SATURDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.SUNDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.TUESDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.HolidayAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workday.domain.asWorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.util.WorkdayConfigurationTestUtils.validateWorkdayConfiguration
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreWorkdayConfigurationSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
  }

  @Test
  open fun `validate that workday configuration created event was processed successfully`() {
    val workdayConfiguration =
        repositories.findWorkdayConfiguration(
            getIdentifier("workdayConfiguration").asWorkdayConfigurationId())!!
    val aggregate = get<WorkdayConfigurationAggregateAvro>("workdayConfiguration")!!

    validateWorkdayConfiguration(workdayConfiguration, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that workday configuration updated event was processed successfully`() {
    eventStreamGenerator.submitWorkdayConfiguration(
        asReference = "workdayConfiguration", eventType = UPDATED) {
          it.startOfWeek = FRIDAY
          it.workingDays = mutableListOf(FRIDAY, SATURDAY, SUNDAY, MONDAY, TUESDAY)
          it.holidays =
              mutableListOf(
                  HolidayAvro("Holiday_1", now().plusDays(1).toEpochMilli()),
                  HolidayAvro("Holiday_2", now().plusDays(2).toEpochMilli()))
          it.allowWorkOnNonWorkingDays = false
        }

    val workdayConfiguration =
        repositories.findWorkdayConfiguration(
            getIdentifier("workdayConfiguration").asWorkdayConfigurationId())!!
    val aggregate = get<WorkdayConfigurationAggregateAvro>("workdayConfiguration")!!

    validateWorkdayConfiguration(workdayConfiguration, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that workday configuration deleted event deletes successfully`() {
    // Testing idempotency
    eventStreamGenerator
        .submitWorkdayConfiguration(asReference = "workdayConfiguration", eventType = DELETED)
        .repeat(1)

    assertThat(
            repositories.findWorkdayConfiguration(
                getIdentifier("workdayConfiguration").asWorkdayConfigurationId()))
        .isNull()
  }
}
