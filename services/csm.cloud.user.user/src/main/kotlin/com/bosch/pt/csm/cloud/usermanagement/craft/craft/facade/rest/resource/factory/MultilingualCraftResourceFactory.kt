/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.TranslationResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.CraftController.Companion.CRAFT_BY_CRAFT_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.CraftController.Companion.PATH_VARIABLE_CRAFT_ID
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.MultilingualCraftResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserNameResolverService
import org.springframework.stereotype.Component

@Component
class MultilingualCraftResourceFactory(
    val userNameResolverService: UserNameResolverService,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun build(craft: Craft): MultilingualCraftResource {

    val userNames = userNameResolverService.findUserDisplayNames(craft)

    val translationResources =
        craft.translations.map { TranslationResource(it.locale!!, it.value!!) }.toSet()

    val resource =
        MultilingualCraftResource(
            craft, translationResources, userNames.createdByOf(craft), userNames.createdByOf(craft))

    // add self reference
    resource.add(
        linkFactory
            .linkTo(CRAFT_BY_CRAFT_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_CRAFT_ID to craft.identifier))
            .withSelfRel())

    return resource
  }
}
