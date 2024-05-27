/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.FeatureResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model.Feature
import org.springframework.stereotype.Component

@Component
class FeatureResourceFactory(
    private val featureResourceFactoryHelper: FeatureResourceFactoryHelper
) {
  fun build(feature: Feature): FeatureResource = featureResourceFactoryHelper.build(feature)
}
