/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest

import org.springframework.hateoas.Link
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentServletMapping
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder

object LinkUtils {

  /**
   * Creates a [UriComponentsBuilder] with given path segments for the current servlet mapping.
   *
   * @param segments path segments
   * @return uri components builder
   */
  fun linkTemplateWithPathSegments(vararg segments: String): UriComponentsBuilder =
      fromCurrentServletMapping().pathSegment(*segments)

  /**
   * Creates a [Link] from [UriComponents] representing the links URL.
   *
   * @param components URI components
   * @param rel link relation
   * @return new [Link]
   */
  fun linkFromUriComponents(components: UriComponents, rel: String): Link =
      Link.of(components.toUriString(), rel)
}
