/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.security.AbstractCustomUserAuthenticationConverter
import com.bosch.pt.csm.cloud.common.security.CustomAuthenticationEntryPoint
import com.bosch.pt.csm.cloud.common.security.CustomTrustedIssuerJwtAuthenticationManagerResolver
import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver.BasicPatResolver
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationEntryPoint
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationManagerResolver
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver.PatResolver
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.RmsPat1AuthenticationTokenConverter
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.configurer.basicAuthentication
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.configurer.patAuthentication
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum.RMSPAT1
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.service.PatQueryService
import jakarta.servlet.DispatcherType
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.boot.autoconfigure.security.StaticResourceLocation.FAVICON
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.boot.autoconfigure.security.servlet.StaticResourceRequest
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.oauth2.jwt.JwtDecoderFactory
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher

@Configuration
@EnableWebSecurity
class WebSecurityConfiguration {

  @Bean
  @Order(HIGHEST_PRECEDENCE)
  fun basic(
      http: HttpSecurity,
      authenticationEntryPoint: PatAuthenticationEntryPoint,
      authenticationManagerResolver: PatAuthenticationManagerResolver
  ): SecurityFilterChain =
      http
          .basicChainInitialization()
          .securityMatcher(authenticationTypeMatcher("basic"))
          .authorizeHttpRequests()
          .anyRequest()
          .authenticated()
          .and()
          .basicAuthentication()
          .tokenResolver(BasicPatResolver())
          .authenticationManagerResolver(authenticationManagerResolver)
          .authenticationEntryPoint(authenticationEntryPoint)
          .and()
          .build()

  @Bean
  @Order(SECOND_HIGHEST_PRECEDENCE)
  fun pat(
      http: HttpSecurity,
      authenticationEntryPoint: PatAuthenticationEntryPoint,
      authenticationManagerResolver: PatAuthenticationManagerResolver
  ): SecurityFilterChain =
      http
          .basicChainInitialization()
          .securityMatcher(authenticationTypeMatcher("pat"))
          .authorizeHttpRequests()
          .anyRequest()
          .authenticated()
          .and()
          .patAuthentication()
          .tokenResolver(PatResolver())
          .authenticationManagerResolver(authenticationManagerResolver)
          .authenticationEntryPoint(authenticationEntryPoint)
          .and()
          .build()

  @Bean
  @Order(THIRD_HIGHEST_PRECEDENCE)
  fun oauth(
      http: HttpSecurity,
      authenticationEntryPoint: CustomAuthenticationEntryPoint,
      bearerTokenResolver: BearerTokenResolver,
      jwtIssuerAuthenticationManagerResolver: JwtIssuerAuthenticationManagerResolver,
  ): SecurityFilterChain =
      http
          .basicChainInitialization()
          .securityMatcher(authenticationTypeMatcher("bearer"))
          .authorizeHttpRequests()
          .anyRequest()
          .authenticated()
          .and()
          .oauth2ResourceServer()
          .bearerTokenResolver(bearerTokenResolver)
          .authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver)
          .authenticationEntryPoint(authenticationEntryPoint)
          .and()
          .build()

  /**
   * Register basic auth chain as fallback to return forbidden for unauthorized request that are not
   * handled by the other filter chains. Don't give this chain a higher priority to have it as a
   * fallback. Basic auth is actually handled by the subsequent chain.
   */
  @Bean
  @Order(FOURTH_HIGHEST_PRECEDENCE)
  fun publicResourcesAndAccessDeniedFallback(
      http: HttpSecurity,
      authenticationEntryPoint: PatAuthenticationEntryPoint,
  ): SecurityFilterChain =
      http
          .basicChainInitialization()
          .authorizeHttpRequests()
          .requestMatchers(faviconRequestMatcher(), allActuatorEndpointsRequestMatcher())
          .permitAll()
          .requestMatchers("/docs/**", "/swagger/**", "/graphiql/**", "/error")
          .permitAll()
          .requestMatchers(
              AndRequestMatcher(
                  DispatcherTypeRequestMatcher(DispatcherType.ERROR),
                  AntPathRequestMatcher.antMatcher("/error"),
              ))
          .permitAll()
          .and()
          .authorizeHttpRequests()
          .anyRequest()
          .authenticated()
          .and()
          .httpBasic()
          .authenticationEntryPoint(authenticationEntryPoint)
          .and()
          .build()

  @Bean
  fun oauthAuthenticationEntryPoint(messageSource: MessageSource, environment: Environment) =
      CustomAuthenticationEntryPoint(messageSource, environment)

  @Bean
  fun oauthAuthManagerResolver(
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
  fun patAuthenticationEntryPoint(messageSource: MessageSource, environment: Environment) =
      PatAuthenticationEntryPoint(messageSource, environment)

  @Bean
  fun patAuthenticationManagerResolver(patQueryService: PatQueryService) =
      PatAuthenticationManagerResolver(
          mapOf(RMSPAT1 to RmsPat1AuthenticationTokenConverter(patQueryService)))

  private fun HttpSecurity.basicChainInitialization(): HttpSecurity =
      this.sessionManagement().sessionCreationPolicy(STATELESS).and().csrf().disable()

  private fun faviconRequestMatcher(): StaticResourceRequest.StaticResourceRequestMatcher? =
      PathRequest.toStaticResources().at(FAVICON)

  private fun allActuatorEndpointsRequestMatcher(): EndpointRequest.EndpointRequestMatcher? =
      EndpointRequest.toAnyEndpoint()

  private fun authenticationTypeMatcher(type: String) = RequestMatcher { request ->
    request.getHeader(AUTHORIZATION)?.startsWith(type, ignoreCase = true) == true
  }

  companion object {
    const val HIGHEST_PRECEDENCE = 1
    const val SECOND_HIGHEST_PRECEDENCE = 2
    const val THIRD_HIGHEST_PRECEDENCE = 3
    const val FOURTH_HIGHEST_PRECEDENCE = 4
  }
}
