/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response

import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import java.util.UUID

data class WhitelistedSubjectIdResource(val subjectId: UUID, val featureId: FeatureId)
