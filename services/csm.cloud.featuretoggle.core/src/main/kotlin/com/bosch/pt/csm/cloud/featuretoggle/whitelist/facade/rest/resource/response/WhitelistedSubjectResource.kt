/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response

import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import java.util.UUID

data class WhitelistedSubjectResource(
    val featureId: FeatureId,
    val featureName: String,
    val subjectId: UUID,
    val type: SubjectTypeEnum?,
)
