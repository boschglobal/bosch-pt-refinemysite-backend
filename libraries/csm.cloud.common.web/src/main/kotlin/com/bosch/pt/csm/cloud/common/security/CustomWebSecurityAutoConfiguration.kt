/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.security.StaticResourceLocation.FAVICON
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.PathRequest.toStaticResources
import org.springframework.boot.autoconfigure.security.servlet.StaticResourceRequest
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.JwtDecoderFactory
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain

/**
 * Autoconfigures a [JwtIssuerAuthenticationManagerResolver], [BearerTokenResolver],
 * [DefaultCustomUserAuthenticationConverter] and [AuthenticationEntryPoint] if not defined in
 * specific application configuration. Still requires the application to configure a
 * [SecurityFilterChain] bean which will reference the components mentioned.
 */
@EnableConfigurationProperties(CustomTrustedJwtIssuersProperties::class)
@ConditionalOnClass(BearerTokenAuthenticationToken::class)
@AutoConfiguration(before = [OAuth2ResourceServerAutoConfiguration::class])
class CustomWebSecurityAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(AuthenticationManagerResolver::class)
  fun csmAuthManagerResolver(
      customUserAuthenticationConverter: AbstractCustomUserAuthenticationConverter,
      customTrustedJwtIssuersProperties: CustomTrustedJwtIssuersProperties,
      jwtDecoderFactory: JwtDecoderFactory<String>
  ) =
      JwtIssuerAuthenticationManagerResolver(
          CustomTrustedIssuerJwtAuthenticationManagerResolver(
              customTrustedJwtIssuersProperties,
              customUserAuthenticationConverter,
              jwtDecoderFactory))

  @Bean
  @ConditionalOnMissingBean(JwtDecoderFactory::class)
  fun customJwtDecoderFactory() = CustomJwtDecoderFactory()

  @Bean
  @ConditionalOnMissingBean(AuthenticationEntryPoint::class)
  fun authenticationEntryPoint(
      messageSource: MessageSource,
      environment: Environment
  ): CustomAuthenticationEntryPoint = CustomAuthenticationEntryPoint(messageSource, environment)

  @Bean
  @ConditionalOnMissingBean(BearerTokenResolver::class)
  fun bearerTokenResolver(): BearerTokenResolver =
      DefaultBearerTokenResolver().apply { setAllowUriQueryParameter(true) }

  @Bean
  @ConditionalOnMissingBean(AbstractCustomUserAuthenticationConverter::class)
  fun defaultCustomUserAuthenticationConverter(
      userDetailsService: UserDetailsService,
      jwtVerificationListeners: List<JwtVerificationListener>,
      customTrustedJwtIssuersProperties: CustomTrustedJwtIssuersProperties
  ): AbstractCustomUserAuthenticationConverter =
      DefaultCustomUserAuthenticationConverter(
          userDetailsService, jwtVerificationListeners, customTrustedJwtIssuersProperties)

  /**
   * Autoconfigures a [SecurityFilterChain] which utilizes [JwtIssuerAuthenticationManagerResolver]
   * and [BearerTokenResolver] under the [AuthenticationEntryPoint] configured.
   *
   * Permits all access to favicon, actuator endpoints and paths prefixed with /docs/.
   */
  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain::class)
  @Throws(Exception::class)
  fun filterChain(
      http: HttpSecurity,
      authenticationEntryPoint: AuthenticationEntryPoint,
      bearerTokenResolver: BearerTokenResolver,
      jwtIssuerAuthenticationManagerResolver: JwtIssuerAuthenticationManagerResolver,
  ): SecurityFilterChain =
      http
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .csrf()
          .disable()
          .authorizeHttpRequests()
          .requestMatchers(faviconRequestMatcher(), allActuatorEndpointsRequestMatcher())
          .permitAll()
          .requestMatchers("/docs/**", "/swagger/**")
          .permitAll()
          .anyRequest()
          .authenticated()
          .and()
          .oauth2ResourceServer()
          .bearerTokenResolver(bearerTokenResolver)
          .authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver)
          .authenticationEntryPoint(authenticationEntryPoint)
          .and()
          .build()

  private fun faviconRequestMatcher(): StaticResourceRequest.StaticResourceRequestMatcher? =
      toStaticResources().at(FAVICON)

  private fun allActuatorEndpointsRequestMatcher(): EndpointRequest.EndpointRequestMatcher? =
      EndpointRequest.toAnyEndpoint()
}
