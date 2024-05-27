/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.api

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersion
import java.net.URL
import java.time.LocalDateTime
import java.util.Locale

data class DocumentCreatedEvent(
    val documentIdentifier: DocumentId,
    val documentType: DocumentType,
    val country: IsoCountryCodeEnum,
    val locale: Locale,
    val clientSet: ClientSet,
    val title: String,
    val url: URL,
    val initialVersion: DocumentVersion,
    val timestamp: LocalDateTime,
    val userIdentifier: UserId,
)

data class DocumentVersionIncrementedEvent(
    val documentIdentifier: DocumentId,
    val version: DocumentVersion,
    val timestamp: LocalDateTime,
    val userIdentifier: UserId,
)

data class DocumentChangedEvent(
    val documentIdentifier: DocumentId,
    val title: String?,
    val url: URL?,
    val timestamp: LocalDateTime,
    val userIdentifier: UserId,
)
