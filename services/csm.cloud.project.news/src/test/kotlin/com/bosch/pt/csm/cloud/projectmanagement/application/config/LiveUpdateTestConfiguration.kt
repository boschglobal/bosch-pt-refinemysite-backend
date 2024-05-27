/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.projectmanagement.event.facade.listener.LiveUpdateEventProcessor
import com.ninjasquad.springmockk.MockkBean
import org.springframework.context.annotation.Configuration

@Configuration
class LiveUpdateTestConfiguration {

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var liveUpdateEventProcessor: LiveUpdateEventProcessor
}
