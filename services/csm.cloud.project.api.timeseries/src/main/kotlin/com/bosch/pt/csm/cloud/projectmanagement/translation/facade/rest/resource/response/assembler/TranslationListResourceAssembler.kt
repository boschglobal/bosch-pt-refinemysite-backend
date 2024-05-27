/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.TranslationListResource
import org.springframework.stereotype.Component

@Component
class TranslationListResourceAssembler(
    private val translationResourceAssembler: TranslationResourceAssembler
) {

  fun assemble(): TranslationListResource =
      TranslationListResource(translationResourceAssembler.assemble())
}
