/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.TranslationListResource
import com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.assembler.TranslationListResourceAssembler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class TranslationRestController(
    private val translationListResourceAssembler: TranslationListResourceAssembler
) {

  @GetMapping("/translations")
  fun getTranslations(): TranslationListResource = translationListResourceAssembler.assemble()
}
