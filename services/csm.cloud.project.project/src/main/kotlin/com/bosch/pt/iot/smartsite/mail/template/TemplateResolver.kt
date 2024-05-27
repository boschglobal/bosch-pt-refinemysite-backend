/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.mail.template

import com.bosch.pt.iot.smartsite.application.config.MailProperties
import org.springframework.stereotype.Component

@Component
class TemplateResolver(private val properties: MailProperties) {

  fun getMailjetTemplateId(template: MailTemplate, countryCode: String?): Long {
    val templateName = template.templateName
    val templateProperties =
        properties.templates[templateName]
            ?: throw IllegalArgumentException(
                "Could not find configuration for template name: $templateName")

    return templateProperties.countries[countryCode] ?: templateProperties.default
  }
}
