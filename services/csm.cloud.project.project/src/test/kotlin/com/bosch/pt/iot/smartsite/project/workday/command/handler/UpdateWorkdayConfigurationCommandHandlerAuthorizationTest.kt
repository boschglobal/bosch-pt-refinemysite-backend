/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.workday.command.api.UpdateWorkdayConfigurationCommand
import com.bosch.pt.iot.smartsite.project.workday.domain.asWorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate.now
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class UpdateWorkdayConfigurationCommandHandlerAuthorizationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: UpdateWorkdayConfigurationCommandHandler

  private val workdayConfiguration by lazy {
    repositories.findWorkdayConfiguration(
        getIdentifier("workdayConfiguration").asWorkdayConfigurationId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("userCsm").submitWorkdayConfiguration()
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify update workday configuration is authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(
            UpdateWorkdayConfigurationCommand(
                version = workdayConfiguration.version,
                projectRef = workdayConfiguration.project.identifier,
                startOfWeek = FRIDAY,
                workingDays = listOf(FRIDAY, SATURDAY, SUNDAY, MONDAY, TUESDAY),
                holidays = listOf(Holiday("Holiday", now())),
                allowWorkOnNonWorkingDays = true))
      }
}
