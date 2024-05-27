/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.config.EnableKafkaListeners
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePictureTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import java.util.Locale
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.security.core.context.SecurityContextHolder

@EnableKafkaListeners
@SmartSiteSpringBootTest
class UserTests : AbstractIntegrationTest() {

  private val user by lazy {
    repositories.userRepository.findOneByIdentifier(getIdentifier("user"))!!
  }

  private val userWithLocaleNull by lazy {
    repositories.userRepository.findOneByIdentifier(getIdentifier("userWithLocaleNull"))!!
  }

  private val userWithGermanLocale by lazy {
    repositories.userRepository.findOneByIdentifier(getIdentifier("userWithGermanLocale"))!!
  }

  @BeforeEach
  fun beforeEach() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitCompany()
        .submitUserAndActivate(asReference = "userCsm")
        .submitUser("user")
        .submitUser("deletedUser")
        .submitUser("userWithLocaleNull") { it.locale = null }
        .submitUser("userWithGermanLocale") { it.locale = Locale.GERMANY.toString() }
  }

  @AfterEach
  fun cleanupBase() {
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }

  @Test
  fun `check that users are created for user events`() {
    assertThat(user).isNotNull
  }

  @Test
  fun `check that users without locale are created for user events`() {
    assertThat(userWithLocaleNull.locale).isNull()
  }

  @Test
  fun `check that users with locale are created for user events`() {
    assertThat(userWithGermanLocale.locale).isEqualTo(Locale.GERMANY)
  }

  @Test
  fun `check that existing user is updated for a user updated event`() {
    eventStreamGenerator.submitUser(
        asReference = "user",
        eventType = UserEventEnumAvro.UPDATED,
        aggregateModifications = { it.userId = randomUUID().toString() })
    assertThat(user).isNotNull
  }

  @Test
  fun `check that existing user is deleted for a user deleted event`() {
    eventStreamGenerator.submitUser(
        asReference = "deletedUser", eventType = UserEventEnumAvro.DELETED)
    assertThat(repositories.userRepository.findOneByIdentifier(getIdentifier("deletedUser")))
        .isNull()
  }

  @Test
  fun `check that existing user is deleted for a user tombstone event`() {
    eventStreamGenerator.submitUserTombstones(reference = "user")
    assertThat(repositories.userRepository.findOneByIdentifier(getIdentifier("user"))).isNull()
  }

  @Test
  fun `check that nothing happens for a user picture tombstone event`() {
    assertDoesNotThrow {
      eventStreamGenerator
          .submitProfilePicture()
          .submitProfilePictureTombstones(reference = "profilePicture")
    }
  }
}
