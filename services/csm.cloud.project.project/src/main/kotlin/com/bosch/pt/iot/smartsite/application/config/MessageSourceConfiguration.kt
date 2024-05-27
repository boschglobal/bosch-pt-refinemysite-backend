package com.bosch.pt.iot.smartsite.application.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

@Configuration
open class MessageSourceConfiguration {

  @Bean
  open fun messageSource(): MessageSource =
      ResourceBundleMessageSource().apply {
        setBasenames("i18n/messages", "i18n/common/messages")
        setDefaultEncoding("UTF-8")
      }
}
