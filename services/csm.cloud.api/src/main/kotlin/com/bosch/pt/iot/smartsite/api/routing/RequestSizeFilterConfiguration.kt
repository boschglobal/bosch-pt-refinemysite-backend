/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.routing

import com.bosch.pt.iot.smartsite.api.routing.request.config.RequestFilterProperties
import com.bosch.pt.iot.smartsite.api.routing.request.filter.GlobalRequestSizeFilter
import com.bosch.pt.iot.smartsite.api.routing.request.matcher.LargeRequestServerWebExchangeMatcher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.RequestSizeGatewayFilterFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.unit.DataSize

@Configuration
@EnableConfigurationProperties(RequestFilterProperties::class)
class RequestSizeFilterConfiguration(
    private val requestFilterProperties: RequestFilterProperties,
    private val requestSizeGatewayFilterFactory: RequestSizeGatewayFilterFactory
) {

  @Bean
  fun largeRequestServerWebExchangeMatcher(): LargeRequestServerWebExchangeMatcher =
      LargeRequestServerWebExchangeMatcher(requireNotNull(requestFilterProperties.large.paths))

  @Bean
  fun globalRequestSizeFilter(): GlobalRequestSizeFilter =
      GlobalRequestSizeFilter(
          largeRequestSizeFilter(),
          defaultRequestSizeFilter(),
          largeRequestServerWebExchangeMatcher())

  @Bean
  @Qualifier("defaultRequestSizeFilter")
  fun defaultRequestSizeFilter(): GatewayFilter =
      requestSizeGatewayFilterFactory.apply(
          requestSizeGatewayFilterFactory.newConfig().apply {
            maxSize = DataSize.ofMegabytes(requestFilterProperties.default.maxSizeInMb)
          })

  @Bean
  @Qualifier("largeRequestSizeFilter")
  fun largeRequestSizeFilter(): GatewayFilter =
      requestSizeGatewayFilterFactory.apply(
          requestSizeGatewayFilterFactory.newConfig().apply {
            maxSize = DataSize.ofMegabytes(requestFilterProperties.large.maxSizeInMb)
          })
}
