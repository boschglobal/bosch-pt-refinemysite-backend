/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import com.bosch.pt.csm.cloud.projectmanagement.util.doWithAuthorization
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.springframework.security.access.AccessDeniedException

abstract class AbstractAuthorizationIntegrationTest : AbstractActivityIntegrationTest() {

  protected fun activeProjectParticipants() =
      listOf(
          UserAccess("Foreman", fmUser, true),
          UserAccess("Inactive participant", fmUserInactive, false),
          UserAccess("No participant", fmUserNoParticipant, false),
          UserAccess("Participant from other project", otherProjectUser, false))

  protected fun checkAccessWith(
      accessList: List<UserAccess>,
      procedure: () -> Unit
  ): List<DynamicTest> =
      accessList.map {
        if (it.isGranted) {
          dynamicTest("${it.description} is granted") {
            doWithAuthorization(repositories.findUser(it.user)) { procedure() }
          }
        } else {
          dynamicTest("${it.description} is denied") {
            assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
              doWithAuthorization(repositories.findUser(it.user)) { procedure() }
            }
          }
        }
      }

  data class UserAccess(
      val description: String,
      val user: UserAggregateAvro,
      val isGranted: Boolean
  )
}
