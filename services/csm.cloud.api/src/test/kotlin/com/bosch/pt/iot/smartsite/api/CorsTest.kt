/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cloud.gateway.config.GlobalCorsProperties
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.HOST
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpHeaders.ORIGIN
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.cors.CorsConfiguration

@SpringBootTest(classes = [ApiApplication::class], webEnvironment = RANDOM_PORT)
@ActiveProfiles("test", "keycloak1")
class CorsTest {

  @Autowired lateinit var globalCorsProperties: GlobalCorsProperties

  @Value("\${web.ui.url:}") private lateinit var webUiUrl: String

  @Value("\${admin.web.ui.url:}") private lateinit var adminWebUiUrl: String

  @Test
  fun optionsEnabled() {
    assertThat(globalCors.allowedMethods)
        .describedAs("Options need to be explicitly activated.")
        .contains("OPTIONS")
  }

  @Test
  fun allowedOrigins() {
    assertThat(globalCors.allowedOrigins)
        .describedAs("Options need to be explicitly activated.")
        .containsExactlyInAnyOrder(webUiUrl, adminWebUiUrl)
  }

  @Test
  fun disallowRandomHeaderCheck() {
    assertThat(globalCors.checkHeaders(listOf("somerandomheader"))).isNullOrEmpty()
  }

  @Test
  fun allowHeaderCheck() {
    assertThat(globalCors.checkHeaders(allowedHeaders))
        .containsExactlyInAnyOrderElementsOf(allowedHeaders)
  }

  private val globalCors: CorsConfiguration
    get() {
      assertThat(globalCorsProperties.corsConfigurations).isNotEmpty
      assertThat(globalCorsProperties.corsConfigurations).containsKey(GLOBAL)
      assertThat(globalCorsProperties.corsConfigurations[GLOBAL]).isNotNull
      return globalCorsProperties.corsConfigurations[GLOBAL]!!
    }

  companion object {
    private const val GLOBAL = "/**"
    private val allowedHeaders =
        listOf(AUTHORIZATION, IF_MATCH, ACCEPT_LANGUAGE, CONTENT_TYPE, HOST, ORIGIN)
  }
}
