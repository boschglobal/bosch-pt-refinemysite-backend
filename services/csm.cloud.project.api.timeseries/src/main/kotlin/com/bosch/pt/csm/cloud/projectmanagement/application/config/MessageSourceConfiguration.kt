/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

@Configuration
class MessageSourceConfiguration {

  @Bean
  fun messageSource() =
      ResourceBundleMessageSource().apply {
        setBasenames("i18n/messages", "i18n/common/messages")
        setDefaultEncoding("UTF-8")
      }
}
