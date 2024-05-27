/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.application.MongoDbTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = [KafkaAutoConfiguration::class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MongoDbTest
open class AuthenticationBaseIntegrationTest : AbstractIntegrationTest() {

  @LocalServerPort protected var port: Long = -1

  protected val webClient: WebClient = WebClient.create()

  protected fun callGraphQlApi(authorizationHeader: String): Mono<String> =
      webClient
          .post()
          .uri("http://localhost:$port/graphql")
          .bodyValue("{ \"query\" : \"{ projects { id } } \"}")
          .header(AUTHORIZATION, authorizationHeader)
          .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
          .retrieve()
          .bodyToMono(String::class.java)

  protected fun callTimelineApi(authorizationHeader: String): Mono<String> =
      webClient
          .get()
          .uri("http://localhost:$port/v1/projects")
          .header(AUTHORIZATION, authorizationHeader)
          .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
          .retrieve()
          .bodyToMono(String::class.java)
}
