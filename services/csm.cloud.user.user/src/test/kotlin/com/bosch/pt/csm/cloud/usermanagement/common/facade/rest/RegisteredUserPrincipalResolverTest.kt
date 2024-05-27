/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.UserNotRegisteredException
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiIntegrationTest
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.ModelAndViewContainer

class RegisteredUserPrincipalResolverTest : AbstractApiIntegrationTest() {

  private val cut = RegisteredUserPrincipalResolver()

  @MockK(relaxed = true) private lateinit var methodParameter: MethodParameter

  @MockK(relaxed = true) private lateinit var modelAndViewContainer: ModelAndViewContainer

  @MockK(relaxed = true) private lateinit var webRequest: NativeWebRequest

  @MockK(relaxed = true) private lateinit var webDataBinderFactory: WebDataBinderFactory

  @Test
  fun verifyResolveArgumentForEmptyAuthenticationReturnsNull() {
    SecurityContextHolder.getContext().authentication = null
    assertThat(
            cut.resolveArgument(
                methodParameter, modelAndViewContainer, webRequest, webDataBinderFactory))
        .isNull()
  }

  @Test
  fun verifyResolveArgumentForUnregisteredUserFails() {
    setSecurityContextAsUnregisteredUser()
    assertThatExceptionOfType(UserNotRegisteredException::class.java).isThrownBy {
      cut.resolveArgument(methodParameter, modelAndViewContainer, webRequest, webDataBinderFactory)
    }
  }
}
