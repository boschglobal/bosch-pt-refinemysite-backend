/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.model

import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import java.time.LocalDate
import jakarta.persistence.Access
import jakarta.persistence.AccessType.FIELD
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
data class UserConsent(
    @Column(nullable = false) val date: LocalDate,
    @Embedded
    @Access(FIELD)
    @AttributeOverride(
        name = "identifier", column = Column(name = "document_version_id", nullable = false))
    val documentVersionId: DocumentVersionId,
)
