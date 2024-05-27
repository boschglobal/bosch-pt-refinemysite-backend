/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskstatistics.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization in Task Statistics Service")
class TaskStatisticsServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskStatisticsService

  private val taskIdentifier by lazy { getIdentifier("task").asTaskId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        // Only the csm users can add project crafts to a project
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .setUserContext("userCreator")
        .submitTask()
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `view permission for single task is granted to`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findTaskStatistics(taskIdentifier) }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `view permission for multiple tasks is granted to`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findTaskStatistics(setOf(taskIdentifier)) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `view permission is denied for non-existing task`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findTaskStatistics(TaskId()) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `view permission is denied for non-existing tasks`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findTaskStatistics(setOf(TaskId())) }
}
