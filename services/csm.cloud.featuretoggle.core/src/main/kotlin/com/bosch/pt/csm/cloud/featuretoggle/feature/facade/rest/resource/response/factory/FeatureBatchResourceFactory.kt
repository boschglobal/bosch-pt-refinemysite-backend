/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.FeatureResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model.Feature
import org.springframework.stereotype.Component

@Component
class FeatureBatchResourceFactory(
    private val featureResourceFactoryHelper: FeatureResourceFactoryHelper
) {
  fun build(features: List<Feature>): BatchResponseResource<FeatureResource> =
      BatchResponseResource(featureResourceFactoryHelper.buildResources(features))
}
