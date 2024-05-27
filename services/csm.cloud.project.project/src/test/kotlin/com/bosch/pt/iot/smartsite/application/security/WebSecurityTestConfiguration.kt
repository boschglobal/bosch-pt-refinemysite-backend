/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.JwtDecoderFactory

@Configuration
@Profile("test")
open class WebSecurityTestConfiguration {

  @Bean open fun jwtDecoderFactory(): JwtDecoderFactory<String> = mockk(relaxed = true)
}
