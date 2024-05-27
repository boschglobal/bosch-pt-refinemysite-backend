/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.routing

import org.springdoc.core.properties.SwaggerUiConfigParameters
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springdoc.core.properties.SwaggerUiOAuthProperties
import org.springdoc.core.providers.ObjectMapperProvider
import org.springdoc.webflux.ui.SwaggerIndexPageTransformer
import org.springdoc.webflux.ui.SwaggerWelcomeCommon
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.web.reactive.resource.ResourceTransformerChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Configuration
class CustomSwaggerIndexTransformerConfiguration {

  @Bean
  fun customSwaggerIndexTransformer(
      swaggerUiConfig: SwaggerUiConfigProperties,
      swaggerUiOAuthProperties: SwaggerUiOAuthProperties,
      swaggerUiConfigParameters: SwaggerUiConfigParameters,
      swaggerWelcomeCommon: SwaggerWelcomeCommon,
      objectMapperProvider: ObjectMapperProvider
  ) =
      CustomSwaggerIndexTransformer(
          swaggerUiConfig,
          swaggerUiOAuthProperties,
          swaggerUiConfigParameters,
          swaggerWelcomeCommon,
          objectMapperProvider)
}

class CustomSwaggerIndexTransformer(
    swaggerUiConfig: SwaggerUiConfigProperties,
    swaggerUiOAuthProperties: SwaggerUiOAuthProperties,
    swaggerUiConfigParameters: SwaggerUiConfigParameters,
    swaggerWelcomeCommon: SwaggerWelcomeCommon,
    objectMapperProvider: ObjectMapperProvider
) :
    SwaggerIndexPageTransformer(
        swaggerUiConfig,
        swaggerUiOAuthProperties,
        swaggerUiConfigParameters,
        swaggerWelcomeCommon,
        objectMapperProvider) {

  override fun transform(
      serverWebExchange: ServerWebExchange,
      resource: Resource,
      resourceTransformerChain: ResourceTransformerChain
  ): Mono<Resource> {
    if (serverWebExchange.request.path.toString().contains("/swagger-ui")) {
      serverWebExchange.response.headers.add(
          "Content-Security-Policy",
          "default-src 'self' *.bosch-refinemysite.com *.key-cloak-one.com; " +
              "script-src 'self' 'unsafe-inline'; " +
              "style-src 'self'; " +
              "img-src 'self' blob: data:; " +
              "font-src 'self' data:; " +
              "frame-ancestors 'none';")
    }
    return super.transform(serverWebExchange, resource, resourceTransformerChain)
  }
}
