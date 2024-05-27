/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest

import java.lang.reflect.Method
import org.springframework.core.annotation.AnnotationUtils.findAnnotation
import org.springframework.web.servlet.mvc.condition.RequestCondition
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.pattern.PathPatternParser

class ApiVersionRequestMappingHandlerMapping(private val properties: ApiVersionProperties) :
    RequestMappingHandlerMapping() {

  override fun getMappingForMethod(method: Method, handlerType: Class<*>): RequestMappingInfo? {
    val info = super.getMappingForMethod(method, handlerType) ?: return null

    // Create mapping if ApiVersion annotation is defined on the specified method
    findAnnotation(method, ApiVersion::class.java)?.apply {
      // Concatenate our ApiVersion with the usual request mapping
      return createApiVersionInfo(method, this, getCustomMethodCondition(method)).combine(info)
    }

    // Create mapping if ApiVersion annotation is defined on the controller class
    findAnnotation(handlerType, ApiVersion::class.java)?.apply {
      // Concatenate our ApiVersion with the usual request mapping
      return createApiVersionInfo(method, this, getCustomTypeCondition(handlerType)).combine(info)
    }

    // Handle normally without API versioning, i.e. map URLs directly
    return super.getMappingForMethod(method, handlerType)
  }

  private fun createApiVersionInfo(
      method: Method,
      annotation: ApiVersion,
      customCondition: RequestCondition<*>?
  ): RequestMappingInfo {

    // Get specified API versions for the endpoint
    var from = annotation.from
    var to = annotation.to

    // If no min version is defined, set to min api version
    if (from == 0) {
      from = properties.version.min
    }

    // If no limit is defined, set to max api version
    if (to == 0) {
      to = properties.version.max
    }

    // Calculate URL version prefixes to map for the given endpoint
    val urlVersionPrefixes =
        createUrlPrefixArray(method, from, to, properties.version.min, properties.version.max)
    var index = 0
    var version = from
    while (version <= to) {
      urlVersionPrefixes[index] = properties.version.prefix + version
      version++
      index++
    }

    // Create the url mapping
    return RequestMappingInfo.paths(*urlVersionPrefixes)
        .apply {
          if (customCondition != null) {
            this.customCondition(customCondition)
          }
          this.options(
              RequestMappingInfo.BuilderConfiguration().apply {
                patternParser = PathPatternParser()
              })
        }
        .build()
  }

  private fun createUrlPrefixArray(
      method: Method,
      from: Int,
      to: Int,
      minVersion: Int,
      maxApiVersion: Int
  ): Array<String?> {
    require(from >= minVersion) {
      "From API version is older than the min version for method: $method"
    }
    require(to <= maxApiVersion) { "Invalid To API version for method: $method defined" }
    require(from <= to) { "From/To API versions are mixed up for method: $method defined" }
    return arrayOfNulls(to - from + 1)
  }
}
