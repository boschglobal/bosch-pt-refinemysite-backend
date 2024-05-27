/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.util.UUID
import jakarta.annotation.Nonnull

class UserReference(identifier: UUID, displayName: String, val email: String?) :
    ResourceReference(identifier, displayName) {

  companion object {
    fun from(@Nonnull user: User) =
        UserReference(user.getIdentifierUuid(), user.getDisplayName()!!, user.email)
  }
}
