/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.WhitelistedSubjectResource
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.shared.model.WhitelistedSubject
import org.springframework.stereotype.Component

@Component
class WhitelistedSubjectResourceFactory {

  fun build(
      whitelistedSubject: WhitelistedSubject,
      featureId: FeatureId
  ): WhitelistedSubjectResource =
      WhitelistedSubjectResource(
          featureId = featureId,
          featureName = whitelistedSubject.featureName,
          subjectId = whitelistedSubject.subjectRef,
          type = whitelistedSubject.type)
}
