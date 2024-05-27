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

data class DelayConsentCommand(val userIdentifier: UserId)

data class GiveConsentCommand(
    val userIdentifier: UserId,
    val documentVersionIds: Collection<DocumentVersionId>
)
