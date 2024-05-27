/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.extensions.toUserId
import com.bosch.pt.csm.cloud.usermanagement.common.repository.PageableDefaults.DEFAULT_PAGE_REQUEST
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_USER_NOT_LOCKING_THEMSELVES
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_USER_NOT_REMOVING_OWN_ADMIN_PERMISSION
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.SetUserLockedResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.SetUserRoleResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.SuggestionResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_DELETE
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK

@CodeExample
class UserApiIntegrationTest : AbstractApiIntegrationTest() {

  @Autowired private lateinit var cut: UserController

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate().submitUserAndActivate("admin") {
      it.admin = true
    }

    setAuthentication("admin")
  }

  @Test
  fun `verify find user by id succeeds`() {
    eventStreamGenerator.submitUser("user").submitProfilePicture("picture")

    val user = eventStreamGenerator.get<UserAggregateAvro>("user")!!

    cut.findOneById(user.toUserId()).apply {
      assertThat(statusCode).isEqualTo(OK)
      body!!.apply {
        assertThat(identifier).isEqualTo(user.getIdentifier())
        assertThat(email).isEqualTo(user.getEmail())
      }
    }
  }

  @Test
  fun `verify find all users succeeds`() {
    eventStreamGenerator.submitUser("user").submitProfilePicture("picture")

    cut.findAllUsers(DEFAULT_PAGE_REQUEST).apply {
      assertThat(statusCode).isEqualTo(OK)
      body!!.apply {
        assertThat(users).hasSize(3)
        users.forEach { user ->
          assertThat(user).isNotNull
          if (user.identifier == eventStreamGenerator.getIdentifier("user")) {
            assertThat(user.getLink(LINK_DELETE)).isNotEmpty
          } else {
            // system user and testadmin user (being the current user)
            assertThat(user.getLink(LINK_DELETE)).isEmpty
          }
        }
      }
    }
  }

  @Test
  fun `verify suggest users term matches first name`() {
    eventStreamGenerator
        .submitUser("user1") { it.firstName = "AAA1" }
        .submitUser("user2") { it.firstName = "AAA2" }

    cut.suggestUsersByTerm(SuggestionResource().apply { term = "AAA" }, Pageable.unpaged()).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.apply {
        assertThat(items).hasSize(2)
        assertThat(items)
            .extracting("identifier")
            .containsExactlyInAnyOrder(
                eventStreamGenerator.getIdentifier("user1"),
                eventStreamGenerator.getIdentifier("user2"))
      }
    }
  }

  @Test
  fun `verify suggest users term matches email`() {
    eventStreamGenerator.submitUser("user") {
      it.firstName = "AAA1"
      it.email = "email@web.de"
    }

    cut.suggestUsersByTerm(SuggestionResource().apply { term = "AAA" }, Pageable.unpaged()).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.apply {
        assertThat(items).hasSize(1)
        assertThat(items.first().identifier).isEqualTo(eventStreamGenerator.getIdentifier("user"))
      }
    }
  }

  @Test
  fun `verify suggest users ignores unregistered users`() {
    eventStreamGenerator
        .submitUser("user1") { it.firstName = "AAA1" }
        .submitUser("user2") {
          it.firstName = "AAA2"
          it.registered = false
        }

    cut.suggestUsersByTerm(SuggestionResource().apply { term = "AAA" }, Pageable.unpaged()).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.apply {
        assertThat(items).hasSize(1)
        assertThat(items.first().identifier).isEqualTo(eventStreamGenerator.getIdentifier("user1"))
      }
    }
  }

  @Test
  fun `verify suggest users fails for invalid term`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.suggestUsersByTerm(
          SuggestionResource().apply { term = StringUtils.repeat("test", 51) }, Pageable.unpaged())
    }
  }

  @Test
  fun `verify removing admin role from another user is successful`() {

    eventStreamGenerator.submitUser("anotherAdminUser") { it.admin = true }

    setAuthentication("anotherAdminUser")

    val userId = UserId(eventStreamGenerator.getIdentifier("admin"))
    cut.setUserRole(userId, SetUserRoleResource(false))

    repositories.userRepository.findOneByIdentifier(userId)!!.apply { assertThat(admin).isFalse() }
  }

  @Test
  fun `verify admin user cannot remove admin role from himself`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.setUserRole(
              UserId(eventStreamGenerator.getIdentifier("admin")), SetUserRoleResource(false))
        }
        .withMessage(USER_VALIDATION_ERROR_USER_NOT_REMOVING_OWN_ADMIN_PERMISSION)
  }

  @Test
  fun `verify system user cannot become admin`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.setUserRole(
              UserId(eventStreamGenerator.getIdentifier("system")), SetUserRoleResource(true))
        }
        .withMessage(USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED)
  }

  @Test
  fun `verify unlocking another user is successful`() {
    eventStreamGenerator.submitUser("anotherUser") { it.locked = true }

    setAuthentication("admin")

    val userId = UserId(eventStreamGenerator.getIdentifier("anotherUser"))
    cut.lockUser(userId, SetUserLockedResource(false))

    repositories.userRepository.findOneByIdentifier(userId)!!.apply { assertThat(locked).isFalse() }
  }

  @Test
  fun `verify admin user cannot lock himself`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.lockUser(
              UserId(eventStreamGenerator.getIdentifier("admin")), SetUserLockedResource(true))
        }
        .withMessage(USER_VALIDATION_ERROR_USER_NOT_LOCKING_THEMSELVES)
  }

  @Test
  fun `verify system user cannot be locked`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.lockUser(
              UserId(eventStreamGenerator.getIdentifier("system")), SetUserLockedResource(true))
        }
        .withMessage(USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED)
  }

  @Test
  fun `verify system user cannot be deleted`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.deleteUser(UserId(eventStreamGenerator.getIdentifier("system"))) }
        .withMessage(USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED)
  }

  @Test
  fun `verify delete user succeeds`() {
    eventStreamGenerator.submitUser("user")

    val userIdentifier = UserId(eventStreamGenerator.getIdentifier("user"))

    // make sure that you find that user before deleting
    cut.findOneById(userIdentifier).apply { assertThat(statusCode).isEqualTo(OK) }

    cut.deleteUser(userIdentifier).apply { assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT) }

    // check that the find behaviour is now different
    assertThatThrownBy { cut.findOneById(userIdentifier) }
        .isInstanceOf(AggregateNotFoundException::class.java)
        .hasMessage(USER_VALIDATION_ERROR_NOT_FOUND)
  }

  @Test
  fun `deleting a user creates tombstone messages for every version of the user`() {

    eventStreamGenerator
        .submitUser("toBeDeleted") { it.firstName = "A" }
        .submitUser("toBeDeleted", eventType = UPDATED) { it.firstName = "B" }
        .submitUser("toBeDeleted", eventType = UPDATED) { it.firstName = "C" }

    userEventStoreUtils.reset()

    cut.deleteUser(getIdentifier("toBeDeleted").asUserId())

    userEventStoreUtils.verifyContainsTombstoneMessageAndGet(3).also {
      validateTombstoneMessageKey(it[0], "toBeDeleted", 0)
      validateTombstoneMessageKey(it[1], "toBeDeleted", 1)
      validateTombstoneMessageKey(it[2], "toBeDeleted", 2)
    }
  }

  @Test
  fun `verify delete unknown user throws exception`() {
    assertThatThrownBy { cut.deleteUser(UserId()) }
        .isInstanceOf(AggregateNotFoundException::class.java)
        .hasMessage(USER_VALIDATION_ERROR_NOT_FOUND)
  }

  private fun validateTombstoneMessageKey(
      messageKey: MessageKeyAvro,
      reference: String,
      version: Long
  ) {
    getByReference(reference)
        .let {
          AggregateIdentifierAvro.newBuilder()
              .setType(it.type)
              .setVersion(version)
              .setIdentifier(it.identifier)
              .build()
        }
        .apply { assertThat(messageKey.aggregateIdentifier).isEqualTo(this) }
  }
}
