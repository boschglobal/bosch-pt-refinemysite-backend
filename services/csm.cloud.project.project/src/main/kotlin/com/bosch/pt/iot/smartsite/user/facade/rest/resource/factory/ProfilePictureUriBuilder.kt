/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.linkTemplateWithPathSegments
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.linkTemplateWithPathSegmentsUnversioned
import com.bosch.pt.iot.smartsite.user.model.ProfilePicture
import java.net.URI
import java.util.UUID
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/** Provides static methods to generate the uri for the (default) profile picture. */
@Component
open class ProfilePictureUriBuilder private constructor() : ApplicationContextAware {

  override fun setApplicationContext(applicationContext: ApplicationContext) {
    userApiVersion = applicationContext.getBean(ApiVersionProperties::class.java).user!!.version
  }

  companion object {
    const val FILE_NAME_DEFAULT_PICTURE = "default-profile-picture.png"

    private var userApiVersion = 0

    @JvmStatic
    fun buildProfilePictureUri(profilePicture: ProfilePicture): URI =
        requireNotNull(profilePicture.user) { "User must not be null" }
            .let { buildProfilePictureUri(profilePicture.identifier!!, it.identifier!!) }

    @JvmStatic
    fun buildProfilePictureUri(profilePictureIdentifier: UUID, userIdentifier: UUID): URI =
        linkTemplateWithPathSegments(
                userApiVersion, "users", "{userIdentifier}", "picture", "{identifier}", "{size}")
            .buildAndExpand(userIdentifier, profilePictureIdentifier, SMALL.name)
            .toUri()

    /**
     * Provides a url of the gender-neutral default profile picture.
     *
     * @return the url of the default profile picture (blob)
     */
    @JvmStatic
    fun buildDefaultProfilePictureUri(): URI =
        linkTemplateWithPathSegmentsUnversioned(FILE_NAME_DEFAULT_PICTURE).build().toUri()

    /**
     * Provides a url of the profile picture. A default is provided as the fallback.
     *
     * @param profilePictureIdentifier The profile picture identifier of an user
     * @param userIdentifier The user identifier
     * @return the url of the profile picture (blob)
     */
    @JvmStatic
    fun buildWithFallback(profilePictureIdentifier: UUID?, userIdentifier: UUID?): URI =
        if (profilePictureIdentifier == null || userIdentifier == null) {
          buildDefaultProfilePictureUri()
        } else {
          buildProfilePictureUri(profilePictureIdentifier, userIdentifier)
        }

    /**
     * Provides a url of the profile picture. A default is provided as the fallback.
     *
     * @param profilePicture The profile picture of an user
     * @return the url of the profile picture (blob)
     */
    @JvmStatic
    fun buildWithFallback(profilePicture: ProfilePicture?): URI =
        if (profilePicture == null) {
          buildDefaultProfilePictureUri()
        } else {
          buildProfilePictureUri(profilePicture)
        }
  }
}
