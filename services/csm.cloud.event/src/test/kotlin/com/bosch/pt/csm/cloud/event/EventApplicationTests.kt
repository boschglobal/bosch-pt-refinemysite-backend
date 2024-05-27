/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.event

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration

@SpringBootTest(classes = [TestConfiguration::class])
class EventApplicationTests {

  /** Verifies that spring context can be started successfully. */
  @Test
  fun contextLoads() {
    // Verifies successful loading of spring context with current config
  }
}
