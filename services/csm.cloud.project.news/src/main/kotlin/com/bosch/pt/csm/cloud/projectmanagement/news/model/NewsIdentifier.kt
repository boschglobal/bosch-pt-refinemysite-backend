/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.model

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.MappedSuperclass
import jakarta.validation.constraints.NotNull
import java.util.UUID

@MappedSuperclass
open class NewsIdentifier(
    // userIdentifier
    @field:NotNull @Column(name = "USER_IDENTIFIER") var userIdentifier: UUID? = null,

    // contextObject
    @Embedded
    @AttributeOverride(name = "type", column = Column(name = "CONTEXT_TYPE"))
    @AttributeOverride(name = "identifier", column = Column(name = "CONTEXT_IDENTIFIER"))
    var contextObject: ObjectIdentifier
) : AbstractPersistable<Long>()
