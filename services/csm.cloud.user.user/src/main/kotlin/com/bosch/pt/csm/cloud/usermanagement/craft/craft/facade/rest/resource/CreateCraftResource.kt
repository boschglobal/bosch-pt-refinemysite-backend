/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.CreateTranslationResource
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.api.CreateCraftCommand
import java.util.Locale

class CreateCraftResource {
  var translations: Set<CreateTranslationResource> = HashSet()

  fun toCommand(identifier: CraftId): CreateCraftCommand {
    val translationEntities = translations.map { it.createEntity() }.toSet()
    val defaultName = translations.firstOrNull { Locale.ENGLISH.language == it.locale }?.value

    if (defaultName.isNullOrEmpty()) {
      throw PreconditionViolationException(Key.CRAFT_VALIDATION_ERROR_NO_DEFAULT_NAME)
    }

    return CreateCraftCommand(
        identifier = identifier, defaultName = defaultName, translations = translationEntities)
  }
}
