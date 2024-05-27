/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest

import java.net.URI
import org.springframework.lang.Nullable
import org.springframework.util.Assert
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder

object CustomUriComponentsBuilderFactory {

  private val CACHE_KEY = CustomUriComponentsBuilderFactory::class.java.name + "#BUILDER_CACHE" //

  /**
   * Returns a [UriComponentsBuilder] obtained from the current servlet mapping with scheme tweaked
   * in case the request contains an `X-Forwarded-Ssl` header. If no [ ] exists (you're outside a
   * Spring Web call), fall back to relative URIs.
   *
   * @return the builder
   */
  @JvmStatic
  fun getBuilder(): UriComponentsBuilder {
    if (RequestContextHolder.getRequestAttributes() == null) {
      return UriComponentsBuilder.fromPath("/")
    }
    val baseUri = getCachedBaseUri()
    return if (baseUri != null //
    ) UriComponentsBuilder.fromUri(baseUri)
    else cacheBaseUri(ServletUriComponentsBuilder.fromCurrentServletMapping())
  }

  @JvmStatic fun getComponents(): UriComponents = getBuilder().build()

  private fun getRequestAttributes(): RequestAttributes {
    val requestAttributes =
        RequestContextHolder.getRequestAttributes() ?: error("Could not look up RequestAttributes!")
    Assert.isInstanceOf(ServletRequestAttributes::class.java, requestAttributes)
    return requestAttributes
  }

  private fun cacheBaseUri(builder: UriComponentsBuilder): UriComponentsBuilder {
    val uri = builder.build().toUri()
    getRequestAttributes().setAttribute(CACHE_KEY, uri, SCOPE_REQUEST)
    return builder
  }

  @Nullable
  private fun getCachedBaseUri(): URI? =
      getRequestAttributes().getAttribute(CACHE_KEY, SCOPE_REQUEST) as URI?
}
