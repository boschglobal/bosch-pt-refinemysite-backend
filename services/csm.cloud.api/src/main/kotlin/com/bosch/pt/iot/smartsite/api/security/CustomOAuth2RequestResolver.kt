/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import com.bosch.pt.iot.smartsite.api.security.config.IdentityProviderConfigurationProperties
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * This class is required during the login flow. When /login is called on the API Gateway the
 * gateway forwards from /login to the /oauth2/authorization/{registrationId}. This resolver then
 * kicks-is to add the style id (the information used to pick the correct background picture at the
 * IdP UI) to the request being sent to the IdP.
 */
class CustomOAuth2RequestResolver(
    clientRepository: ReactiveClientRegistrationRepository,
    private val properties: IdentityProviderConfigurationProperties
) : ServerOAuth2AuthorizationRequestResolver {

  private val authorizationRequestMatcher: ServerWebExchangeMatcher =
      PathPatternParserServerWebExchangeMatcher("/oauth2/authorization/{registrationId}")

  private val defaultResolver: DefaultServerOAuth2AuthorizationRequestResolver =
      DefaultServerOAuth2AuthorizationRequestResolver(clientRepository, authorizationRequestMatcher)

  override fun resolve(exchange: ServerWebExchange): Mono<OAuth2AuthorizationRequest> =
      defaultResolver.resolve(exchange).map(this::addStyleId)

  override fun resolve(
      exchange: ServerWebExchange,
      clientRegistrationId: String
  ): Mono<OAuth2AuthorizationRequest> =
      defaultResolver.resolve(exchange, clientRegistrationId).map(this::addStyleId)

  private fun addStyleId(request: OAuth2AuthorizationRequest): OAuth2AuthorizationRequest =
      OAuth2AuthorizationRequest.from(request)
          .additionalParameters(mapOf(properties.styleIdParameter to properties.styleId))
          .build()
}
