/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.authorization

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.WorkDayConfigurationRestController.Companion.WORK_DAYS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.WorkDayConfigurationListResource
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.HolidayAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class WorkDayConfigurationRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitWorkdayConfiguration {
      it.startOfWeek = DayEnumAvro.MONDAY
      it.workingDays = listOf(DayEnumAvro.MONDAY)
      it.holidays = listOf(HolidayAvro("New year", LocalDate.now().toEpochMilli()))
      it.allowWorkOnNonWorkingDays = false
    }
  }

  @Test
  fun `verify that an authorized user is allowed to read work day configurations`() {
    setAuthentication("csm-user")
    val workDayConfigurationList =
        query(
            latestProjectApi(WORK_DAYS_ENDPOINT),
            false,
            WorkDayConfigurationListResource::class.java)
    assertThat(workDayConfigurationList.workDayConfigurations).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read work day configurations`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestProjectApi(WORK_DAYS_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestProjectApi(WORK_DAYS_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
