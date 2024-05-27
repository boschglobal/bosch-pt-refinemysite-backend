/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.CraftController.Companion.CRAFT_BY_CRAFT_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.CraftController.Companion.PATH_VARIABLE_CRAFT_ID
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.CraftResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.query.CraftQueryService
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.CraftTranslationProjection
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class CraftResourceFactory(
    private val craftQueryService: CraftQueryService,
    private val linkFactory: CustomLinkBuilderFactory
) {

  /**
   * Builds a resource for a [Craft].
   *
   * @param craft the craft to be represented by the resource
   * @param locale the locale to add self-reference with user's locale
   * @return the resource
   */
  fun build(craft: Craft, locale: Locale): CraftResource? =
      build(
          craftQueryService.findByIdentifierAndTranslationsLocale(
              craft.identifier, locale.language))

  /**
   * Builds a resource for a [CraftTranslationProjection].
   *
   * @param translation the translation
   * @return the resource
   */
  fun build(translation: CraftTranslationProjection?): CraftResource? {
    if (translation == null) {
      return null
    }

    val value = translation.value ?: translation.defaultName
    val resource = CraftResource(translation.craftId, value)

    // add self reference
    resource.add(
        linkFactory
            .linkTo(CRAFT_BY_CRAFT_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_CRAFT_ID to translation.craftId))
            .withSelfRel())

    return resource
  }
}
