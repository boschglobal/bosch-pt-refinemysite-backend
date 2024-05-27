/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/** Verifies that spring context can be started successfully. */
@SpringBootTest(classes = [ApiApplication::class])
@ActiveProfiles("local", "test")
class ApiApplicationIntegrationTest {

  /** Verifies that spring context can be started successfully. */
  @Test
  fun startup() {
    // Verifies successful loading of spring context with current config
  }
}
