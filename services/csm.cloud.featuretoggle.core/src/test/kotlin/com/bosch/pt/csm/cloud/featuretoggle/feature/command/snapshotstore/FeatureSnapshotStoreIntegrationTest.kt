/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore

import com.bosch.pt.csm.cloud.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextKafkaEvent
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextLocalEventBus
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureCreatedEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureDeletedEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureDisabledEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureEnabledEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureWhitelistActivatedEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.asFeatureId
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api.SubjectAddedToWhitelistEvent
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api.SubjectDeletedFromWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.createFeature
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

class FeatureSnapshotStoreIntegrationTest : AbstractApiIntegrationTest() {

  @Autowired
  private lateinit var featureToggleEventStoreUtils: EventStoreUtils<FeaturetoggleContextKafkaEvent>

  @Autowired private lateinit var eventBus: FeaturetoggleContextLocalEventBus

  @Test
  fun `fails upon FeatureCreatedEvent for existing feature`() {
    eventStreamGenerator.createFeature("testFeature") { it.featureName = "testFeature" }

    assertThatExceptionOfType(DataIntegrityViolationException::class.java)
        .isThrownBy {
          transactionTemplate.executeWithoutResult {
            eventBus.emit(
                FeatureCreatedEvent(
                    getIdentifier("testFeature").asFeatureId(),
                    "testEvent",
                    randomUUID().asUserId(),
                    now()),
                1)
          }
        }
        .withMessage("currentSnapshot cannot exist already on create")
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `fails upon FeatureEnabledEvent for non-existing feature`() {

    assertThatExceptionOfType(DataIntegrityViolationException::class.java)
        .isThrownBy {
          transactionTemplate.executeWithoutResult {
            eventBus.emit(
                FeatureEnabledEvent(
                    "90491898-09c1-45c8-9196-714a19de8ae1".toUUID().asFeatureId(),
                    "testEvent",
                    randomUUID().asUserId(),
                    now()),
                0)
          }
        }
        .withMessage("currentSnapshot cannot be null on update / delete")

    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `fails upon FeatureDisabledEvent for non-existing feature`() {

    assertThatExceptionOfType(DataIntegrityViolationException::class.java)
        .isThrownBy {
          transactionTemplate.executeWithoutResult {
            eventBus.emit(
                FeatureDisabledEvent(
                    "90491898-09c1-45c8-9196-714a19de8ae1".toUUID().asFeatureId(),
                    "testEvent",
                    randomUUID().asUserId(),
                    now()),
                0)
          }
        }
        .withMessage("currentSnapshot cannot be null on update / delete")
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `fails upon FeatureWhitelistActivatedEvent for non-existing feature`() {

    assertThatExceptionOfType(DataIntegrityViolationException::class.java)
        .isThrownBy {
          transactionTemplate.executeWithoutResult {
            eventBus.emit(
                FeatureWhitelistActivatedEvent(
                    "90491898-09c1-45c8-9196-714a19de8ae1".toUUID().asFeatureId(),
                    "testEvent",
                    randomUUID().asUserId(),
                    now()),
                0)
          }
        }
        .withMessage("currentSnapshot cannot be null on update / delete")
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `fails upon FeatureDeleted for non-existing feature`() {

    assertThatExceptionOfType(DataIntegrityViolationException::class.java)
        .isThrownBy {
          transactionTemplate.executeWithoutResult {
            eventBus.emit(
                FeatureDeletedEvent(
                    "90491898-09c1-45c8-9196-714a19de8ae1".toUUID().asFeatureId(),
                    "testEvent",
                    randomUUID().asUserId(),
                    now()),
                0)
          }
        }
        .withMessage("currentSnapshot cannot be null on update / delete")
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `fails upon SubjectAddedToWhitelistEvent for non-existing feature`() {

    assertThatExceptionOfType(DataIntegrityViolationException::class.java)
        .isThrownBy {
          transactionTemplate.executeWithoutResult {
            eventBus.emit(
                SubjectAddedToWhitelistEvent(
                    "90491898-09c1-45c8-9196-714a19de8ae1".toUUID().asFeatureId(),
                    randomUUID(),
                    COMPANY,
                    "testFeature",
                    randomUUID().asUserId(),
                    now()),
                0)
          }
        }
        .withMessage("currentSnapshot cannot be null on update / delete")
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `fails upon SubjectDeletedFromWhitelistEvent for non-existing feature`() {

    assertThatExceptionOfType(DataIntegrityViolationException::class.java)
        .isThrownBy {
          transactionTemplate.executeWithoutResult {
            eventBus.emit(
                SubjectDeletedFromWhitelistEvent(
                    "90491898-09c1-45c8-9196-714a19de8ae1".toUUID().asFeatureId(),
                    randomUUID(),
                    COMPANY,
                    "testFeature",
                    randomUUID().asUserId(),
                    now()),
                0)
          }
        }
        .withMessage("currentSnapshot cannot be null on update / delete")
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `fails upon unsupported event on eventbus`() {
    assertThatExceptionOfType(NoSuchElementException::class.java)
        .isThrownBy { simulateUnsupportedEvent() }
        .withMessage(
            "No avro event mapper found for class " +
                "com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore." +
                "FeatureSnapshotStoreIntegrationTest\$UnsupportedEvent. " +
                "Did you forget to register the mapper on the event bus?")
  }

  private fun simulateUnsupportedEvent(): FeatureId {
    val featureId = "90491898-09c1-45c8-9196-714a19de8ae1".toUUID().asFeatureId()
    transactionTemplate.executeWithoutResult { eventBus.emit(UnsupportedEvent(featureId), 0) }
    featureToggleEventStoreUtils.reset()
    return featureId
  }

  private data class UnsupportedEvent(val featureId: FeatureId)
}
