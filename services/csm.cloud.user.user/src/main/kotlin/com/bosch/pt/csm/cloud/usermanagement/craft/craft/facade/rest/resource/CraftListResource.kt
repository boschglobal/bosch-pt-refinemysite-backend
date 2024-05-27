/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractPageResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.CraftTranslationProjection
import org.springframework.data.domain.Page

class CraftListResource(
    val crafts: Collection<CraftResource?>,
    craftPage: Page<CraftTranslationProjection>,
) :
    AbstractPageResource(
        craftPage.number, craftPage.size, craftPage.totalPages, craftPage.totalElements)
