/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.projectmanagement.TimeLineGeneratorImpl
import com.bosch.pt.csm.cloud.projectmanagement.common.event.StatisticsServiceEventStreamContext
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.listener.CompanyEventListenerImpl
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.listener.ProjectEventListenerImpl
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.listener.UserEventListenerImpl
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class KafkaListenerConfiguration {

  @Bean
  fun eventStreamContext(
      companyEventListenerImpl: CompanyEventListenerImpl,
      projectEventListenerImpl: ProjectEventListenerImpl,
      userEventListenerImpl: UserEventListenerImpl
  ) =
      StatisticsServiceEventStreamContext(
          HashMap(),
          HashMap(),
          TimeLineGeneratorImpl(),
          mutableMapOf(
              "company" to listOf(companyEventListenerImpl::listenToCompanyEvents),
              "project" to listOf(projectEventListenerImpl::listenToProjectEvents),
              "user" to listOf(userEventListenerImpl::listenToUserEvents)))

  @Bean
  fun eventStreamGenerator(eventStreamContext: StatisticsServiceEventStreamContext) =
      EventStreamGenerator(eventStreamContext)
}
