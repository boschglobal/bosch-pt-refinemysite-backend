/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.FeatureIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.WhitelistedSubject
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureCreatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDeletedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDisabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureEnabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureWhitelistActivatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectAddedToWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectDeletedFromWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.UpstreamFeatureEvent
import com.bosch.pt.csm.cloud.testapp.TestApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@DataMongoTest
@ContextConfiguration(classes = [TestApplication::class])
@ActiveProfiles("test", "local")
class FeatureQueryServiceTest {

  private val featureName = "FEATURE_XY"

  @Autowired private lateinit var featureProjector: FeatureProjector

  @Autowired private lateinit var featureProjectionRepository: FeatureProjectionRepository

  @Autowired private lateinit var featureQueryService: FeatureQueryService

  private val projectIdentifier = "37b62054-321d-4af7-aa15-f1df9512a7aa"

  @BeforeEach
  fun init() {
    featureProjectionRepository.deleteAll()
  }

  @Nested
  inner class IsBimFeatureActive {
    @Test
    fun `returns false if not whitelisted`() {
      given(createdEvent, projectWhitelistedEvent)

      val notWhitelistedProjectId = "847894a8-e995-4a5a-83fa-e5ad992e479b"
      val isFeatureEnabled =
          featureQueryService.isFeatureEnabled(
              featureName, WhitelistedSubject(notWhitelistedProjectId, PROJECT))

      assertThat(isFeatureEnabled).isFalse
    }

    @Test
    fun `returns true if whitelisted`() {
      given(createdEvent, projectWhitelistedEvent)

      val isFeatureEnabled =
          featureQueryService.isFeatureEnabled(
              featureName, WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(isFeatureEnabled).isTrue
    }

    @Test
    fun `returns true if feature is enabled even if not whitelisted`() {
      given(createdEvent, featureEnabledEvent)

      val isFeatureEnabled =
          featureQueryService.isFeatureEnabled(
              featureName, WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(isFeatureEnabled).isTrue
    }

    @Test
    fun `returns false if feature is disabled even if whitelisted`() {
      given(createdEvent, projectWhitelistedEvent, featureDisabledEvent)

      val isFeatureEnabled =
          featureQueryService.isFeatureEnabled(
              featureName, WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(isFeatureEnabled).isFalse
    }

    @Test
    fun `returns true after whitelist activated again`() {
      given(
          createdEvent,
          projectWhitelistedEvent,
          featureDisabledEvent,
          featureWhitelistActivatedEvent)

      val isFeatureEnabled =
          featureQueryService.isFeatureEnabled(
              featureName, WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(isFeatureEnabled).isTrue
    }

    @Test
    fun `returns false after removed from whitelist`() {
      given(createdEvent, projectWhitelistedEvent, projectRemovedFromWhitelistEvent)

      val isFeatureEnabled =
          featureQueryService.isFeatureEnabled(
              featureName, WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(isFeatureEnabled).isFalse
    }

    @Test
    fun `returns false if feature is deleted`() {
      given(createdEvent, projectWhitelistedEvent, deletedEvent)

      val isFeatureEnabled =
          featureQueryService.isFeatureEnabled(
              featureName, WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(isFeatureEnabled).isFalse
    }
  }

  @Nested
  inner class GetAllEnabledFeatures {

    @Test
    fun `returns empty list for unknown project`() {
      val unknownProject = "9430eedb-2d7f-4a13-a4a1-3015465946eb"

      val enabledFeatures =
          featureQueryService.getEnabledFeatures(WhitelistedSubject(unknownProject, PROJECT))

      assertThat(enabledFeatures).isEmpty()
    }

    @Test
    fun `returns empty list if not whitelisted`() {
      given(createdEvent, projectWhitelistedEvent)

      val notWhitelistedProjectId = "847894a8-e995-4a5a-83fa-e5ad992e479b"
      val enabledFeatures =
          featureQueryService.getEnabledFeatures(
              WhitelistedSubject(notWhitelistedProjectId, PROJECT))

      assertThat(enabledFeatures).isEmpty()
    }

    @Test
    fun `returns list with feature if whitelisted`() {
      given(createdEvent, projectWhitelistedEvent)

      val enabledFeatures =
          featureQueryService.getEnabledFeatures(WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(enabledFeatures).containsOnly(featureName)
    }

    @Test
    fun `returns list with feature if feature is enabled even if not whitelisted`() {
      given(createdEvent, featureEnabledEvent)

      val enabledFeatures =
          featureQueryService.getEnabledFeatures(WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(enabledFeatures).containsOnly(featureName)
    }

    @Test
    fun `returns empty list if feature is disabled even if whitelisted`() {
      given(createdEvent, projectWhitelistedEvent, featureDisabledEvent)

      val enabledFeatures =
          featureQueryService.getEnabledFeatures(WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(enabledFeatures).isEmpty()
    }

    @Test
    fun `returns list with feature after whitelist activated again`() {
      given(
          createdEvent,
          projectWhitelistedEvent,
          featureDisabledEvent,
          featureWhitelistActivatedEvent)

      val enabledFeatures =
          featureQueryService.getEnabledFeatures(WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(enabledFeatures).containsOnly(featureName)
    }

    @Test
    fun `returns empty list after removed from whitelist`() {
      given(createdEvent, projectWhitelistedEvent, projectRemovedFromWhitelistEvent)

      val enabledFeatures =
          featureQueryService.getEnabledFeatures(WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(enabledFeatures).isEmpty()
    }

    @Test
    fun `returns empty list if feature is deleted`() {
      given(createdEvent, projectWhitelistedEvent, deletedEvent)

      val enabledFeatures =
          featureQueryService.getEnabledFeatures(WhitelistedSubject(projectIdentifier, PROJECT))

      assertThat(enabledFeatures).isEmpty()
    }
  }

  private val featureId = FeatureIdentifier("64544a2a-aa9d-488e-b841-225c7407820f")
  private val createdEvent = FeatureCreatedEvent(featureId, featureName)
  private val projectWhitelistedEvent =
      SubjectAddedToWhitelistEvent(featureId, WhitelistedSubject(projectIdentifier, PROJECT))
  private val projectRemovedFromWhitelistEvent =
      SubjectDeletedFromWhitelistEvent(featureId, WhitelistedSubject(projectIdentifier, PROJECT))
  private val featureEnabledEvent = FeatureEnabledEvent(featureId)
  private val featureDisabledEvent = FeatureDisabledEvent(featureId)
  private val featureWhitelistActivatedEvent = FeatureWhitelistActivatedEvent(featureId)
  private val deletedEvent = FeatureDeletedEvent(featureId)

  private fun given(vararg events: UpstreamFeatureEvent) {
    events.forEach { featureProjector.handle(it) }
  }
}
