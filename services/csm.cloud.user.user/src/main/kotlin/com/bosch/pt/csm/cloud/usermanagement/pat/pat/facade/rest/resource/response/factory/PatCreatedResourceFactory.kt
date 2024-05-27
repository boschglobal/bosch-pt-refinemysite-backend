/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatCreatedCommandResult
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.PatCreatedResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.Pat
import org.springframework.stereotype.Component

@Component
class PatCreatedResourceFactory {
  fun build(patCreatedCommandResult: PatCreatedCommandResult, pat: Pat): PatCreatedResource =
      PatCreatedResource(
          id = patCreatedCommandResult.patId.toUuid(),
          version = pat.version,
          token = patCreatedCommandResult.token,
          type = pat.type,
          impersonatedUser = pat.impersonatedUser,
          description = pat.description,
          issuedAt = pat.issuedAt.toDate(),
          expiresAt = pat.expiresAt.toDate(),
          scopes = pat.scopes,
          createdDate = pat.createdDate.get().toDate(),
          createdBy = referTo(pat.createdBy),
          lastModifiedDate = pat.lastModifiedDate.get().toDate(),
          lastModifiedBy = referTo(pat.lastModifiedBy),
      )
}
