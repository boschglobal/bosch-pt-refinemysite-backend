/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationProvider
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.PatAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.details.PatUserDetailsAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.InvalidPatException
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection.Companion.USER_AUTHORITY
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

@SmartSiteMockKTest
class PatAuthenticationProviderTest {

  @RelaxedMockK
  private lateinit var patAuthenticationTokenConverter:
      Converter<PatToken, out AbstractAuthenticationToken>

  private val cut by lazy { PatAuthenticationProvider(patAuthenticationTokenConverter) }

  @Test
  fun `authentication null`() {
    assertThatExceptionOfType(InvalidPatException::class.java)
        .isThrownBy { cut.authenticate(null) }
        .withMessage("Invalid pat")
  }

  @Test
  fun `unsupported authentication token type`() {
    assertThatExceptionOfType(InvalidPatException::class.java)
        .isThrownBy { cut.authenticate(UsernamePasswordAuthenticationToken("user", "password")) }
        .withMessage("Invalid pat type")
  }

  @Test
  fun `empty token`() {
    val authentication = PatAuthenticationToken("")
    assertThatExceptionOfType(InvalidPatException::class.java)
        .isThrownBy { cut.authenticate(authentication) }
        .withMessage("Invalid pat")
  }

  @Test
  fun `parsed token cannot be converted`() {
    every { patAuthenticationTokenConverter.convert(any()) } returns null

    val authentication =
        PatAuthenticationToken(
            "RMSPAT1.7281b943e25b4822bd92c330ebcc9a59.xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd")
    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy { cut.authenticate(authentication) }
        .withMessage("Required value was null.")
  }

  @Test
  fun `authenticated successfully`() {
    val patProjection = mockk<PatProjection>()
    val token = PatUserDetailsAuthenticationToken(patProjection, USER_AUTHORITY)
    every { patAuthenticationTokenConverter.convert(any()) } returns token

    val authentication =
        PatAuthenticationToken(
            "RMSPAT1.7281b943e25b4822bd92c330ebcc9a59.xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd")

    assertThat(cut.authenticate(authentication)).isEqualTo(token)
  }

  @Test
  fun `supports PatUserDetailsAuthenticationToken`() {
    assertThat(cut.supports(PatUserDetailsAuthenticationToken::class.java)).isTrue()
  }

  @Test
  fun `does not support UsernamePasswordAuthenticationToken`() {
    assertThat(cut.supports(UsernamePasswordAuthenticationToken::class.java)).isFalse()
  }
}
