/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application.security

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.ReactiveAuthenticationManagerResolver
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoderFactory
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

class CustomTrustedIssuerJwtReactiveAuthenticationManagerResolver(
    private val trustedJwtIssuers: List<String>,
    private val userAuthenticationConverter: CustomReactiveUserAuthenticationConverter,
    private val jwtDecoderFactory: ReactiveJwtDecoderFactory<String>
) : ReactiveAuthenticationManagerResolver<String> {

  private val authenticationManagers: MutableMap<String, Mono<ReactiveAuthenticationManager>> =
      ConcurrentHashMap()

  override fun resolve(issuer: String): Mono<ReactiveAuthenticationManager> =
      if (trustedJwtIssuers.contains(issuer)) {
        authenticationManagers.computeIfAbsent(issuer) { iss: String ->
          authenticationManagerFromCallable(iss).subscribeOn(Schedulers.boundedElastic()).cache(
              { Duration.ofMillis(Long.MAX_VALUE) }, { Duration.ZERO }) {
                Duration.ZERO
              }
        }
      } else {
        LOGGER.debug("Unable to resolve ReactiveAuthenticationManager for untrusted issuer $issuer")
        Mono.empty()
      }

  private fun authenticationManagerFromCallable(iss: String): Mono<ReactiveAuthenticationManager> =
      Mono.fromCallable { createAuthenticationManager(iss) }

  private fun createAuthenticationManager(iss: String): JwtReactiveAuthenticationManager =
      JwtReactiveAuthenticationManager(jwtDecoderFactory.createDecoder(iss)).apply {
        setJwtAuthenticationConverter(userAuthenticationConverter)
      }

  companion object {
    private val LOGGER =
        LoggerFactory.getLogger(
            CustomTrustedIssuerJwtReactiveAuthenticationManagerResolver::class.java)
  }
}
