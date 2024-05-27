/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.constants.RoleConstants
import com.bosch.pt.iot.smartsite.user.model.GenderEnum
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
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

  @MockK(relaxed = true) private lateinit var userService: UserService

  @InjectMockKs private lateinit var cut: UserDetailsServiceImpl

  @Test
  fun verifyLoadUserByUsername() {
    every { userService.findOneWithPictureByUserId(USERID) } returns
        user()
            .withUserId(USERID)
            .withGender(GenderEnum.MALE)
            .withFirstName(FIRST_NAME)
            .withLastName(LAST_NAME)
            .withEmail(EMAIL)
            .build()

    val userDetails = cut.loadUserByUsername(USERID)

    assertThat(userDetails).isNotNull
    assertThat(userDetails.username).isEqualTo(USERID)
    assertThat(userDetails).isInstanceOf(User::class.java)

    val user = userDetails as User
    assertThat(user.firstName).isEqualTo(FIRST_NAME)
    assertThat(user.lastName).isEqualTo(LAST_NAME)
    assertThat(user.gender).isEqualTo(GenderEnum.MALE)
    assertThat(user.authorities).extracting("authority").contains(RoleConstants.USER.roleName())

    verify { userService.findOneWithPictureByUserId(any()) }
  }

  @Test
  fun verifyLoadUserByUsernameNotFound() {
    every { userService.findOneWithPictureByUserId(INVALID_USERID) } returns null

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
