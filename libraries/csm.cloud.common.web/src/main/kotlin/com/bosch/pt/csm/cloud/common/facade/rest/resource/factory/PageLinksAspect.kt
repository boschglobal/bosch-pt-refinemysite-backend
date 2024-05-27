/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest.resource.factory

import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.hateoas.Link
import org.springframework.hateoas.RepresentationModel
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.util.UriComponentsBuilder

/**
 * This aspect applies to methods with the [PageLinks] annotation and implements their behaviour.
 */
@Aspect
@Component
class PageLinksAspect {

  /**
   * This advice intercepts methods annotated with [PageLinks] that return a [ ] and take a [Slice]
   * as first argument. The call to the original method will be performed in the same way but the
   * return type will be extended by page links (prev, next).
   *
   * @param joinPoint AspectJ join point
   * @param slice the slice whose links shall be added to the resource
   * @return The extended resource with additional links if appropriate
   * @throws Throwable not expected
   */
  @Around(
      "@annotation(com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks) " +
          "&& execution(org.springframework.hateoas.RepresentationModel+ *(..)) && args(slice, ..)")
  fun pageLinksAdvice(joinPoint: ProceedingJoinPoint, slice: Slice<*>): Any {

    // make regular call to the annotated method and obtain result
    val listResource = joinPoint.proceed() as RepresentationModel<*>

    // Obtain original HTTP request and rebuild the request URI
    val requestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?
    if (requestAttributes != null) {
      val request = requestAttributes.request
      val originalUri = originalUri(request)

      // add links if appropriate
      if (slice.hasNext()) {
        val nextUri = replacePageParams(originalUri, slice.nextPageable())
        listResource.add(Link.of(nextUri.toUriString()).withRel("next"))
      }
      if (slice.hasPrevious()) {
        val prevUri = replacePageParams(originalUri, slice.previousPageable())
        listResource.add(Link.of(prevUri.toUriString()).withRel("prev"))
      }
    }

    return listResource
  }

  /**
   * Uses controller and original request to rebuild the original request URI.
   *
   * @param request the original HTTP request
   * @return an uri builder that represents the original URI
   */
  private fun originalUri(request: HttpServletRequest): UriComponentsBuilder =
      ServletUriComponentsBuilder.fromServletMapping(request).path(request.requestURI).apply {
        // add parameters
        for ((key, value) in request.parameterMap) {
          for (queryParam in value) {
            queryParam(key, queryParam)
          }
        }
      }

  /**
   * Takes an uri builder and returns a new one with parameters for the specified pageable.
   *
   * @param original original URI
   * @param pageable the pageable to request
   * @return a URI representing the request of the specified pageable
   */
  private fun replacePageParams(
      original: UriComponentsBuilder,
      pageable: Pageable
  ): UriComponentsBuilder =
      original.cloneBuilder().apply {
        replaceQueryParam("page", pageable.pageNumber)
        replaceQueryParam("size", pageable.pageSize)
      }
}
