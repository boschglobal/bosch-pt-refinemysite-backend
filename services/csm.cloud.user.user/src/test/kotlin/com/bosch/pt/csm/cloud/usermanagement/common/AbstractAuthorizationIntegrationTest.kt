/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.usermanagement.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.UserTypeAccess.Companion.createGrantedGroup
import com.bosch.pt.csm.cloud.usermanagement.user.authorization.model.UserCountryRestriction
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.util.concurrent.Executors.callable
import java.util.stream.Stream
import org.apache.commons.lang3.tuple.Pair
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.springframework.security.access.AccessDeniedException

/** Server side base class for testing database based tests. */
@EnableAllKafkaListeners
@SmartSiteSpringBootTest
abstract class AbstractAuthorizationIntegrationTest : AbstractIntegrationTest() {

  var userMap: MutableMap<String, Pair<User, Boolean>> = HashMap()
  lateinit var user: User
  lateinit var otherUser: User
  lateinit var admin: User

  @BeforeEach
  protected fun initAbstractAuthorizationIntegrationTest() {
    eventStreamGenerator
        .setUserContext("system")
        .submitUser("user")
        .submitUser("otherUser") { it.userId = "otherUser" }
        .submitUser("admin") {
          it.userId = "admin"
          it.admin = true
        }

    user = repositories.userRepository.findWithDetailsByIdentifier(userIdOf("user"))!!
    admin = repositories.userRepository.findWithDetailsByIdentifier(userIdOf("admin"))!!
    otherUser = repositories.userRepository.findWithDetailsByIdentifier(userIdOf("otherUser"))!!

    userMap[USER] = Pair.of(user, false)
    userMap[ADMIN] = Pair.of(admin, true)
    userMap[OTHER_USER] = Pair.of(otherUser, false)

    setAuthentication("user")
  }

  protected fun checkAccessWith(userTypeAccess: UserTypeAccess, runnable: Runnable) {
    checkAccessWith(userTypeAccess.userType, userTypeAccess.isAccessGranted, runnable)
  }

  private fun checkAccessWith(userRole: String, isAccessGranted: Boolean, procedure: Runnable) {
    if (isAccessGranted) {
      doWithAuthorization(userMap[userRole]!!, callable(procedure))
    } else {
      doWithAuthorization(userMap[userRole]!!) {
        assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy { procedure.run() }
      }
    }
  }

  protected fun addUsersAndRestrictAdminAccessToOneCountry(
      locked: Boolean = false,
      admin: Boolean = false
  ) {
    eventStreamGenerator.submitUser("userInAL") {
      it.firstName = "findMe"
      it.country = IsoCountryCodeEnumAvro.AL
      it.locked = locked
      it.admin = admin
    }
    eventStreamGenerator.submitUser("userInAD") {
      it.firstName = "findMe"
      it.country = IsoCountryCodeEnumAvro.AD
      it.locked = locked
      it.admin = admin
    }
    UserCountryRestriction(eventStreamGenerator.getIdentifier("admin"), IsoCountryCodeEnum.AL)
        .apply { repositories.userCountryRestrictionRepository.save(this) }
  }

  protected fun userIdOf(reference: String) = UserId(eventStreamGenerator.getIdentifier(reference))

  companion object {
    const val USER = "USER"
    const val ADMIN = "ADMIN"
    const val OTHER_USER = "OTHER_USER"

    val userTypes = arrayOf(USER, ADMIN, OTHER_USER)

    @JvmStatic fun adminOnly(): Stream<UserTypeAccess> = createGrantedGroup(userTypes, setOf(ADMIN))
  }
}
