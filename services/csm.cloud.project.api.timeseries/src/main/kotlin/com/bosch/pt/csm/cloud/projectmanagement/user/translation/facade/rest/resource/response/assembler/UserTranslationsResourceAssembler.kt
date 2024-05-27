/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.translation.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.common.utils.toLanguage
import com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.TranslationResource
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.PhoneNumberTypeEnum
import java.util.Locale
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class UserTranslationsResourceAssembler(
    private val messageSource: MessageSource,
    @Value("\${locale.supported}") private val supportedLocales: List<Locale>
) {

  fun assemble(): List<TranslationResource> = PhoneNumberTypeEnum.values().flatMap(::translate)

  private fun translate(enumType: TranslatableEnum): List<TranslationResource> =
      supportedLocales.map { locale ->
        TranslationResource(
            enumType.key,
            locale.toLanguage(),
            messageSource.getMessage(enumType.messageKey, emptyArray(), locale))
      }
}
