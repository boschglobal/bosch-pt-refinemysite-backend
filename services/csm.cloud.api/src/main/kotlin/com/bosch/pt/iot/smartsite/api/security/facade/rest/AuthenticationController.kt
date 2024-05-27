/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.facade.rest

import com.bosch.pt.iot.smartsite.api.security.AuthenticationVerificationService
import com.bosch.pt.iot.smartsite.api.security.RedirectConstants.REDIRECT_SESSION_ATTRIBUTE_NAME
import com.bosch.pt.iot.smartsite.api.security.RedirectConstants.REDIRECT_URL_PARAMETER
import com.bosch.pt.iot.smartsite.api.security.RedirectUrlValidationService
import com.bosch.pt.iot.smartsite.api.security.config.IdentityProviderConfigurationProperties
import java.net.URI
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.HttpStatus.PERMANENT_REDIRECT
import org.springframework.http.HttpStatus.TEMPORARY_REDIRECT
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.http.ResponseEntity.status
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.SessionAttribute
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebSession
import org.springframework.web.util.UriComponentsBuilder.fromUri
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just

/** Controller with core methods that the Auth Flow needs. */
@RestController
class AuthenticationController(
    private val identityProviderConfigurationProperties: IdentityProviderConfigurationProperties,
    private val redirectUrlValidationService: RedirectUrlValidationService,
    private val authenticationVerificationService: AuthenticationVerificationService
) {

  /**
   * Redirects the user, after having logged in with the IdP if he is properly authenticated to the
   * provided redirect_url OR one stored in the session. If neither is present, this will fail.
   *
   * Will only be executed if Spring Security filter does not intercept and redirect to
   * /oauth2/authorization/{idp} in case there is no authorized client found based on the session.
   */
  @GetMapping("/login")
  fun login(
      @RequestParam(name = REDIRECT_URL_PARAMETER) encodedRedirectString: String
  ): Mono<ResponseEntity<Any>> {
    val redirectUri = redirectUrlValidationService.decodeRedirectString(encodedRedirectString)
    // reaching here means user is authenticated, redirect to provided redirect URI, or throw
    // AccessDenied when URI is not valid
    require(redirectUrlValidationService.isRedirectAllowed(redirectUri)) {
      "RedirectURL not whitelisted"
    }
    return just(status(FOUND).location(redirectUri).build())
  }

  /**
   * Redirects from legacy /api/login to /login (ensuring single login and authentication entry
   * point configuration is used).
   */
  @GetMapping("/api/login")
  fun legacyLogin(
      @RequestParam(name = REDIRECT_URL_PARAMETER) encodedRedirectString: String
  ): Mono<ResponseEntity<Any>> =
      just(
          status(PERMANENT_REDIRECT)
              .location(URI.create("/login?$REDIRECT_URL_PARAMETER=$encodedRedirectString"))
              .build())

  @GetMapping("/api/logout")
  fun logout(
      @RequestParam(name = REDIRECT_URL_PARAMETER) redirectUri: String,
      session: WebSession,
      exchange: ServerWebExchange,
      @AuthenticationPrincipal user: OidcUser
  ): Mono<ResponseEntity<Any>> {
    val idToken = user.idToken.tokenValue

    // store redirect after logout at KEYCLOAK1, might be null, will be sanitized in
    // ClearSessionLogoutFilter
    session.attributes[REDIRECT_SESSION_ATTRIBUTE_NAME] = redirectUri

    // Redirect to KEYCLOAK1 Logout with id_token_hint and redirect
    return just(
        status(TEMPORARY_REDIRECT).location(getIdpLogoutUri(idToken, exchange, "/api")).build())
  }

  @GetMapping("/logout")
  fun logoutInternal(
      @RequestParam(name = REDIRECT_URL_PARAMETER) redirectUri: String,
      session: WebSession,
      exchange: ServerWebExchange,
      @AuthenticationPrincipal user: OidcUser
  ): Mono<ResponseEntity<Any>> {
    val idToken = user.idToken.tokenValue

    // store redirect after logout at KEYCLOAK1, might be null, will be sanitized in
    // ClearSessionLogoutFilter
    session.attributes[REDIRECT_SESSION_ATTRIBUTE_NAME] = redirectUri

    // Redirect to MYIDP2 Logout with id_token_hint and redirect
    return just(
        status(TEMPORARY_REDIRECT).location(getIdpLogoutUri(idToken, exchange, "/")).build())
  }

  /**
   * Verify that the session cookie is sent with the request. If so a session exists and everything
   * else should be handled within Spring session management. Unfortunately we can not check if the
   * Cookie has specific attributes, as only the value is sent. We then check if the session has all
   * the needed Spring Security Attributes and if the User is authenticated.
   */
  @GetMapping("/api/login/verify", "/login/verify")
  fun verifyLogin(session: WebSession?): Mono<ResponseEntity<Boolean>> =
      authenticationVerificationService.isUserAuthenticated(session).flatMap { just(ok().body(it)) }

  /**
   * Read a redirectUrl from the Session and redirect to it if allowed. Used after signup and
   * pw-reset
   */
  @GetMapping("/api/session-redirect", "/session-redirect")
  fun redirectFormSession(
      @SessionAttribute(REDIRECT_SESSION_ATTRIBUTE_NAME, required = true)
      encodedRedirectString: String
  ): Mono<ResponseEntity<Void>> {
    val redirectUri = redirectUrlValidationService.decodeRedirectString(encodedRedirectString)
    require(redirectUrlValidationService.isRedirectAllowed(redirectUri)) {
      "RedirectURL not whitelisted"
    }
    return just(status(FOUND).location(redirectUri).build())
  }

  /**
   * Builds the logout URI with the query parameters id_token_hint and post_logout_redirect_uri that
   * is used to log out with the IdP.
   */
  private fun getIdpLogoutUri(token: String, exchange: ServerWebExchange, route: String): URI {
    val currentUri = exchange.request.uri
    val postLogoutRedirect =
        fromUri(currentUri)
            .replacePath(null)
            // remove admin from host as this would break post-logout
            // e.g. sandbox1-admin -> sandbox1
            .host(currentUri.host.replace("-admin", ""))
            // Route prefix provided with /, so path instead of pathSegment
            .path(route)
            .pathSegment("logout", "redirect")
            .replaceQuery(null)
            .build()
            .toUriString()

    return fromUri(identityProviderConfigurationProperties.logoutUrl)
        .queryParam("id_token_hint", token)
        .queryParam("post_logout_redirect_uri", postLogoutRedirect)
        .build()
        .toUri()
  }
}
