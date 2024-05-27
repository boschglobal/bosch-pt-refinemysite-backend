/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture

class ProfilePictureResource : AbstractAuditableResource {

  val width: Long?
  val height: Long?
  val fileSize: Long?
  val userReference: ResourceReference

  /** This is the regular constructor used to create a profile picture resource. */
  constructor(
      profilePicture: ProfilePicture,
      createByName: String,
      lastModifiedByName: String
  ) : super(profilePicture, createByName, lastModifiedByName) {
    width = profilePicture.width
    height = profilePicture.height
    fileSize = profilePicture.fileSize
    userReference = ResourceReference.from(profilePicture.user)
  }

  /** This constructor is used to create a profile picture resource with a default picture. */
  constructor(userReference: ResourceReference) {
    width = DEFAULT_PICTURE_WIDTH
    height = DEFAULT_PICTURE_HEIGHT
    fileSize = DEFAULT_PICTURE_FILESIZE
    this.userReference = userReference
  }

  companion object {
    const val LINK_FULL_PICTURE = "full"
    const val LINK_SMALL_PICTURE = "small"
    const val LINK_DELETE_PICTURE = "delete"
    const val LINK_UPDATE_PICTURE = "update"
    private const val DEFAULT_PICTURE_WIDTH = 389L
    private const val DEFAULT_PICTURE_HEIGHT = 389L
    private const val DEFAULT_PICTURE_FILESIZE = 18768L
  }
}
