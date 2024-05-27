/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model

import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.CompanyId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val COMPANY_PROJECTION = "CompanyProjection"

@Document(COMPANY_PROJECTION)
@TypeAlias(COMPANY_PROJECTION)
data class Company(
    @Id val identifier: CompanyId,
    val version: Long,
    val name: String,
    val streetAddress: StreetAddress? = null,
    val postBoxAddress: PostBoxAddress? = null,
    val eventAuthor: UserId,
    val deleted: Boolean = false,
    val eventDate: LocalDateTime,
    val history: List<CompanyVersion>
)

data class CompanyVersion(
    val version: Long,
    val name: String,
    val streetAddress: StreetAddress? = null,
    val postBoxAddress: PostBoxAddress? = null,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)

data class StreetAddress(
    val street: String,
    val houseNumber: String,
    val city: String,
    val zipCode: String,
    val area: String? = null,
    val country: String,
)

data class PostBoxAddress(
    val postBox: String,
    val city: String,
    val zipCode: String,
    val area: String? = null,
    val country: String,
)
