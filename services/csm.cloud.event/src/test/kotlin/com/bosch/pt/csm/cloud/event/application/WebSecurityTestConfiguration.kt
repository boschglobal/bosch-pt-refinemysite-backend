/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application

import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder

@Configuration
class WebSecurityTestConfiguration {

  @Bean fun jwtDecoder(): ReactiveJwtDecoder = mockk(relaxed = true)
}
