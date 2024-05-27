/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model.Feature
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.FeatureToggleSubjectResource
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.ENABLED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.WHITELIST_ACTIVATED
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class FeatureToggleSubjectResourceFactoryHelper {

  fun buildResources(features: List<Feature>, subjectId: UUID): List<FeatureToggleSubjectResource> =
      if (features.isEmpty()) {
        emptyList()
      } else {
        features.map { build(it, subjectId) }
      }

  fun build(feature: Feature, subjectId: UUID): FeatureToggleSubjectResource {

    val whitelistedSubject = feature.whitelistedSubjects.find { it.subjectRef == subjectId }

    return FeatureToggleSubjectResource(
        subjectId = subjectId,
        featureId = requireNotNull(feature.identifier),
        version = feature.version,
        name = feature.name,
        displayName = feature.getDisplayName(),
        type = whitelistedSubject?.type,
        whitelisted = whitelistedSubject != null,
        enabledForSubject =
            feature.state == ENABLED ||
                (feature.state == WHITELIST_ACTIVATED && whitelistedSubject != null),
        featureState = feature.state,
        lastModifiedBy = referTo(feature.lastModifiedBy),
        lastModifiedDate = feature.lastModifiedDate.get().toDate(),
        createdBy = referTo(feature.createdBy),
        createdDate = feature.createdDate.get().toDate())
  }

  private fun referTo(userId: Optional<UserId>): ResourceReference =
      ResourceReference(userId.get().identifier, "User ${userId.get().identifier}")
}
