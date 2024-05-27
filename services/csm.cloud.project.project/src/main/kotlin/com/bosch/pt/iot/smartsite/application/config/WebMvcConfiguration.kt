/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.csm.cloud.common.config.DisableVersionedRestRequestsFilter
import com.bosch.pt.csm.cloud.common.config.MdcInsertingRequestParametersFilter
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionRequestMappingHandlerMapping
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLocaleResolver
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETagArgumentResolver
import com.bosch.pt.iot.smartsite.common.businesstransaction.facade.rest.BusinessTransactionCleanupFilter
import java.util.Locale
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.filter.ForwardedHeaderFilter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Configuration
open class WebMvcConfiguration(
    private val apiVersionProperties: ApiVersionProperties,
    @Value("\${locale.default}") private val defaultLocale: Locale,
    @Value("\${locale.supported}") private val supportedLocales: List<Locale>
) : WebMvcConfigurer {

  @Bean
  open fun businessTransactionCleanupFilter() =
      FilterRegistrationBean(BusinessTransactionCleanupFilter()).apply {
        order = Ordered.LOWEST_PRECEDENCE
      }

  @Profile("restore-db")
  @Bean
  open fun disableVersionedRestRequestsFilter() = DisableVersionedRestRequestsFilter()

  @Bean
  open fun forwardedHeaderFilter() =
      FilterRegistrationBean(ForwardedHeaderFilter()).apply { order = Ordered.HIGHEST_PRECEDENCE }

  @Bean
  open fun mdcFilter() =
      FilterRegistrationBean(MdcInsertingRequestParametersFilter()).apply {
        order = TRACING_FILTER_ORDER + 1
      }

  @Bean
  @Profile("log-requests")
  @Order(2)
  open fun logFilter(): CommonsRequestLoggingFilter =
      CommonsRequestLoggingFilter().apply {
        setIncludeQueryString(true)
        setIncludePayload(true)
        setIncludeHeaders(true)
        setMaxPayloadLength(10000)
      }

  @Bean
  open fun localeResolver(): LocaleResolver =
      CustomLocaleResolver().apply {
        this.setDefaultLocale(this@WebMvcConfiguration.defaultLocale)
        this.supportedLocales = this@WebMvcConfiguration.supportedLocales
      }

  @Bean
  open fun webMvcRegistrations(): WebMvcRegistrations =
      object : WebMvcRegistrations {
        override fun getRequestMappingHandlerMapping(): RequestMappingHandlerMapping =
            ApiVersionRequestMappingHandlerMapping(apiVersionProperties)
      }

  override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
    resolvers.add(ETagArgumentResolver())
  }

  companion object {
    const val TRACING_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 5
  }
}
