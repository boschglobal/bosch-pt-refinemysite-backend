/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2018
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.user.integration

import com.bosch.pt.csm.cloud.event.user.integration.resource.UserResource
import com.bosch.pt.csm.cloud.event.user.model.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class UserIntegrationService(
    private val webClient: WebClient,
    @Value("\${csm.user.url}") private val userServiceUrl: String,
    @Value("\${custom.api.user.version}") private val customApiUserVersion: String
) {

  fun findCurrentUser(): Mono<User> =
      webClient
          .get()
          .uri(buildUserServiceUrl())
          .retrieve()
          .bodyToMono(UserResource::class.java)
          .map { User(it.identifier, AuthorityUtils.createAuthorityList("ROLE_USER")) }

  private fun buildUserServiceUrl(): String = "$userServiceUrl/v$customApiUserVersion/users/current"
}
