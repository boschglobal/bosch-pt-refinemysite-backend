/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model

import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import java.time.LocalDateTime
import jakarta.persistence.Access
import jakarta.persistence.AccessType.FIELD
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
data class DocumentVersion(

    // identifier
    @Embedded
    @Access(FIELD)
    @AttributeOverride(name = "identifier", column = Column(nullable = false))
    val identifier: DocumentVersionId,

    // lastChanged
    @Column(nullable = false) val lastChanged: LocalDateTime,
)
