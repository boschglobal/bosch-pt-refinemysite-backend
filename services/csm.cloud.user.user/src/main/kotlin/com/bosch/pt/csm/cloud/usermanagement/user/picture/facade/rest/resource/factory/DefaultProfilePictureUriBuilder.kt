/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.linkTemplateWithPathSegmentsUnversioned
import java.net.URI
import org.springframework.stereotype.Component

@Component
object DefaultProfilePictureUriBuilder {

  const val FILE_NAME_DEFAULT_PICTURE = "default-profile-picture.png"

  /**
   * Provides a url of the gender-neutral default profile picture.
   *
   * @return the url of the default profile picture (blob)
   */
  fun build(): URI =
      linkTemplateWithPathSegmentsUnversioned(FILE_NAME_DEFAULT_PICTURE).build().toUri()
}
