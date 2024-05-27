/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.linkTemplateWithPathSegments
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.linkTemplateWithPathSegmentsUnversioned
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import java.net.URI
import org.springframework.stereotype.Component

@Component
class ProfilePictureUriBuilder(val apiVersionProperties: ApiVersionProperties) {

  fun buildProfilePictureUri(user: User?): URI =
      if (user?.userPictureIdentifier != null)
          buildProfilePictureUri(user.identifier.toString(), user.userPictureIdentifier.toString())
      else buildDefaultProfilePictureUri()

  private fun buildProfilePictureUri(userIdentifier: String, pictureIdentifier: String): URI =
      linkTemplateWithPathSegments(
              apiVersionProperties.user!!.version,
              "users",
              "{userIdentifier}",
              "picture",
              "{pictureIdentifier}",
              "{size}")
          .buildAndExpand(userIdentifier, pictureIdentifier, "SMALL")
          .toUri()

  private fun buildDefaultProfilePictureUri(): URI =
      linkTemplateWithPathSegmentsUnversioned("default-profile-picture.png").build().toUri()
}
