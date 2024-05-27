/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

@Configuration
class GraphQlTestConfiguration {

  @Bean
  fun graphQlClient(webApplicationContext: WebApplicationContext): WebTestClient =
      MockMvcWebTestClient.bindToApplicationContext(webApplicationContext)
          .configureClient()
          .baseUrl("/graphql")
          .build()

  @Bean
  fun graphQlTester(graphQlClient: WebTestClient): HttpGraphQlTester =
      HttpGraphQlTester.create(graphQlClient)
}
