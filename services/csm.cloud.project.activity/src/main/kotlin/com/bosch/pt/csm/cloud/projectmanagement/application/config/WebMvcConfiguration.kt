/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.config.ExceptionMessageHideFilter
import com.bosch.pt.csm.cloud.common.config.MdcInsertingRequestParametersFilter
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionRequestMappingHandlerMapping
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLocaleResolver
import java.util.Locale
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.filter.ForwardedHeaderFilter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Configuration
class WebMvcConfiguration(
    private val apiVersionProperties: ApiVersionProperties,
    @Value("\${locale.default}") private val defaultLocale: Locale,
    @Value("\${locale.supported}") private val supportedLocales: List<Locale>
) {

  @Bean
  fun forwardedHeaderFilter() =
      FilterRegistrationBean(ForwardedHeaderFilter()).apply {
        filter = ForwardedHeaderFilter()
        order = HIGHEST_PRECEDENCE
      }

  @Bean
  fun mdcFilter() =
      FilterRegistrationBean(MdcInsertingRequestParametersFilter()).apply {
        // Must be after the Tracing Filter whose order number is 5 but cannot be referenced
        order = HIGHEST_PRECEDENCE + 6
      }

  @Bean
  fun exceptionMessageHideFilter(messageSource: MessageSource) =
      FilterRegistrationBean(ExceptionMessageHideFilter(messageSource)).apply {
        order = HIGHEST_PRECEDENCE + 7
      }

  @Bean
  @Profile("log-requests")
  @Order(2)
  fun logFilter() =
      CommonsRequestLoggingFilter().apply {
        setIncludeQueryString(true)
        setIncludePayload(true)
        setMaxPayloadLength(10000)
        setIncludeHeaders(false)
        setAfterMessagePrefix("REQUEST DATA : ")
      }

  @Bean
  fun localeResolver() =
      CustomLocaleResolver().apply {
        this.setDefaultLocale(this@WebMvcConfiguration.defaultLocale)
        supportedLocales = this@WebMvcConfiguration.supportedLocales
      }

  @Bean
  fun webMvcRegistrations(): WebMvcRegistrations? =
      object : WebMvcRegistrations {
        override fun getRequestMappingHandlerMapping(): RequestMappingHandlerMapping =
            ApiVersionRequestMappingHandlerMapping(this@WebMvcConfiguration.apiVersionProperties)
      }
}
