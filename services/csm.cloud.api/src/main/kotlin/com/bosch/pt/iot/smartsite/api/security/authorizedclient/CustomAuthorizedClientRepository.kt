/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.authorizedclient

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientId

interface CustomAuthorizedClientRepository :
    ReactiveMongoRepository<AuthorizedClient, OAuth2AuthorizedClientId>
