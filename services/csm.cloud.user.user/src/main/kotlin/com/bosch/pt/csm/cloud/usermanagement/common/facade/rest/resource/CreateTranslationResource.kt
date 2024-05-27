/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource

import com.bosch.pt.csm.cloud.usermanagement.common.model.Translation
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import jakarta.validation.constraints.Size

class CreateTranslationResource(
    @param:Size(min = 2, max = Craft.MAX_LANGUAGE_LENGTH) val locale: String,
    @param:Size(min = 1, max = Craft.MAX_CRAFT_NAME_LENGTH) val value: String
) {

  /**
   * Creates a new [Translation] with the data from the resource.
   *
   * @return the [Translation] created
   */
  fun createEntity() = Translation(locale.lowercase(), value)
}
