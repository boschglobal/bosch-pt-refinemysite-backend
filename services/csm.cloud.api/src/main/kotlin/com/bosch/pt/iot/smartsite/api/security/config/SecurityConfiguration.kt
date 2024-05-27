/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.config

import com.bosch.pt.iot.smartsite.api.security.ClearSessionLogoutFilter
import com.bosch.pt.iot.smartsite.api.security.CustomOAuth2RequestResolver
import com.bosch.pt.iot.smartsite.api.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.iot.smartsite.api.security.ReactiveCustomAuthenticationEntryPoint
import com.bosch.pt.iot.smartsite.api.security.RedirectUrlValidationService
import com.bosch.pt.iot.smartsite.api.security.SaveSessionFilter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.LOGOUT
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerReactiveAuthenticationManagerResolver
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import org.springframework.web.cors.reactive.CorsUtils

/**
 * Security Configuration for KeyCloak1 auth flow. Defines multiple [SecurityWebFilterChain]
 * beans which are ordered by precedence.
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!test-zero-security")
@EnableConfigurationProperties(CustomTrustedJwtIssuersProperties::class)
class SecurityConfiguration {

  private val authMatcher =
      pathMatchers(
          "/login",
          "/login/oauth2/**",
          "/oauth2/**",
          "/logout",
          "/logout/redirect",
          "/signup",
          "/api/login/oauth2/**",
          "/api/oauth2/**",
          "/api/logout",
          "/api/logout/redirect",
          "/api/signup")

  // Match public paths that are all allowed without authentication
  private val publicApiMatcher =
      pathMatchers(
          "/health",
          "/v?/versions",
          "/mobile/version",
          "/api/login",
          "/login/verify",
          "/api/health",
          "/api/v?/versions",
          "/api/mobile/version",
          "/api/login/verify",
          "/api/v?/users/unregistered/documents",
          "/internal/v?/users/unregistered/documents",
          "/internal/docs/**",
          "/internal/mobile/version",
          "/graphiql/**",
          "/swagger/**",
          "/swagger-ui/**")

  private val staticFiles =
      listOf(
          "/default-profile-picture.png",
          "/default-profile-picture-female.png",
          "/default-profile-picture-male.png",
          "/default-project-picture.png",
          "/deleted-image-large.png",
          "/deleted-image-small.png",
          "/favicon.ico",
      )

  private val resourceMatcher =
      pathMatchers(
          *staticFiles.toTypedArray(),
          *staticFiles.map { staticFile -> "/api$staticFile" }.toTypedArray(),
          *staticFiles.map { staticFile -> "/internal$staticFile" }.toTypedArray())

  // No route prefix, as this is only called internally from Kubernetes
  private val actuatorMatcher = pathMatchers("/actuator/**")

  // Needed to not authenticate users on CORS Request. These requests might not send credentials,
  // especially in local development on cross site requests
  private val corsPreflightRequestMatcher = ServerWebExchangeMatcher {
    if (CorsUtils.isPreFlightRequest(it.request)) ServerWebExchangeMatcher.MatchResult.match()
    else ServerWebExchangeMatcher.MatchResult.notMatch()
  }

  private val carriesBearerToken = ServerWebExchangeMatcher {
    val hasAuthorizationHeader = it.request.headers.containsKey(AUTHORIZATION)
    val hasAccessTokenQueryParameter = it.request.queryParams.containsKey("access_token")
    if (hasAccessTokenQueryParameter || hasAuthorizationHeader)
        ServerWebExchangeMatcher.MatchResult.match()
    else ServerWebExchangeMatcher.MatchResult.notMatch()
  }

  private val docsMatcher = pathMatchers("/internal/", "/internal/docs/**")

  /**
   * [SecurityWebFilterChain] which is applied to all endpoints that have not matched with
   * [publicEndpointsSecurity], [docsSecurity] or [idpAuthSecurity]. Requires authentication and
   * resolves JWT based authentication if applicable. Also works with session-based authentication
   * (where JWT is supplied from previously persisted
   * [com.bosch.pt.iot.smartsite.api.security.authorizedclient.AuthorizedClient].
   */
  @Bean
  @Order(FOURTH_HIGHEST_PRECEDENCE)
  fun protectedApiSecurity(
      http: ServerHttpSecurity,
      messageSource: MessageSource,
      customTrustedJwtIssuersProperties: CustomTrustedJwtIssuersProperties
  ): SecurityWebFilterChain =
      http
          .cors(withDefaults())
          .authorizeExchange { authorize -> authorize.anyExchange().authenticated() }
          .oauth2ResourceServer { oauth2 ->
            oauth2.authenticationManagerResolver(
                JwtIssuerReactiveAuthenticationManagerResolver(
                    customTrustedJwtIssuersProperties.issuerUris))
          }
          .exceptionHandling {
            it.authenticationEntryPoint(ReactiveCustomAuthenticationEntryPoint(messageSource))
          }
          // disable CSRF, as the frontend currently does not fully support it
          .csrf { csrf -> csrf.disable() }
          .build()
  /**
   * [SecurityWebFilterChain] which covers access to special authentication-flow-related endpoints
   * (like login, logout and oauth2-related redirects). Takes precedence after
   * [publicEndpointsSecurity] and [docsSecurity] have been applied.
   */
  @Bean
  @Order(THIRD_HIGHEST_PRECEDENCE)
  fun idpAuthSecurity(
      http: ServerHttpSecurity,
      clientRepository: ReactiveClientRegistrationRepository,
      idpProperties: IdentityProviderConfigurationProperties,
      redirectUrlValidationService: RedirectUrlValidationService,
  ): SecurityWebFilterChain =
      http
          .securityMatcher(authMatcher)
          .authorizeExchange { authorize ->
            // login, logout requests must be authenticated, else OAuth2Login
            authorize
                .pathMatchers(
                    "/login", "/logout", "/logout/redirect", "/api/logout", "/api/logout/redirect")
                .authenticated()
            // Redirects and OAuth internal calls are just passed through
            authorize.anyExchange().permitAll()
          }
          // Configure OAuth2Login
          .oauth2Login { login ->
            login
                .authenticationMatcher(
                    PathPatternParserServerWebExchangeMatcher(
                        "/login/oauth2/code/{registrationId}"))
                // Custom RequestResolver
                .authorizationRequestResolver(
                    CustomOAuth2RequestResolver(
                        clientRepository = clientRepository, properties = idpProperties))
          }
          .exceptionHandling {
            // Since we use a custom OauthRequestResolver to add styleId,
            // we have to configure the entry point
            it.authenticationEntryPoint(
                RedirectServerAuthenticationEntryPoint(
                    "/oauth2/authorization/${idpProperties.clientRegistration}"))
          }
          // disable logout as we implement our own to inject id_token_hint
          .logout { it.disable() }
          // disable CSRF, as the frontend does not fully support it
          .csrf { it.disable() }
          // allow CORS Configuration, else we will get 401/403 on CORS preflight calls
          .cors(withDefaults())
          // ClearSessionFilter needs to be placed AFTER SECURITY_CONTEXT_SERVER_WEB_EXCHANGE
          // Filter, else the exchange does not have a principal as the Security Context is not
          // injected
          .addFilterBefore(ClearSessionLogoutFilter("/api", redirectUrlValidationService), LOGOUT)
          .addFilterBefore(ClearSessionLogoutFilter("", redirectUrlValidationService), LOGOUT)
          .addFilterBefore(SaveSessionFilter(), LOGOUT)
          .build()

  /**
   * Permissive access docs endpoints. [SecurityWebFilterChain] only configured in docs profile with
   * the highest precedence next to [publicEndpointsSecurity].
   */
  @Bean
  @Order(SECOND_HIGHEST_PRECEDENCE)
  @Profile("docs")
  fun docsSecurity(http: ServerHttpSecurity): SecurityWebFilterChain =
      http
          .securityMatcher(docsMatcher)
          .authorizeExchange { authorize -> authorize.anyExchange().permitAll() }
          .build()

  /**
   * Permissive access to the public API. This is needed for public endpoints, public resources,
   * actuator endpoints CORS preflight options requests. [SecurityWebFilterChain] with highest
   * precedence.
   */
  @Bean
  @Order(HIGHEST_PRECEDENCE)
  fun publicEndpointsSecurity(http: ServerHttpSecurity): SecurityWebFilterChain =
      http
          .securityMatcher(
              OrServerWebExchangeMatcher(
                  actuatorMatcher,
                  corsPreflightRequestMatcher,
                  publicApiMatcher,
                  resourceMatcher,
                  carriesBearerToken,
              ))
          .cors(withDefaults())
          .authorizeExchange { authorize -> authorize.anyExchange().permitAll() }
          .csrf { csrf -> csrf.disable() }
          .exceptionHandling(withDefaults())
          .build()

  companion object {
    const val HIGHEST_PRECEDENCE = 1
    const val SECOND_HIGHEST_PRECEDENCE = 2
    const val THIRD_HIGHEST_PRECEDENCE = 3
    const val FOURTH_HIGHEST_PRECEDENCE = 4
  }
}
