/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.graphql.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.PostBoxAddress
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.StreetAddress
import java.time.LocalDateTime
import java.util.UUID

data class CompanyPayloadV1(
    val id: UUID,
    val version: Long,
    val name: String,
    val streetAddress: StreetAddress? = null,
    val postBoxAddress: PostBoxAddress? = null,
    val eventDate: LocalDateTime,
)
