/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.model

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.COMPANY_STATE
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = COMPANY_STATE)
@TypeAlias("Company")
data class Company(
    @Id val identifier: AggregateIdentifier,
    override val companyIdentifier: UUID,
    val name: String,
    val streetAddress: StreetAddress?,
    val postBoxAddress: PostBoxAddress?,
    val deleted: Boolean = false
) : ShardedByCompanyIdentifier

data class PostBoxAddress(
    val postBox: String,
    val city: String,
    val zipCode: String,
    val area: String?,
    val country: String
)

data class StreetAddress(
    val street: String,
    val houseNumber: String,
    val city: String,
    val zipCode: String,
    val area: String?,
    val country: String
)
