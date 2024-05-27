/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.config

import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.csm.cloud.common.security.JwtVerificationListener
import com.bosch.pt.csm.cloud.usermanagement.application.security.CustomUserAuthenticationConverter
import jakarta.servlet.DispatcherType.ERROR
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.boot.autoconfigure.security.StaticResourceLocation
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher
import org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher

@Configuration
@EnableWebSecurity
class WebSecurityConfiguration(private val userDetailsService: UserDetailsService) {

  /**
   * Allows access to docs endpoint (REST API docs), actuator endpoints and
   * /v[1-9]/users/unregistered/documents endpoints.
   */
  @Bean
  @Throws(Exception::class)
  fun filterChain(
      http: HttpSecurity,
      jwtIssuerAuthenticationManagerResolver: JwtIssuerAuthenticationManagerResolver,
      authenticationEntryPoint: AuthenticationEntryPoint
  ): SecurityFilterChain =
      http
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .csrf()
          .disable()
          .authorizeHttpRequests()
          .requestMatchers(
              PathRequest.toStaticResources().at(StaticResourceLocation.FAVICON),
              EndpointRequest.toAnyEndpoint())
          .permitAll()
          .requestMatchers("/docs/**", "/swagger/**", "/error")
          .permitAll()
          .requestMatchers(
              regexMatcher(HttpMethod.GET, "/v[1-9]/users/unregistered/documents\\?.*"))
          .permitAll()
          .requestMatchers(
              AndRequestMatcher(
                  DispatcherTypeRequestMatcher(ERROR),
                  antMatcher("/error"),
              ))
          .permitAll()
          .anyRequest()
          .authenticated()
          .and()
          .oauth2ResourceServer()
          .bearerTokenResolver(
              DefaultBearerTokenResolver().apply { setAllowUriQueryParameter(true) })
          .authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver)
          .authenticationEntryPoint(authenticationEntryPoint)
          .and()
          .build()

  @Bean
  fun customAuthenticationConverter(
      jwtVerificationListeners: List<JwtVerificationListener>,
      customTrustedJwtIssuersProperties: CustomTrustedJwtIssuersProperties
  ): CustomUserAuthenticationConverter =
      CustomUserAuthenticationConverter(
          userDetailsService, jwtVerificationListeners, customTrustedJwtIssuersProperties)
}
