/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.MappedSuperclass
import java.util.UUID

@Embeddable
@MappedSuperclass
open class DayCardIdentifier(
    // project identifier
    @Column(name = "project_identifier") var projectIdentifier: UUID,

    // type / identifier
    @AttributeOverrides(
        AttributeOverride(name = "type", column = Column(name = "CONTEXT_TYPE")),
        AttributeOverride(name = "identifier", column = Column(name = "CONTEXT_IDENTIFIER")))
    var contextObject: ObjectIdentifier
) : AbstractPersistable<Long>()
