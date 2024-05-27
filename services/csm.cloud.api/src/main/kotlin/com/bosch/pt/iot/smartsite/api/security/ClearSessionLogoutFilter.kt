/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import com.bosch.pt.iot.smartsite.api.security.RedirectConstants.REDIRECT_SESSION_ATTRIBUTE_NAME
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.FOUND
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class ClearSessionLogoutFilter(
    routePrefix: String,
    private val redirectUrlValidationService: RedirectUrlValidationService,
) : WebFilter {

  private val logoutMatcher = ServerWebExchangeMatchers.pathMatchers("$routePrefix/logout/redirect")

  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> =
      logoutMatcher
          .matches(exchange)
          .filter { it.isMatch }
          .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
          .flatMap { clearSessionAndRedirect(exchange) }

  private fun clearSessionAndRedirect(
      exchange: ServerWebExchange,
  ): Mono<Void> =
      exchange.session.flatMap { session ->
        session.invalidate().also {
          val redirectString = session.attributes[REDIRECT_SESSION_ATTRIBUTE_NAME] as String?

          requireNotNull(redirectString) {
            "RedirectUrl not provided".also {
              LOGGER.warn(
                  "No redirectUri in in session, session authenticated?:{}",
                  session.attributes.containsKey("SPRING_SECURITY_CONTEXT"))
            }
          }

          val redirectUri = redirectUrlValidationService.decodeRedirectString(redirectString)

          require(redirectUrlValidationService.isRedirectAllowed(redirectUri)) {
            "RedirectURL not whitelisted".also {
              LOGGER.warn(
                  "Redirecting to requested redirectUri {} not allowed", redirectUri.toString())
            }
          }
          exchange.response.statusCode = FOUND
          exchange.response.headers.location = redirectUri
        }
      }

  companion object {
    val LOGGER: Logger = LoggerFactory.getLogger(ClearSessionLogoutFilter::class.java)
  }
}
