/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.PatResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.Pat
import org.springframework.stereotype.Component

@Component
class PatResourceFactoryHelper {

  fun buildResources(features: List<Pat>): List<PatResource> =
      if (features.isEmpty()) {
        emptyList()
      } else {
        features.map { build(it) }
      }

  fun build(pat: Pat): PatResource =
      PatResource(
          id = pat.getIdentifierUuid(),
          version = pat.version,
          impersonatedUser = pat.impersonatedUser,
          type = pat.type,
          description = pat.description,
          issuedAt = pat.issuedAt.toDate(),
          expiresAt = pat.expiresAt.toDate(),
          scopes = pat.scopes,
          lastModifiedBy = referTo(pat.lastModifiedBy),
          lastModifiedDate = pat.lastModifiedDate.get().toDate(),
          createdBy = referTo(pat.createdBy),
          createdDate = pat.createdDate.get().toDate(),
      )
}
