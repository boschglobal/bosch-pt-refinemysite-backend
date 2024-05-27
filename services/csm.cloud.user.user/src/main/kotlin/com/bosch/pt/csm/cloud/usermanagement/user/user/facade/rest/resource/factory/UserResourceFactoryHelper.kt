/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper.getCurrentUser
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.query.CraftQueryService
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.CraftTranslationProjection
import com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest.resource.factory.ProfilePictureResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.user.picture.query.ProfilePictureQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.PATH_VARIABLE_USER_ID
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USER_BY_USER_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USER_LOCK_BY_USER_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USER_ROLES_BY_USER_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory.UserResourceFactory.Companion.EMBEDDED_NAME_PROFILE_PICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_DELETE
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_LOCK
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_SET_ADMIN
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_UNLOCK
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource.Companion.LINK_UNSET_ADMIN
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserNameResolverService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Auditable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Component
class UserResourceFactoryHelper(
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UserId,
    private val craftQueryService: CraftQueryService,
    private val profilePictureQueryService: ProfilePictureQueryService,
    private val profilePictureResourceFactory: ProfilePictureResourceFactory,
    private val userNameResolverService: UserNameResolverService,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun buildUserResources(users: List<User>): Collection<UserResource> {
    if (users.isEmpty()) {
      return emptyList()
    }

    val userIdentifiers = users.map { it.identifier }.toSet()

    val craftIdentifiers = users.flatMap { it.crafts }.map { it.identifier }
    val craftReferences =
        craftQueryService
            .findByIdentifiersAndTranslationsLocale(
                craftIdentifiers, LocaleContextHolder.getLocale().language)
            .associateBy { it.craftId }

    val profilePictures: Map<UserId, ProfilePicture> =
        profilePictureQueryService.findProfilePictureByUserIdentifiers(userIdentifiers)

    return users.map { build(it, craftReferences, profilePictures) }
  }

  @Suppress("UNCHECKED_CAST")
  fun build(
      user: User,
      craftTranslationProjectionMap: Map<CraftId, CraftTranslationProjection>,
      profilePictureMap: Map<UserId, ProfilePicture>
  ): UserResource {
    val craftResourceReferences: List<ResourceReference> =
        user.crafts
            .mapNotNull { craftTranslationProjectionMap[it.identifier] }
            .sortedWith(Comparator.comparing { it.value ?: it.defaultName })
            .map { ResourceReference(it.craftId.identifier, it.value ?: it.defaultName) }

    val userNames =
        userNameResolverService.findUserDisplayNames(user as Auditable<UuidIdentifiable, *, *>)

    val resource =
        UserResource(
            user,
            craftResourceReferences,
            userNames.createdByOf(user),
            userNames.lastModifiedByOf(user))

    // Add the profile picture (or the default profile picture in case the user has not uploaded one
    // yet).
    resource.addResourceSupplier(EMBEDDED_NAME_PROFILE_PICTURE) {
      val profilePicture = profilePictureMap[user.identifier]

      return@addResourceSupplier when (profilePicture != null) {
        true -> profilePictureResourceFactory.build(profilePicture)
        else -> profilePictureResourceFactory.build(user)
      }
    }
    resource.embed(EMBEDDED_NAME_PROFILE_PICTURE)

    // Add self reference
    resource.add(
        linkFactory
            .linkTo(USER_BY_USER_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_USER_ID to user.identifier))
            .withSelfRel())

    // Return the resource if the user resource is the system user or the user itself, otherwise add
    // additional links
    if (user.identifier == systemUserIdentifier ||
        getCurrentUser().getIdentifierUuid() == user.getIdentifierUuid()) {
      return resource
    }

    // Add links for the user administration
    if (getCurrentUser().admin) {
      resource.add(
          linkFactory
              .linkTo(USER_BY_USER_ID_ENDPOINT_PATH)
              .withParameters(mapOf(PATH_VARIABLE_USER_ID to user.identifier))
              .withRel(LINK_DELETE))

      resource.add(
          linkFactory
              .linkTo(USER_ROLES_BY_USER_ID_ENDPOINT_PATH)
              .withParameters(mapOf(PATH_VARIABLE_USER_ID to user.identifier))
              .withRel(if (user.admin) LINK_UNSET_ADMIN else LINK_SET_ADMIN))

      resource.add(
          linkFactory
              .linkTo(USER_LOCK_BY_USER_ID_ENDPOINT_PATH)
              .withParameters(mapOf(PATH_VARIABLE_USER_ID to user.identifier))
              .withRel(if (user.locked) LINK_UNLOCK else LINK_LOCK))
    }
    return resource
  }
}
