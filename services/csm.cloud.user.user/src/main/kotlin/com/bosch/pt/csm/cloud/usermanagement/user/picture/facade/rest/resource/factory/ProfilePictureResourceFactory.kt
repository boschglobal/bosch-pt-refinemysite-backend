/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.usermanagement.user.authorization.UserAuthorizationComponent
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.PATH_VARIABLE_PICTURE_ID
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.PATH_VARIABLE_SIZE
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.PATH_VARIABLE_USER_ID
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.ProfilePictureController.Companion.USER_PICTURE_BY_USER_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.response.ProfilePictureResource
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.response.ProfilePictureResource.Companion.LINK_DELETE_PICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.response.ProfilePictureResource.Companion.LINK_FULL_PICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.response.ProfilePictureResource.Companion.LINK_SMALL_PICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.response.ProfilePictureResource.Companion.LINK_UPDATE_PICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserNameResolverService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.hateoas.Link
import org.springframework.stereotype.Component

@Component
class ProfilePictureResourceFactory(
    private val userAuthorizationComponent: UserAuthorizationComponent,
    private val userNameResolverService: UserNameResolverService,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun build(profilePicture: ProfilePicture): ProfilePictureResource {
    val userNames = userNameResolverService.findUserDisplayNames(profilePicture)
    val resource =
        ProfilePictureResource(
            profilePicture,
            userNames.createdByOf(profilePicture),
            userNames.lastModifiedByOf(profilePicture))

    resource.add(
        linkFactory
            .linkTo(USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT_PATH)
            .withParameters(
                mapOf(
                    PATH_VARIABLE_USER_ID to profilePicture.user.getIdentifierUuid(),
                    PATH_VARIABLE_PICTURE_ID to profilePicture.getIdentifierUuid(),
                    PATH_VARIABLE_SIZE to FULL.name.lowercase()))
            .withRel(LINK_FULL_PICTURE))

    resource.add(
        linkFactory
            .linkTo(USER_PICTURE_BY_USER_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT_PATH)
            .withParameters(
                mapOf(
                    PATH_VARIABLE_USER_ID to profilePicture.user.getIdentifierUuid(),
                    PATH_VARIABLE_PICTURE_ID to profilePicture.getIdentifierUuid(),
                    PATH_VARIABLE_SIZE to SMALL.name.lowercase()))
            .withRel(LINK_SMALL_PICTURE))

    if (userAuthorizationComponent.isCurrentUser(profilePicture.user.identifier)) {
      resource.add(
          linkFactory
              .linkTo(USER_PICTURE_BY_USER_ID_ENDPOINT_PATH)
              .withParameters(
                  mapOf(PATH_VARIABLE_USER_ID to profilePicture.user.getIdentifierUuid()))
              .withRel(LINK_UPDATE_PICTURE))

      resource.add(
          linkFactory
              .linkTo(USER_PICTURE_BY_USER_ID_ENDPOINT_PATH)
              .withParameters(
                  mapOf(PATH_VARIABLE_USER_ID to profilePicture.user.getIdentifierUuid()))
              .withRel(LINK_DELETE_PICTURE))
    }

    return resource
  }

  /**
   * This operation can be used to create a profile picture resource that contains the default
   * profile picture.
   *
   * @param user the user is required to get the gender (and to pick the correct default picture
   * with it) as well as to build the user resource reference.
   */
  fun build(user: User): ProfilePictureResource {
    val resource = ProfilePictureResource(ResourceReference.from(user))

    resource.add(Link.of(DefaultProfilePictureUriBuilder.build().toString(), LINK_FULL_PICTURE))

    resource.add(Link.of(DefaultProfilePictureUriBuilder.build().toString(), LINK_SMALL_PICTURE))

    if (userAuthorizationComponent.isCurrentUser(user.identifier)) {
      resource.add(
          linkFactory
              .linkTo(USER_PICTURE_BY_USER_ID_ENDPOINT_PATH)
              .withParameters(mapOf(PATH_VARIABLE_USER_ID to user.getIdentifierUuid()))
              .withRel(LINK_UPDATE_PICTURE))
    }

    return resource
  }
}
