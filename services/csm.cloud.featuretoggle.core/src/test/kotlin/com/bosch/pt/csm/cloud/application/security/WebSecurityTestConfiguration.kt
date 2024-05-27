/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.security

import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.JwtDecoderFactory

@Configuration
@Profile("test")
class WebSecurityTestConfiguration {

  @Bean fun jwtDecoderFactory(): JwtDecoderFactory<String> = mockk(relaxed = true)
}
