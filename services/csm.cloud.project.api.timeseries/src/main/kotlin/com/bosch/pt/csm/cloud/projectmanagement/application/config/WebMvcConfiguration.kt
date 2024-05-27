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
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLocaleResolver
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Configuration
class WebMvcConfiguration(
    private val apiVersionProperties: ApiVersionProperties,
    @Value("\${locale.default}") private val defaultLocale: Locale,
    @Value("\${locale.supported}") private val supportedLocales: List<Locale>
) {

  @Bean
  fun forwardedHeaderFilter() =
      FilterRegistrationBean(ForwardedHeaderFilter()).apply { order = HIGHEST_PRECEDENCE }

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
            CustomApiVersionRequestMappingHandlerMapping(apiVersionProperties)
      }

  @Bean
  fun webMvcConfigurer(): WebMvcConfigurer =
      object : WebMvcConfigurer {
        override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
          registry
              .addResourceHandler("/graphiql/resources/**")
              .addResourceLocations("classpath:/graphiql/resources/")
        }

        override fun addInterceptors(registry: InterceptorRegistry) {
          registry.addInterceptor(
              object : HandlerInterceptor {

                override fun preHandle(
                    request: HttpServletRequest,
                    response: HttpServletResponse,
                    handler: Any
                ): Boolean {
                  if (request.requestURI.contains("/graphiql")) {
                    response.setHeader(
                        "Content-Security-Policy",
                        "default-src 'self' *.bosch-refinemysite.com; " +
                            "script-src 'self'; " +
                            "style-src 'self'; " +
                            "img-src 'self' blob: data:; " +
                            "font-src 'self' data:; " +
                            "frame-ancestors 'none';")
                  }
                  return super.preHandle(request, response, handler)
                }
              })
        }
      }
}
