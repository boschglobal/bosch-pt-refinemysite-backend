/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.facade.rest

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.application.security.CustomUserAuthenticationConverter
import com.bosch.pt.csm.cloud.usermanagement.application.security.UserDetailsServiceImpl
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CommonApiDocumentationIntegrationTest : AbstractApiDocumentationTest() {

  @MockkBean private lateinit var userDetailsService: UserDetailsServiceImpl

  @MockkBean
  private lateinit var customUserAuthenticationConverter: CustomUserAuthenticationConverter

  @BeforeEach
  fun setUp() {
    val user =
        repositories.userRepository.findOneByIdentifier(
            UserId(eventStreamGenerator.getIdentifier("admin")))!!
    val token =
        UsernamePasswordAuthenticationToken(
            user, "n/a", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    every { customUserAuthenticationConverter.convert(any()) } returns token
    every { userDetailsService.loadUserByUsername(any()) } returns user
  }
  @Test
  fun documentHeaders() {
    eventStreamGenerator.submitUser("user") { it.email = "User's email" }

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(UserController.USERS_ENDPOINT_PATH))
                    .header(
                        AUTHORIZATION,
                        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFt" +
                            "ZSI6IkpvaG4gRG9lIiwiaXNzIjoiaHR0cHM6Ly9qd3RjcmVhdG9yLmV4YW1wbGUuY29tIiwiaWF0IjoxNT" +
                            "E2MjM5MDIyfQ.prX7A3FUhK_m3tRjCj3fgLkA84_gaSikQ2m7q1XPtx4")))
        .andExpect(status().isOk)
        .andDo(
            document(
                "common/document-headers",
                requestHeaders(
                    headerWithName(ACCEPT)
                        .description(
                            "The MIME Types of the media that the client is willing to " +
                                "process. Supported values are currently " +
                                "'application/hal+json;charset=UTF-8' or " +
                                "'application/json;charset=UTF-8'"),
                    headerWithName(ACCEPT_LANGUAGE)
                        .description(
                            "Indicates the language preference of the user. " +
                                "Supported values are currently 'en' or 'de'"),
                    headerWithName(AUTHORIZATION)
                        .description(
                            "Required for authenticating to backend services. " +
                                "Must contain a bearer token in JWT format")),
                responseHeaders(
                    headerWithName(CONTENT_TYPE)
                        .description(
                            "The Content-Type of the payload, 'application/hal+json;" +
                                "charset=UTF-8' and 'application/json;charset=UTF-8'"))))
  }
}
