/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.translation.facade.rest.resource.response.assembler.ProjectTranslationsResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.TranslationResource
import com.bosch.pt.csm.cloud.projectmanagement.user.translation.facade.rest.resource.response.assembler.UserTranslationsResourceAssembler
import org.springframework.stereotype.Component

@Component
class TranslationResourceAssembler(
    private val projectTranslationsResourceAssembler: ProjectTranslationsResourceAssembler,
    private val userTranslationsResourceAssembler: UserTranslationsResourceAssembler
) {

  fun assemble(): List<TranslationResource> =
      (projectTranslationsResourceAssembler.assemble() +
              userTranslationsResourceAssembler.assemble())
          .sortedWith(compareBy(TranslationResource::key, TranslationResource::language))
}
