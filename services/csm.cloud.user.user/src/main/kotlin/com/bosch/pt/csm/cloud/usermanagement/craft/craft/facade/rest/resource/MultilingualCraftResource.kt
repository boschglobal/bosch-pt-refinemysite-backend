/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource

import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.TranslationResource
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft

class MultilingualCraftResource(
    craft: Craft,
    translationResources: Set<TranslationResource>,
    createByName: String,
    lastModifiedByName: String
) : AbstractAuditableResource(craft, createByName, lastModifiedByName) {

  val defaultName: String? = craft.defaultName
  val translations: Set<TranslationResource> = translationResources
}
