/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.api

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import java.time.LocalDateTime

data class ConsentDelayedEvent(val userIdentifier: UserId, val timestamp: LocalDateTime)

data class UserConsentedEvent(
    val userIdentifier: UserId,
    val documentVersionIdentifier: DocumentVersionId,
    val timestamp: LocalDateTime,
)
