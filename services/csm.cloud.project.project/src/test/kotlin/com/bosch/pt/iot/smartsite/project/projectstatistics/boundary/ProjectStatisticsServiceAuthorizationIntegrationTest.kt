/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectstatistics.boundary

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class ProjectStatisticsServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectStatisticsService

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify get task statistics authorized`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.getTaskStatistics(project.identifier)
      cut.getTaskStatistics(setOf(project.identifier))
    }
  }

  @ParameterizedTest
  @MethodSource("allAndAnonymous")
  fun `verify get task statistics authorized for empty list of projects`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.getTaskStatistics(emptySet()) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify get topic statistics authorized`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.getTopicStatistics(project.identifier)
      cut.getTopicStatistics(setOf(project.identifier))
    }
  }

  @ParameterizedTest
  @MethodSource("allAndAnonymous")
  fun `verify get topic statistics authorized for empty list of projects`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.getTopicStatistics(emptySet()) }
  }
}
