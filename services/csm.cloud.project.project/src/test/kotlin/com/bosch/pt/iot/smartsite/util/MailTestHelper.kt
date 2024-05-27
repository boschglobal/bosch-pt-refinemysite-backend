/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.util

import com.bosch.pt.iot.smartsite.application.config.MailProperties
import org.springframework.stereotype.Component

@Component
class MailTestHelper(private val mailProperties: MailProperties) {

  fun findAllTemplateIds(templateName: String): Set<Long> {
    val template =
        mailProperties.templates[templateName]
            ?: throw IllegalArgumentException("No template found for name $templateName")
    return setOf(template.default) + template.countries.values.toSet()
  }
}
