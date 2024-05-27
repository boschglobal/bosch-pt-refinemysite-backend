/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.CraftController.Companion.CRAFTS_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.CraftListResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.CraftTranslationProjection
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class CraftListResourceFactory(
    private val craftResourceFactory: CraftResourceFactory,
    private val linkFactory: CustomLinkBuilderFactory
) {

  @PageLinks
  fun build(craftPage: Page<CraftTranslationProjection>): CraftListResource {
    val craftResources = craftPage.content.map { craftResourceFactory.build(it) }
    val resource = CraftListResource(craftResources, craftPage)

    resource.add(linkFactory.linkTo(CRAFTS_ENDPOINT_PATH).withSelfRel())

    return resource
  }
}
