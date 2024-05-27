/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.command.api

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.featuretoggle.common.command.api.AuditedEvent
import com.bosch.pt.csm.cloud.featuretoggle.common.command.api.FeaturetoggleContextEvent
import java.time.LocalDateTime

data class FeatureCreatedEvent(
    override val featureId: FeatureId,
    val name: String,
    override val userIdentifier: UserId,
    override val timestamp: LocalDateTime
) : FeaturetoggleContextEvent, AuditedEvent

data class FeatureEnabledEvent(
    override val featureId: FeatureId,
    val name: String,
    override val userIdentifier: UserId,
    override val timestamp: LocalDateTime
) : FeaturetoggleContextEvent, AuditedEvent

data class FeatureDisabledEvent(
    override val featureId: FeatureId,
    val name: String,
    override val userIdentifier: UserId,
    override val timestamp: LocalDateTime
) : FeaturetoggleContextEvent, AuditedEvent

data class FeatureWhitelistActivatedEvent(
    override val featureId: FeatureId,
    val name: String,
    override val userIdentifier: UserId,
    override val timestamp: LocalDateTime
) : FeaturetoggleContextEvent, AuditedEvent

data class FeatureDeletedEvent(
    override val featureId: FeatureId,
    val name: String,
    override val userIdentifier: UserId,
    override val timestamp: LocalDateTime
) : FeaturetoggleContextEvent, AuditedEvent
