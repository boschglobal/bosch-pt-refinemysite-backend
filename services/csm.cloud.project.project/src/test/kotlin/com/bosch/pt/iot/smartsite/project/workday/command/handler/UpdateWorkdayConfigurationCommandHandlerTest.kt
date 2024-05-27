/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workday.command.api.UpdateWorkdayConfigurationCommand
import com.bosch.pt.iot.smartsite.project.workday.domain.asWorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday.Companion.MAX_HOLIDAY_AMOUNT
import java.time.DayOfWeek
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class UpdateWorkdayConfigurationCommandHandlerTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: UpdateWorkdayConfigurationCommandHandler

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify update workday configuration fails with number of holidays that exceed the maximum value`() {
    val holidays = mutableListOf<Holiday>()

    // Generate more than MAX_HOLIDAY_NUMBER of holidays
    for (i in 1..MAX_HOLIDAY_AMOUNT + 1) {
      holidays.add(Holiday("Holiday_$i", LocalDate.now().plusDays(i.toLong())))
    }

    val workdayConfigurationVersion =
        repositories
            .findWorkdayConfiguration(
                getIdentifier("workdayConfiguration").asWorkdayConfigurationId())!!
            .version

    val updateWorkdayConfigurationCommand =
        UpdateWorkdayConfigurationCommand(
            version = workdayConfigurationVersion,
            projectRef = getIdentifier("project").asProjectId(),
            startOfWeek = DayOfWeek.FRIDAY,
            workingDays =
                mutableListOf(
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY,
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY),
            holidays = holidays,
            allowWorkOnNonWorkingDays = false)

    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      cut.handle(updateWorkdayConfigurationCommand)
    }

    projectEventStoreUtils.verifyEmpty()
  }
}
