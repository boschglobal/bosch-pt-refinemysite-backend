/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import java.lang.reflect.Method
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.condition.RequestCondition
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.pattern.PathPatternParser

class CustomApiVersionRequestMappingHandlerMapping(private val properties: ApiVersionProperties) :
    RequestMappingHandlerMapping() {

  override fun getMappingForMethod(method: Method, handlerType: Class<*>): RequestMappingInfo? {
    val info = super.getMappingForMethod(method, handlerType) ?: return null

    // Create mapping if ApiVersion annotation is defined on the specified method
    AnnotationUtils.findAnnotation(method, ApiVersion::class.java)?.apply {
      // Concatenate our ApiVersion with the usual request mapping
      return createApiVersionInfo(method, this, getCustomMethodCondition(method)).combine(info)
    }

    // Create mapping if ApiVersion annotation is defined on the controller class
    AnnotationUtils.findAnnotation(handlerType, ApiVersion::class.java)?.apply {
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
      val path: String? = getPath(method)
      from =
          if (path != null) {
            if (path in listOf("", "/")) {
              properties.authenticationStatus.min
            } else if (path.startsWith("/projects")) {
              properties.project.min
            } else if (path.startsWith("/company")) {
              properties.company.min
            } else if (path.startsWith("/users")) {
              properties.user.min
            } else if (path.startsWith("/translation")) {
              properties.translation.min
            } else {
              properties.unknown.min
            }
          } else {
            properties.unknown.min
          }
    }

    // If no limit is defined, set to max api version
    if (to == 0) {
      val path: String? = getPath(method)
      to =
          if (path != null) {
            if (path in listOf("", "/")) {
              properties.authenticationStatus.max
            } else if (path.startsWith("/projects")) {
              properties.project.max
            } else if (path.startsWith("/companies")) {
              properties.company.max
            } else if (path.startsWith("/users")) {
              properties.user.max
            } else if (path.startsWith("/translations")) {
              properties.translation.max
            } else {
              properties.unknown.max
            }
          } else {
            properties.unknown.max
          }
    }

    // Calculate URL version prefixes to map for the given endpoint
    val urlVersionPrefixes = createUrlPrefixArray(method, from, to)
    var index = 0
    var version = from
    while (version <= to) {
      urlVersionPrefixes[index] = "/v$version"
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

  private fun createUrlPrefixArray(method: Method, from: Int, to: Int): Array<String?> =
      if (to < from) {
        throw IllegalArgumentException(
            "From/To API versions are mixed up for method: $method defined")
      } else {
        arrayOfNulls(to - from + 1)
      }

  private fun getPath(method: Method): String? {
    var path: String? = null
    if (method.isAnnotationPresent(RequestMapping::class.java)) {
      val annotation = method.getDeclaredAnnotation(RequestMapping::class.java)
      path = annotation?.path?.firstOrNull() ?: annotation?.value?.firstOrNull()
    } else if (method.isAnnotationPresent(GetMapping::class.java)) {
      val annotation = method.getDeclaredAnnotation(GetMapping::class.java)
      path = annotation?.path?.firstOrNull() ?: annotation?.value?.firstOrNull()
    } else if (method.isAnnotationPresent(PostMapping::class.java)) {
      val annotation = method.getDeclaredAnnotation(PostMapping::class.java)
      path = annotation?.path?.firstOrNull() ?: annotation?.value?.firstOrNull()
    } else if (method.isAnnotationPresent(PutMapping::class.java)) {
      val annotation = method.getDeclaredAnnotation(PutMapping::class.java)
      path = annotation?.path?.firstOrNull() ?: annotation?.value?.firstOrNull()
    } else if (method.isAnnotationPresent(DeleteMapping::class.java)) {
      val annotation = method.getDeclaredAnnotation(DeleteMapping::class.java)
      path = annotation?.path?.firstOrNull() ?: annotation?.value?.firstOrNull()
    }
    return path
  }
}
