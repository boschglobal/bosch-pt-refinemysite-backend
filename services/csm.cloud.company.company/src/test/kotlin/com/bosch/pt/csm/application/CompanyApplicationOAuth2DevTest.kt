/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.application

import com.bosch.pt.csm.CompanyApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.web.WebAppConfiguration

/** Template for spring integration tests. */
@SpringBootTest(classes = [CompanyApplication::class])
@WebAppConfiguration
@ActiveProfiles("test", "idp-bosch-dev", "event-listener-disabled")
@MySqlTest
class CompanyApplicationOAuth2DevTest {

  @Autowired private lateinit var resource: OAuth2ResourceServerProperties

  /** Verifies that spring context can be started successfully. */
  @Test
  fun contextLoads() {
    // Verifies successful loading of spring context with current config
    assertThat(resource.jwt.issuerUri).isNotBlank
  }
}
