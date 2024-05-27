/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum.MALE
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UserBuilder.defaultUser
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

  @MockK private lateinit var userQueryService: UserQueryService

  @InjectMockKs private lateinit var cut: UserDetailsServiceImpl

  /** Verifies loading user by username. */
  @Test
  fun verifyLoadUserByUsername() {
    every { userQueryService.findOneWithPictureByUserId(USERID) } returns
        defaultUser().apply {
          this.externalUserId = USERID
          this.gender = MALE
          this.firstName = FIRST_NAME
          this.lastName = LAST_NAME
          this.email = EMAIL
        }

    val userDetails = cut.loadUserByUsername(USERID)
    assertThat(userDetails).isNotNull
    assertThat(userDetails.username).isEqualTo(USERID)
    assertThat(userDetails).isInstanceOf(User::class.java)

    val user = userDetails as User
    assertThat(user.firstName).isEqualTo(FIRST_NAME)
    assertThat(user.lastName).isEqualTo(LAST_NAME)
    assertThat(user.gender).isEqualTo(MALE)
    assertThat(user.authorities).containsExactly(SimpleGrantedAuthority(USER.roleName()))

    verify { userQueryService.findOneWithPictureByUserId(any()) }
  }

  /** Verifies loading user by username with user not found error. */
  @Test
  fun verifyLoadUserByUsernameNotFound() {
    every { userQueryService.findOneWithPictureByUserId(INVALID_USERID) } returns null
    assertThatThrownBy { cut.loadUserByUsername(INVALID_USERID) }
        .isInstanceOf(UsernameNotFoundException::class.java)
  }

  /** Verifies loading user by username with username being empty. */
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
