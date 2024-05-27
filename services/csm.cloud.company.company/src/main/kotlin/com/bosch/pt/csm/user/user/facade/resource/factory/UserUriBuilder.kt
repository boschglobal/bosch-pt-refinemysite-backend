/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.user.user.facade.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils
import java.net.URI
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class UserUriBuilder(private val apiVersionProperties: ApiVersionProperties) {

  fun buildUserUri(identifier: UUID?): URI =
      LinkUtils.linkTemplateWithPathSegmentsUnversioned(
              "v" + apiVersionProperties.user!!.version, USERS, USER_IDENTIFIER
      )
          .buildAndExpand(identifier)
          .toUri()

  fun buildUserRoleUri(identifier: UUID?): URI =
      LinkUtils.linkTemplateWithPathSegmentsUnversioned(
              "v" + apiVersionProperties.user!!.version, USERS, USER_IDENTIFIER, "roles")
          .buildAndExpand(identifier)
          .toUri()

  fun buildUserLockUri(identifier: UUID?): URI =
      LinkUtils.linkTemplateWithPathSegmentsUnversioned(
              "v" + apiVersionProperties.user!!.version, USERS, USER_IDENTIFIER, "lock")
          .buildAndExpand(identifier)
          .toUri()

  companion object {
    private const val USERS = "users"
    private const val USER_IDENTIFIER = "{userIdentifier}"
  }
}
