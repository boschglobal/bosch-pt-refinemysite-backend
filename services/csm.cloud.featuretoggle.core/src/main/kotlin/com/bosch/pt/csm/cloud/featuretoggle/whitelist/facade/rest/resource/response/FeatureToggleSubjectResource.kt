/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import java.util.Date
import java.util.UUID

data class FeatureToggleSubjectResource(
    val featureId: FeatureId,
    val subjectId: UUID,
    val type: SubjectTypeEnum?,
    val name: String,
    val displayName: String,
    val whitelisted: Boolean,
    val featureState: FeatureStateEnum,
    val enabledForSubject: Boolean,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
) :
    AbstractAuditableResource(
        subjectId, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy)
