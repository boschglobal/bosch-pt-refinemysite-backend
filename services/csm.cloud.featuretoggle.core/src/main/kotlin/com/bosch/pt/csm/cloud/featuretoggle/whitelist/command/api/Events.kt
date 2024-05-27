/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.featuretoggle.common.command.api.AuditedEvent
import com.bosch.pt.csm.cloud.featuretoggle.common.command.api.FeaturetoggleContextEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import java.time.LocalDateTime
import java.util.UUID

data class SubjectAddedToWhitelistEvent(
    override val featureId: FeatureId,
    val subjectRef: UUID,
    val type: SubjectTypeEnum,
    val feature: String,
    override val userIdentifier: UserId,
    override val timestamp: LocalDateTime
) : AuditedEvent, FeaturetoggleContextEvent

data class SubjectDeletedFromWhitelistEvent(
    override val featureId: FeatureId,
    val subjectRef: UUID,
    val type: SubjectTypeEnum,
    val feature: String,
    override val userIdentifier: UserId,
    override val timestamp: LocalDateTime
) : AuditedEvent, FeaturetoggleContextEvent
