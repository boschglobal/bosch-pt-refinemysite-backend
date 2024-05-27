/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events

import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.FeatureIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.WhitelistedSubject

internal sealed interface UpstreamFeatureEvent {
  val featureIdentifier: FeatureIdentifier
}

internal data class FeatureCreatedEvent(
    override val featureIdentifier: FeatureIdentifier,
    val name: String
) : UpstreamFeatureEvent

internal data class FeatureDeletedEvent(override val featureIdentifier: FeatureIdentifier) :
    UpstreamFeatureEvent

internal data class FeatureWhitelistActivatedEvent(
    override val featureIdentifier: FeatureIdentifier
) : UpstreamFeatureEvent

internal data class FeatureDisabledEvent(override val featureIdentifier: FeatureIdentifier) :
    UpstreamFeatureEvent

internal data class FeatureEnabledEvent(override val featureIdentifier: FeatureIdentifier) :
    UpstreamFeatureEvent

internal data class SubjectAddedToWhitelistEvent(
    override val featureIdentifier: FeatureIdentifier,
    val subject: WhitelistedSubject,
) : UpstreamFeatureEvent

internal data class SubjectDeletedFromWhitelistEvent(
    override val featureIdentifier: FeatureIdentifier,
    val subject: WhitelistedSubject,
) : UpstreamFeatureEvent
