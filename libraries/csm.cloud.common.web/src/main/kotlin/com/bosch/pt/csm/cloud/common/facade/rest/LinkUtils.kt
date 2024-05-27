/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest

import java.util.regex.Pattern
import org.springframework.hateoas.Link
import org.springframework.util.Assert
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder

object LinkUtils {

  private val pattern = Pattern.compile(".*/v([1-9][0-9]*)/.*")

  /**
   * Creates a [UriComponentsBuilder] with given path segments for the current servlet mapping
   * including the given API version.
   *
   * @param apiVersion the desired API version
   * @param segments path segments
   * @return uri components builder
   */
  @JvmStatic
  fun linkTemplateWithPathSegments(
      apiVersion: Int,
      vararg segments: String?
  ): UriComponentsBuilder =
      ServletUriComponentsBuilder.fromCurrentServletMapping()
          .pathSegment("v$apiVersion")
          .pathSegment(*segments)

  /**
   * Creates a [UriComponentsBuilder] with given path segments for the current servlet mapping
   * including the API version from the current request.
   *
   * @param segments path segments
   * @return uri components builder
   */
  @JvmStatic
  fun linkTemplateWithPathSegments(vararg segments: String?): UriComponentsBuilder =
      linkTemplateWithPathSegments(getCurrentApiVersion(), *segments)

  /**
   * Creates a [UriComponentsBuilder] with given path segments for the current servlet mapping.
   *
   * @param segments path segments
   * @return uri components builder
   */
  @JvmStatic
  fun linkTemplateWithPathSegmentsUnversioned(vararg segments: String?): UriComponentsBuilder =
      ServletUriComponentsBuilder.fromCurrentServletMapping().pathSegment(*segments)

  /**
   * Creates a [Link] from [UriComponents] representing the links URL.
   *
   * @param components URI components
   * @param rel link relation
   * @return new [Link]
   */
  @JvmStatic
  fun linkFromUriComponents(components: UriComponents, rel: String): Link =
      Link.of(components.toUriString(), rel)

  @JvmStatic fun getCurrentApiVersionPrefix(): String = "/v" + getCurrentApiVersion()

  @JvmStatic
  fun getCurrentApiVersion(): Int {
    val attrs = RequestContextHolder.getRequestAttributes()
    Assert.state(attrs is ServletRequestAttributes, "No current ServletRequestAttributes")
    val request = (attrs as ServletRequestAttributes).request
    val version = extractApiVersion(request.requestURI)
    return if (version > 0) {
      version
    } else {
      error("No version information found in url path")
    }
  }

  @JvmStatic
  fun extractApiVersion(requestUri: String?): Int {
    val matcher = pattern.matcher(requestUri)
    return if (matcher.find()) {
      matcher.group(1).toInt()
    } else -1
  }
}
