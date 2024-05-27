/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.util

import org.springframework.hateoas.IanaLinkRelations.SELF
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.LinkDescriptor

object RestDocsUtils {

  /**
   * Get http headers object with a set of default headers to be used in REST API documentation.
   *
   * @return http headers object
   */
  fun defaultHeaders(): HttpHeaders =
      HttpHeaders().apply {
        add(ACCEPT, HAL_JSON_VALUE)
        add(ACCEPT_LANGUAGE, "en")
        add(AUTHORIZATION, "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dC...")
      }

  /**
   * Returns link descriptor for self link
   *
   * @return the link descriptor
   */
  fun selfLink(): LinkDescriptor =
      linkWithRel(SELF.value()).description("Link to the resource itself")
}
