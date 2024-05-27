/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.rest.authorization

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.DayCardRestController.Companion.DAY_CARDS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.DayCardListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.BAD_WEATHER
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.NOTDONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class DayCardRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitTask()
        .submitTaskSchedule()
        .submitDayCardG2() {
          it.reason = BAD_WEATHER
          it.status = NOTDONE
        }
        .submitTaskSchedule(eventType = UPDATED) {
          it.slots = listOf(getByReference("dayCard").asSlot(LocalDate.now()))
        }
  }

  @Test
  fun `verify that an authorized user is allowed to read day cards`() {
    setAuthentication("csm-user")
    val dayCardList =
        query(latestProjectApi(DAY_CARDS_ENDPOINT), false, DayCardListResource::class.java)
    assertThat(dayCardList.dayCards).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read day cards`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestProjectApi(DAY_CARDS_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestProjectApi(DAY_CARDS_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
