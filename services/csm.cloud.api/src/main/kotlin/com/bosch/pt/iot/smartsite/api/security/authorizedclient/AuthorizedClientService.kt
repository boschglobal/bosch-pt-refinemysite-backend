/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.authorizedclient

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientId
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * This service is used to store access tokens and refresh tokens for a given principal and client
 * ID. In our case the principal is a technical name and the client ID is always "myidp2" since we do
 * only use MYIDP2 as our OAuth provider. The tokens are stored in MongoDB (Atlas Cloud).
 */
@Component
class AuthorizedClientService(
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository,
    private val customAuthorizedClientRepository: CustomAuthorizedClientRepository,
) : ReactiveOAuth2AuthorizedClientService {

  @Suppress("unchecked_cast")
  override fun <T : OAuth2AuthorizedClient> loadAuthorizedClient(
      clientRegistrationId: String,
      principalName: String
  ): Mono<T> =
      convertToClientId(clientRegistrationId, principalName).let { clientId ->
        customAuthorizedClientRepository.findById(clientId).map {
          with(it) {
            OAuth2AuthorizedClient(
                clientRegistration,
                principalName,
                OAuth2AccessToken(tokenType, accessTokenValue, accessIssuedAt, accessExpiresAt),
                OAuth2RefreshToken(refreshTokenValue, refreshIssuedAt, refreshExpiresAt))
          }
        }
      } as
          Mono<T>

  override fun saveAuthorizedClient(
      authorizedClient: OAuth2AuthorizedClient,
      principal: Authentication
  ): Mono<Void> =
      OAuth2AuthorizedClientId(authorizedClient.clientRegistration.registrationId, principal.name)
          .let {
            customAuthorizedClientRepository.save(
                AuthorizedClient(
                    clientId = it,
                    clientRegistration = authorizedClient.clientRegistration,
                    principalName = principal.name,
                    tokenType = authorizedClient.accessToken.tokenType,
                    scopes = authorizedClient.accessToken.scopes,
                    accessTokenValue = authorizedClient.accessToken.tokenValue,
                    accessIssuedAt = authorizedClient.accessToken.issuedAt,
                    accessExpiresAt = authorizedClient.accessToken.expiresAt,
                    refreshTokenValue = authorizedClient.refreshToken?.tokenValue,
                    refreshIssuedAt = authorizedClient.refreshToken?.issuedAt,
                    refreshExpiresAt = authorizedClient.refreshToken?.expiresAt,
                ))
          }
          .then(Mono.empty())

  override fun removeAuthorizedClient(
      clientRegistrationId: String,
      principalName: String
  ): Mono<Void> =
      convertToClientId(clientRegistrationId, principalName).let {
        customAuthorizedClientRepository.deleteById(it)
      }

  fun existsAuthorizedClient(
      principalName: String,
      clientRegistrationId: String
  ): Mono<Boolean> =
      convertToClientId(clientRegistrationId, principalName).flatMap {
        customAuthorizedClientRepository.existsById(it)
      }

  private fun convertToClientId(
      clientRegistrationId: String,
      principalName: String
  ): Mono<OAuth2AuthorizedClientId> =
      this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId).map {
        OAuth2AuthorizedClientId(clientRegistrationId, principalName)
      }
}
