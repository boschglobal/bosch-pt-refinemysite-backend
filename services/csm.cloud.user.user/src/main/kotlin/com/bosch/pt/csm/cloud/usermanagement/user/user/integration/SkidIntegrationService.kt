/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.integration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono.error

@Profile("skid-deleted-user-propagation")
@Service
class SkidIntegrationService(@Autowired private val skidWebClient: WebClient) {

  /**
   * Non-blocking approach to retrieving large lists of deleted users within the time frame
   * specified. Will only consume the request when the Iterable returned is iterated.
   */
  fun findUsersDeletedInDateRangeInBatches(
      fromInclusive: LocalDateTime,
      toExclusive: LocalDateTime
  ): Iterable<List<String>> =
      skidWebClient
          .get()
          .uri {
            it.path("/auth/api/v1/UserDeletion/deleted")
                .queryParam("from", fromInclusive.formatForSkid())
                .queryParam("to", toExclusive.formatForSkid())
                .build()
          }
          .attributes(clientRegistrationId("skid")) // use "skid" oauth credentials
          .retrieve()
          .onStatus({ it.equals(BAD_REQUEST) }) { error { SkidBadRequestException() } }
          .bodyToFlux(DeletedUsersResponse::class.java)
          .map { it.id }
          .buffer(100)
          .toIterable()

  private fun LocalDateTime.formatForSkid() =
      this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))

  @JsonIgnoreProperties(ignoreUnknown = true)
  private data class DeletedUsersResponse(val id: String, val deleteDate: LocalDateTime)
}

class SkidBadRequestException : RuntimeException("Bad or missing from/to parameters")
