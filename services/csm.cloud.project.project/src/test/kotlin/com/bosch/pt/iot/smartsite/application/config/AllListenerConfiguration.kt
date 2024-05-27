/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.event.HibernateListenerAspect
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategyDispatcher
import com.bosch.pt.csm.cloud.common.test.KafkaListenerFunction
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.listener.FeatureEventListener
import com.bosch.pt.csm.cloud.imagemanagement.image.listener.ImageEventListener
import com.bosch.pt.iot.smartsite.attachment.facade.listener.ImageDeletingProcessor
import com.bosch.pt.iot.smartsite.attachment.facade.listener.ImageScalingListener
import com.bosch.pt.iot.smartsite.attachment.facade.listener.ImageScalingProcessor
import com.bosch.pt.iot.smartsite.common.event.ProjectServiceEventStreamContext
import com.bosch.pt.iot.smartsite.common.facade.listener.NoOpOffsetSynchronizationManager
import com.bosch.pt.iot.smartsite.company.facade.listener.online.CompanyEventListenerImpl
import com.bosch.pt.iot.smartsite.company.facade.listener.restore.RestoreDbCompanyEventListenerImpl
import com.bosch.pt.iot.smartsite.company.facade.listener.restore.strategy.CompanyContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.craft.facade.listener.online.CraftEventListenerImpl
import com.bosch.pt.iot.smartsite.craft.facade.listener.restore.RestoreDbCraftEventListenerImpl
import com.bosch.pt.iot.smartsite.craft.facade.listener.restore.strategy.CraftContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.job.facade.listener.JobEventListenerImpl
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreSnapshotsEventListener
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextRestoreSnapshotsEventListener
import com.bosch.pt.iot.smartsite.project.relation.facade.listener.online.CalculateRelationCriticalityListener
import com.bosch.pt.iot.smartsite.test.TimeLineGeneratorImpl
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.facade.listener.online.UserEventListenerImpl
import com.bosch.pt.iot.smartsite.user.facade.listener.restore.RestoreDbUserEventListenerImpl
import com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy.UserContextRestoreDbStrategy
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.support.Acknowledgment
import org.springframework.transaction.support.TransactionTemplate

@TestConfiguration
open class AllListenerConfiguration(
    private val transactionTemplate: TransactionTemplate,
    @param:Value("\${system.user.identifier}") private val systemUserIdentifier: UUID,
) {

  private val offsetSynchronizationManager = NoOpOffsetSynchronizationManager()

  @Bean
  open fun imageScalingListener(
      imageDeletingProcessor: ImageDeletingProcessor,
      imageScalingProcessor: ImageScalingProcessor,
      userQueryService: UserService,
  ) =
      ImageScalingListener(
          imageDeletingProcessor,
          imageScalingProcessor,
          transactionTemplate,
          userQueryService,
          systemUserIdentifier,
      )

  @Bean
  open fun restoreDbCompanyEventListenerImpl(strategies: List<CompanyContextRestoreDbStrategy>) =
      RestoreDbCompanyEventListenerImpl(
          RestoreDbStrategyDispatcher(transactionTemplate, strategies),
          offsetSynchronizationManager)

  @Bean
  open fun restoreDbCraftEventListenerImpl(strategies: List<CraftContextRestoreDbStrategy>) =
      RestoreDbCraftEventListenerImpl(
          RestoreDbStrategyDispatcher(transactionTemplate, strategies),
          offsetSynchronizationManager)

  @Bean
  open fun projectInvitationContextRestoreSnapshotsEventListener(
      transactionTemplate: TransactionTemplate,
      eventBus: ProjectInvitationContextLocalEventBus,
      logger: Logger
  ) = ProjectInvitationContextRestoreSnapshotsEventListener(transactionTemplate, eventBus, logger)

  @Bean
  open fun projectContextRestoreSnapshotEventListener(
      transactionTemplate: TransactionTemplate,
      eventBus: ProjectContextLocalEventBus,
      strategies: List<ProjectContextRestoreDbStrategy>,
      logger: Logger
  ) =
      ProjectContextRestoreSnapshotsEventListener(
          transactionTemplate,
          eventBus,
          RestoreDbStrategyDispatcher(transactionTemplate, strategies),
          logger)

  @Bean
  open fun restoreDbUserEventListenerImpl(strategies: List<UserContextRestoreDbStrategy>) =
      RestoreDbUserEventListenerImpl(
          RestoreDbStrategyDispatcher(transactionTemplate, strategies),
          offsetSynchronizationManager)

  @Bean
  open fun eventStreamContext(
      companyEventListener: CompanyEventListenerImpl,
      craftEventListener: CraftEventListenerImpl,
      userEventListener: UserEventListenerImpl,
      calculateRelationCriticalityListener: CalculateRelationCriticalityListener,
      jobEventLister: JobEventListenerImpl,
      hibernateListenerAspect: HibernateListenerAspect,
      companyRestoreEventListener: RestoreDbCompanyEventListenerImpl,
      craftRestoreEventListener: RestoreDbCraftEventListenerImpl,
      featureEventListener: FeatureEventListener,
      projectInvitationContextRestoreSnapshotsEventListener:
          ProjectInvitationContextRestoreSnapshotsEventListener,
      projectRestoreEventListener: ProjectContextRestoreSnapshotsEventListener,
      userRestoreEventListener: RestoreDbUserEventListenerImpl,
      imageScalingListener: ImageEventListener,
  ) =
      ProjectServiceEventStreamContext(
              events = HashMap(),
              lastIdentifierPerType = HashMap(),
              timeLineGenerator = TimeLineGeneratorImpl(),
              onlineListener =
                  mutableMapOf(
                      "company" to listOf(companyEventListener::listenToCompanyEvents),
                      "craft" to listOf(craftEventListener::listenToCraftEvents),
                      "feature" to listOf(featureEventListener::listenToFeatureEvents),
                      "user" to listOf(userEventListener::listenToUserEvents),
                      "project" to
                          listOf(
                              DisableHibernateListener(
                                  projectRestoreEventListener::listenToProjectEvents,
                                  hibernateListenerAspect)::listen,
                              calculateRelationCriticalityListener::listenToProjectEvents),
                      "job" to listOf(jobEventLister::listenToJobEvents),
                      "image" to listOf(imageScalingListener::listenToImageEvents)),
              restoreListener =
                  mutableMapOf(
                      "company" to
                          listOf(
                              DisableHibernateListener(
                                  companyRestoreEventListener::listenToCompanyEvents,
                                  hibernateListenerAspect)::listen),
                      "craft" to
                          listOf(
                              DisableHibernateListener(
                                  craftRestoreEventListener::listenToCraftEvents,
                                  hibernateListenerAspect)::listen),
                      "feature" to listOf(featureEventListener::listenToFeatureEvents),
                      "invitation" to
                          listOf(
                              DisableHibernateListener(
                                  projectInvitationContextRestoreSnapshotsEventListener::
                                      listenToInvitationEvents,
                                  hibernateListenerAspect)::listen),
                      "project" to
                          listOf(
                              DisableHibernateListener(
                                  projectRestoreEventListener::listenToProjectEvents,
                                  hibernateListenerAspect)::listen),
                      "user" to
                          listOf(
                              DisableHibernateListener(
                                  userRestoreEventListener::listenToUserEvents,
                                  hibernateListenerAspect)::listen),
                      "image" to listOf(imageScalingListener::listenToImageEvents)),
              hibernateListenerAspect = hibernateListenerAspect)
          .useRestoreListener()

  class DisableHibernateListener(
      private val listener: KafkaListenerFunction,
      private val hibernateListenerAspect: HibernateListenerAspect
  ) {

    fun listen(record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>, ack: Acknowledgment) {
      hibernateListenerAspect.enableListeners(false)
      listener(record, ack)
      hibernateListenerAspect.enableListeners(true)
    }
  }

  @Bean
  open fun eventStreamGenerator(eventStreamContext: ProjectServiceEventStreamContext) =
      EventStreamGenerator(eventStreamContext)
}
