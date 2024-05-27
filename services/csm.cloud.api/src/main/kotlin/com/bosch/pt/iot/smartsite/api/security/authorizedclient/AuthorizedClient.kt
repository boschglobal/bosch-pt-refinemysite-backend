/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.authorizedclient

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientId
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.OAuth2AccessToken

@Document(collection = "AuthorizedClients")
@TypeAlias("AuthorizedClient")
data class AuthorizedClient(
    @Id val clientId: OAuth2AuthorizedClientId,
    // OAuth2Authorized Client stored flat, since it has no NO_ARGS Ctor
    val clientRegistration: ClientRegistration,
    val principalName: String,
    // Access Token flattened
    val tokenType: OAuth2AccessToken.TokenType,
    val scopes: Set<String>? = null,
    val accessTokenValue: String,
    val accessIssuedAt: Instant? = null,
    val accessExpiresAt: Instant? = null,
    // refresh token flattened
    val refreshTokenValue: String?,
    val refreshIssuedAt: Instant? = null,
    val refreshExpiresAt: Instant? = null,
)