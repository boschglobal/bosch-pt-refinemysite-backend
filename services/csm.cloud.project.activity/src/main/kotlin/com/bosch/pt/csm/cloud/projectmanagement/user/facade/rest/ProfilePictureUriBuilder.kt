/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.linkTemplateWithPathSegments
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.linkTemplateWithPathSegments
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import java.net.URI
import org.springframework.stereotype.Component

@Component
class ProfilePictureUriBuilder(val apiVersionProperties: ApiVersionProperties) {

  fun buildProfilePictureUri(user: User?): URI =
      if (user?.userPictureIdentifier != null)
          buildProfilePictureUri(user.identifier.toString(), user.userPictureIdentifier.toString())
      else buildDefaultProfilePictureUri()

  private fun buildProfilePictureUri(userIdentifier: String, pictureIdentifier: String) =
      linkTemplateWithPathSegments(
              this.apiVersionProperties.user!!.version,
              "users",
              "{toIdentifierString}",
              "picture",
              "{identifier}",
              "{size}")
          .buildAndExpand(userIdentifier, pictureIdentifier, "SMALL")
          .toUri()

  private fun buildDefaultProfilePictureUri() =
      FILE_NAME_DEFAULT_PICTURE.linkTemplateWithPathSegments().build().toUri()

  companion object {
    private const val FILE_NAME_DEFAULT_PICTURE = "default-profile-picture.png"
  }
}
