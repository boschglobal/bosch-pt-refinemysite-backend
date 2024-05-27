/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.query.CraftQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.factory.ProfilePictureResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.user.picture.query.ProfilePictureQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.CurrentUserController.Companion.CURRENT_USER_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.CurrentUserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.CurrentUserResource.Companion.LINK_CURRENT_USER_UPDATE
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.context.i18n.LocaleContextHolder.getLocale
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class CurrentUserResourceFactory(
    private val craftQueryService: CraftQueryService,
    private val profilePictureQueryService: ProfilePictureQueryService,
    private val profilePictureResourceFactory: ProfilePictureResourceFactory,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun build(user: User): CurrentUserResource {
    val craftTranslations =
        craftQueryService.findByIdentifiersAndTranslationsLocale(
            user.crafts.map { it.identifier }.toSet(), getLocale().language)

    val craftResourceReferences =
        craftTranslations.map {
          ResourceReference(it.craftId.identifier, it.value ?: it.defaultName)
        }

    val currentUserResource = CurrentUserResource(user, craftResourceReferences)

    // embed profile picture
    currentUserResource.addResourceSupplier(EMBEDDED_NAME_PROFILE_PICTURE) {
      val profilePicture = profilePictureQueryService.findProfilePictureByUser(user.identifier)

      when (profilePicture != null) {
        true -> return@addResourceSupplier profilePictureResourceFactory.build(profilePicture)
        else -> return@addResourceSupplier profilePictureResourceFactory.build(user)
      }
    }
    currentUserResource.embed(EMBEDDED_NAME_PROFILE_PICTURE)

    currentUserResource.add(linkFactory.linkTo(CURRENT_USER_ENDPOINT_PATH).withSelfRel())
    currentUserResource.add(
        linkFactory.linkTo(CURRENT_USER_ENDPOINT_PATH).withRel(LINK_CURRENT_USER_UPDATE))

    return currentUserResource
  }

  companion object {
    const val EMBEDDED_NAME_PROFILE_PICTURE = "profilePicture"
  }
}
