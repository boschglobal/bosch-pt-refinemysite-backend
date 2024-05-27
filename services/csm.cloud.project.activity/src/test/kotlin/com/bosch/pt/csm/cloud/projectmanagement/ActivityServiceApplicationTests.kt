/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SmartSiteSpringBootTest
class ActivityServiceApplicationTests {

  @Test
  fun `context load succeeds`() {
    // do nothing - just load context
  }
}
