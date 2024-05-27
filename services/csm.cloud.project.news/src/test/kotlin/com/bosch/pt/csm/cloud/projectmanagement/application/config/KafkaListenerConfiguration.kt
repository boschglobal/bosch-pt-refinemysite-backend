/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.projectmanagement.common.TimeLineGeneratorImpl
import com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener.CompanyKafkaEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.facade.listener.ProjectKafkaEventListener
import com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.UserKafkaEventListener
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class KafkaListenerConfiguration {

  @Bean
  fun eventStreamContext(
      companyKafkaEventListener: CompanyKafkaEventListener,
      projectKafkaEventListener: ProjectKafkaEventListener,
      userKafkaEventListener: UserKafkaEventListener
  ): NewsServiceEventStreamContext =
      NewsServiceEventStreamContext(
          HashMap(),
          HashMap(),
          TimeLineGeneratorImpl(),
          mutableMapOf(
              "company" to listOf(companyKafkaEventListener::listenToCompanyEvents),
              "project" to listOf(projectKafkaEventListener::listenToProjectEvents),
              "user" to listOf(userKafkaEventListener::listenToUserEvents)))

  @Bean
  fun eventStreamGenerator(eventStreamContext: NewsServiceEventStreamContext) =
      EventStreamGenerator(eventStreamContext)
}
