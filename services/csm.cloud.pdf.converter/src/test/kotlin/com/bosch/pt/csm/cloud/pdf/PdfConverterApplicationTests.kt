/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.pdf

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(profiles = ["local", "test"])
@EnableAutoConfiguration(exclude = [KafkaAutoConfiguration::class])
@SpringBootTest
class PdfConverterApplicationTests {

  @Test
  fun `context load succeeds`() {
    // do nothing - just load context
  }
}
