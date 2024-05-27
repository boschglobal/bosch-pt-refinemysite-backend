/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import com.bosch.pt.csm.cloud.projectmanagement.application.common.ProjectTimeSeriesApiServiceEventStreamContext
import com.bosch.pt.csm.cloud.projectmanagement.application.common.TimeLineGeneratorImpl
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.usermanagement.pat.event.listener.PatEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class KafkaListenerConfiguration {

  @Bean
  fun eventStreamContext(
      companyEventListeners: List<CompanyEventListener>,
      projectEventListeners: List<ProjectEventListener>,
      userEventListeners: List<UserEventListener>,
      patEventListeners: List<PatEventListener>
  ): ProjectTimeSeriesApiServiceEventStreamContext =
      ProjectTimeSeriesApiServiceEventStreamContext(
          HashMap(),
          HashMap(),
          TimeLineGeneratorImpl(),
          mutableMapOf(
              "company" to companyEventListeners.map { it::listenToCompanyEvents },
              "project" to projectEventListeners.map { it::listenToProjectEvents },
              "user" to userEventListeners.map { it::listenToUserEvents },
              "pat" to patEventListeners.map { it::listenToPatEvents }))

  @Bean
  fun eventStreamGenerator(eventStreamContext: ProjectTimeSeriesApiServiceEventStreamContext) =
      EventStreamGenerator(eventStreamContext)
}
