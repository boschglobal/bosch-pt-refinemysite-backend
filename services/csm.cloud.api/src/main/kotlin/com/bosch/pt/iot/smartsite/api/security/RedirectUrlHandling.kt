/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import java.net.URI
import java.util.Base64
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class RedirectUrlValidationService(private val redirectWhitelist: RedirectWhitelist) {

  fun isRedirectAllowed(
      redirectUri: URI,
  ): Boolean = redirectWhitelist.whitelisted.contains(redirectUri.withoutPathAndQueryParams())

  fun decodeRedirectString(redirectString: String): URI =
      URI.create(String(Base64.getUrlDecoder().decode(redirectString)).trim())

  private fun URI.withoutPathAndQueryParams() =
      UriComponentsBuilder.fromUri(this).replacePath(null).replaceQuery(null).build().toUri()
}

object RedirectConstants {
  const val REDIRECT_URL_PARAMETER = "redirect_url"
  const val REDIRECT_SESSION_ATTRIBUTE_NAME = "postLogoutRedirectUri"
}

@Configuration
@EnableConfigurationProperties(RedirectWhitelist::class)
class RedirectWhitelistConfiguration

@ConfigurationProperties(prefix = "custom.redirects")
data class RedirectWhitelist(val whitelisted: List<URI>)
