/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationManager
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.PatAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.InvalidPatException
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum.RMSPAT1
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication

@SmartSiteMockKTest
class PatAuthenticationManagerTest {

  @RelaxedMockK private lateinit var resolver: AuthenticationManagerResolver<PatTypeEnum>

  private val cut by lazy { PatAuthenticationManager(resolver) }

  @Test
  fun `authentication is null`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { cut.authenticate(null) }
        .withMessage("Authentication must be of type PatAuthenticationToken")
  }

  @Test
  fun `authentication is no pat`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { cut.authenticate(UsernamePasswordAuthenticationToken("user", "password")) }
        .withMessage("Authentication must be of type PatAuthenticationToken")
  }

  @Test
  fun `pat type not supported`() {
    val pat = PatAuthenticationToken("$RMSPAT1.${randomUUID().toString().replace("-","")}.XXX")
    every { resolver.resolve(any()) } returns null

    assertThatExceptionOfType(InvalidPatException::class.java)
        .isThrownBy { cut.authenticate(pat) }
        .withMessage("Invalid pat type")
  }

  @Test
  fun `authentication is pat`() {
    val authenticationManager = mockk<AuthenticationManager>()
    val authentication = mockk<Authentication>()

    every { authenticationManager.authenticate(any()) } returns authentication
    // List every pat type explicitly to ensure that we really support it
    every { resolver.resolve(RMSPAT1) } returns authenticationManager

    for (patType in PatTypeEnum.values()) {
      val pat = PatAuthenticationToken("$patType.${randomUUID().toString().replace("-","")}.XXX")
      assertThat(cut.authenticate(pat)).isEqualTo(authentication)
    }
  }
}
