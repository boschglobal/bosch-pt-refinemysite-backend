/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2018
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite

import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.web.WebAppConfiguration

/** Template for spring integration tests. */
@ActiveProfiles("idp-bosch-prod", "test")
@SpringBootTest(classes = [ProjectApplication::class])
@WebAppConfiguration
internal class ProjectApplicationOAuth2ProdTest {

  @Autowired private lateinit var resource: CustomTrustedJwtIssuersProperties

  /** Verifies that spring context can be started successfully. */
  @Test
  fun contextLoads() {
    // Verifies successful loading of spring context with current config
    assertThat(resource.issuerUris).isNotEmpty
  }
}
