/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver.PatResolver
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.web.MockHttpServletRequest

class PatResolverTest {

  private val cut = PatResolver()

  private val token = "RMSPAT1.7281b943e25b4822bd92c330ebcc9a59.xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd"

  @Test
  fun `no authorization header`() {
    assertThat(cut.resolve(MockHttpServletRequest())).isNull()
  }

  @Test
  fun `invalid scheme`() {
    val request = MockHttpServletRequest().apply { addHeader(AUTHORIZATION, "basic xxx") }
    assertThat(cut.resolve(request)).isNull()
  }

  @Test
  fun `invalid characters`() {
    val request = MockHttpServletRequest().apply { addHeader(AUTHORIZATION, "pat \$*$token") }
    assertThatExceptionOfType(PatAuthenticationException::class.java).isThrownBy {
      cut.resolve(request)
    }
  }

  @Test
  fun `valid token scheme lowercase`() {
    val request = MockHttpServletRequest().apply { addHeader(AUTHORIZATION, "pat $token") }
    assertThat(cut.resolve(request)).isEqualTo(token)
  }

  @Test
  fun `valid token scheme uppercase`() {
    val request = MockHttpServletRequest().apply { addHeader(AUTHORIZATION, "PAT $token") }
    assertThat(cut.resolve(request)).isEqualTo(token)
  }
}
