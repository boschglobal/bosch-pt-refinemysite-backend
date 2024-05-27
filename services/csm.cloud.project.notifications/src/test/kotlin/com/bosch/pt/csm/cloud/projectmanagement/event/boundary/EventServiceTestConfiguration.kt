/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.boundary

import com.bosch.pt.csm.cloud.projectmanagement.event.integration.EventIntegrationService
import com.ninjasquad.springmockk.MockkBean
import org.springframework.boot.test.context.TestConfiguration

@TestConfiguration
class EventServiceTestConfiguration {

  @MockkBean(relaxed = true) lateinit var eventIntegrationService: EventIntegrationService
}
