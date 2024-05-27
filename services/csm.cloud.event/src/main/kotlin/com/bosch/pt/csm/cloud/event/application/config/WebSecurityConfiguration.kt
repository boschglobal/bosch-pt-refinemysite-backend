package com.bosch.pt.csm.cloud.event.application.config

import com.bosch.pt.csm.cloud.event.application.security.CustomReactiveUserAuthenticationConverter
import com.bosch.pt.csm.cloud.event.application.security.CustomTrustedIssuerJwtReactiveAuthenticationManagerResolver
import com.bosch.pt.csm.cloud.event.application.security.CustomTrustedJwtIssuersProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest.toAnyEndpoint
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.GET
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoderFactory
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerReactiveAuthenticationManagerResolver
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono.error

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(CustomTrustedJwtIssuersProperties::class)
class WebSecurityConfiguration(
    @Qualifier("eventUserDetails") private val userDetailsService: ReactiveUserDetailsService
) {

  @Bean
  fun springSecurityFilterChain(
      http: ServerHttpSecurity,
      authenticationManagerResolver: JwtIssuerReactiveAuthenticationManagerResolver
  ): SecurityWebFilterChain =
      http
          .csrf()
          .disable()
          .authorizeExchange()
          .matchers(toAnyEndpoint())
          .permitAll()
          .pathMatchers(GET, "/docs/**")
          .permitAll()
          .anyExchange()
          .authenticated()
          .and()
          .exceptionHandling()
          .and()
          .oauth2ResourceServer()
          .authenticationManagerResolver(authenticationManagerResolver) // we do not use a custom
          // ServerAuthenticationEntryPoint here because it cannot add
          // a response body. With this here, the exception can be handled with the ErrorController
          .authenticationEntryPoint { _, exception -> error(exception) }
          .accessDeniedHandler { _, exception -> error(exception) }
          .let { http.build() }

  @Bean
  fun jwtIssuerReactiveAuthenticationManagerResolver(
      trustedIssuerJwtReactiveAuthenticationManagerResolver:
          CustomTrustedIssuerJwtReactiveAuthenticationManagerResolver
  ): JwtIssuerReactiveAuthenticationManagerResolver =
      JwtIssuerReactiveAuthenticationManagerResolver(
          trustedIssuerJwtReactiveAuthenticationManagerResolver)

  @Bean
  fun jwtIssuerResolver(
      issuersProperties: CustomTrustedJwtIssuersProperties,
      userAuthenticationConverter: CustomReactiveUserAuthenticationConverter,
      jwtDecoderFactory: ReactiveJwtDecoderFactory<String>
  ): CustomTrustedIssuerJwtReactiveAuthenticationManagerResolver =
      CustomTrustedIssuerJwtReactiveAuthenticationManagerResolver(
          issuersProperties.issuerUris, userAuthenticationConverter, jwtDecoderFactory)

  @Bean
  fun jwtDecoderFactory(): ReactiveJwtDecoderFactory<String> =
      ReactiveJwtDecoderFactory { oidcIssuerLocation: String ->
        ReactiveJwtDecoders.fromOidcIssuerLocation(oidcIssuerLocation)
      }

  @Bean
  fun customUserAuthenticationConverter(
      issuersProperties: CustomTrustedJwtIssuersProperties
  ): CustomReactiveUserAuthenticationConverter =
      CustomReactiveUserAuthenticationConverter(userDetailsService, issuersProperties.issuerUris)
}
