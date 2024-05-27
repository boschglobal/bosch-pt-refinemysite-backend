/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.config

import jakarta.servlet.DispatcherType
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.boot.autoconfigure.security.StaticResourceLocation
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher

@Configuration
@EnableWebSecurity
class WebSecurityConfiguration {

  @Bean
  @Throws(Exception::class)
  fun filterChain(
      http: HttpSecurity,
      authenticationEntryPoint: AuthenticationEntryPoint,
      jwtIssuerAuthenticationManagerResolver: JwtIssuerAuthenticationManagerResolver,
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
              AndRequestMatcher(
                  DispatcherTypeRequestMatcher(DispatcherType.ERROR),
                  AntPathRequestMatcher.antMatcher("/error"),
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
}
