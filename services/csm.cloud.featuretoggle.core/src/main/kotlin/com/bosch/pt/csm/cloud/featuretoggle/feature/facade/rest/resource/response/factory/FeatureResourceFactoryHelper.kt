/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.FeatureResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model.Feature
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class FeatureResourceFactoryHelper {

  fun buildResources(features: List<Feature>): List<FeatureResource> =
      if (features.isEmpty()) {
        emptyList()
      } else {
        features.map { build(it) }
      }

  fun build(feature: Feature): FeatureResource =
      FeatureResource(
          id = feature.getIdentifierUuid(),
          version = feature.version,
          name = feature.name,
          displayName = feature.getDisplayName(),
          state = feature.state,
          lastModifiedBy = referTo(feature.lastModifiedBy),
          lastModifiedDate = feature.lastModifiedDate.get().toDate(),
          createdBy = referTo(feature.createdBy),
          createdDate = feature.createdDate.get().toDate())

  private fun referTo(userId: Optional<UserId>): ResourceReference =
      ResourceReference(userId.get().identifier, "User ${userId.get().identifier}")
}
