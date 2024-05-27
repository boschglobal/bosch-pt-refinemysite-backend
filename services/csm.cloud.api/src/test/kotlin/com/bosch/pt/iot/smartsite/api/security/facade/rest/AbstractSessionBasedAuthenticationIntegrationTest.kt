/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.facade.rest

import com.bosch.pt.iot.smartsite.api.security.authorizedclient.AuthorizedClientService
import java.time.Instant.now
import java.util.UUID.randomUUID
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.EXP
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.IAT
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.SUB
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME
import org.springframework.session.ReactiveSessionRepository
import org.springframework.session.Session
import org.springframework.session.data.mongo.ReactiveMongoSessionRepository
import reactor.core.publisher.Mono.just

/**
 * Mimics a logged-in state by creating mock authentication state in a session in
 * [ReactiveMongoSessionRepository] as well as an [OAuth2AuthorizedClient] via
 * [AuthorizedClientService]. Extending tests can use the session by adding a session cookie named
 * [com.bosch.pt.iot.smartsite.api.security.config.SessionConfiguration.Companion.COOKIE_NAME] with
 * the [session]-ID as value.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class AbstractSessionBasedAuthenticationIntegrationTest {

  @Autowired lateinit var reactiveMongoSessionRepository: ReactiveMongoSessionRepository

  // Cast ReactiveMongoSessionRepository to work with Session instead of MongoSession
  // as MongoSession is package-private
  @Suppress("UNCHECKED_CAST")
  private val reactiveSessionRepository: ReactiveSessionRepository<Session> by lazy {
    reactiveMongoSessionRepository as ReactiveSessionRepository<Session>
  }

  @Autowired lateinit var authorizedClientService: AuthorizedClientService

  lateinit var session: Session

  // token issued by http://example.com
  val tokenValue =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
          ".eyJpc3MiOiJodHRwOi8vZXhhbXBsZS5jb20ifQ" +
          ".HxFXyU5kqd_-o7ZiIbVmtG52KN7QyzdY_Pj1PAgxk-w"

  @BeforeEach
  fun createMockAuthSession() {

    val principalId = randomUUID().toString()
    val issuedAt = now()
    val expiresAt = now().plusSeconds(ONE_HOUR_IN_SECONDS)

    val authentication =
        OAuth2AuthenticationToken(
            DefaultOidcUser(
                listOf(),
                OidcIdToken.withTokenValue(tokenValue)
                    .claim(IAT, issuedAt)
                    .claim(EXP, expiresAt)
                    .claim(SUB, principalId)
                    .build()),
            listOf(),
            AUTH_SYSTEM)

    val securityContext = SecurityContextImpl(authentication)

    // stores a session with the authentication above in the security context
    session =
        requireNotNull(
            reactiveSessionRepository
                .createSession()
                .map { newSession ->
                  newSession.setAttribute(
                      DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME, securityContext)
                  reactiveSessionRepository.save(newSession).then(just(newSession))
                }
                .block()
                ?.block())

    // stores a fake authorized client matching the principal and OAuth2AuthenticationToken in the
    // session
    authorizedClientService
        .saveAuthorizedClient(
            OAuth2AuthorizedClient(
                ClientRegistration.withRegistrationId(AUTH_SYSTEM)
                    .authorizationGrantType(AUTHORIZATION_CODE)
                    .clientId("refinemysite-fake")
                    .redirectUri(
                        "{baseScheme}://{baseHost}{basePort}/login/oauth2/code/{registrationId}")
                    .authorizationUri("http://fake-issuer/openid-connect/auth")
                    .tokenUri("https://fake-issuer/openid-connect/token")
                    .build(),
                principalId,
                OAuth2AccessToken(BEARER, tokenValue, issuedAt, expiresAt),
                OAuth2RefreshToken("refresh-token-value", issuedAt)),
            authentication)
        .block()
  }
  companion object {
    private const val AUTH_SYSTEM = "keycloak1"
    private const val ONE_HOUR_IN_SECONDS: Long = 3600
  }
}
