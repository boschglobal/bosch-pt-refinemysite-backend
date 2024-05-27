/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2018
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ActiveProfiles("local")
@ExtendWith(SpringExtension::class)
@SpringBootTest
@TestPropertySource(properties = ["stage=unit-test"])
class SmartsiteResetApplicationTests {

  @Test
  fun contextLoads() {
    // do nothing
  }
}
