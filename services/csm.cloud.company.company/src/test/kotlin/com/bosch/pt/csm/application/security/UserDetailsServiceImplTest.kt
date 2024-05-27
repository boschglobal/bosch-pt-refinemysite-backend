/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.security

import com.bosch.pt.csm.application.SmartSiteMockKTest
import com.bosch.pt.csm.user.user.constants.RoleConstants
import com.bosch.pt.csm.user.user.model.UserBuilder
import com.bosch.pt.csm.user.user.query.UserProjection
import com.bosch.pt.csm.user.user.query.UserQueryService
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UsernameNotFoundException

@SmartSiteMockKTest
class UserDetailsServiceImplTest {

  @MockK private lateinit var userQueryService: UserQueryService

  @InjectMockKs private lateinit var cut: UserDetailsServiceImpl

  @Test
  fun verifyLoadUserByUsername() {
    every { userQueryService.findOneByCiamUserId(USERID) } returns
        UserBuilder.user()
            .withUserId(USERID)
            .withFirstName(FIRST_NAME)
            .withLastName(LAST_NAME)
            .withEmail(EMAIL)
            .build()

    val userDetails = cut.loadUserByUsername(USERID)
    assertThat(userDetails).isNotNull
    assertThat(userDetails.username).isEqualTo(USERID)
    assertThat(userDetails).isInstanceOf(UserProjection::class.java)

    val user = userDetails as UserProjection
    assertThat(user.firstName).isEqualTo(FIRST_NAME)
    assertThat(user.lastName).isEqualTo(LAST_NAME)
    assertThat(user.authorities).extracting("authority").contains(RoleConstants.USER.roleName())

    verify { userQueryService.findOneByCiamUserId(any()) }
  }

  @Test
  fun verifyLoadUserByUsernameNotFound() {
    every { userQueryService.findOneByCiamUserId(INVALID_USERID) } returns null
    assertThatThrownBy { cut.loadUserByUsername(INVALID_USERID) }
        .isInstanceOf(UsernameNotFoundException::class.java)
  }

  @Test
  fun verifyLoadUserByUsernameIsEmpty() {
    assertThatThrownBy { cut.loadUserByUsername(StringUtils.EMPTY) }
        .isInstanceOf(UsernameNotFoundException::class.java)
  }

  companion object {
    private const val EMAIL = "hans.mustermann@example.com"
    private const val USERID = EMAIL
    private const val FIRST_NAME = "Hans"
    private const val LAST_NAME = "Mustermann"
    private const val INVALID_USERID = "dummy"
  }
}
