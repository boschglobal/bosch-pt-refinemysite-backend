/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum
import java.util.Date
import java.util.UUID

data class FeatureResource(
    val name: String,
    val displayName: String,
    val state: FeatureStateEnum,
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy)
