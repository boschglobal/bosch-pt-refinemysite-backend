/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatCreated
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro.GRAPHQL_API_READ
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro.TIMELINE_API_READ
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatTypeEnumAvro.RMSPAT1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class AuthenticationBasicIntegrationTest : AuthenticationBaseIntegrationTest() {

  val basicPat =
      "Basic OlJNU1BBVDEuNzI4MWI5NDNlMjViNDgyMmJkOTJjMzMwZWJjYzlhNTkueGJtYjd0OXBROHo4dmVBMk1sd2xPWHZCeE12RERjcGQ="

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `graphql api with basic pat`() {
    submitPat(listOf(GRAPHQL_API_READ))

    val response = callGraphQlApi(basicPat).block()

    assertThat(response).contains(getIdentifier("project").toString())
  }

  @Test
  fun `graphql api with basic pat wrong scope`() {
    submitPat(listOf(TIMELINE_API_READ))

    val response =
        callGraphQlApi(basicPat)
            .onErrorResume(WebClientResponseException.Forbidden::class.java) {
              Mono.just("Expected access denied")
            }
            .block()

    assertThat(response).isEqualTo("Expected access denied")
  }

  @Test
  fun `timeline api with basic pat`() {
    submitPat(listOf(TIMELINE_API_READ))

    val response = callTimelineApi(basicPat).block()

    assertThat(response).contains(getIdentifier("project").toString())
  }

  @Test
  fun `timeline api with basic pat wrong scope`() {
    submitPat(listOf(GRAPHQL_API_READ))

    val response =
        callTimelineApi(basicPat)
            .onErrorResume(WebClientResponseException.Forbidden::class.java) {
              Mono.just("Expected access denied")
            }
            .block()

    assertThat(response).isEqualTo("Expected access denied")
  }

  private fun submitPat(scopes: List<PatScopeEnumAvro>) {
    eventStreamGenerator.submitPatCreated(impersonatedUserReference = "csm-user") {
      it.hash = "\$2a\$10\$.alDG9I8Q2l1YR265lFxDeJ88iTecGPCRSSOA9b4HHKFfPk1k/zcC"
      it.aggregateIdentifierBuilder.identifier = "7281b943-e25b-4822-bd92-c330ebcc9a59"
      it.scopes = scopes
      it.type = RMSPAT1
    }
  }
}
