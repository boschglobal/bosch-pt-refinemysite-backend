/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.application

import com.bosch.pt.csm.cloud.FeatureToggleApplication
import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.web.WebAppConfiguration

@SpringBootTest(classes = [FeatureToggleApplication::class])
@WebAppConfiguration
@ActiveProfiles("test", "idp-bosch-dev", "event-listener-disabled")
@MySqlTest
class FeatureApplicationOAuth2DevTest {

  @Autowired private lateinit var resource: CustomTrustedJwtIssuersProperties

  @Test
  fun contextLoads() {
    // Verifies successful loading of spring context with current config
    assertThat(resource.issuerUris)
        .isEqualTo(listOf("https://p32.authz.bosch.com/auth/realms/central_profile"))
  }
}
