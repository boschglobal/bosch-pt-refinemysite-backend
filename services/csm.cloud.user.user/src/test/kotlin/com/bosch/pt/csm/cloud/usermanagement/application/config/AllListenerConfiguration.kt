/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.application.config

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.imagemanagement.image.listener.ImageEventListener
import com.bosch.pt.csm.cloud.usermanagement.attachment.facade.listener.ImageScalingListener
import com.bosch.pt.csm.cloud.usermanagement.attachment.facade.listener.ImageScalingProcessor
import com.bosch.pt.csm.cloud.usermanagement.common.event.UserServiceEventStreamContext
import com.bosch.pt.csm.cloud.usermanagement.craft.eventstore.CraftContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.craft.eventstore.CraftContextRestoreEventListener
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatRestoreEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextRestoreEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserQueryService
import java.util.UUID
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.support.TransactionTemplate

@TestConfiguration
class AllListenerConfiguration(
    private val transactionTemplate: TransactionTemplate,
    private val userEventBus: UserContextLocalEventBus,
    private val patEventBus: PatLocalEventBus,
    private val craftEventBus: CraftContextLocalEventBus,
    private val logger: Logger,
    @param:Value("\${system.user.identifier}") private val systemUserIdentifier: UUID
) {

  @Bean
  fun restoreDbUserEventListenerImpl() =
      UserContextRestoreEventListener(transactionTemplate, userEventBus, logger)

  @Bean
  fun restoreDbPatEventListenerImpl() =
      PatRestoreEventListener(transactionTemplate, patEventBus, logger)

  @Bean
  fun restoreDbCraftEventListenerImpl() =
      CraftContextRestoreEventListener(transactionTemplate, craftEventBus, logger)

  @Bean
  fun imageScalingListener(
      userQueryService: UserQueryService,
      imageScalingProcessor: ImageScalingProcessor
  ) =
      ImageScalingListener(
          userQueryService, imageScalingProcessor, transactionTemplate, systemUserIdentifier)

  @Bean
  fun eventStreamContext(
      userRestoreEventListener: UserContextRestoreEventListener,
      patRestoreEventListener: PatRestoreEventListener,
      craftRestoreEventListener: CraftContextRestoreEventListener,
      imageScalingListener: ImageEventListener,
  ) =
      UserServiceEventStreamContext(
              HashMap(),
              HashMap(),
              TimeLineGeneratorImpl(),
              mutableMapOf(
                  "user" to listOf(userRestoreEventListener::listenToUserEvents),
                  "pat" to listOf(patRestoreEventListener::listenToPatEvents),
                  "craft" to listOf(craftRestoreEventListener::listenToCraftEvents),
                  "image" to listOf(imageScalingListener::listenToImageEvents)),
              mutableMapOf(
                  "user" to listOf(userRestoreEventListener::listenToUserEvents),
                  "pat" to listOf(patRestoreEventListener::listenToPatEvents),
                  "craft" to listOf(craftRestoreEventListener::listenToCraftEvents),
                  "image" to listOf(imageScalingListener::listenToImageEvents)))
          .useRestoreListener()

  @Bean
  fun eventStreamGenerator(eventStreamContext: UserServiceEventStreamContext) =
      EventStreamGenerator(eventStreamContext)
}
