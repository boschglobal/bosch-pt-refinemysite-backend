/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatTypeEnum
import java.util.Date
import java.util.UUID

data class PatResource(
    override val id: UUID,
    override val version: Long,
    val type: PatTypeEnum,
    val impersonatedUser: UserId,
    val description: String,
    val scopes: List<PatScopeEnum>,
    val issuedAt: Date,
    val expiresAt: Date,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
) :
    AbstractAuditableResource(
        id,
        version,
        createdDate,
        createdBy,
        lastModifiedDate,
        lastModifiedBy,
    )
