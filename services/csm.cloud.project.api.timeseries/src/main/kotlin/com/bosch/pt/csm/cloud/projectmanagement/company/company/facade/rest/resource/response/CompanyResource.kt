/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.CompanyId
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.PostBoxAddress
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.StreetAddress

data class CompanyResource(
    val id: CompanyId,
    val version: Long,
    val name: String,
    val streetAddress: StreetAddress? = null,
    val postBoxAddress: PostBoxAddress? = null,
    val deleted: Boolean,
    val eventTimestamp: Long
)
