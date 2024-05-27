/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.projectmanagement.application.common.ActivityServiceEventStreamContext
import com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener.CompanyEventListenerImpl
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener.ProjectEventListenerImpl
import com.bosch.pt.csm.cloud.projectmanagement.test.TimeLineGeneratorImpl
import com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.UserEventListenerImpl
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class KafkaListenerConfiguration {

  @Bean
  fun eventStreamContext(
      userEventListenerImpl: UserEventListenerImpl,
      companyEventListenerImpl: CompanyEventListenerImpl,
      projectEventListenerImpl: ProjectEventListenerImpl
  ): ActivityServiceEventStreamContext =
      ActivityServiceEventStreamContext(
          HashMap(),
          HashMap(),
          TimeLineGeneratorImpl(),
          mutableMapOf(
              "company" to listOf(companyEventListenerImpl::listenToCompanyEvents),
              "project" to listOf(projectEventListenerImpl::listenToProjectEvents),
              "user" to listOf(userEventListenerImpl::listenToUserEvents)))

  @Bean
  fun eventStreamGenerator(eventStreamContext: ActivityServiceEventStreamContext) =
      EventStreamGenerator(eventStreamContext)
}
