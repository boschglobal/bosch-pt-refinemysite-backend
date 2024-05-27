/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.user.constants.RoleConstants.USER
import com.bosch.pt.csm.cloud.user.query.UserProjection
import com.bosch.pt.csm.cloud.user.query.UserProjectionBuilder
import com.bosch.pt.csm.cloud.user.query.UserProjector
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException

@SmartSiteMockKTest
internal class UserDetailsServiceImplTest {

  @MockK private lateinit var userProjector: UserProjector

  @InjectMockKs private lateinit var cut: UserDetailsServiceImpl

  @Test
  fun verifyLoadUserByUsername() {
    val originalUser = UserProjectionBuilder.user().withCiamUserId(USERID).build()
    every { userProjector.loadUserProjectionByCiamId(USERID) } returns originalUser

    val userDetails = cut.loadUserByUsername(USERID)
    assertThat(userDetails).isNotNull
    assertThat(userDetails.username).isEqualTo(originalUser.username)
    assertThat(userDetails).isInstanceOf(UserProjection::class.java)

    val user = userDetails as UserProjection
    assertThat(user.authorities).containsExactly(SimpleGrantedAuthority(USER.roleName()))

    verify { userProjector.loadUserProjectionByCiamId(any()) }
  }

  @Test
  fun verifyLoadUserByUsernameNotFound() {
    every { userProjector.loadUserProjectionByCiamId(INVALID_USERID) } returns null
    assertThatThrownBy { cut.loadUserByUsername(INVALID_USERID) }
        .isInstanceOf(UsernameNotFoundException::class.java)
  }

  @Test
  fun verifyLoadUserByUsernameIsEmpty() {
    assertThatThrownBy { cut.loadUserByUsername(StringUtils.EMPTY) }
        .isInstanceOf(UsernameNotFoundException::class.java)
  }

  companion object {
    private const val USERID = "hans.mustermann@example.com"
    private const val INVALID_USERID = "dummy"
  }
}
