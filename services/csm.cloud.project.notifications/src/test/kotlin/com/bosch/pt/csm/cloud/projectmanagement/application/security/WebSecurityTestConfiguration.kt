/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.JwtDecoder

@Profile("test")
@Configuration
open class WebSecurityTestConfiguration {

    @Bean
    open fun jwtDecoder(): JwtDecoder {
        return Mockito.mock(JwtDecoder::class.java)
    }
}
