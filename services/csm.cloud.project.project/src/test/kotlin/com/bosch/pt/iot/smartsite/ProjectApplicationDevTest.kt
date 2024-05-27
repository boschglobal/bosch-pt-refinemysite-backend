/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2016
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite

import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import java.util.TimeZone
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.web.WebAppConfiguration

/** Template for spring integration tests. */
@SpringBootTest(classes = [ProjectApplication::class])
@WebAppConfiguration
@ActiveProfiles("test")
internal class ProjectApplicationDevTest {

  @Autowired private lateinit var resource: CustomTrustedJwtIssuersProperties

  /** Verifies that spring context can be started successfully. */
  @Test
  fun contextLoads() {
    // Verifies successful loading of spring context with current config
    Assertions.assertThat(resource.issuerUris).isNotEmpty
  }

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
  }
}
