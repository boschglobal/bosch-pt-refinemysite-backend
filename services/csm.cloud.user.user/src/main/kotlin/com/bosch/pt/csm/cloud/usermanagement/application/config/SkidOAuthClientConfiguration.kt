/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.application.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType

@Profile("skid-deleted-user-propagation")
@Configuration
class SkidOAuthClientConfiguration {

  @Bean
  fun skidClientRegistration(
      @Value("\${spring.security.oauth2.client.provider.skid.token-uri}") tokenUri: String,
      @Value("\${spring.security.oauth2.client.registration.skid.client-id}") clientId: String,
      @Value("\${spring.security.oauth2.client.registration.skid.client-secret}")
      clientSecret: String,
      @Value("\${spring.security.oauth2.client.registration.skid.scope}") scope: String,
      @Value("\${spring.security.oauth2.client.registration.skid.authorization-grant-type}")
      authorizationGrantType: String
  ): ClientRegistration =
      ClientRegistration.withRegistrationId("skid")
          .tokenUri(tokenUri)
          .clientId(clientId)
          .clientSecret(clientSecret)
          .scope(scope)
          .authorizationGrantType(AuthorizationGrantType(authorizationGrantType))
          .build()

  @Bean
  fun clientRegistrationRepository(
      clientRegistration: ClientRegistration
  ): ClientRegistrationRepository = InMemoryClientRegistrationRepository(clientRegistration)

  @Bean
  fun authorizedClientService(
      clientRegistrationRepository: ClientRegistrationRepository
  ): OAuth2AuthorizedClientService =
      InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)

  @Bean
  fun authorizedClientManager(
      clientRegistrationRepository: ClientRegistrationRepository,
      authorizedClientService: OAuth2AuthorizedClientService
  ): AuthorizedClientServiceOAuth2AuthorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(
              clientRegistrationRepository, authorizedClientService)
          .also {
            it.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build())
          }
}
