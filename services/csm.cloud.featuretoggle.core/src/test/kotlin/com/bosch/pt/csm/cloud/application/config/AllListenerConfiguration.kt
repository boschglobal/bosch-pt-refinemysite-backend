/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.application.config

import com.bosch.pt.csm.cloud.application.TimeLineGeneratorImpl
import com.bosch.pt.csm.cloud.application.event.FeaturetoggleServiceEventStreamContext
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextLocalEventBus
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextRestoreSnapshotsEventListener
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.listener.FeatureEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import org.slf4j.Logger
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.support.TransactionTemplate

@TestConfiguration
class AllListenerConfiguration {

  @Bean
  fun featuretoggleContextRestoreSnapshotEventListener(
      transactionTemplate: TransactionTemplate,
      eventBus: FeaturetoggleContextLocalEventBus,
      logger: Logger
  ) = FeaturetoggleContextRestoreSnapshotsEventListener(transactionTemplate, eventBus, logger)

  @Bean
  fun eventStreamContext(
      userProjectorEventListener: UserEventListener,
      featureEventListener: FeatureEventListener
  ) =
      FeaturetoggleServiceEventStreamContext(
              HashMap(),
              HashMap(),
              TimeLineGeneratorImpl(),
              mutableMapOf("user" to listOf(userProjectorEventListener::listenToUserEvents)),
              mutableMapOf(
                  "user" to listOf(userProjectorEventListener::listenToUserEvents),
                  "feature" to listOf(featureEventListener::listenToFeatureEvents)))
          .useRestoreListener()

  @Bean
  fun eventStreamGenerator(eventStreamContext: FeaturetoggleServiceEventStreamContext) =
      EventStreamGenerator(eventStreamContext)
}
