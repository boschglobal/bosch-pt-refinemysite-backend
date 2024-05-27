/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import com.bosch.pt.iot.smartsite.api.security.authorizedclient.AuthorizedClientService
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME
import org.springframework.stereotype.Service
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just

@Service
class AuthenticationVerificationService(
    private val authorizedClientService: AuthorizedClientService,
) {

  /**
   * This validates, if for a given request a session is sent, the session has a valid security
   * context and an authorizedClient exists for the given principal.
   */
  fun isUserAuthenticated(session: WebSession?): Mono<Boolean> {

    val securityContext =
        session?.attributes?.get(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME) as SecurityContext?

    return if (session == null || securityContext == null) {
      // no session or security context at all
      just(false)
    } else if (!securityContext.authentication.isAuthenticated ||
        securityContext.authentication !is OAuth2AuthenticationToken) {
      // not authenticated or unsupported authentication type
      just(false)
    } else {
      // use the client registration ID (myidp2 or keycloak1) from the session to get the authorized client
      // originally created with that session
      val oauth2AuthToken = securityContext.authentication as OAuth2AuthenticationToken

      authorizedClientService.existsAuthorizedClient(
          securityContext.authentication.name, oauth2AuthToken.authorizedClientRegistrationId)
    }
  }
}
