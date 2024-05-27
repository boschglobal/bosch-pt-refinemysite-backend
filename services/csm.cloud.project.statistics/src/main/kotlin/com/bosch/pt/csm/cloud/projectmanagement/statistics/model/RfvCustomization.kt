/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    indexes =
        [
            Index(name = "UK_RfvCust_Identifier", columnList = "identifier", unique = true),
            Index(
                name = "UK_RfvCust_ProjId",
                columnList = "project_identifier,identifier",
                unique = true),
        ])
class RfvCustomization(
    // identifier
    @Column(nullable = false) var identifier: UUID,

    // project reference
    @Column(name = "project_identifier", nullable = false) var projectIdentifier: UUID,

    // key
    @Column(name = "rfv_key", nullable = false) var key: DayCardReasonNotDoneEnum,

    // active
    @Column(nullable = false) var active: Boolean,

    // optional name
    var name: String?
) : AbstractPersistable<Long>()
