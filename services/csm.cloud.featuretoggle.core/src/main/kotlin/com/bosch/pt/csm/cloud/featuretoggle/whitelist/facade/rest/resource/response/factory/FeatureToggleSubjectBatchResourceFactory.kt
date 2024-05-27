/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model.Feature
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.FeatureToggleSubjectResource
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class FeatureToggleSubjectBatchResourceFactory(
    private val featureToggleSubjectResourceFactoryHelper: FeatureToggleSubjectResourceFactoryHelper
) {
  fun build(features: List<Feature>, subjectId: UUID): BatchResponseResource<FeatureToggleSubjectResource> =
      BatchResponseResource(featureToggleSubjectResourceFactoryHelper.buildResources(features, subjectId))
}
