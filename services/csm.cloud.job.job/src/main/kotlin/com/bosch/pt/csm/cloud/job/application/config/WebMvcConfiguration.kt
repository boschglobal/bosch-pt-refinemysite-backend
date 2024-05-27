/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.job.application.config

import com.bosch.pt.csm.cloud.common.config.ExceptionMessageHideFilter
import com.bosch.pt.csm.cloud.common.config.MdcInsertingRequestParametersFilter
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionRequestMappingHandlerMapping
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLocaleResolver
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETagArgumentResolver
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
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.filter.ForwardedHeaderFilter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Configuration
class WebMvcConfiguration(
    private val apiVersionProperties: ApiVersionProperties,
    @Value("\${locale.default}") private val defaultLocale: Locale,
    @Value("\${locale.supported}") private val supportedLocales: List<Locale>
) : WebMvcConfigurer {

  @Bean
  fun forwardedHeaderFilter() =
      FilterRegistrationBean(ForwardedHeaderFilter()).apply { order = HIGHEST_PRECEDENCE }

  @Bean
  fun mdcFilter() =
      FilterRegistrationBean(MdcInsertingRequestParametersFilter()).apply {
        order = TRACING_FILTER_ORDER + 1
      }

  @Bean
  fun exceptionMessageHideFilter(
      messageSource: MessageSource
  ): FilterRegistrationBean<ExceptionMessageHideFilter> =
      FilterRegistrationBean(ExceptionMessageHideFilter(messageSource)).apply {
        order = TRACING_FILTER_ORDER + 2
      }

  @Bean
  @Profile("log-requests")
  @Order(2)
  fun logFilter(): CommonsRequestLoggingFilter =
      CommonsRequestLoggingFilter().apply {
        setIncludeQueryString(true)
        setIncludePayload(true)
        setIncludeHeaders(true)
        setMaxPayloadLength(10000)
      }

  @Bean
  fun localeResolver(): LocaleResolver =
      CustomLocaleResolver().apply {
        this.setDefaultLocale(this@WebMvcConfiguration.defaultLocale)
        this.supportedLocales = this@WebMvcConfiguration.supportedLocales
      }

  @Bean
  fun webMvcRegistrations(): WebMvcRegistrations =
      object : WebMvcRegistrations {
        override fun getRequestMappingHandlerMapping(): RequestMappingHandlerMapping =
            ApiVersionRequestMappingHandlerMapping(apiVersionProperties)
      }

  override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
    resolvers.add(ETagArgumentResolver())
    resolvers.add(AuthenticationPrincipalArgumentResolver())
  }

  companion object {
    const val TRACING_FILTER_ORDER = HIGHEST_PRECEDENCE + 5
  }
}
