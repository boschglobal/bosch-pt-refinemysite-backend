/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import com.bosch.pt.iot.smartsite.api.security.Base64Utils.encodeUrlSafe
import com.bosch.pt.iot.smartsite.api.security.config.IdentityProviderConfigurationProperties
import java.nio.charset.StandardCharsets.US_ASCII
import java.security.MessageDigest
import java.util.Base64
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REGISTRATION_ID
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.NONCE
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

/**
 * This service generates OAuth2AuthorizationRequests as well as stores and encodes those. The
 * requests are needed by the IdP when registering a new user (signup) or changing the password. The
 * request is stored in order to retrieve it again when the user activates the account by following
 * a link from the registration email the user received.
 */
@Service
class AuthorizationRequestService(
    private val identityProviderConfigurationProperties: IdentityProviderConfigurationProperties,
) {

  private val authorizationRequestRepository =
      WebSessionOAuth2ServerAuthorizationRequestRepository()

  private val stateGenerator: StringKeyGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder())
  private val secureKeyGenerator: StringKeyGenerator =
      Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), SECURE_KEY_LENGTH)

  // Based on DefaultServerOAuth2AuthorizationRequestResolver
  fun getAuthorizationRequest(
      clientRegistration: ClientRegistration,
      exchange: ServerWebExchange
  ): OAuth2AuthorizationRequest =
      secureKeyGenerator.generateKey().let {
        OAuth2AuthorizationRequest.authorizationCode()
            .additionalParameters(additionalParameters(it))
            .attributes(attributes(it, clientRegistration.registrationId))
            .clientId(clientRegistration.clientId)
            .authorizationUri(clientRegistration.providerDetails.authorizationUri)
            .redirectUri(expandRedirectUri(exchange.request, clientRegistration))
            .scopes(clientRegistration.scopes)
            .state(this.stateGenerator.generateKey())
            .build()
      }

  private fun attributes(nonce: String, registrationId: String): MutableMap<String, Any> =
      // append nonce and registration_id, to have the same request as with a usual login
      mutableMapOf(NONCE to nonce, REGISTRATION_ID to registrationId)

  private fun additionalParameters(nonce: String): MutableMap<String, Any> =
      // append hashed nonce and styleId, to have the same request as with a usual login
      mutableMapOf(
          NONCE to hash(nonce),
          identityProviderConfigurationProperties.styleIdParameter to
              identityProviderConfigurationProperties.styleId)

  private fun hash(nonce: String): Any {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(nonce.toByteArray(US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  // Replace Path placeholders and expand Base URL
  private fun expandRedirectUri(
      request: ServerHttpRequest,
      clientRegistration: ClientRegistration
  ): String? {
    val uriVariables: MutableMap<String, String?> = HashMap()

    uriVariables["registrationId"] = clientRegistration.registrationId

    val uriComponents =
        UriComponentsBuilder.fromUri(request.uri)
            .replacePath(request.path.contextPath().value())
            .replaceQuery(null)
            .fragment(null)
            .build()

    uriVariables["baseScheme"] = uriComponents.scheme ?: ""
    uriVariables["baseHost"] = uriComponents.host ?: ""

    // following logic is based on HierarchicalUriComponents#toUriString()
    // {basePort} = port
    val port = uriComponents.port
    uriVariables["basePort"] = if (port == -1) "" else ":$port"

    // {basePath} = path
    var path = uriComponents.path
    if (!path.isNullOrEmpty() && !path.startsWith("/")) {
      path = "/$path"
    }

    uriVariables["basePath"] = path ?: ""
    uriVariables["baseUrl"] = uriComponents.toUriString()

    return UriComponentsBuilder.fromUriString(clientRegistration.redirectUri)
        .buildAndExpand(uriVariables)
        .toUriString()
  }

  fun storeAuthorizationRequest(
      request: OAuth2AuthorizationRequest,
      exchange: ServerWebExchange
  ): Mono<Void> = authorizationRequestRepository.saveAuthorizationRequest(request, exchange)

  fun encodeRequestUri(request: OAuth2AuthorizationRequest): String =
      String(encodeUrlSafe(request.authorizationRequestUri.toByteArray()))

  companion object {
    const val SECURE_KEY_LENGTH = 96
  }
}
