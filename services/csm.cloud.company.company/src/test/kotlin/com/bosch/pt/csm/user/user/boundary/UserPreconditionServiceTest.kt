/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.user.user.boundary

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.user.user.model.UserBuilder
import com.bosch.pt.csm.common.util.toUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

class UserPreconditionServiceTest {

  private val systemUserId = SYSTEM_USER_IDENTIFIER.toUUID().asUserId()
  private val cut = UserPreconditionService(systemUserId)

  @BeforeEach
  fun init() {
    ReflectionTestUtils.setField(cut, "systemUserIdentifier", systemUserId)
  }

  @Test
  fun verifyValidateUserDeletionIsPossible() {
    val user = UserBuilder.user().build()
    assertThat(cut.isDeleteUserPossible(user.id)).isTrue
  }

  @Test
  fun verifyValidateUserDeletionIsNotPossible() {
    val user = UserBuilder.user().withIdentifier(systemUserId).build()
    assertThat(cut.isDeleteUserPossible(user.id)).isFalse
  }

  @Test
  fun verifyValidateUserDeletionUserNotFound() {
    assertThat(cut.isDeleteUserPossible(null)).isFalse
  }

  companion object {
    private const val SYSTEM_USER_IDENTIFIER = "c37da613-8e70-4003-9106-12412c9d2496"
  }
}
