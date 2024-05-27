/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.model

import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated

@Embeddable
data class RelationElement(

    // identifier
    var identifier: UUID,

    // type
    @Column(nullable = false) @Enumerated(STRING) var type: RelationElementTypeEnum,
)
