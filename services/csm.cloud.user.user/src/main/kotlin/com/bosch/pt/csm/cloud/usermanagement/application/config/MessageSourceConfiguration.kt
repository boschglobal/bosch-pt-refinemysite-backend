/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

@Configuration
class MessageSourceConfiguration {

  @Bean
  fun messageSource(): MessageSource =
      ResourceBundleMessageSource().apply {
        setBasenames("i18n/messages", "i18n/common/messages")
        setDefaultEncoding("UTF-8")
      }
}
